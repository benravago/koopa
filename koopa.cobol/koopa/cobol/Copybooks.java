package koopa.cobol;

import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import koopa.cobol.copybooks.CopybookLocator;
import koopa.cobol.copybooks.DefaultCopybookLocator;

import java.util.logging.Logger;
import static java.util.logging.Level.*;

/**
 * This class helps you look up a copybook based on its name.
 * <p>
 * There are two ways of configuring how this works.
 *
 * One is by setting the paths in which to look.
 *
 * The other is by installing a custom {@linkplain CopybookLocator}
 * via the <code>koopa.copybooks.locator</code> system property.
 */
public class Copybooks {

    private static final Logger LOGGER = Logger.getLogger("copybooks");

    private static CopybookLocator locator;

    static {
        var className = System.getProperty("koopa.copybooks.locator");
        try {
            if (className != null) {
                if (LOGGER.isLoggable(FINE)) {
                    LOGGER.fine("Attempting to instantiate copybook locator: " + className);
                }
                var locatorClass = Class.forName(className);
                // This cast may fail if the class we loaded is not a CopybookLocator.
                // In that case we just want the thing to break with a ClassCastException, I think.
                setLocator((CopybookLocator) locatorClass.getConstructor().newInstance());
            }
        } catch (Exception e) {
            LOGGER.log(SEVERE, "Failed to set copybook locator: " + className + ". Using default instead.", e);
        }
        if (locator == null) {
            setLocator(null);
        }
    }

    /**
     * Set the {@linkplain CopybookLocator} instance to use when resolving copybooks.
     * <p>
     * If passed <code>null</code> will install a {@linkplain DefaultCopybookLocator} instance instead.
     */
    static void setLocator(CopybookLocator l) {
        if (l == null) {
            locator = new DefaultCopybookLocator();
        } else {
            locator = l;
        }
    }

    private final List<Path> copybookPaths = new ArrayList<>();

    public Path locate(String textName, String libraryName, Path sourceFile) {
        return locator.locate(textName, libraryName, sourceFile, copybookPaths);
    }

    public void addAllFrom(Copybooks copybooks) {
        copybookPaths.addAll(copybooks.copybookPaths);
    }

    public void addPath(Path path) {
        copybookPaths.add(path);
    }

    public void removePath(Path path) {
        copybookPaths.remove(path);
    }

    public List<Path> getPaths() {
        return Collections.unmodifiableList(copybookPaths);
    }

}
