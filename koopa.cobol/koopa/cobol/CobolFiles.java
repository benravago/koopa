package koopa.cobol;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

/**
 * This class offers a number of utility functions which help in selecting Cobol source files and copybooks.
 *
 * You can basically ask it whether a file is a Cobol file,
 * and it will test for this by checking the file's extension
 * against a number of known extensions (ignoring case).
 * <p>
 * In addition you can ask this class for {@linkplain java.io.FileFilter} and
 * {@linkplain FilenameFilter} instances which will fall back on the same logic.
 * <p>
 * So what extensions are covered ? Well, by default you get these:
 * <ul>
 * <li>For source files: CBL and COB.</li>
 * <li>For copybooks: CPY and COPY.</li>
 * </ul>
 * <p>
 * These defaults can be modified by passing a comma separated list of
 * extensions to one of the following system properties:
 * <ul>
 * <li>koopa.cobol.sources; for identifying source files</li>
 * <li>koopa.cobol.copybooks; for identifying copybooks</li>
 * </ul>
 * So, for example,
 * <code>-Dkoopa.cobol.sources=cbl,cob -Dkoopa.cobol.copybooks=cpy,copy</code>
 * is equivalent to the default.
 * <p>
 * By setting your own extensions for any category, the defaults for that
 * category will not be set. So setting
 * <code>-Dkoopa.cobol.sources=cbl -Dkoopa.cobol.copybooks=cob</code> means that
 * <b>only</b> files with extension 'cbl' will be seen as source files, and
 * <b>only</b> files with extension 'cob' will be seen as copybooks.
 */

public final class CobolFiles {
    private CobolFiles() {}

    public static PathMatcher cobolFilenames = matcher("koopa.cobol.sources","cbl,CBL,cob,COB");
    public static PathMatcher copybookFilenames = matcher("koopa.cobol.copybooks","cpy,CPY,copy,COPY");

    static PathMatcher matcher(String k, String extensions) {
        return FileSystems.getDefault()
            .getPathMatcher("glob:*.{"+System.getProperty(k,extensions)+"}");
    }

    public static DirectoryStream<Path> cobolFiles(Path dir) {
    	try { return Files.newDirectoryStream(dir,CobolFiles::isCobol); }
    	catch (IOException e) { throw new UncheckedIOException(e); }
    }    
    /**
     * If the extension for the given file indicate that it's a Cobol file.
     */
    public static boolean isCobol(Path file) {
        if (Files.isRegularFile(file)) {
            var name = file.getFileName();
            return cobolFilenames.matches(name) || copybookFilenames.matches(name);
        }
        return false;
    }

    /**
     * If the extension for the given file indicate that it's a Copybook file.
     */
    public static boolean isCopybook(Path file) {
        if (Files.isRegularFile(file)) {
            return copybookFilenames.matches(file.getFileName());
        }
        return false;
    }
    
}
