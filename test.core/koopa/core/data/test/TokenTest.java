package koopa.core.data.test;

import static koopa.core.util.test.Util.asListOfRanges;

import koopa.core.data.Position;
import koopa.core.data.Replaced;
import koopa.core.data.Token;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests basic {@linkplain Token} functionality.
 */
public class TokenTest {

    static String TEXT = "One accurate measurement is worth a thousand expert opinions.";
    static int LENGTH = TEXT.length();
    static Position START = new Position(0, 0, 0);
    static Position STOP = START.offsetBy(LENGTH - 1);

    @Test
    void testCanCreateAToken() {
        var t = new Token(TEXT, START, STOP);
        assertEquals(LENGTH, t.getLength());
    }

    @Test
    void testCanAddTags() {
        var t = new Token(TEXT, START, STOP, "Quote", "Grace Hopper");
        var mod = t.withTags("Cobol");
        assertTrue(mod.hasTag("Quote"));
        assertTrue(mod.hasTag("Grace Hopper"));
        assertTrue(mod.hasTag("Cobol"));
        assertEquals(asListOfRanges(0, LENGTH - 1), t.getRanges());
        assertEquals(TEXT.length(), t.getRanges().get(0).getLength());
    }

    @Test
    void testCanRemoveTags() {
        var t = new Token(TEXT, START, STOP, "Quote", "Grace Hopper");
        var mod = t.withoutTags("Quote");
        assertFalse(mod.hasTag("Quote"));
        assertTrue(mod.hasTag("Grace Hopper"));
        assertEquals(asListOfRanges(0, LENGTH - 1), t.getRanges());
        assertEquals(TEXT.length(), t.getRanges().get(0).getLength());
    }

    @Test
    void testCanAddAndRemoveTags() {
        var t = new Token(TEXT, START, STOP, "Quote", "Grace Hopper");
        var mod = t.replacingTag("Quote", "Cobol");
        assertFalse(mod.hasTag("Quote"));
        assertTrue(mod.hasTag("Grace Hopper"));
        assertTrue(mod.hasTag("Cobol"));
        assertEquals(asListOfRanges(0, LENGTH - 1), t.getRanges());
        assertEquals(TEXT.length(), t.getRanges().get(0).getLength());
    }

    @Test
    void testNotReallyChangingATokenReturnsItself() {
        var t = new Token(TEXT, START, STOP, "Quote", "Grace Hopper");
        assertSame(t, t.withTags());
        assertSame(t, t.withoutTags());
    }

    @Test
    void canBeReplacingAToken() {
        var r = new Replaced(START, STOP, null);
        var text = "It is often easier to ask for forgiveness than to ask for permission.";
        var start = Position.ZERO;
        var end = start.offsetBy(text.length());
        var t = new Token(text, start, end);
        assertNull(t.getReplaced());
        assertSame(t, t.asReplacing(null));
        var s = t.asReplacing(r);
        assertEquals(text, s.getText());
        assertEquals(start, s.getStart());
        assertEquals(end, s.getEnd());
        assertSame(r, s.getReplaced());
    }

}
