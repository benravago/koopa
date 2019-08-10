package koopa.cobol.parser.preprocessing.replacing;

import static koopa.cobol.parser.preprocessing.replacing.ReplacingPhraseOperand.Type.PSEUDO;

import java.util.LinkedList;

import java.util.logging.Logger;
import static java.util.logging.Level.*;

import koopa.core.data.Data;
import koopa.core.data.Token;
import koopa.core.data.Tokens;
import koopa.core.data.tags.AreaTag;
import koopa.core.sources.Source;

public class ReplaceLeading extends ReplacingPhrase {

    private static final Logger LOGGER = Logger.getLogger("source.cobol.replacing.leading");

    private final String pattern;
    private final int patternLength;
    private final LinkedList<Token> replacement;

    public ReplaceLeading(ReplacingPhraseOperand replacing, ReplacingPhraseOperand by) {
        super(replacing, by);
        var replacingWords = replacing.getTextWords();
        var byWords = by.getTextWords();
        assert (replacing.getType() == PSEUDO && replacingWords.size() == 1);
        assert (by.getType() == PSEUDO && byWords.size() <= 1);
        var leading = replacingWords.get(0);
        pattern = leading.toUpperCase();
        patternLength = pattern.length();
        if (byWords.isEmpty()) {
            replacement = null;
        } else {
            replacement = new LinkedList<>();
            replacement.addAll(by.getTokens());
            replacement.add(null);
        }
    }

    @Override
    public boolean appliedTo(Source library, LinkedList<Data> newTokens) {
        var seen = new LinkedList<Token>();
        var next = nextTextWord(library, seen);
        if (LOGGER.isLoggable(FINER)) {
            LOGGER.finer("Trying " + this);
            LOGGER.finer("  On " + next);
        }
        if (next != null) {
            var text = text(next).toUpperCase();
            if (text.startsWith(pattern)) {
                if (LOGGER.isLoggable(FINER)) {
                    LOGGER.finer("  We have a match.");
                }
                var tail = Tokens.subtoken(Tokens.join(next), patternLength);
                if (replacement == null) {
                    if (LOGGER.isLoggable(FINER)) {
                        LOGGER.finer("  Replacing with: " + tail);
                    }
                    newTokens.add(tail);
                    return true;
                } else {
                    replacement.removeLast();
                    replacement.addLast(tail);
                    var newToken = Tokens.join(replacement, AreaTag.PROGRAM_TEXT_AREA);
                    if (LOGGER.isLoggable(FINER)) {
                        LOGGER.finer("  Replacing with: " + newToken);
                    }
                    newTokens.add(newToken);
                    return true;
                }
            }
        }
        // library.unshift(next);
        unshiftStack(library, seen);
        return false;
    }

    @Override
    public String toString() {
        return "REPLACING LEADING " + replacing + " BY " + by;
    }

}
