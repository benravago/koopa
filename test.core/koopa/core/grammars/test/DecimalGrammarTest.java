package koopa.core.grammars.test;

import static koopa.core.grammars.test.TestTag.INTEGER_LITERAL;
import static koopa.core.grammars.test.TestTag.SIGNED;
import static koopa.core.grammars.test.TestTag.UNSIGNED;

import koopa.core.parsers.ParserCombinator;
import koopa.core.parsers.combinators.Opt;

import org.junit.jupiter.api.Test;

/**
 * This class tests a possible fix for issue #48 "Delay categorization of decimals".
 * It let's the parser define decimals based on {@linkplain TestTag#INTEGER_LITERAL} and separators.
 */
class DecimalGrammarTest extends GrammarTest {

    DecimalGrammarTest() {
        super(",");
    }

    /**
     * This basically implements:
     *
     * <pre>
     *  integer %noskip ((','|'.') unsignedInteger)
     * </pre>
     */
    ParserCombinator decimal() {
        var integer = G.sequence( G.tagged(INTEGER_LITERAL), G.any() );
        var comma = G.choice( G.token(","), G.token(".") );
        var unsignedInteger = G.sequence( G.tagged(INTEGER_LITERAL), G.tagged(UNSIGNED), G.any() );
        return G.sequence( integer, G.opt(Opt.NOSKIP, G.sequence(comma, unsignedInteger)) );
    }

    /**
     * This basically implements:
     *
     * <pre>
     *  integer+
     * </pre>
     */
    ParserCombinator list() {
        return G.plus(G.sequence(G.tagged(INTEGER_LITERAL), G.any()));
    }

    @Test
    void testCommaDecimal() {
        // 3,14
        var sample = input(UNSIGNED, INTEGER_LITERAL, "3");
        sample.addAll(input(","));
        sample.addAll(input(UNSIGNED, INTEGER_LITERAL, "14"));
        // This sample can be both a decimal and a list, so both should match.
        shouldAccept(decimal(), sample);
        shouldAccept(list(), sample);
    }

    @Test
    void testDotDecimal() {
        // 3.14
        var sample = input(UNSIGNED, INTEGER_LITERAL, "3");
        sample.addAll(input("."));
        sample.addAll(input(UNSIGNED, INTEGER_LITERAL, "14"));
        shouldAccept(decimal(), sample);
        shouldReject(list(), sample);
    }

    @Test
    void testSignedCommaDecimal() {
        // +3,14
        var sample = input(SIGNED, INTEGER_LITERAL, "+3");
        sample.addAll(input(","));
        sample.addAll(input(UNSIGNED, INTEGER_LITERAL, "14"));
        // This sample can be both a decimal and a list, so both should match.
        shouldAccept(decimal(), sample);
        shouldAccept(list(), sample);
    }

    @Test
    void testSignedDotDecimal() {
        // +3.14
        var sample = input(SIGNED, INTEGER_LITERAL, "+3");
        sample.addAll(input("."));
        sample.addAll(input(UNSIGNED, INTEGER_LITERAL, "14"));
        shouldAccept(decimal(), sample);
        shouldReject(list(), sample);
    }

    @Test
    void testSignedList() {
        // +3,+14
        var sample = input(SIGNED, INTEGER_LITERAL, "+3");
        sample.addAll(input(","));
        sample.addAll(input(SIGNED, INTEGER_LITERAL, "+14"));
        shouldReject(decimal(), sample);
        shouldAccept(list(), sample);
    }

}
