class CobolTestSuite extends koopa.core.util.test.TestShell {
  static final String[] tests = {

    "koopa.cobol.grammar.test.CobolGrammarTests",

    "koopa.cobol.grammar.directives.test.FixedDirectivesTests",
    "koopa.cobol.grammar.directives.test.FreeDirectivesTests",

    "koopa.cobol.grammar.preprocessing.test.CobolPreprocessingGrammarTests",

    "koopa.cobol.parser.preprocessing.test.PreprocessingSourceTest",

    "koopa.cobol.parser.preprocessing.replacing.test.ReplacingPhraseOperandTest",

    "koopa.cobol.sources.test.CobolSourcesValidationTest"

  };
  @Override protected String[] testClassNames() {return tests;}
}
