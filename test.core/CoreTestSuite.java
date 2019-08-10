class CoreTestSuite extends koopa.core.util.test.TestShell {
  static final String[] tests = {

    "koopa.core.data.test.PositionTest",
    "koopa.core.data.test.TokenTest",
    "koopa.core.data.test.TokensTest",

    "koopa.core.grammars.test.KoopaGrammarTest",
    "koopa.core.grammars.test.DecimalGrammarTest",
    "koopa.core.grammars.test.BinaryGrammarTest",
    "koopa.core.grammars.test.AutomaticKeywordsTest",

    "koopa.core.grammars.fluent.test.FluentGrammarTest",

    "koopa.core.parsers.test.ParseStackTest",

    "koopa.core.parsers.combinators.test.BalancingTest",

    "koopa.core.streams.test.BaseStreamTest",
    "koopa.core.streams.test.LimitedStreamTest",

    "koopa.core.trees.test.TreeIteratorsTest",
    "koopa.core.trees.test.TreePositionsTest",
    "koopa.core.trees.test.TreeWalkerTest",
    "koopa.core.trees.test.ProgramTextTest",
    "koopa.core.trees.test.XMLSerializerTest"

  };
  @Override protected String[] testClassNames() {return tests;}
}
