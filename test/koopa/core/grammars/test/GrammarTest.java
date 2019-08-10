package koopa.core.grammars.test;

import static koopa.core.data.tags.AreaTag.PROGRAM_TEXT_AREA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import koopa.core.grammars.combinators.Scoped;
import koopa.core.parsers.Parse;
import koopa.core.parsers.ParserCombinator;
import koopa.core.sources.test.HardcodedSource;
import koopa.core.targets.ListTarget;
import koopa.core.trees.KoopaTreeBuilder;
import koopa.core.trees.Tree;
import koopa.core.util.test.TreeSample;

import static org.junit.jupiter.api.Assertions.*;

public abstract class GrammarTest {

    protected TestGrammar G;

    public GrammarTest(String... separators) {
        G = new TestGrammar(separators);
    }

    public List<Object> spaced(String text) {
        var split = text.split("\\s+");
        var taggedWords = new Object[split.length * 2];
        for (var i = 0; i < split.length; i++) {
            taggedWords[2 * i + 0] = PROGRAM_TEXT_AREA;
            taggedWords[2 * i + 1] = split[i];
        }
        return input(taggedWords);
    }

    public List<Object> input(Object... taggedWords) {
        var r = new ArrayList<Object>(taggedWords.length);
        r.addAll(Arrays.asList(taggedWords));
        return r;
    }

    public void shouldReject(ParserCombinator parser, List<Object> input) {
        var rule = (Scoped) G.scoped("rule");
        rule.setParser(parser);
        var source = HardcodedSource.from(input);
        var target = new ListTarget();
        assertFalse(rule.accepts(Parse.of(source).to(target)) && source.next() == null);
    }

    public void shouldAccept(ParserCombinator parser, List<Object> input) {
        var rule = (Scoped) G.scoped("rule");
        rule.setParser(parser);
        var source = HardcodedSource.from(input);
        var target = new ListTarget();
        assertTrue(rule.accepts(Parse.of(source).to(target)));
        assertTrue(target.size() > 0);
        assertNull(source.next());
    }

    public void shouldAccept(ParserCombinator parser, TreeSample sample) {
        var rule = (Scoped) G.scoped("rule");
        rule.setParser(parser);
        var source = HardcodedSource.from(sample.getTaggedWords());
        var target = new KoopaTreeBuilder(G);
        assertTrue(rule.accepts(Parse.of(source).to(target)));
        assertNull(source.next());
        var treeForRule = target.getTree();
        assertNotNull(treeForRule);
        assertEquals(sample.getTrees().size(), treeForRule.getChildCount());
        for (var e : sample.getTrees()) {
            var actual = treeForRule.getChild(0);
            assertNotNull(treeForRule);
            treeForRule.removeChild(0);
            shouldBeEqual(e, actual);
        }
    }

    public void shouldBeEqual(Tree expected, Tree actual) {
        var expectedPath = getPath(expected);
        var actualPath = getPath(actual);
        if (expected.isNode()) {
            assertTrue(actual.isNode(), expectedPath + " == " + actualPath);
            assertEquals(expected.getName(), actual.getName(), expectedPath + " == " + actualPath);
            for (var i = 0; i < expected.getChildCount(); i++) {
                shouldBeEqual(expected.getChild(i), actual.getChild(i));
            }
            assertEquals(expected.getChildCount(), actual.getChildCount(), expectedPath + ": child count");
        } else {
            assertTrue(actual.isToken(), expectedPath + " == " + actualPath);
            assertEquals(expected.getText(), actual.getText());
        }
    }

    String getPath(Tree tree) {
        if (tree == null) {
            return "";
        }
        if (tree.isNode()) {
            return getPath(tree.getParent()) + "/" + tree.getName() + "[" + tree.getChildIndex() + "]";
        } else {
            return getPath(tree.getParent()) + "/text(" + tree.getText() + ")" + "[" + tree.getChildIndex() + "]";
        }
    }

}
