package koopa.app.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import koopa.cobol.CobolFiles;
import koopa.cobol.parser.CobolParser;
import koopa.cobol.projects.StandardCobolProject;
import koopa.cobol.sources.SourceFormat;
import koopa.core.trees.KoopaTreeBuilder;
import koopa.core.trees.XMLSerializer;

public class ToXml {

    public static void main(String[] args) throws IOException {
        var options = new CommandLineOptions(args);
        var toXml = new ToXml(options.getFormat(), options.isPreprocess(), options.getCopybookPaths());
        var other = options.getOther();
        var source = Paths.get(other.get(0));
        var target = Paths.get(other.get(1));
        toXml.process(source, target);
    }

    private final CobolParser parser;

    public ToXml(SourceFormat format, boolean preprocessing, List<String> copybookPaths) {
        var project = new StandardCobolProject(); // ApplicationConfig.getANewProject();
        project.setDefaultFormat(format);
        project.setDefaultPreprocessing(preprocessing);
        for (var path : copybookPaths) {
            project.addCopybookPath(Paths.get(path));
        }
        this.parser = new CobolParser();
        this.parser.setProject(project);
    }

    private void process(Path source, Path target) throws IOException {
        if (Files.isRegularFile(source)) {
            toXml(source, target);
        } else if (Files.isDirectory(source)) {
            var files = CobolFiles.cobolFiles(source);
            for (var fileInFolder : files) {
                System.out.println(fileInFolder);
                var targetPath = target.resolve(fileInFolder.getFileName());
                process(fileInFolder, targetPath);
            }
        }
    }

    private void toXml(Path source, Path target) throws IOException {
        System.out.println("Processing " + source);
        var targetPath = target.getFileName().toString();
        var dot = targetPath.lastIndexOf('.');
        if (dot > -1) targetPath.substring(0, dot);
        target = Paths.get(targetPath+".xml");
        System.out.println("Writing XML to " + target);
        var targetFolder = target.getParent();
        if (targetFolder != null && !Files.isDirectory(target)) {
            Files.createDirectories(target);
        }
        var results = parser.parse(source);
        var messages = results.getParse().getMessages();
        if (messages.hasErrors()) {
            for (var error : messages.getErrors()) {
                System.out.println("Error: " + error.getFirst() + " " + error.getSecond());
            }
        }
        if (messages.hasWarnings()) {
            for (var warning : messages.getWarnings()) {
                System.out.println("Warning: " + warning.getFirst() + " " + warning.getSecond());
            }
        }
        if (!results.isValidInput()) {
            System.out.println("Could not parse " + source);
            return;
        }
        var ast = results.getParse().getTarget(KoopaTreeBuilder.class).getTree();
        var writer = Files.newBufferedWriter(Paths.get(targetPath));
        XMLSerializer.serialize(ast, writer);
    }
    
}
