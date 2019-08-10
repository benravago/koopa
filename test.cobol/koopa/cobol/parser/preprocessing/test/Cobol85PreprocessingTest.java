package koopa.cobol.parser.preprocessing.test;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Iterator;

import koopa.cobol.CobolProject;
import koopa.cobol.projects.StandardCobolProject;
import koopa.cobol.parser.test.CobolParsingRegressionTest;
import koopa.core.util.Glob;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

class Cobol85PreprocessingTest extends CobolParsingRegressionTest {

    @Override
    protected Iterator<Path> getFiles() {
        return Glob.find("data/testsuite/cobol85","*.CBL");
    }

    @Override
    protected CobolProject getConfiguredProject() {
        var project = new StandardCobolProject();
        project.setDefaultPreprocessing(true);
        project.addCopybookPath(Paths.get("data/testsuite/cobol85/"));
        return project;
    }

    @BeforeAll
    static void getTargetResultsFile() {
        loadResults("data/testsuite/cobol85_pp.csv");
    }

    @AfterAll
    static void getActualResultsFile() {
        storeResults("data/testsuite/cobol85_pp-actuals.csv");
    }

}
