package koopa.cobol.sources;

import static koopa.cobol.data.tags.CobolAreaTag.IDENTIFICATION_AREA;
import static koopa.cobol.data.tags.CobolAreaTag.INDICATOR_AREA;
import static koopa.cobol.data.tags.CobolAreaTag.SEQUENCE_NUMBER_AREA;
import static koopa.cobol.sources.SourceFormat.FIXED;
import static koopa.cobol.sources.SourceFormat.FREE;
import static koopa.cobol.sources.SourceFormat.VARIABLE;
import static koopa.core.data.tags.AreaTag.COMMENT;
import static koopa.core.data.tags.AreaTag.PROGRAM_TEXT_AREA;
import static koopa.core.data.tags.SyntacticTag.END_OF_LINE;

import java.util.LinkedList;

import java.util.logging.Logger;
import static java.util.logging.Level.*;

import koopa.cobol.data.tags.CobolAreaTag;
import koopa.core.data.Data;
import koopa.core.data.Token;
import koopa.core.data.Tokens;
import koopa.core.data.tags.AreaTag;
import koopa.core.sources.ChainingSource;
import koopa.core.sources.Source;

/**
 * This {@linkplain Source} takes individual lines, and splits them up into the different program areas
 * (cfr. {@linkplain AreaTag} and {@linkplain CobolAreaTag}) as defined by the {@linkplain SourceFormat}.
 */
public class ProgramArea extends ChainingSource implements Source {

    private static final Logger LOGGER = Logger.getLogger("source.cobol.program_area");

    private final int tabLength;

    private final LinkedList<Token> pendingTokens = new LinkedList<>();

    public ProgramArea(Source source, int tabLength) {
        super(source);
        this.tabLength = tabLength;
    }

    @Override
    protected Data nextElement() {
        if (!pendingTokens.isEmpty()) {
            return pendingTokens.removeFirst();
        }
        var d = source.next();
        if (d == null || !(d instanceof Token)) {
            return d;
        }
        var t = (Token) d;
        // End of lines are marked as program text, as they count towards whitespace.
        if (t.hasTag(END_OF_LINE)) {
            return t.withTags(PROGRAM_TEXT_AREA);
        }
        // Anything which has already been marked with program area information
        // gets passed along as-is to prevent it from being split erroneously.
        // This may happen when COPY statements are expanded, in which case we
        // may see parts of the line the COPY statement was on more than once.       
        if (AreaTag.isDefinedOn(t) || CobolAreaTag.isDefinedOn(t)) {
            return t;
        }
        tokenizeLine(t);
        return pendingTokens.removeFirst();
    }

    protected void tokenizeLine(Token token) {
        var text = token.getText();
        var length = text.length();
        if (length == 0) {
            // No text. Shouldn't really happen; but better safe than sorry.
            pendingTokens.add(token);
            return;
        }
        var format = SourceFormat.forToken(token);
        if (format == FREE) {
            var c = text.charAt(0);
            if (c == 'D' || c == 'd') {
                // This is only an indicator if it gets followed by a space.
                // Otherwise it's program text.
                if (text.charAt(1) == ' ') {
                    extract(token, 0, 1, INDICATOR_AREA, format);
                    extract(token, 1, length, COMMENT, format);

                } else {
                    extract(token, PROGRAM_TEXT_AREA);
                }
            } else if (indicatesComment(c)) {
                // These are definitely indicators.
                // Note: Keep this after the debug line check,
                //  as the indicatesComment method accepts 'd' and 'D' as well.
                extract(token, 0, 1, INDICATOR_AREA, format);
                extract(token, 1, length, COMMENT, format);
            } else {
                extract(token, PROGRAM_TEXT_AREA);
            }
        } else if (format == FIXED) {
            extract(token, 0, 6, SEQUENCE_NUMBER_AREA, format);
            var indicator = extract(token, 6, 7, INDICATOR_AREA, format);
            var lineIsComment = indicator != null && indicatesComment(indicator.charAt(0));
            extract(token, 7, 72, lineIsComment ? COMMENT : PROGRAM_TEXT_AREA, format);
            extract(token, 72, length, IDENTIFICATION_AREA, format);
        } else if (format == VARIABLE) {
            extract(token, 0, 6, SEQUENCE_NUMBER_AREA, format);
            var indicator = extract(token, 6, 7, INDICATOR_AREA, format);
            var lineIsComment = indicator != null && indicatesComment(indicator.charAt(0));
            extract(token, 7, length, lineIsComment ? COMMENT : PROGRAM_TEXT_AREA, format);
        } else {
            throw new UnsupportedOperationException("Unexpected referenceFormat: " + format);
        }
    }

    private Token extract(Token token, int start, int end, Object tag, SourceFormat format) {
        var columns = columns(token);
        if (start >= columns) {
            return null;
        }
        var startIndex = characterIndexForColumn(token, start);
        var endIndex = characterIndexForColumn(token, end);
        if (endIndex <= startIndex) {
            return null;
        }
        var extracted = tokenizeArea(token, startIndex, endIndex, tag, format);
        if (LOGGER.isLoggable(FINER)) {
            LOGGER.finer(tag + ": " + extracted);
        }
        pendingTokens.add(extracted);
        return extracted;
    }

    private Token extract(Token token, Object tag) {
        var extracted = token.withTags(tag);
        if (LOGGER.isLoggable(FINER)) {
            LOGGER.finer(tag + ": " + extracted);
        }
        pendingTokens.add(extracted);
        return extracted;
    }

    public static boolean indicatesComment(int c) {
        return c == '*' || c == '/' || c == '$' || c == 'D' || c == 'd';
    }

    private Token tokenizeArea(Token token, int begin, int end, Object... tags) {
        return Tokens.subtoken(token, begin, end).withTags(tags);
    }

    private int columns(Token token) {
        var text = token.getText();
        if (tabLength == 1) {
            return text.length();
        }
        var columns = 0;
        for (var i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\t') {
                columns += tabLength;
            } else {
                columns += 1;
            }
        }
        return columns;
    }

    private int characterIndexForColumn(Token token, int column) {
        if (column <= 0) {
            return 0;
        }
        var text = token.getText();
        var columns = 0;
        for (var i = 0; i < text.length(); i++) {
            if (column <= columns) {
                return i;
            }
            if (text.charAt(i) == '\t') {
                columns += tabLength;
            } else {
                columns += 1;
            }
        }
        return text.length();
    }

}
