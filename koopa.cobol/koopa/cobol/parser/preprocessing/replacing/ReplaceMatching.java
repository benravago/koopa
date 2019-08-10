package koopa.cobol.parser.preprocessing.replacing;

import java.util.LinkedList;

import java.util.logging.Logger;
import static java.util.logging.Level.*;

import koopa.core.data.Data;
import koopa.core.data.Token;
import koopa.core.sources.Source;

public class ReplaceMatching extends ReplacingPhrase {

    private static final Logger LOGGER = Logger.getLogger("source.cobol.replacing.matching");

    public ReplaceMatching(ReplacingPhraseOperand replacing, ReplacingPhraseOperand by) {
        super(replacing, by);
    }

    @Override
    public boolean appliedTo(Source library, LinkedList<Data> newTokens) {
        var matchOccurred = true;
        var seenWhileMatching = new LinkedList<Token>();
        if (LOGGER.isLoggable(FINER)) {
            LOGGER.finer("Trying " + this);
        }
        var it = replacing.getTextWords().iterator();
        while (it.hasNext()) {
            var libraryTextWord = text(nextTextWord(library, seenWhileMatching));
            if (libraryTextWord == null) {
                if (LOGGER.isLoggable(FINER)) {
                    LOGGER.finer("  <EOF>");
                }
                matchOccurred = false;
                break;
            }
            var textWord = it.next();
            if (LOGGER.isLoggable(FINER)) {
                LOGGER.finer("  TESTING " + textWord);
                LOGGER.finer("    AGAINST " + libraryTextWord);
            }
            var text = textWord;
            var libraryText = libraryTextWord;
            if (!text.equalsIgnoreCase(libraryText)) {
                matchOccurred = false;
                break;
            }
        }
        if (LOGGER.isLoggable(FINER)) {
            LOGGER.finer("  => " + (matchOccurred ? "MATCH FOUND" : "NO MATCH"));
        }
        if (matchOccurred) {
            // "When a match occurs between pseudo-text-1, text-1, word-1, or literal-3 and the library text,
            //  the corresponding pseudo-text-2, text-2, word-2, or literal-4 is placed into the resultant text."

            if (LOGGER.isLoggable(FINE)) {
                LOGGER.fine("Matched " + replacing);
                LOGGER.fine("  Replaced with " + by);
            }
            // The output should include any whitespace we skipped while matching.
            if (!seenWhileMatching.isEmpty()) {
                for (var token : seenWhileMatching) {
                    if (isNewline(token) || isConsideredSingleSpace(token)) {
                        newTokens.add(token);
                    } else {
                        break;
                    }
                }
            }
            newTokens.addAll(by.getTokens());
        } else {
            unshiftStack(library, seenWhileMatching);
        }
        return matchOccurred;
    }

    @Override
    public String toString() {
        return "REPLACING MATCHING " + replacing + " BY " + by;
    }

}
