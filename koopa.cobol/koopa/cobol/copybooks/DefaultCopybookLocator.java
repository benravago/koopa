package koopa.cobol.copybooks;

import java.io.IOException;
import java.io.UncheckedIOException;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;

import java.util.logging.Logger;
import static java.util.logging.Level.*;

import koopa.cobol.CobolFiles;

/**
 * The default locator looks for copybooks by name.
 *
 * It searches for these first in the same location as the source file, then in each copybook path.
 * The first match found is used.
 */
public class DefaultCopybookLocator implements CopybookLocator {

    private static final Logger LOGGER = Logger.getLogger("copybooks");

    @Override
    public Path locate(String textName, String libraryName, Path sourceFile, List<Path> copybookPaths) {
        return locateCopybook(textName,libraryName,sourceFile,copybookPaths);
    }

    public static Path locateCopybook(String textName, String libraryName, Path sourceFile, List<Path> copybookPaths) {

        // Unquote the copybook and library names if needed.
        textName = unquote(textName);
        libraryName = unquote(libraryName);

        // How do we match candidate files?
        //   Well, they must be different from the current file (which avoids recursion),
        //   and they must be a copybook with a matching name.

        // How do we decide which of the candidate matching files to accept?
        //   Well, for now we just pick the first match found...

        // Where do we look for matching files?
        Path match = null;

        // We look in the current folder first, possibly offset by the library name.
        if (sourceFile != null) {
            match = find(sourceFile.getParent(),libraryName,sourceFile,textName);
        }

        // Then we try all copybook paths in order, again possibly offset by the library name.
        if (match == null) {
            for (var copy : copybookPaths) {
                match = find(copy,libraryName,sourceFile,textName);
                if (match != null) {
                    break;
                }
            }
        }

        if (LOGGER.isLoggable(FINER)) {
            if (match != null) {
                LOGGER.finer("Lookup of copybook " + textName + " in " + libraryName + " succeeded; found " + match);
            } else {
                LOGGER.finer("Lookup of copybook " + textName + " in " + libraryName + " failed: not found.");
            }
        }

        return match;
    }

    static Path find(Path dir, String library, Path source, String name) {
    	var files = files(dir, library);
    	if (files != null) {
    		for (var file : files(dir, library)) {
    			if (file.equals(source)) continue;
    			if (isCopybookNamed(file, name)) return file;
    		}
        }
        return null;
    }

    static DirectoryStream<Path> files(Path sourcePath, String libraryName) {
        if (libraryName != null) {
            sourcePath = sourcePath.resolve(libraryName);
        }
        if (Files.isDirectory(sourcePath)) {
        	try {
        		return Files.newDirectoryStream(sourcePath,Files::isRegularFile);
        	}
        	catch (IOException e) { throw new UncheckedIOException(e); }
        }
        return null;
    }

    public static boolean isCopybookNamed(Path path, String textName) {
        var file = path.getFileName();
        if (CobolFiles.copybookFilenames.matches(file)) {
            var name = file.toString();
            if (name.regionMatches(true, 0, textName, 0, name.lastIndexOf('.') )) {
                return true;
            }
        }
        return false;
    }


    static String unquote(String s) {
        if (s != null) {
            var n = s.length() - 1;
            if (n > 1) {
                var c = s.charAt(0);
                if ((c == '"' || c == '\'') && (s.charAt(n) == c )) {
                    return s.substring(1,n);
                }
            }
        }
        return s;
    }

}
