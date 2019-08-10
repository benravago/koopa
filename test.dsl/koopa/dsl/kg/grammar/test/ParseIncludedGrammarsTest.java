package koopa.dsl.kg.grammar.test;

import java.nio.file.Path;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import koopa.core.util.Glob;
import koopa.core.util.test.TestShell;
import koopa.dsl.kg.grammar.KGGrammar;
import koopa.dsl.kg.util.KGUtil;
import koopa.stage.runtime.model.Stage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test comes in handy when tweaking {@linkplain KGGrammar},
 * as it can quickly be run from within the IDE without having to recompile everything.
 * <p>
 * In the full build you don't really need it as all grammar files will get parsed and processed anyway.
 */
class ParseIncludedGrammarsTest extends TestShell {

    static Iterator<Path> getKGFiles() {
        return Glob.walk("data/koopa/grammar","*.kg");
    }

    @Override
    protected Iterator<Entry<String,Runnable>> runnableTests() {
        return getRunners().iterator();
    }

    // Test
    static void kgShouldParse(Path source) {
        assertNotNull(source);
        var ast = KGUtil.getAST(source);
        assertNotNull(ast);
    }

    static List<Entry<String,Runnable>> getRunners() {
        var runners = new LinkedList<Entry<String,Runnable>>();
        var sources = getKGFiles();
        while (sources.hasNext()) {
            var source = sources.next();
            var name = Stage.stageName(source);
            runners.add(new AbstractMap.SimpleEntry<String,Runnable>( name, () -> kgShouldParse(source)) );
        }
        return runners;
    }

}
