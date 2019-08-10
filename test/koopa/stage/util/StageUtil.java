package koopa.stage.util;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import koopa.core.parsers.Parse;
import koopa.core.trees.KoopaTreeBuilder;
import koopa.core.trees.Tree;
import koopa.stage.grammar.StageGrammar;
import koopa.stage.source.StageTokens;

public final class StageUtil {
    private StageUtil() {}

    public static Tree getAST(Path input) throws IOException {
        return getAST(input, false);
    }

    public static Tree getAST(Path input, boolean quiet) {
        var name = input.getFileName().toString();
        var source = StageTokens.getNewSource(name,reader(input));
        var kg = new StageGrammar();
        var parse = Parse.of(source).to(new KoopaTreeBuilder(kg));
        parse.getTrace().quiet(quiet);
        var accepts = kg.stage().accepts(parse);
        if (!accepts) {
            System.out.println("Parse failed. Got up to: " + parse.getFinalPosition());
            return null;
        }
        var builder = parse.getTarget(KoopaTreeBuilder.class);
        var ast = builder.getTree();
        return ast;
    }

    static Reader reader(Path path) {
        try { return Files.newBufferedReader(path); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public static Iterable<Path> stageFiles(String dir) {
        try {
            return Files.newDirectoryStream(Paths.get(dir),"**.stage");
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

}
