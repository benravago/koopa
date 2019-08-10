package koopa.cobol.sources.test;

import static koopa.core.data.tags.AreaTag.COMMENT;
import static koopa.core.data.tags.AreaTag.COMPILER_DIRECTIVE;
import static koopa.core.data.tags.AreaTag.PROGRAM_TEXT_AREA;
import static koopa.core.data.tags.AreaTag.SKIPPED;
import static koopa.core.data.tags.SyntacticTag.END_OF_LINE;
import static koopa.core.data.tags.SyntacticTag.INCOMPLETE;
import static koopa.core.data.tags.SyntacticTag.NUMBER;
import static koopa.core.data.tags.SyntacticTag.SEPARATOR;
import static koopa.core.data.tags.SyntacticTag.STRING;
import static koopa.core.data.tags.SyntacticTag.WHITESPACE;
import static koopa.core.data.tags.SyntacticTag.WORD;

import java.nio.file.Path;
import java.nio.file.Paths;

import koopa.core.sources.LineSplitter;
import koopa.core.sources.Source;
import koopa.cobol.sources.test.samples.Sample;
import koopa.cobol.sources.test.samples.SourcesValidationTest;

import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This class provides the infrastructure for testing the different sources.
 * It looks for ".sample" files, and runs each one it finds through a JUnit test.
 */
public class CoreSourcesValidationTest extends SourcesValidationTest {

    @Override
    protected Path getFolder() {
        return Paths.get("test/core/koopa/core/sources/test/");
    }

    @BeforeEach
    @Override
    protected void initialize() {
    	super.initialize();
        var programText = new Object[] { PROGRAM_TEXT_AREA };
        var notProgramText = new Object[] { COMMENT, SKIPPED, COMPILER_DIRECTIVE };
        addTokenCategory("TEXT", programText, notProgramText);
        addTokenCategory("T", programText, notProgramText);
        var comment = new Object[] { COMMENT };
        var notComment = new Object[] { PROGRAM_TEXT_AREA, SKIPPED, COMPILER_DIRECTIVE };
        addTokenCategory("COMMENT", comment, notComment);
        addTokenCategory("C", comment, notComment);
        var skipped = new Object[] { SKIPPED };
        var notSkipped = new Object[] { PROGRAM_TEXT_AREA, COMMENT, COMPILER_DIRECTIVE };
        addTokenCategory("SKIPPED", skipped, notSkipped);
        addTokenCategory("SKIP", skipped, notSkipped);
        addTokenCategory("SKP", skipped, notSkipped);
        var compilerDirective = new Object[] { COMPILER_DIRECTIVE };
        var notCompilerDirective = new Object[] { PROGRAM_TEXT_AREA, COMMENT, SKIPPED };
        addTokenCategory("COMPILER_DIRECTIVE", compilerDirective, notCompilerDirective);
        addTokenCategory("DIRECTIVE", compilerDirective, notCompilerDirective);
        addTokenCategory("DIR", compilerDirective, notCompilerDirective);
        addTokenCategory("D", compilerDirective, notCompilerDirective);
        var separator = new Object[] { SEPARATOR };
        addTokenCategory(":", separator);
        addTokenCategory("SEP", separator);
        var eoln = new Object[] { END_OF_LINE };
        addTokenCategory("EOLN", eoln);
        var whitespace = new Object[] { SEPARATOR, WHITESPACE };
        addTokenCategory(".", whitespace);
        addTokenCategory("WS", whitespace);
        var string = new Object[] { STRING };
        addTokenCategory("STRING", string);
        addTokenCategory("STR", string);
        addTokenCategory("S", string);
        var number = new Object[] { NUMBER };
        addTokenCategory("NUMBER", number);
        addTokenCategory("NUM", number);
        addTokenCategory("N", number);
        var word = new Object[] { WORD };
        addTokenCategory("WORD", word);
        addTokenCategory("W", word);
        var incomplete = new Object[] { INCOMPLETE };
        addTokenCategory("INCOMPLETE", incomplete);
    }

    @Override
    protected Source getSource(Path file, Sample sample) {
        String resourceName = file.getFileName().toString();
        if (resourceName.startsWith("LineSplitter")) {
            return new LineSplitter(resourceName, sample.getReader());
        }
        fail("Don't know how to setup source for " + resourceName);
        return null;
    }
}
