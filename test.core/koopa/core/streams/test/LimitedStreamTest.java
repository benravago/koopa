package koopa.core.streams.test;

import koopa.core.data.Token;
import koopa.core.parsers.Parse;
import koopa.core.parsers.ParserCombinator;
import koopa.core.sources.test.HardcodedSource;
import koopa.core.streams.BaseStream;
import koopa.core.targets.ListTarget;

import org.junit.jupiter.api.Test;

/**
 * Tests the core operations which make up a {@linkplain BaseStream}.
 */
class LimitedStreamTest extends ParseStreamTest {

    // This is so I can set up a root parser on the parse stack.
    // Without it the LimitedStream would commit the stream too soon.
    class Dummy extends ParserCombinator {
        @Override
        public boolean matches(Parse parse) {
            return false;
        }
    }

    class NthWord extends ParserCombinator {
        int n;

        NthWord(int n) {
            this.n = n;
        }

        @Override
        public boolean matches(Parse parse) {
            var d = parse.getStream().forward();
            return d != null && d instanceof Token && WORDS[n].equals(((Token) d).getText());
        }
    }

    static Object[] WORDS = "The quick brown fox jumped over the lazy dog".split("\\s+");

    @Test
    void testCanStream() {
        for (var n = 0; n < WORDS.length; n++) {
            var source = HardcodedSource.from(WORDS);
            var target = new ListTarget();
            var parse = Parse.of(source).to(target);
            var limitedStream = parse.getFlow().getLimitedStream();
            var limiter = new NthWord(n);
            limitedStream.addLimiter(limiter);
            parse.getStack().push(new Dummy());
            assertCanStream(limitedStream, WORDS, n, target);
            limitedStream.removeLimiter(limiter);
        }
    }

    @Test
    public void testCanRewind() {
        for (var n = 0; n < WORDS.length; n++) {
            var source = HardcodedSource.from(WORDS);
            var target = new ListTarget();
            var parse = Parse.of(source).to(target);
            var limitedStream = parse.getFlow().getLimitedStream();
            var limiter = new NthWord(n);
            limitedStream.addLimiter(limiter);
            parse.getStack().push(new Dummy());
            assertCanRewind(limitedStream, WORDS, n, target);
            limitedStream.removeLimiter(limiter);
        }
    }

    @Test
    public void testCanBookmark() {
        for (var n = 0; n < WORDS.length; n++) {
            var source = HardcodedSource.from(WORDS);
            var target = new ListTarget();
            var parse = Parse.of(source).to(target);
            var limitedStream = parse.getFlow().getLimitedStream();
            var limiter = new NthWord(n);
            limitedStream.addLimiter(limiter);
            parse.getStack().push(new Dummy());
            assertCanBookmark(limitedStream, WORDS, n, target);
            limitedStream.removeLimiter(limiter);
        }
    }

}
