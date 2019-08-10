package koopa.core.sources;

import static koopa.core.data.tags.SyntacticTag.INCOMPLETE;
import static koopa.core.data.tags.SyntacticTag.NUMBER;
import static koopa.core.data.tags.SyntacticTag.SEPARATOR;
import static koopa.core.data.tags.SyntacticTag.STRING;
import static koopa.core.data.tags.SyntacticTag.WHITESPACE;
import static koopa.core.data.tags.SyntacticTag.WORD;

import java.util.LinkedList;
import java.util.List;

import koopa.core.data.Token;
import koopa.core.data.Tokens;
import koopa.core.data.tags.SyntacticTag;

/**
 * This class knows how to split up a {@linkplain Token} into its basic elements,
 * as defined in {@linkplain SyntacticTag}.
 */
public class TokenSeparationLogic {

    public static List<Token> apply(final Token token) {
        var tokens = new LinkedList<Token>();
        var text = token.getText();
        var length = text.length();
        var position = 0;
        while (position < length) {
            var c = text.charAt(position);
            if (isWhitespace(c)) {
                position = whitespace(token, text, position, length, tokens);
            } else if (startsString(c)) {
                position = string(token, text, position, 0, length, tokens);
            } else if (isDigit(c)) {
                position = number(token, text, position, length, tokens);
            } else if (isLetter(c)) {
                position = word(token, text, position, length, tokens);
            } else {
                // Everything else...
                position = separator(token, text, position, length, tokens);
            }
        }
        return tokens;
    }

    private static boolean startsString(char c) {
        return c == '"' || c == '\'';
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t';
    }

    private static int whitespace(Token token, String text, int start, int length, List<Token> tokens) {
        int position = start + 1;
        while (position < length && isWhitespace(text.charAt(position))) {
            position += 1;
        }
        tokens.add( Tokens.subtoken(token, start, position).withTags(SEPARATOR, WHITESPACE) );
        return position;
    }

    private static int string(Token token, String text, int start, int prefixLength, int length, List<Token> tokens) {
        var quotationMark = text.charAt(start + prefixLength);
        var position = start + prefixLength + 1;
        while (position < length) {
            var c = text.charAt(position);
            if (c != quotationMark) {
                // Still in string literal.
                position += 1;
                continue;
            }
            if (position + 1 == length) {
                tokens.add( Tokens.subtoken(token, start).withTags(STRING) );
                return position + 1;
            }
            var d = text.charAt(position + 1);
            if (d == quotationMark) {
                // Escaped quotation mark.
                position += 2;
                continue;
            }
            // Completed string literal.
            // Check for floating continuation marker.
            var hasFloatingContinuationIndicator = (d == '-');
            if (hasFloatingContinuationIndicator) {
                position += 1;
            }
            if (hasFloatingContinuationIndicator) {
                tokens.add( Tokens.subtoken(token, start, position + 1).withTags(STRING, INCOMPLETE) );
            } else {
                tokens.add( Tokens.subtoken(token, start, position + 1).withTags(STRING) );
            }

            return position + 1;
        }
        // Incomplete string literal.
        tokens.add( Tokens.subtoken(token, start).withTags(STRING, INCOMPLETE) );
        return length;
    }

    private static int word(Token token, String text, int start, int length, List<Token> tokens) {
        var position = start + 1;
        while (position < length) {
            var c = text.charAt(position);
            if (!isLetter(c) && c != '-') {
                break;
            }
            position += 1;
        }
        tokens.add( Tokens.subtoken(token, start, position).withTags(WORD) );
        return position;
    }

    private static int number(Token token, String text, int start, int length, List<Token> tokens) {
        var position = start + 1;
        while (position < length) {
            var c = text.charAt(position);
            if (!isDigit(c)) {
                break;
            }
            position += 1;
        }
        tokens.add( Tokens.subtoken(token, start, position).withTags(NUMBER) );
        return position;
    }

    private static boolean isLetter(char c) {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
    }

    private static boolean isDigit(char c) {
        return '0' <= c && c <= '9';
    }

    private static int separator(Token token, String text, int start, int length, List<Token> tokens) {
        var position = start + 1;
        tokens.add( Tokens.subtoken(token, start, position).withTags(SEPARATOR) );
        return position;
    }

}
