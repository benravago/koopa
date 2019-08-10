package koopa.core.sources.test;

import java.util.List;

import static koopa.core.data.tags.AreaTag.PROGRAM_TEXT_AREA;

import koopa.core.data.Token;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestTokenizerTest {

    final String input = "A B C ^ X Y Z";

    final List<Object> objects = List.of(
        PROGRAM_TEXT_AREA, "A",
        PROGRAM_TEXT_AREA, " ",
        PROGRAM_TEXT_AREA, "B",
        PROGRAM_TEXT_AREA, " ",
        PROGRAM_TEXT_AREA, "C",
        PROGRAM_TEXT_AREA, " ",
        PROGRAM_TEXT_AREA, "^",
        PROGRAM_TEXT_AREA, " ",
        PROGRAM_TEXT_AREA, "X",
        PROGRAM_TEXT_AREA, " ",
        PROGRAM_TEXT_AREA, "Y",
        PROGRAM_TEXT_AREA, " ",
        PROGRAM_TEXT_AREA, "Z" );

    final static String[] expected =
        new String[] { "A", " ", "B", " ", "C", " ", " ", "X", " ", "Y", " ", "Z" };

    int C = input.indexOf('C');
    int X = input.indexOf('X') - 1;

    @Test
    void beforeC() {
        for (var i = 0; i < C; i++) {
            assertState("" + i, i, false);
        }
    }

    @Test
    void cUpToX() {
        for (var i = C; i < X; i++) {
            assertState(i + ": '" + expected[i] + "'", i, true);
        }
    }

    @Test
    void xAndBeyond() {
        for (var i = X; i < input.length() - 1; i++) {
            assertState("" + i, i, false);
        }
    }

    void assertState(String message, int count, boolean atMark) {
        var source = HardcodedSource.from(objects);
        var tokenizer = new TestTokenizer(source);
        for (var i = 0; i <= count; i++) {
            var d = tokenizer.next();
            assertTrue(d instanceof Token, expected[i]);
            var t = (Token) d;
            assertEquals(t.toString(), expected[i], t.getText());
        }
        if (atMark) {
            assertTrue(tokenizer.isWhereExpected(), message);
        } else {
            assertFalse(tokenizer.isWhereExpected(), message);
        }
    }

}
