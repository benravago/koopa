package koopa.dsl.stage.grammar.test;

import java.io.StringReader;
import java.nio.file.Path;

import koopa.core.grammars.Grammar;
import koopa.core.sources.Source;
import koopa.stage.grammar.StageGrammar;
import koopa.stage.runtime.GrammarTestSuite;
import koopa.stage.source.StageTokens;
import koopa.stage.util.StageUtil;

class StageGrammarTests extends GrammarTestSuite {

    @Override
    protected Iterable<Path> getStageFiles() {
        return StageUtil.stageFiles("data/koopa/dsl/stage/grammar/test/");
    }

    @Override
    protected Grammar getGrammar() {
        return new StageGrammar();
    }

    @Override
    protected Source getSourceForSample(String sample, Grammar grammar) {
        var reader = new StringReader(sample);
        return StageTokens.getNewSource("", reader);
    }

}
