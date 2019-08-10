package koopa.cobol.sources.test;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.HashMap;
import java.util.Map;

import static koopa.cobol.data.tags.CobolAreaTag.IDENTIFICATION_AREA;
import static koopa.cobol.data.tags.CobolAreaTag.INDICATOR_AREA;
import static koopa.cobol.data.tags.CobolAreaTag.SEQUENCE_NUMBER_AREA;
import static koopa.cobol.data.tags.CobolTag.SOURCE_FORMAT_DIRECTIVE;
import static koopa.cobol.data.tags.CobolTag.SOURCE_LISTING_DIRECTIVE;
import static koopa.cobol.sources.SourceFormat.FIXED;
import static koopa.cobol.sources.SourceFormat.FREE;
import static koopa.cobol.sources.SourceFormat.VARIABLE;
import static koopa.core.data.tags.AreaTag.COMPILER_DIRECTIVE;

import koopa.cobol.CobolTokens;
import koopa.cobol.projects.StandardCobolProject;
import koopa.cobol.sources.CompilerDirectives;
import koopa.cobol.sources.ContinuationOfLines;
import koopa.cobol.sources.InlineComments;
import koopa.cobol.sources.ProgramArea;
import koopa.cobol.sources.Replace;
import koopa.core.sources.LineSplitter;
import koopa.core.sources.Source;
import koopa.core.sources.TokenSeparator;
import koopa.cobol.sources.test.CoreSourcesValidationTest;
import koopa.cobol.sources.test.samples.Sample;

import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This class provides the infrastructure for testing the different Cobol sources.
 * It looks for ".sample" files, and runs each one it finds through a JUnit test.
 */
// @RunWith(Files.class)
public class CobolSourcesValidationTest extends CoreSourcesValidationTest {

    static final Map<String, Class<? extends Source>> CLASSES = new HashMap<>();
    static {
        CLASSES.put("LineSplitter", LineSplitter.class);
        CLASSES.put("SourceFormatDirectives", CompilerDirectives.class);
        CLASSES.put("CompilerDirectives", CompilerDirectives.class);
        CLASSES.put("SourceListingDirectives", CompilerDirectives.class);
        CLASSES.put("ProgramArea", ProgramArea.class);
        CLASSES.put("TokenSeparator", TokenSeparator.class);
        CLASSES.put("InlineComments", InlineComments.class);
        CLASSES.put("ContinuationOfLines", ContinuationOfLines.class);
        CLASSES.put("Replace", Replace.class);
    }

    @Override
    protected Path getFolder() {
        return Paths.get("data/koopa/cobol/sources/test/");
    }

    @Override
    protected Source getSource(Path file, Sample sample) {
        var fileName = file.getFileName().toString();
        var className = fileName.substring(0, fileName.indexOf('.'));
        Class<? extends Source> clazz;
        if ("All".equals(className)) {
            clazz = null;
        } else {
            clazz = CLASSES.get(className);
            assertNotNull(clazz, "Missing key: " + className);
        }
        var project = new StandardCobolProject();
        project.setDefaultFormat(FIXED);
        project.setDefaultPreprocessing(true);
        var source = CobolTokens.getNewSource(file, sample.getReader(), project);
        var selectedSource = (clazz == null) ? source : source.getSource(clazz);
        assertNotNull(selectedSource, "No such source: " + clazz);
        return selectedSource;
    }

    @BeforeEach
    @Override
    protected void initialize() {
        super.initialize();
        var fixed = new Object[] { FIXED };
        addTokenCategory("FIXED", fixed);
        addTokenCategory("FXD", fixed);
        addTokenCategory("F", fixed);
        var free = new Object[] { FREE };
        addTokenCategory("FREE", free);
        addTokenCategory("f", free);
        var variable = new Object[] { VARIABLE };
        addTokenCategory("VARIABLE", variable);
        addTokenCategory("VAR", variable);
        addTokenCategory("V", variable);
        var seqnr = new Object[] { SEQUENCE_NUMBER_AREA };
        addTokenCategory("SEQNR", seqnr);
        var indicator = new Object[] { INDICATOR_AREA };
        addTokenCategory("INDIC", indicator);
        addTokenCategory("I", indicator);
        var identification = new Object[] { IDENTIFICATION_AREA };
        addTokenCategory("IDENT", identification);
        var sourceListingDirective = new Object[] { SOURCE_LISTING_DIRECTIVE, COMPILER_DIRECTIVE };
        addTokenCategory("SOURCE_LISTING_DIRECTIVE", sourceListingDirective);
        addTokenCategory("SOURCE_LISTING", sourceListingDirective);
        addTokenCategory("LISTING", sourceListingDirective);
        var sourceFormatDirective = new Object[] { SOURCE_FORMAT_DIRECTIVE, COMPILER_DIRECTIVE };
        addTokenCategory("SOURCE_FORMAT_DIRECTIVE", sourceFormatDirective);
        addTokenCategory("SOURCE_FORMAT", sourceFormatDirective);
        addTokenCategory("FORMAT", sourceFormatDirective);
        addNodeCategory("copy", "cobol", "copyStatement");
        addNodeCategory("replace", "cobol", "replaceStatement");
    }

}
