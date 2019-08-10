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

public class ReplaceTrailing extends ReplacingPhrase {

    private static final Logger LOGGER = Logger.getLogger("source.cobol.replacing.trailing");

    private final String pattern;
    private final int patternLength;
    private final LinkedList<Token> replacement;

    public ReplaceTrailing(ReplacingPhraseOperand replacing, ReplacingPhraseOperand by) {
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
            replacement.add(null);
            replacement.addAll(by.getTokens());
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
            if (text.endsWith(pattern)) {
                if (LOGGER.isLoggable(FINER)) {
                    LOGGER.finer("  We have a match.");
                }
                var head = Tokens.subtoken(Tokens.join(next), 0, text.length() - patternLength);
                if (replacement == null) {
                    if (LOGGER.isLoggable(FINER)) {
                        LOGGER.finer("  Replacing with: " + head);
                    }
                    newTokens.add(head);
                    return true;
                } else {
                    replacement.removeFirst();
                    replacement.addFirst(head);
                    var newToken = Tokens.join(replacement, AreaTag.PROGRAM_TEXT_AREA);
                    if (LOGGER.isLoggable(FINER)) {
                        LOGGER.finer("  Replacing with: " + newToken);
                    }
                    newTokens.add(newToken);
                    return true;
                }
            }
        }
        unshiftStack(library, seen);
        return false;
    }

    @Override
    public String toString() {
        return "REPLACING TRAILING " + replacing + " BY " + by;
    }

}
