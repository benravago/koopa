package koopa.dsl.kg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import koopa.core.grammars.KoopaGrammar;
import koopa.core.util.Glob;
import koopa.dsl.kg.generator.KGGenerator;
import koopa.dsl.kg.util.KGUtil;

/**
 * Main class to generate {@linkplain KoopaGrammar}s from .kg source files.
 */
public class KGG {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("No path given.");
            System.exit(-1);
        }

        var path = Paths.get(args[0]);
        if (!Files.exists(path)) {
            System.err.println("Not found: " + path);
            System.exit(-1);
        }
        if (Files.isRegularFile(path)) {
            translate(path);
        } else if (Files.isDirectory(path)) {
            translateAllIn(path);
        } else {
            System.err.println("Not a file or folder: " + path);
            System.exit(-1);
        }
    }

    private static void translateAllIn(Path folder) throws Exception {
        var files = Glob.walk(folder,"*.kg");
        while (files.hasNext()) {
            translate(files.next());
        }
    }

    public static void translate(Path file) {
        System.out.println("Reading " + file + "...");
        var ast = KGUtil.getAST(file);
        KGGenerator.translate(file,ast);
    }

}
