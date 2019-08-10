package koopa.dsl.kg.util;

import java.io.Reader;
import java.io.IOException;
import java.io.UncheckedIOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import koopa.core.parsers.Parse;
import koopa.core.trees.KoopaTreeBuilder;
import koopa.core.trees.Tree;

import koopa.dsl.kg.grammar.KGGrammar;
import koopa.dsl.kg.source.KGTokens;

public final class KGUtil {

    private KGUtil() {}

    /**
     * Parse the contents of a grammar file, and return the syntax tree.
     */
    public static Tree getAST(Path input) {
        try {
            return getAST(input.getFileName().toString(), Files.newBufferedReader(input));
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    /**
     * Parse the contents of a {@linkplain Reader}, and return the syntax tree.
     * The name you pass along will be used as a reference name for its contents.
     */
    public static Tree getAST(String name, Reader reader) {
        var source = KGTokens.getNewSource(name, reader);
        var kg = new KGGrammar();
        var parse = Parse.of(source).to(new KoopaTreeBuilder(kg));
        var accepts = kg.grammar().accepts(parse);
        if (!accepts) {
            System.out.println("Parse failed. Got up to: " + parse.getFinalPosition());
            return null;
        }
        var builder = parse.getTarget(KoopaTreeBuilder.class);
        return builder.getTree();
    }

    public static Path relatedFile(Path basis, String suffix) {
        String name = basis.getFileName().toString();
        if (name.endsWith(".kg")) {
            name = name.substring(0,name.length()-3);
        }
        return basis.getParent().resolve(name+suffix);
    }

    public static void writeFile(Path path, String text) {
        try {
            Files.writeString(path,text);
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public static Properties loadProperties(Path path) {
        try {
            Properties p = new Properties();
            p.load(Files.newInputStream(path));
            return p;
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

}
