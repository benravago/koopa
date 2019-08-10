package koopa.core.data.test;

import koopa.core.data.Position;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PositionTest {

    static String resourceName = "koopa.core.data.test.PositionTest";

    @Test
    void testPositionWithResourceName() {
        var p = new Position(resourceName, 11, 2, 7);
        assertEquals(resourceName, p.getResourceName());
        assertEquals(11, p.getPositionInFile());
        assertEquals(2, p.getLinenumber());
        assertEquals(7, p.getPositionInLine());
    }

    @Test
    void testPositionWithoutResourceName() {
        var p = new Position(11, 2, 7);
        assertEquals(null, p.getResourceName());
        assertEquals(11, p.getPositionInFile());
        assertEquals(2, p.getLinenumber());
        assertEquals(7, p.getPositionInLine());
    }

    @Test
    void testOffsetPositionWithResourceName() {
        var p = new Position(resourceName, 11, 2, 7).offsetBy(17);
        assertEquals(resourceName, p.getResourceName());
        assertEquals(28, p.getPositionInFile());
        assertEquals(2, p.getLinenumber());
        assertEquals(24, p.getPositionInLine());
    }

    @Test
    void testOffsetPositionWithoutResourceName() {
        var p = new Position(11, 2, 7).offsetBy(17);
        assertEquals(null, p.getResourceName());
        assertEquals(28, p.getPositionInFile());
        assertEquals(2, p.getLinenumber());
        assertEquals(24, p.getPositionInLine());
    }

}
