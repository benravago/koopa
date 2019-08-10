package koopa.cobol.parser.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import koopa.cobol.CobolProject;
import koopa.cobol.parser.CobolParser;
import koopa.cobol.parser.test.TestResult;
import koopa.core.util.test.FileBasedTestSuite;

import static org.junit.jupiter.api.Assertions.*;

public abstract class CobolParsingRegressionTest extends FileBasedTestSuite {

    static {
        Logger.getLogger("parser").setLevel(Level.WARNING);
    }

    @Override
    protected void testFile(Path source) {
        testParsing(source);
    }

    protected abstract CobolProject getConfiguredProject();

    static Map<String, TestResult> targetResults = null;
    static Map<String, TestResult> actualResults = null;

    // Test
    void testParsing(Path file) {
        var project = getConfiguredProject();
        var parser = new CobolParser();
        parser.setProject(project);
        parser.setKeepingTrackOfTokens(true);
        var target = getTargetResult(file);
        // Parse the file...
        var result = parser.parse(file);
        var actual = TestResult.from(result);
        addActualResult(actual);
        if (target == null) {
            // Unknown test file. We will evaluate this on its overall performance.
            assertTrue(result.isValidInput());
            // TODO Assert for no warnings ?
        } else {
            // We have previous test results, which we'll now compare...
            var messages = target.getComparison(actual);
            var info = new StringBuilder();
            if (messages != null && messages.size() > 0) {
                for (var message : messages) {
                    info.append(message);
                    info.append("  ");
                }
            }
            assertFalse(messages != null && messages.size() > 0, info.toString());
        }
    }

    TestResult getTargetResult(Path source) {
        if (targetResults != null) {
            return targetResults.get(source.getFileName().toString());
        } else {
            return null;
        }
    }

    void addActualResult(TestResult result) {
        if (actualResults != null) {
            actualResults.put(result.getName(), result);
        }
    }

    // BeforeAll
    public static void loadResults(String file) {
        try {
        var targetResultsFile = Paths.get(file);
        if (Files.exists(targetResultsFile)) {
            targetResults = TestResult.loadFromFile(targetResultsFile);
            actualResults = TestResult.newResultMap();
        } else {
            targetResults = null;
            actualResults = null;
        }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // AfterAll
    public static void storeResults(String file) {
        var actualResultsFile = Paths.get(file);
        TestResult.saveToFile(actualResults, actualResultsFile);
    }

}
