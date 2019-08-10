package koopa.cobol.projects;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import koopa.cobol.CobolProject;
import koopa.cobol.copybooks.DefaultCopybookLocator;
import koopa.cobol.copybooks.CopybookPaths;

/**
 * A {@link BasicCobolProject} which resolves copybooks by means of copybook paths.
 */
public class StandardCobolProject extends BasicCobolProject implements CobolProject, CopybookPaths {

    private List<Path> copybookPaths = new ArrayList<>();

    @Override
    public CobolProject duplicate() {
        var project = new StandardCobolProject();
        copyBasicSettingsInto(project);
        copyDefaultSettingsInto(project);
        return project;
    }

    private void copyDefaultSettingsInto(final StandardCobolProject project) {
        project.copybookPaths.addAll(copybookPaths);
    }

    @Override
    public void addCopybookPath(Path path) {
        if (path == null) {
            throw new NullPointerException("Null path");
        }
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Not a folder: " + path);
        }
        copybookPaths.add(path);
    }

    @Override
    public void removeCopybookPath(Path path) {
        copybookPaths.remove(path);
    }

    @Override
    public List<Path> getCopybookPaths() {
        return Collections.unmodifiableList(copybookPaths);
    }

    @Override
    public Path locateCopybook(String textName, String libraryName, Path sourceFile) {
        return DefaultCopybookLocator.locateCopybook(textName,libraryName,sourceFile,copybookPaths);
    }

}
