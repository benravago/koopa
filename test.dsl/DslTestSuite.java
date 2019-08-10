class DslTestSuite extends koopa.core.util.test.TestShell {
  static final String[] tests = {

    "koopa.dsl.kg.grammar.test.KGGrammarTests",
    "koopa.dsl.kg.grammar.test.ParseIncludedGrammarsTest",

    "koopa.dsl.stage.grammar.test.StageGrammarTests"

  };
  @Override protected String[] testClassNames() {return tests;}
}
