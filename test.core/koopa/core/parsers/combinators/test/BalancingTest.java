package koopa.core.parsers.combinators.test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import koopa.core.grammars.test.GrammarTest;
import koopa.core.parsers.Parse;
import koopa.core.parsers.ParserCombinator;
import koopa.core.parsers.combinators.Balancing;
import koopa.core.parsers.combinators.NotNested;
import koopa.core.parsers.combinators.UnaryParserDecorator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// TODO Redo with a fluent grammar ?
public class BalancingTest extends GrammarTest {

    ParserCombinator openParen = G.literal("(");
    ParserCombinator closeParen = G.literal(")");
    ParserCombinator openBrace = G.literal("{");
    ParserCombinator closeBrace = G.literal("}");
    ParserCombinator openBracket = G.literal("[");
    ParserCombinator closeBracket = G.literal("]");

    ParserCombinator any = G.notAKeyword(G.any());

    @Test
    void testBalancing() {
        var state = new BalancingTracker(any);
        var balancing = G.balancing( openParen, closeParen, G.star(state) );
        shouldAccept(balancing, spaced("one ( two three ) four"));
        var expected = Arrays.asList(new Boolean[] { true, false, false, false, false, true });
        assertEquals(expected.size(), state.actual.size());
        assertEquals(expected, state.actual);
    }

    @Test
    void testBalancingAtStart() {
        var state = new BalancingTracker(any);
        var balancing = G.balancing( openParen, closeParen, G.star(state) );
        shouldAccept(balancing, spaced("( one two three ) four"));
        var expected = Arrays.asList(new Boolean[] { false, false, false, false, false, true });
        assertEquals(expected.size(), state.actual.size());
        assertEquals(expected, state.actual);
    }

    @Test
    void testMultiplePairs() {
        var a = G.literal("A");
        var outside = new NotNested(a);
        var sequence = G.sequence( any, any, outside, any, any );
        var balancing = G.balancing( openBrace, closeBrace, openBracket, closeBracket, sequence );
        shouldAccept(balancing, spaced("A A A A A"));
        shouldAccept(balancing, spaced("A ( A ) A"));
        shouldReject(balancing, spaced("A [ A ] A"));
        shouldReject(balancing, spaced("A { A } A"));
    }

    @Test
    void testNotNested() {
        var a = G.literal("A");
        var outside = new NotNested(a);
        var sequence = G.sequence( any, any, outside, any, any);
        var balancing = G.balancing( openParen, closeParen, sequence );
        shouldAccept(balancing, spaced("A A A A A"));
        shouldAccept(balancing, spaced("( ) A ( )"));
        shouldReject(balancing, spaced("A ( A ) A"));
        shouldReject(balancing, spaced("( A A A )"));
        shouldReject(balancing, spaced("( ( A ) )"));
        shouldAccept(balancing, spaced("A [ A ] A"));
        shouldAccept(balancing, spaced("A { A } A"));
    }

    @Test
    void testNested() {
        // This accepts anything, as long as it is nested.
        var nested = G.balancing( G.literal("("), G.literal(")"), G.star(G.nested(G.any())) );
        shouldAccept(nested, spaced("( ONE TWO THREE )"));
        shouldReject(nested, spaced("ONE TWO THREE"));
        // This tests that the matching of tested elements stops where we expect it.
        var nestedAndMore = G.sequence( nested, G.literal(")"), G.token("A") );
        shouldAccept(nestedAndMore, spaced("( ONE TWO THREE ) ) A"));
        shouldReject(nestedAndMore, spaced("ONE TWO THREE ) A"));
    }

    class BalancingTracker extends UnaryParserDecorator {
        List<Boolean> actual = new LinkedList<>();

        BalancingTracker(ParserCombinator parser) {
            super(parser);
        }

        @Override
        protected boolean matches(Parse parse) {
            var frame = parse.getStack().find(Balancing.Balancer.class);
            var balancing = (Balancing.Balancer) frame.getParser();
            var balanced = balancing == null || balancing.isBalanced();
            var accepts = parser.accepts(parse);
            if (accepts) {
                actual.add(balanced);
            }
            return accepts;
        }
    }

}
