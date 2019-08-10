package koopa.cobol.parser.preprocessing.test;

import java.nio.file.Files;
import java.nio.file.Path;

import java.io.StringReader;
import java.io.IOException;
import java.io.UncheckedIOException;

import java.util.Iterator;

import koopa.cobol.CobolTokens;
import koopa.cobol.projects.StandardCobolProject;
import koopa.cobol.sources.SourceFormat;
import koopa.core.data.Data;
import koopa.core.data.Token;

import koopa.core.util.Glob;
import koopa.core.util.test.FileBasedTestSuite;

import static org.junit.jupiter.api.Assertions.*;

public class PreprocessingSourceTest extends FileBasedTestSuite {

    static final String INPUT_PREFIX = "<";
    static final String EXPECTED_PREFIX = ">";

    @Override
    protected Iterator<Path> getFiles() {
        return Glob.find("data/koopa/cobol/parser/preprocessing/test/","*.ppsample");
    }

    @Override
    protected void testFile(Path source) {
        testSampleValidates(source);
    }

    // Test
    static void testSampleValidates(Path file) {
        var input = new StringBuilder();
        var expected = new StringBuilder();
        readSample(file,input,expected);

        var project = new StandardCobolProject();
        project.setDefaultFormat(SourceFormat.FREE);
        project.setDefaultPreprocessing(true);

        var reader = new StringReader(input.toString());
        var source = CobolTokens.getNewSource(file,reader,project);
        var actual = new StringBuilder();
        Data d = null;
        while ((d = source.next()) != null) {
            if (d instanceof Token) {
                actual.append(((Token) d).getText());
            }
        }
        assertEquals(expected.toString(), actual.toString());
    }

    static void readSample(Path file, StringBuilder input, StringBuilder expected) {
        try (
            var br = Files.newBufferedReader(file);
        ) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(INPUT_PREFIX)) {
                    input.append(line.substring(INPUT_PREFIX.length()));
                    input.append('\n');
                    } else if (line.startsWith(EXPECTED_PREFIX)) {
                        expected.append(line.substring(EXPECTED_PREFIX.length()));
                        expected.append('\n');
                    }
                }
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
}
