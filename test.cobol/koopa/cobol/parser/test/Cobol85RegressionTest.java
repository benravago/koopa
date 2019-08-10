package koopa.cobol.parser.test;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Iterator;

import koopa.cobol.CobolFiles;
import koopa.cobol.CobolProject;
import koopa.cobol.projects.StandardCobolProject;
import koopa.cobol.parser.test.CobolParsingRegressionTest;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

class Cobol85RegressionTest extends CobolParsingRegressionTest {

    @Override
    protected Iterator<Path> getFiles() {
        var folder = Paths.get("data/testsuite/cobol85");
        return CobolFiles.cobolFiles(folder).iterator();
    }

    @Override
    protected CobolProject getConfiguredProject() {
        return new StandardCobolProject();
    }

    @BeforeAll
    static void getTargetResultsFile() {
        loadResults("data/testsuite/cobol85.csv");
    }

    @AfterAll
    static void getActualResultsFile() {
        storeResults("data/testsuite/cobol85-actuals.csv");
    }

}
