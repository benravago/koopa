package koopa.core.data.test;

import static koopa.core.util.test.Util.asListOfRanges;

import java.util.ArrayList;
import java.util.Arrays;

import koopa.core.data.Position;
import koopa.core.data.Token;
import koopa.core.data.Tokens;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the different operations in {@linkplain Tokens}.
 */
class TokensTest {

    static String TEXT = "One accurate measurement is worth a thousand expert opinions.";
    static int LENGTH = TEXT.length();

    static Position START = new Position(0, 0, 0);
    static Position STOP = START.offsetBy(LENGTH - 1);

    static Token TOKEN = new Token(TEXT, START, STOP);

    @Test
    void testFullSubtokenIsSameAsOriginal() {
        var t = new Token(TEXT, START, STOP);
        var sub = Tokens.subtoken(t, 0, LENGTH);
        assertSame(t, sub);
    }

    @Test
    void testAnyLengthSubtokens() {
        var t = new Token(TEXT, START, STOP);
        for (var l = 0; l < LENGTH; l++) {
            for (var i = 0; i < LENGTH - l; i++) {
                var sub = Tokens.subtoken(t, i, i + l);
                assertEquals(TEXT.substring(i, i + l), sub.getText());
                assertEquals(l, sub.getLength());
            }
        }
    }

    @Test
    void testFullSubtokenToEndIsSameAsOriginal() {
        var t = new Token(TEXT, START, STOP);
        var sub = Tokens.subtoken(t, 0);
        assertSame(t, sub);
    }

    @Test
    void testCanHasSubtokenToEnd() {
        var t = new Token(TEXT, START, STOP);
        var from = LENGTH / 4;
        var sub = Tokens.subtoken(t, from);
        assertEquals(TEXT.substring(from), sub.getText());
        assertEquals(LENGTH - from, sub.getLength());
        assertEquals(asListOfRanges(from, LENGTH - 1), sub.getRanges());
    }

    @Test
    void testCanHasSubtoken() {
        var t = new Token(TEXT, START, STOP);
        var from = LENGTH / 4;
        var to = from + LENGTH / 2;
        var sub = Tokens.subtoken(t, from, to);
        assertEquals(TEXT.substring(from, to), sub.getText());
        assertEquals(to - from, sub.getLength());
        assertEquals(asListOfRanges(from, to - 1), sub.getRanges());
    }

    @Test
    void testSubtokensInheritTags() {
        var t = new Token(TEXT, START, STOP, "Quote", "Grace Hopper");
        var sub = Tokens.subtoken(t, LENGTH / 2);
        assertEquals(t.getTags(), sub.getTags());
    }

    Token tokenFromRanges(int... positions) {
        var parts = new ArrayList<Token>(positions.length / 2);
        for (var i = 0; i < positions.length; i += 2) {
            parts.add(Tokens.subtoken(TOKEN, positions[i], positions[i + 1]));
        }
        return Tokens.join(parts);
    }

    @Test
    void testSubtokenToEndSplitsRanges1() {
        var mid = LENGTH / 2;
        var t = tokenFromRanges(0, mid, mid, LENGTH);
        var from = mid / 2;
        var sub = Tokens.subtoken(t, from);
        var substring = TEXT.substring(from);
        assertEquals(substring, sub.getText());
        assertEquals(substring.length(), sub.getLength());
        assertEquals(asListOfRanges(from, mid - 1, mid, LENGTH - 1), sub.getRanges());
    }

    @Test
    void testSubtokenToEndSplitsRanges2() {
        var mid = LENGTH / 2;
        var t = tokenFromRanges(0, mid, mid, LENGTH);
        var from = mid + mid / 2;
        var sub = Tokens.subtoken(t, from);
        var substring = TEXT.substring(from);
        assertEquals(substring, sub.getText());
        assertEquals(substring.length(), sub.getLength());
        assertEquals(asListOfRanges(from, LENGTH - 1), sub.getRanges());
    }

    @Test
    void testSubtokenSplitsRanges() {
        var mid = LENGTH / 2;
        var t = tokenFromRanges(0, mid, mid, LENGTH);
        var from = mid / 2;
        var to = mid + mid / 2;
        var sub = Tokens.subtoken(t, from, to);
        var substring = TEXT.substring(from, to);
        assertEquals(substring, sub.getText());
        assertEquals(substring.length(), sub.getLength());
        assertEquals(asListOfRanges(from, mid - 1, mid, to - 1), sub.getRanges());
    }

    @Test
    void testCanSplitToken() {
        var t = tokenFromRanges(0, LENGTH);
        var mid = LENGTH / 2;
        var tokens = Tokens.split(t, mid);
        assertEquals(2, tokens.length);
        var first = tokens[0];
        var second = tokens[1];
        assertEquals(TEXT.substring(0, mid), first.getText());
        assertEquals(TEXT.substring(mid, LENGTH), second.getText());
        assertEquals(asListOfRanges(0, mid - 1), first.getRanges());
        assertEquals(asListOfRanges(mid, LENGTH - 1), second.getRanges());
    }

    @Test
    void testCanJoinTokens() {
        var t1 = new Token(TEXT, START, STOP, "Quote", "Grace Hopper");
        var text = "From then on, when anything went wrong with a computer, we said it had bugs in it.";
        var length = text.length();
        var start = STOP.offsetBy(1);
        var end = start.offsetBy(length - 1);
        var t2 = new Token(text, start, end, "Cobol");
        var t = Tokens.join(Arrays.asList(new Token[]{t1, t2}));
        var expected = TEXT + text;
        assertEquals(expected, t.getText());
        assertEquals(expected.length(), t.getLength());
        // The combined token does not inherit the tags from its parts.
        assertEquals(0, t.getTags().size());
        assertEquals( asListOfRanges(0, LENGTH - 1, LENGTH, LENGTH + length - 1), t.getRanges() );
    }

}
