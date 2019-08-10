package koopa.core.trees.test;

import java.io.IOException;

import static koopa.core.util.test.Util.comment;
import static koopa.core.util.test.Util.text;
import static koopa.core.util.test.Util.tree;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProgramTextTest {

    @Test
    void testEmptyProgramText() throws IOException {
        var tree = tree("");
        var expected = "";
        assertEquals(expected, tree.getProgramText());
    }

    @Test
    void testSingleToken() throws IOException {
        var tree = text("COBOL");
        var expected = "COBOL";
        assertEquals(expected, tree.getProgramText());
    }

    @Test
    void testTreeOfTokens() throws IOException {
        var tree = tree(
            "test",
            text("Stop", 0, 3),
            text("bashing", 5, 11),
            text("Cobol", 13, 17) );

        var expected = "StopbashingCobol";
        assertEquals(expected, tree.getProgramText());
    }

    @Test
    void testTreeOfTokensWithComments() throws IOException {
        var tree = tree(
            "test", text("Stop", 0, 3),
            comment("-don't do it any more-"),
            text("bashing", 5, 11),
            comment("-or making jokes about-"),
            text("Cobol", 13, 17),
            comment("-seriously!-") );

        var expected = "StopbashingCobol";
        assertEquals(expected, tree.getProgramText());
    }

    @Test
    void testTreeOfConsecutiveTokens() throws IOException {
        var tree = tree(
            "test",
            text("Stop", 0, 3),
            text("bashing", 4, 10),
            text("Cobol", 11, 15));

        var expected = "StopbashingCobol";
        assertEquals(expected, tree.getProgramText());
    }

}
