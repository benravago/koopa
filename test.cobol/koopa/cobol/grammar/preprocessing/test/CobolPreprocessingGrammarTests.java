package koopa.cobol.grammar.preprocessing.test;

import java.io.StringReader;
import java.nio.file.Path;

import koopa.cobol.CobolProject;
import koopa.cobol.CobolTokens;
import koopa.cobol.projects.BasicCobolProject;
import koopa.cobol.sources.SourceFormat;
import koopa.core.grammars.Grammar;
import koopa.core.sources.Source;
import koopa.stage.runtime.GrammarTestSuite;
import koopa.stage.util.StageUtil;

class CobolPreprocessingGrammarTests extends GrammarTestSuite {

    final CobolProject project = new BasicCobolProject();

    CobolPreprocessingGrammarTests() {
        project.setDefaultFormat(SourceFormat.FREE);
    }

    @Override
    protected Iterable<Path> getStageFiles() {
        return StageUtil.stageFiles("data/koopa/cobol/grammar/preprocessing/test/");
    }

    @Override
    protected Grammar getGrammar() {
        return project.getGrammar();
    }

    @Override
    protected Source getSourceForSample(String sample, Grammar grammar) {
        var reader = new StringReader(sample);
        return CobolTokens.getNewSource(reader, project);
    }

}
