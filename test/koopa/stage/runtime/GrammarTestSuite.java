package koopa.stage.runtime;

import java.nio.file.Path;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;

import static koopa.core.data.tags.IslandTag.WATER;

import koopa.core.data.Token;
import koopa.core.data.markers.End;
import koopa.core.data.markers.Start;
import koopa.core.grammars.Grammar;
import koopa.core.parsers.Parse;
import koopa.core.parsers.ParserCombinator;
import koopa.core.sources.Source;
import koopa.core.sources.test.TestTokenizer;
import koopa.core.targets.ListTarget;
import koopa.core.util.test.TestShell;
import koopa.stage.runtime.model.SuiteOfStages;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Base for a test suite which wants to exercise the different rules of a grammar.
 *
 * All you need to do is provide a list of stage files (via {@linkplain #getStageFiles()},
 * give it an instance of your grammar (via {@linkplain #getGrammar()},
 * and tell it how an input is to be tokenized (via {@link #getSourceForSample(String, Grammar)}).
 */
public abstract class GrammarTestSuite extends TestShell {

    @Override
    protected Iterator<Entry<String,Runnable>> runnableTests() {
        return getRunners(this).iterator();
    }

    protected abstract Iterable<Path> getStageFiles();
    protected abstract Grammar getGrammar();
    protected abstract Source getSourceForSample(String sample, Grammar grammar);

    /**
     * The actual test, based on the data from the {@linkplain GrammarTest} which will have been set.
     * <p>
     * If the test should accept ({@linkplain GrammarTest#shouldAccept()}
     * then we check that the parse was successful and reached the expected point.
     *
     * We also verify that there is no water in the output,
     * apart from that found between "unknown" start and end markers.
     *
     * If the test should reject then we check that the parse failed,
     * or that it did not reach the expected point in the input.
     */
    static void testParsing(GrammarTestSuite suite, GrammarTest test) {
        var grammar = suite.getGrammar();
        var targetName = test.getTarget();
        var target = getParser(grammar, targetName);
        assertNotNull(target);
        var source = new TestTokenizer(suite.getSourceForSample(test.getSample(), grammar));
        if (test.shouldAccept()) {
            var resultingData = new ListTarget();
            var parse = Parse.of(source).to(resultingData);
            try {
                var accepts = target.accepts(parse);
                assertTrue( accepts,
                    targetName + " should accept [" + test.getSample() + "]" );
                assertTrue( source.isWhereExpected(),
                    targetName + " should accept [" + test.getSample()
                               + "] up to the expected point. Got to "
                               /*+ parse.getFinalFrame().toTrace()*/ + "." );
            } catch (Exception e) {
                e.printStackTrace();
                fail(targetName + " should accept [" + test.getSample() + "], but threw " + e);
            }
            var inUnknown = 0;
            for (var data : resultingData) {
                if (data instanceof Start) {
                    var start = (Start) data;
                    if ("unknown".equals(start.getName())) {
                        inUnknown += 1;
                    }
                } else if (data instanceof End) {
                    var end = (End) data;
                    if ("unknown".equals(end.getName())) {
                        inUnknown -= 1;
                    }
                } else if (inUnknown == 0 && data instanceof Token) {
                    var token = (Token) data;
                    assertFalse( token.hasTag(WATER),
                        targetName + " should find no water in [" + test.getSample() + "]" );
                }
            }
        } else {
            var parse = Parse.of(source);
            assertFalse( target.accepts(parse) && source.isWhereExpected(),
                targetName + " should reject [" + test.getSample() + "]" );
        }
    }

    static ParserCombinator getParser(Grammar grammar, String ruleName) {
        try {
            var method = grammar.getClass().getMethod(ruleName);
            if (ParserCombinator.class.isAssignableFrom(method.getReturnType())) {
                return (ParserCombinator) method.invoke(grammar);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * set up the actual test runners.
     */
    static List<Entry<String,Runnable>> getRunners(GrammarTestSuite suite) {
        var runners = new LinkedList<Entry<String,Runnable>>();
        var sources = suite.getStageFiles();
        var testsuite = new SuiteOfStages(sources);
        for (var stage : testsuite.getStages()) {
            for (var target : stage.getTargets()) {
                var i = 0;
                for (var test : target.getTests()) {
                    var name = test.getStage() + ':' + test.getTarget() + ':' + (i++);
                    runners.add(new SimpleEntry<String,Runnable>( name, () -> testParsing(suite,test) ));
                }
            }
        }
        return runners;
    }

}
