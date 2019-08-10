package koopa.dsl.kg.grammar.test;

import java.io.StringReader;
import java.nio.file.Path;

import koopa.core.grammars.Grammar;
import koopa.core.sources.Source;
import koopa.dsl.kg.grammar.KGGrammar;
import koopa.dsl.kg.source.KGTokens;
import koopa.stage.runtime.GrammarTestSuite;
import koopa.stage.util.StageUtil;

class KGGrammarTests extends GrammarTestSuite {

    @Override
    protected Iterable<Path> getStageFiles() {
        return StageUtil.stageFiles("data/koopa/dsl/kg/grammar/test/");
    }

    @Override
    protected Grammar getGrammar() {
        return new KGGrammar();
    }

    @Override
    protected Source getSourceForSample(String sample, Grammar grammar) {
        var reader = new StringReader(sample);
        return KGTokens.getNewSource("", reader);
    }

}
