package koopa.cobol.grammar.directives.test;

import java.nio.file.Path;

import koopa.cobol.sources.SourceFormat;
import koopa.stage.util.StageUtil;

class FreeDirectivesTests extends DirectivesTests {

    @Override
    protected Iterable<Path> getStageFiles() {
        return StageUtil.stageFiles("data/koopa/cobol/grammar/directives/test/free/");
    }

    @Override
    protected SourceFormat getSourceFormat() {
        return SourceFormat.FREE;
    }

}
