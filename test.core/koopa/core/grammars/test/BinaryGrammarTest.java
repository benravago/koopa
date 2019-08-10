package koopa.core.grammars.test;

import koopa.core.grammars.combinators.Scoped;
import koopa.core.parsers.ParserCombinator;
import koopa.core.util.test.TreeSample;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// TODO Redo with a fluent grammar ?
// TODO AST tests for every input.
class BinaryGrammarTest extends GrammarTest {

    static String FOX = "The quick brown fox jumped over the lazy dog . ";
    static String LIPSUM = "Lorem ipsum dolor sit amet , ";

    static ParserCombinator and = null;
    static ParserCombinator or = null;

    static Scoped cond = null;
    static Scoped disj = null;
    static Scoped conj = null;
    static Scoped atom = null;

    static Scoped nested = null;
    static Scoped unknown = null;

    @BeforeAll
    static void setup() {

        var G = new TestGrammar();

        cond = (Scoped) G.scoped("cond");
        disj = (Scoped) G.scoped("disj");
        conj = (Scoped) G.scoped("conj");
        atom = (Scoped) G.scoped("atom");
        nested = (Scoped) G.scoped("nested");
        unknown = (Scoped) G.scoped("unknown");

        var openParen = G.literal("(");
        var closeParen = G.literal(")");

        and = G.notNested(G.token("AND"));
        or = G.notNested(G.token("OR"));

        // TODO G.eoi(), instead of G.eof() ?
        unknown.setParser(G.skipto(G.eof()));

        nested.setParser(G.sequence(
            openParen,
            G.balancing(
                openParen,
                closeParen,
                G.upTo(
                    cond,
                    G.notNested(closeParen) )),
            closeParen ));

        atom.setParser(G.choice(nested, unknown));

        conj.setParser(G.sequence(
            G.upTo(atom, and),
            G.star(G.sequence(
                and,
                G.upTo(atom, and) ))));

        disj.setParser(G.sequence(
            G.upTo(conj, or),
            G.star(G.sequence(
                or,
                G.upTo(conj, or) ))));

        cond.setParser(G.balancing(
            openParen,
            closeParen,
            disj ));
    }

    @Test
    void testUnknown() {
        // 'unknown' eats everything to the end of the file/input.
        shouldAccept(unknown,
            "+ unknown",
            "  > " + FOX
        );
    }

    @Test
    void testUnknownUpTo() {
        // We can limit what 'unknown' sees. Here it will consume until the next 'AND'.
        var upToAnd = G.upTo(unknown, and);

        // '%match unkown %upto and' should still consume to the end of the input as is though.
        shouldAccept(upToAnd,
            "+ unknown",
            "  > " + FOX
        );

        // But when we expect that 'AND', it should still get matched correctly.
        var upToAndMore = G.sequence(
            upToAnd,
            and,
            G.token("more") );

        // This fails because there is no 'AND' following FOX.
        shouldReject(upToAndMore, spaced(FOX));

        // This fails because there is no 'more' after 'AND'.
        shouldReject(upToAndMore, spaced(FOX + " AND"));

        shouldAccept(upToAndMore,
            "+ unknown",
            "  > " + FOX,
            "> AND more"
        );
        shouldAccept(upToAndMore,
            "+ unknown",
            "  > " + LIPSUM,
            "> AND more"
        );
    }

    @Test
    void testUnknownUpToNotNested() {
        // We can limit what 'unknown' sees.
        // Here it will consume until the next 'AND'.
        // Note that that 'AND' must be a "not nested" 'AND'.
        // An 'AND' which is nested does not count towards the limit.
        var unknownUpToAnd = G.upTo(unknown, and);

        // So let's test that.
        // We will be balancing parentheses while scanning for the "not nested" 'AND'.
        var balanced =
            G.balancing(
                G.literal("("),
                G.literal(")"),
                G.sequence(
                    unknownUpToAnd,
                    and,
                    G.token("more") ));

        // This old example should still work.
        shouldAccept(balanced, spaced(FOX + " AND more"));
        // This one shouldn't, as the wrong thing is following the 'AND'.
        shouldReject(balanced, spaced(FOX + " AND hare"));
        // This one should match again, as the faulty 'AND' is now nested and
        // should therefore be ignored.
        shouldAccept(balanced, spaced(FOX + " ( AND hare ) AND more"));
    }

    @Test
    void testUnknownConditions() {
        shouldAccept(cond,
            "+ cond/disj/conj/atom/unknown",
            "  > " + FOX
        );
        shouldAccept(cond,
            "+ cond/disj/conj/atom/unknown",
            "  > " + LIPSUM
        );
    }

    @Test
    void testConjunction() {
        shouldAccept(cond,
            "+ cond/disj/conj",
            "  + atom/unknown",
            "    > " + FOX,
            "  > AND",
            "  + atom/unknown",
            "    > " + LIPSUM
        );
    }

    @Test
    void testDisjunction() {
        shouldAccept(cond,
            "+ cond/disj",
            "  + conj/atom/unknown",
            "    > " + FOX,
            "  > OR",
            "  + conj/atom/unknown",
            "    > " + LIPSUM
        );
    }

    @Test
    void testCombined() {
        shouldAccept(cond,
            "+ cond/disj",
            "  + conj",
            "    + atom/unknown",
            "      > " + FOX,
            "    > AND",
            "    + atom/unknown",
            "      > " + LIPSUM,
            "  > OR",
            "  + conj",
            "    + atom/unknown",
            "      > " + LIPSUM,
            "    > AND",
            "    + atom/unknown",
            "      > " + FOX
        );
    }

    @Test
    void testDisjunctionWithNesting() {
        shouldAccept(cond,
            "+ cond/disj/conj/atom/nested",
            "  > (",
            "  + cond/disj/conj/atom/unknown",
            "    > " + FOX,
            "  > )"
        );

        shouldAccept(cond, spaced("( " + LIPSUM + " )"));
        shouldAccept(cond, spaced("( " + FOX + " AND " + LIPSUM + " )"));
        shouldAccept(cond, spaced("( " + LIPSUM + " OR " + FOX + " )"));
        shouldAccept(cond, spaced("( " + FOX + " AND " + LIPSUM + " OR "
                + LIPSUM + " AND " + FOX + " )"));

        // This uses parentheses to invert the order of precedence.
        shouldAccept(cond, spaced("( " + FOX + " OR " + LIPSUM + " ) AND ( "
                + LIPSUM + " OR " + FOX + " )"));
    }

    @Test
    void testDeepNesting() {
        shouldAccept(cond,
            "+ cond/disj/conj/atom/nested",
            "  > (",
            "  + cond/disj/conj/atom/nested",
            "    > (",
            "    + cond/disj/conj/atom/unknown",
            "      > A",
            "    > )",
            "  > )"
        );

        // Going overboard on the parentheses.
        shouldAccept(cond,
            spaced( "( ( ( ( ( ( " + FOX + " ) ) ) ) OR " + LIPSUM + " ) )" ));
    }

    @Test
    void testIssue() {
        shouldAccept(cond,
            "+ cond/disj/conj",
            "  + atom/nested",
            "    > (",
            "    + cond/disj",
            "      + conj/atom/unknown",
            "        > A",
            "      > OR",
            "      + conj/atom/unknown",
            "        > B",
            "    > )",
            "  > AND",
            "  + atom/unknown",
            "    > C"
        );
    }

    void shouldAccept(ParserCombinator parser, String... lines) {
        shouldAccept(parser,TreeSample.from(lines));
    }

}
