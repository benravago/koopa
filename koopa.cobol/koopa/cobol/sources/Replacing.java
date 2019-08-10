package koopa.cobol.sources;

import java.util.LinkedList;
import java.util.List;

import java.util.logging.Logger;
import static java.util.logging.Level.*;

import koopa.cobol.parser.preprocessing.replacing.ReplacingPhrase;
import koopa.core.data.Data;
import koopa.core.data.Token;
import koopa.core.sources.ChainingSource;
import koopa.core.sources.Source;

/**
 * A {@linkplain Source} which applies replacement instructions
 * it gets from its source by means of {@linkplain ReplacementData}.
 */
public class Replacing extends ChainingSource implements Source {

    private static final Logger LOGGER = Logger.getLogger("source.cobol.replacing");

    /**
     * The list of all active {@linkplain ReplacingPhrase}s.
     * It is sorted in order of precedence, with the highest precedence appearing first.
     */
    private final LinkedList<List<ReplacingPhrase>> replacements = new LinkedList<>();

    /**
     * This is a list of {@link Token}s which have been substituted in for a match.
     */
    private final LinkedList<Data> replacementTokens = new LinkedList<>();

    public Replacing(Source source) {
        super(source);
    }

    @Override
    protected Data nextElement() {
        replacing: for (;;) {
            // If there is an active replacement, we process that first before looking for more.
            if (!replacementTokens.isEmpty()) {
                return replacementTokens.removeFirst();
            }
            var data = source.next();
            if (data == null) {
                return null;
            }
            if (data instanceof ReplacementData) {
                var r = (ReplacementData) data;
                if (r.activates()) {
                    if (r.isGlobal()) {
                        if (LOGGER.isLoggable(FINE)) {
                            LOGGER.fine("Activating only " + r.getReplacements());
                        }
                        clearReplacements();
                        pushReplacements(r.getReplacements());
                    } else {
                        if (LOGGER.isLoggable(FINE)) {
                            LOGGER.fine("Additionally activating " + r.getReplacements());
                        }
                        pushReplacements(r.getReplacements());
                    }
                } else {
                    if (r.isGlobal()) {
                        if (LOGGER.isLoggable(FINE)) {
                            LOGGER.fine("Deactivating all replacements.");
                        }
                        clearReplacements();
                    } else {
                        if (LOGGER.isLoggable(FINE)) {
                            LOGGER.fine("Deactivating " + replacements.getFirst());
                        }
                        popReplacements();
                    }
                }
                continue;
            }
            // Anything that's not a Token will get passed on as is.
            // Most of that should have been covered by the logic for the balanced Start/End tags,
            // but the final End tag, for instance, won't be.
            if (!(data instanceof Token)) {
                return data;
            }
            // Now we get into the actual replacement logic, as described by the spec.
            // All of that seems to hinge around the leftmost text word,
            // which is what we're trying to identify first.
            var leftmost = (Token) data;

            // "The leftmost library text-word that is not a separator comma or
            // a separator semicolon is the first text-word used for comparison."
               // if (leftmost.hasTag(SyntacticTag.SEPARATOR)) {
            var text = leftmost.getText();
            if (",".equals(text) || ";".equals(text)) {
                return leftmost;
            }
               // }

            // Repositioning the token stream to start with the leftmost token.
            source.unshift(leftmost);

            // There may be multiple sets of replacement directives which are active,
            // thanks to the complexity of the REPLACE statement.
            // We try them all in order of precedence.
            for (var r : replacements) {
                // "Starting with the first text-word for comparison and
                //  first pseudo-text-1, text-1, word-1, literal-3,
                //  or partial-word-1 that was specified in the REPLACING phrase,
                //  the entire REPLACING phrase operand that precedes the reserved word BY
                //  is compared to an equivalent number of contiguous library text-words."
                for (var replacingPhrase : r) {
                    if (replacingPhrase.appliedTo(source, replacementTokens)) {
                        // "When a match occurs between partial-word-1 and the library text-word,
                        //  the library text-word is placed into the resultant text with the matched characters
                        //  either replaced by partial-word-2 or deleted when partial-word-2 consists of zero text-words."
                        // "The library text-word immediately following the rightmost text-word
                        //  that participated in the match is then considered as the leftmost text-word.
                        //  The comparison cycle starts again with the first pseudo-text-1, text-1, word-1,
                        //  literal-3, or partial-word-1 specified in the REPLACING phrase."
                        continue replacing;
                    }
                    // "If no match occurs, the comparison is repeated with each next successive
                    //  pseudo-text-1, text-1, word-1, literal-3, or partial-word-1, if any,
                    //  in the REPLACING phrase until either a match is found or
                    //  there is no next successive REPLACING operand."
                }
            }
            // "When all the REPLACING phrase operands have been compared and no match has occurred,
            //  the leftmost library text-word is copied into the resultant text.
            //  The next successive library text-word is then considered as the leftmost library text-word,
            //  and the comparison cycle starts again with the first pseudo-text-1, text-1, word-1,
            //  literal-3, or partial-word-1 specified in the REPLACING phrase."
            return source.next();
        }
    }

    /**
     * Adds and activates the given {@linkplain Replacement}.
     * The new one will take precedence over all others.
     */
    private void pushReplacements(List<ReplacingPhrase> r) {
        assert (r != null);
        replacements.addFirst(r);
    }

    private void clearReplacements() {
        replacements.clear();
    }

    private void popReplacements() {
        if (!replacements.isEmpty()) {
            replacements.removeFirst();
        } else if (LOGGER.isLoggable(INFO)) {
            LOGGER.info("Tried to pop empty replacements.");
        }
    }

}
