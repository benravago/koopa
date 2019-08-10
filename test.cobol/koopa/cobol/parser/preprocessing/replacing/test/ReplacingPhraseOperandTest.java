package koopa.cobol.parser.preprocessing.replacing.test;

import static koopa.cobol.parser.preprocessing.replacing.ReplacingPhraseOperand.Type.LITERAL;
import static koopa.cobol.parser.preprocessing.replacing.ReplacingPhraseOperand.Type.PSEUDO;
import static koopa.cobol.parser.preprocessing.replacing.ReplacingPhraseOperand.Type.WORD;
import static koopa.core.data.tags.AreaTag.PROGRAM_TEXT_AREA;
import static koopa.core.data.tags.SyntacticTag.SEPARATOR;
import static koopa.core.data.tags.SyntacticTag.STRING;
import static koopa.core.data.tags.SyntacticTag.WHITESPACE;
import static koopa.core.util.test.Util.asTokens;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import koopa.cobol.parser.preprocessing.replacing.ReplaceLeading;
import koopa.cobol.parser.preprocessing.replacing.ReplaceMatching;
import koopa.cobol.parser.preprocessing.replacing.ReplaceTrailing;
import koopa.cobol.parser.preprocessing.replacing.ReplacingPhrase;
import koopa.cobol.parser.preprocessing.replacing.ReplacingPhraseOperand;
import koopa.core.data.Data;
import koopa.core.data.Position;
import koopa.core.data.Token;
import koopa.core.sources.Source;
import koopa.core.sources.TagAll;
import koopa.core.sources.test.HardcodedSource;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReplacingPhraseOperandTest {

    @Test
    void canMatchWord() {
        var phrase = matching(word("COBOL"), word("Hopper"));
        assertMatches(phrase, input("COBOL"));
        assertMatches(phrase, input("cobol"));
        assertRejects(phrase, input("PL/I"));
    }

    @Test
    void canMatchStringLiteral() {
        var phrase = matching(stringLiteral("\"COBOL\""), stringLiteral("\"Hopper\""));
        assertMatches(phrase, input("\"COBOL\""));
        assertMatches(phrase, input("\"cobol\""));
        assertRejects(phrase, input("COBOL"));
        assertRejects(phrase, input("\"PL/I\""));
    }

    @Test
    void canMatchPseudoTextSingleWord() {
        var phrase = matching(
            pseudo("=", "=", "COBOL", "=", "="),
            pseudo("=", "=", "REPLACEMENT", "TOKENS", "=", "=") );
        assertMatches(phrase, input("COBOL"));
        assertMatches(phrase, input("cobol"));
        assertRejects(phrase, input("PL/I"));
    }

    @Test
    void canMatchPseudoTextMultiWord() {
        var phrase = matching(
            pseudo("=", "=", "GRACE", SEPARATOR, WHITESPACE, " ", "HOPPER", "=", "="),
            pseudo("=", "=", "Grace", SEPARATOR, WHITESPACE, " ", "Murray", SEPARATOR, " ", "Hopper", "=", "=") );
        assertMatches(phrase, input("GRACE", SEPARATOR, WHITESPACE, " ", "HOPPER"));
        assertMatches(phrase, input("Grace", SEPARATOR, WHITESPACE, " ", "Hopper"));

        assertRejects(phrase, input("GRACE"));
        assertRejects(phrase, input("HOPPER"));
        assertRejects(phrase, input("HOPPER", SEPARATOR, WHITESPACE, " ", "GRACE"));
    }

    @Test
    void canMatchPseudoTextWithSurroundingSpaces() {
        var phrase = matching(
            pseudo("=", "=", SEPARATOR, WHITESPACE, " ", "GRACE", SEPARATOR, WHITESPACE, " ", "HOPPER", SEPARATOR, WHITESPACE, " ", "=", "="),
            pseudo("=", "=", "Grace", SEPARATOR, WHITESPACE, " ", "Murray", SEPARATOR, " ", "Hopper", "=", "=") );
        assertMatches(phrase, input("GRACE", SEPARATOR, WHITESPACE, " ", "HOPPER"));
        assertRejects(phrase, input("GRACE"));
        assertRejects(phrase, input("HOPPER"));
        assertRejects(phrase, input("HOPPER", "GRACE"));
    }

    @Test
    void canMatchPseudoTextWithMultiSpaces() {
        var phrase = matching(
            pseudo("=", "=", "GRACE", SEPARATOR, WHITESPACE, " ", SEPARATOR, WHITESPACE, " ", "HOPPER", "=", "="), //
            pseudo("=", "=", "Grace", "Murray", "Hopper", "=", "=") );
        // assertMatches(phrase, input("GRACE", "HOPPER"));
        assertMatches(phrase, input("GRACE", SEPARATOR, WHITESPACE, " ", "HOPPER"));
        assertMatches(phrase, input("GRACE", SEPARATOR, WHITESPACE, " ", SEPARATOR, WHITESPACE, " ", "HOPPER"));
    }

    // Cfr. #54 COPY-REPLACING statement not interpreted
    @Test
    void canHandleIssue54() {
        var phrase = matching(
            pseudo("=", "=", SEPARATOR, WHITESPACE, " ", ":", "L", ":", SEPARATOR, WHITESPACE, " ", "=", "="),
            word("FOO") );
        assertMatches(phrase, input(":", "L", ":"));
    }

    static List<Data> token(String text, Position start, Position end, Object... tags) {
        return Arrays.asList( new Data[]{ new Token(text,start,end,tags) });
    }

    @Test
    void canReplaceStartOfWordWithSomething() {
        var phrase = leading(
            pseudo("=", "=", "LANG", "=", "="),
            pseudo("=", "=", "COBOL", "=", "=") );
        assertMatches(phrase, input("LANG-NAME"), token("COBOL-NAME", new Position(3, 0, 3), new Position(9, 0, 9), PROGRAM_TEXT_AREA) );
        assertRejects(phrase, input("LING-NAME"));
    }

    @Test
    void canReplaceStartOfWordWithNothing() {
        var phrase = leading(
            pseudo("=", "=", "LANG", "=", "="),
            pseudo("=", "=", "=", "="));
        assertMatches(phrase, input("LANG-NAME"), token("-NAME", new Position(5, 0, 5), new Position(9, 0, 9), PROGRAM_TEXT_AREA) );
        assertRejects(phrase, input("LING-NAME"));
    }

    @Test
    void canReplaceEndOfWordWithSomething() {
        var phrase = trailing(
            pseudo("=", "=", "LANG", "=", "="),
            pseudo("=", "=", "COBOL", "=", "="));
        assertMatches(phrase, input("NAME-LANG"), token("NAME-COBOL", new Position(1, 0, 1), new Position(7, 0, 7), PROGRAM_TEXT_AREA) );
        assertRejects(phrase, input("NAME-LING"));
    }

    @Test
    void canReplaceEndOfWordWithNothing() {
        var phrase = trailing(
            pseudo("=", "=", "LANG", "=", "="),
            pseudo("=", "=", "=", "="));
        assertMatches(phrase, input("NAME-LANG"), token("NAME-", new Position(1, 0, 1),    new Position(5, 0, 5), PROGRAM_TEXT_AREA) );
        assertRejects(phrase, input("NAME-LING"));
    }

    static void assertMatches(ReplacingPhrase phrase, Source library) {
        assertMatches(phrase, library, new LinkedList<Data>(phrase.getBy().getTokens()));
    }

    static void assertMatches(ReplacingPhrase phrase, Source library, List<Data> expected) {
        var result = new LinkedList<Data>();
        assertTrue(phrase.appliedTo(library, result));
        var next = library.next();
        assertNull(next, "Not null: " + next);
        assertEquals(expected.size(), result.size());
        for (var i = 0; i < expected.size(); i++) {
            var a = expected.get(i);
            var b = result.get(i);
            assertEquals(a.getClass(), b.getClass());
            if (a instanceof Token) {
                var at = (Token) a;
                var bt = (Token) b;
                assertEquals(at.getText(), bt.getText());
                assertEquals(at.getStart(), bt.getStart());
                assertEquals(at.getEnd(), bt.getEnd());
                assertEquals(at.getTags(), bt.getTags());
            }
        }
    }

    static void assertRejects(ReplacingPhrase phrase, Source library) {
        var firstToken = library.next();
        if (firstToken != null) {
            library.unshift(firstToken);
        }
        assertFalse(phrase.appliedTo(library, new LinkedList<Data>()));
        assertSame(firstToken, library.next());
    }

    static ReplacingPhraseOperand word(String word) {
        return new ReplacingPhraseOperand(WORD, asTokens(word));
    }

    static ReplacingPhraseOperand stringLiteral(String literal) {
        return new ReplacingPhraseOperand(LITERAL, asTokens(STRING, literal));
    }

    static ReplacingPhraseOperand pseudo(Object... tagsAndTokens) {
        return new ReplacingPhraseOperand(PSEUDO, asTokens(tagsAndTokens));
    }

    static Source input(Object... tagsAndTokens) {
        return new TagAll(HardcodedSource.from(tagsAndTokens), PROGRAM_TEXT_AREA);
    }

    static ReplaceMatching matching(ReplacingPhraseOperand replacing, ReplacingPhraseOperand by) {
        return new ReplaceMatching(replacing, by);
    }

    static ReplaceLeading leading(ReplacingPhraseOperand replacing,    ReplacingPhraseOperand by) {
        return new ReplaceLeading(replacing, by);
    }

    static ReplaceTrailing trailing(ReplacingPhraseOperand replacing, ReplacingPhraseOperand by) {
        return new ReplaceTrailing(replacing, by);
    }

}
