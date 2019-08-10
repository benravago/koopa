package koopa.core.streams.test;

import koopa.core.sources.test.HardcodedSource;
import koopa.core.streams.BaseStream;
import koopa.core.targets.ListTarget;

import org.junit.jupiter.api.Test;

/**
 * Tests the core operations which make up a {@linkplain BaseStream}.
 */
class BaseStreamTest extends ParseStreamTest {

    static Object[] WORDS = "The quick brown fox jumped over the lazy dog".split("\\s+");

    @Test
    void testCanStream() {
        var source = HardcodedSource.from(WORDS);
        var target = new ListTarget();
        var stream = new BaseStream(source, target);
        assertCanStream(stream, WORDS, target);
    }

    @Test
    void testCanRewind() {
        var source = HardcodedSource.from(WORDS);
        var target = new ListTarget();
        var stream = new BaseStream(source, target);
        assertCanRewind(stream, WORDS, target);
    }

    @Test
    void testCanBookmark() {
        var source = HardcodedSource.from(WORDS);
        var target = new ListTarget();
        var stream = new BaseStream(source, target);
        assertCanBookmark(stream, WORDS, target);
    }

}
