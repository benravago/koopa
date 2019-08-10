package koopa.core.grammars.test;

import static koopa.core.data.tags.IslandTag.LAND;
import static koopa.core.grammars.test.TestTag.CHARACTER_STRING;
import static koopa.core.util.test.Util.token;
import static koopa.core.util.test.Util.tree;

import koopa.core.data.tags.SyntacticTag;
import koopa.core.grammars.KoopaGrammar;
import koopa.core.parsers.ParserCombinator;
import koopa.core.parsers.combinators.Opt;

import org.junit.jupiter.api.Test;

/**
 * Tries to put the basic parser combinators offered in a {@linkplain KoopaGrammar} to the test.
 */
class KoopaGrammarTest extends GrammarTest {

    static String SEPARATOR = "$SEP$";

    KoopaGrammarTest() {
        super(SEPARATOR);
    }

    @Test
    void testCanMatchASingleToken() {
        var parser = G.token("COBOL");
        var input = input("COBOL");
        shouldAccept(parser, input);
    }

    @Test
    void testTokenMustMatch() {
        var parser = G.token("COBOL");
        var input = input("PL/I");
        shouldReject(parser, input);
    }

    @Test
    void testCanSkipSeparators() {
        var parser = G.token("COBOL");
        var input = input(SEPARATOR, SEPARATOR, "COBOL");
        shouldAccept(parser, input);
    }

    @Test
    void testCanPreventSkippingSeparators() {
        var parser = G.opt(Opt.NOSKIP, G.token("COBOL"));
        shouldAccept(parser, input("COBOL"));
        shouldReject(parser, input(SEPARATOR, "COBOL"));
        shouldReject(parser, input(SEPARATOR, SEPARATOR, "COBOL"));
    }

    @Test
    void testCanStillMatchSeparatorsWhenNotSkippingThem() {
        var parser =
            G.opt(
                Opt.NOSKIP,
                G.sequence(
                    G.token(SEPARATOR),
                    G.token("COBOL") ));

        shouldReject(parser, input("COBOL"));
        shouldAccept(parser, input(SEPARATOR, "COBOL"));
        shouldReject(parser, input(SEPARATOR, SEPARATOR, "COBOL"));
    }

    @Test
    void testCanMatchAnyToken() {
        var parser = G.any();
        // Any token will do.
        shouldAccept(parser, input("COBOL"));
        shouldAccept(parser, input("PL/I"));
        // They can be preceded by separators.
        shouldAccept(parser, input(SEPARATOR, SEPARATOR, "COBOL"));
        // But there must be one though.
        shouldReject(parser, input());
    }

    @Test
    void testCanMatchASequenceOfTokens() {
        var parser = G.sequence(    G.token("COmmon"), G.token("Business"), G.token("Oriented"), G.token("Language") );
        var input = input("COmmon", "Business", "Oriented",    "Language");
        shouldAccept(parser, input);
    }

    @Test
    void testSequenceOfTokensMustBeInOrder() {
        var parser = G.sequence( G.token("COmmon"),    G.token("Business"), G.token("Oriented"), G.token("Language") );
        var input = input("Oriented", "Language", "COmmon", "Business");
        shouldReject(parser, input);
    }

    @Test
    void testCanMatchPermutationsOfTokens() {
        var parser = G.permuted( G.token("COmmon"), G.token("Business"), G.token("Oriented"), G.token("Language"));
        // Any order of tokens will do.
        shouldAccept(parser, input("COmmon", "Business", "Oriented", "Language"));
        shouldAccept(parser, input("Language", "Oriented", "COmmon", "Business"));
        // Any number of tokens will do.
        shouldAccept(parser, input("COmmon", "Language"));
        shouldAccept(parser, input("Business", "Language"));
        shouldAccept(parser, input("Language"));
        // But we need at least one match.
        shouldReject(parser, input());
    }

    @Test
    void testPermutationsMatchEachItemAtMostOnce() {
        var parser = G.permuted(G.token("COmmon"), G.token("Business"), G.token("Oriented"), G.token("Language"));
        shouldReject(parser, input("COmmon", "Business", "COmmon", "Language"));
    }

    @Test
    void testCanMatchOneOf() {
        var parser = G.choice(G.token("COmmon"), G.token("Business"), G.token("Oriented"), G.token("Language"));
        shouldAccept(parser, input("COmmon"));
        shouldAccept(parser, input("Business"));
        shouldAccept(parser, input("Oriented"));
        shouldAccept(parser, input("Language"));
        shouldReject(parser, input("PL/I"));
    }

    @Test
    void testCanMatchZeroOrMore() {
        var parser = G.star(G.token("Developers!"));
        shouldAccept(parser, input());
        shouldAccept(parser, input("Developers!"));
        shouldAccept(parser, input("Developers!", "Developers!"));
        shouldAccept(parser, input("Developers!", "Developers!", "Developers!"));
    }

    @Test
    void testCanMatchOneOrMore() {
        var parser = G.plus(G.token("Developers!"));
        shouldAccept(parser, input("Developers!"));
        shouldAccept(parser, input("Developers!", "Developers!"));
        shouldAccept(parser, input("Developers!", "Developers!", "Developers!"));
        shouldReject(parser, input());
    }

    @Test
    void testCanMatchOptional() {
        var parser = G.optional(G.token("Grace"));
        shouldAccept(parser, input("Grace"));
        shouldAccept(parser, input());
    }

    @Test
    void testCanHandleNegation() {
        var parser = G.sequence(G.token("Grace"), G.not(G.token("Kelly")), G.any());
        shouldAccept(parser, input("Grace", "Hopper"));
        shouldAccept(parser, input("Grace", "Murray"));
        shouldReject(parser, input("Grace", "Kelly"));
    }

    @Test
    void testCanSkipTokens() {
        var parser = G.sequence(G.token("COmmon"), G.skipto(G.token("Language")), G.token("Language"));
        shouldAccept(parser, input("COmmon", "Language"));
        shouldAccept(parser, input("COmmon", "Business", "Language"));
        shouldAccept(parser, input("COmmon", "Business", "Oriented", "Language"));
        shouldReject(parser, input("COmmon", "Something", "Or", "Other"));
    }

    @Test
    void testCanMatchOneTag() {
        var parser = G.sequence(G.tagged(CHARACTER_STRING),    G.any());
        shouldAccept(parser, input(CHARACTER_STRING, "COBOL"));
        shouldAccept(parser, input(CHARACTER_STRING, "PL/I"));
        shouldReject(parser, input("Java"));
    }

    @Test
    void testCanMatchMultipleTags() {
        var parser = G.sequence(G.tagged(CHARACTER_STRING),    G.tagged(LAND), G.any());
        shouldAccept(parser, input(CHARACTER_STRING, LAND, "COBOL"));
        shouldAccept(parser, input(CHARACTER_STRING, LAND, "PL/I"));
        shouldAccept(parser, input(LAND, CHARACTER_STRING, "COBOL"));
        shouldReject(parser, input(CHARACTER_STRING, "COBOL"));
        shouldReject(parser, input(LAND, "COBOL"));
        shouldReject(parser, input("Cobol"));
    }

    @Test
    void testCanMatchWithLimit() {
        var unlimited = G.sequence(G.skipto(G.token("COBOL")), G.token("COBOL"));
        var limited = G.limited(unlimited, G.token("."));
        shouldAccept(unlimited, input("COBOL"));
        shouldAccept(limited, input("COBOL"));
        shouldAccept(unlimited, input("FLOWMATIC", "COBOL"));
        shouldAccept(limited, input("FLOWMATIC", "COBOL"));
        shouldAccept(unlimited, input("PL/I", "FLOWMATIC", "COBOL"));
        shouldAccept(limited, input("PL/I", "FLOWMATIC", "COBOL"));
        shouldAccept(unlimited, input("PL/I", "FLOWMATIC", ".", "COBOL"));
        shouldReject(limited, input("PL/I", "FLOWMATIC", ".", "COBOL"));
    }

    @Test
    void testCanMatchUpTo() {
        // (--> COBOL) COBOL
        var base = G.sequence(G.skipto(G.token("COBOL")), G.token("COBOL"));
        // (%match base %upto .) .
        var upTo = G.sequence(G.upTo(base, G.token(".")), G.token("."));
        shouldAccept(upTo, input("COBOL", "."));
        shouldAccept(upTo, input("FLOWMATIC", "COBOL", "."));
        shouldAccept(upTo, input("PL/I", "FLOWMATIC", "COBOL", "."));
        shouldReject(upTo, input("FLOWMATIC", "."));
        shouldReject(upTo, input("COBOL"));
        shouldReject(upTo, input("COBOL", "FLOWMATIC", "."));
        shouldReject(upTo, input("FLOWMATIC", "COBOL", "PL/I", "."));
    }

    @Test
    void testCanMatchWithDispatch() {
        var parser = G.dispatched(
            new String[] { "ADD", "SUBTRACT", "MULTIPLY", "DIVIDE" },
            new ParserCombinator[] {
                G.sequence(G.token("ADD"), G.token("ONE")),
                G.sequence(G.token("SUBTRACT"), G.token("TWO")),
                G.sequence(G.token("MULTIPLY"), G.token("THIS")),
                G.sequence(G.token("DIVIDE"), G.token("THAT")), });
        shouldAccept(parser, input("ADD", "ONE"));
        shouldAccept(parser, input("SUBTRACT", "TWO"));
        shouldAccept(parser, input("multiply", "this"));
        shouldAccept(parser, input("divide", "that"));
    }

    @Test
    void testCanMatchNumbers() {
        var parser = G.number("11");
        shouldAccept(parser, input(SyntacticTag.NUMBER, "11"));
        shouldReject(parser, input(SyntacticTag.NUMBER, "22"));
        shouldReject(parser, input("11"));
    }

    @Test
    void testCanMatchNotEmpty() {
        var parser = G.sequence(
            G.optional(G.token("COmmon")),
            G.optional(G.token("Business")),
            G.optional(G.token("Oriented")),
            G.optional(G.token("Language")) );

        shouldAccept(parser, input("COmmon"));
        shouldAccept(parser, input("Business"));
        shouldAccept(parser, input("Oriented"));
        shouldAccept(parser, input("Language"));
        shouldAccept(parser, input());
        shouldReject(parser, input("PL/I"));

        var notEmpty = G.notEmpty(parser);

        shouldAccept(notEmpty, input("COmmon"));
        shouldAccept(notEmpty, input("Business"));
        shouldAccept(notEmpty, input("Oriented"));
        shouldAccept(notEmpty, input("Language"));
        shouldReject(notEmpty, input());
        shouldReject(notEmpty, input("PL/I"));
    }

    @Test
    void testCanMatchRange() {
        var cobol = input("COmmon", "Business", "Oriented",    "Language");
        var any = G.any();
        shouldAccept(G.sequence(any, any, any, any), cobol);

        // Test exact ranges.
        shouldAccept(G.sequence(
            G.ranged(1, 6), any,
            G.ranged(7, 14), any,
            G.ranged(15, 22), any,
            G.ranged(23, 30), any
        ), cobol);

        // Test loose ranges.
        shouldAccept(G.sequence(
            G.ranged(0, 8), any, // COmmon
            G.ranged(6, 15), any, // Business
            G.ranged(14, 23), any, // Oriented
            G.ranged(22, 31), any // Language
        ), cobol);

        // Test common range.
        var fullRange = G.ranged(1, 30);
        shouldAccept(G.sequence( //
            fullRange, any, // COmmon
            fullRange, any, // Business
            fullRange, any, // Oriented
            fullRange, any // Language
        ), cobol);

        // Test beginnings only.
        shouldAccept(G.sequence(
            G.ranged(1, -1), any, // COmmon
            G.ranged(7, -1), any, // Business
            G.ranged(15, -1), any, // Oriented
            G.ranged(23, -1), any // Language
        ), cobol);

        // Test endings only.
        shouldAccept(G.sequence( //
            G.ranged(-1, 6), any, // COmmon
            G.ranged(-1, 14), any, // Business
            G.ranged(-1, 22), any, // Oriented
            G.ranged(-1, 30), any // Language
        ), cobol);

        // Test failures...
        shouldReject(G.sequence(G.ranged(2, 6), any, any, any, any), cobol);
        shouldReject(G.sequence(G.ranged(1, 5), any, any, any, any), cobol);
        shouldReject(G.sequence(any, G.ranged(8, 14), any, any, any), cobol);
        shouldReject(G.sequence(any, G.ranged(7, 13), any, any, any), cobol);
        shouldReject(G.sequence(any, any, G.ranged(16, 22), any, any), cobol);
        shouldReject(G.sequence(any, any, G.ranged(15, 21), any, any), cobol);
        shouldReject(G.sequence(any, any, any, G.ranged(24, 30), any), cobol);
        shouldReject(G.sequence(any, any, any, G.ranged(23, 29), any), cobol);
    }

    @Test
    void testCanMatchTree() {
        var parser = G.tree("person");
        shouldAccept(parser, input(tree("person")));
        shouldAccept(parser, input(tree("any other namespace:person")));
        shouldAccept(parser, input(tree("person", token("Grace"), token("Murray"), token("Hopper"))));
        shouldReject(parser, input(tree("object")));
    }

    @Test
    void testCanMatchTreeInNamespace() {
        var parser = G.tree("model", "person");
        shouldAccept(parser, input(tree("model:person")));
        shouldAccept(parser, input(tree("model:person", token("Grace"), token("Murray"), token("Hopper"))));
        shouldReject(parser, input(tree("model:object")));
        shouldReject(parser, input(tree("any other namespace:person")));
    }

    @Test
    void testCanMatchTreeContents() {
        var parser = G.tree("language",    G.sequence(G.token("COBOL")));
        shouldAccept(parser, input(tree("language", token("COBOL"))));
        // TODO Test with skipping.
        shouldReject(parser, input(tree("language", token("COBOL"), token("PL/I"))));
        shouldReject(parser, input(tree("language", token("PL/I"))));
    }

    @Test
    void testCanMatchPartialTreeContents() {
        var parser = G.tree("language",    G.sequence(G.token("COBOL"), G.skipto(G.eof())));
        shouldAccept(parser, input(tree("language", token("COBOL"))));
        shouldAccept(parser, input(tree("language", token("COBOL"), token("PL/I"))));
        shouldReject(parser, input(tree("language", token("PL/I"))));
    }

    @Test
    void testCanMatchTreeContentsInNamespace() {
        var parser = G.tree("model", "language", G.sequence(G.token("COBOL")));
        shouldAccept(parser, input(tree("model:language", token("COBOL"))));
        shouldReject(parser, input(tree("model:language", token("COBOL"), token("PL/I"))));
        shouldReject(parser, input(tree("any other namespace:language", token("COBOL"))));
        shouldReject(parser, input(tree("model:language", token("PL/I"))));
    }

    @Test
    void testCanMatchPartialTreeContentsInNamespace() {
        var parser = G.tree("model", "language", G.sequence(G.token("COBOL"), G.skipto(G.eof())));
        shouldAccept(parser, input(tree("model:language", token("COBOL"))));
        shouldAccept(parser, input(tree("model:language", token("COBOL"), token("PL/I"))));
        shouldReject(parser, input(tree("any other namespace:language", token("COBOL"))));
        shouldReject(parser, input(tree("model:language", token("PL/I"))));
    }

    @Test
    void testCanMatchComplexTree() {
        var parser = G.tree("person", G.sequence(G.tree("first_name"), G.tree("last_name")));
        shouldAccept(parser, input(tree("person", tree("first_name", "Grace"), tree("last_name", "Hopper"))));
        shouldReject(parser, input(tree("human", tree("first_name", "Grace"), tree("last_name", "Hopper"))));
        shouldReject(parser, input(tree("person", tree("first_name", "Grace"))));
        shouldReject(parser, input(tree("person", tree("last_name", "Hopper"))));
        shouldReject(parser, input(tree("person", tree("last_name", "Hopper"), tree("first_name", "Grace"))));
    }

    // TODO Nested trees.
    // TODO Recursive tree rule.
}
