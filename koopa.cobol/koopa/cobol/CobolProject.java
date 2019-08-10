package koopa.cobol;

import java.nio.file.Path;

import koopa.cobol.grammar.CobolGrammar;
import koopa.cobol.sources.SourceFormat;
import koopa.core.parsers.ParserCombinator;

/**
 * The CobolProject defines how Cobol files should be handled/parsed.
 *
 * What formatting do they use?
 * What parser/grammar rule should be applied?
 * What files are copybooks?
 * Where is a certain named copybook?
 */
public interface CobolProject {

    /**
     * What {@linkplain CobolGrammar} should be used ?
     */
    CobolGrammar getGrammar();

    /**
     * The <code>sourceFile</code> is looking for copybook <code>textName</code> of <code>libraryName</code>.
     *
     * This should return the right file for that copybook,
     * or <code>null</code> if it couldn't be found.
     * <p>
     * The spec on copybook resolution:
     * <p>
     * "The implementor shall define the rules for locating the library text referenced by text-name-1 or literal-1.
     *  When neither library-name-1 nor literal-2 is specified, a default COBOL library is used.
     *  The implementor defines the mechanism for identifying the default COBOL library."
     */
    Path locateCopybook(String textName, String libraryName, Path sourceFile);

    /**
     * The parser/grammar rule that should be used to parse the file.
     */
    ParserCombinator parserFor(Path file);

    /**
     * The default {@linkplain SourceFormat}.
     */
    SourceFormat getDefaultFormat();

    /**
     * Set the default {@linkplain SourceFormat}.
     */
    void setDefaultFormat(SourceFormat format);

    /**
     * The {@linkplain SourceFormat} for this specific file.
     */
    SourceFormat getFormat(Path file);

    /**
     * The default tab length to use (as a number of spaces).
     */
    int getDefaultTabLength();

    /**
     * Set the default tab length to use (as a number of spaces).
     */
    void setDefaultTabLength(int tabLengthValue);

    /**
     * The default tab length to use for this specific file (as a number of spaces).
     */
    int getTabLength(Path file);

    /**
     * If we are preprocessing files by default.
     */
    boolean isDefaultPreprocessing();

    /**
     * Define whether we should be preprocessing files by default.
     */
    void setDefaultPreprocessing(boolean preprocessing);

    /**
     * If we are preprocessing this specific file.
     */
    boolean isPreprocessing(Path file);

    /**
     * Get a copy of this CobolProject with the same settings.
     */
    CobolProject duplicate();

}
