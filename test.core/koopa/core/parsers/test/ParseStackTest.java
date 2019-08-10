package koopa.core.parsers.test;

import koopa.core.parsers.Parse;
import koopa.core.parsers.ParserCombinator;
import koopa.core.parsers.Stack;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParseStackTest {

    class Named extends ParserCombinator {
        String name;

        Named(String name) {
            this.name = name;
        }
        public boolean matches(Parse parse) {
            return false;
        }
        public boolean isMatching(String name) {
            return this.name.equals(name);
        }
    }

    ParserCombinator X = new Named("X");
    ParserCombinator Y = new Named("Y");
    ParserCombinator Z = new Named("Z");

    @Test
    void testBasicStackOperations() {
        var stack = new Stack();
        assertTrue(stack.isEmpty());
        assertNull(stack.peek());
        stack.push(X);
        assertFalse(stack.isEmpty());
        assertSame(X, stack.peek());
        assertSame(X, stack.pop());
        assertTrue(stack.isEmpty());
        assertNull(stack.peek());
    }

    @Test
    void testIsMatching() {
        var stack = new Stack();
        stack.push(X);
        assertTrue(stack.isMatching("X"));
        stack.push(Y);
        assertTrue(stack.isMatching("X"));
        assertTrue(stack.isMatching("Y"));
        assertTrue(stack.isMatching("Y", "X"));
        assertFalse(stack.isMatching("X", "Y"));
        stack.push(Z);
        assertTrue(stack.isMatching("X"));
        assertTrue(stack.isMatching("Y"));
        assertTrue(stack.isMatching("Z"));
        assertTrue(stack.isMatching("Z", "Y", "X"));
        assertTrue(stack.isMatching("Z", "Y"));
        assertTrue(stack.isMatching("Y", "X"));
        assertTrue(stack.isMatching("Z", "X"));
        assertFalse(stack.isMatching("X", "Y"));
        assertFalse(stack.isMatching("X", "Z"));
        assertFalse(stack.isMatching("Y", "Z"));
    }

}
