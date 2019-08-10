package koopa.dsl.kg.generator;

import java.nio.file.Path;

import koopa.core.trees.Tree;
import koopa.dsl.kg.util.KGUtil;
import koopa.templates.TemplateLoader;

public class KGGenerator {
    private KGGenerator() {}

    private static final Generation GRAMMAR_TEMPLATE = new Generation(
        TemplateLoader.fromResource(KGGenerator.class,
            KGGenerator.class.getPackageName() + "/grammar.template") );

    public static void translate(Path grammarFile, Tree ast) {
        var code = GRAMMAR_TEMPLATE.generate(grammarFile,ast);
        var java = KGUtil.relatedFile(grammarFile,"Grammar.java");
        System.out.println("Generating " + java);
        KGUtil.writeFile(java,code);
        System.out.println("Generation complete.");
    }

}
