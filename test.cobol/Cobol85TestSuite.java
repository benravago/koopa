class Cobol85TestSuite extends koopa.core.util.test.TestShell {
  static final String[] tests = {

    "koopa.cobol.parser.test.Cobol85RegressionTest",
    "koopa.cobol.parser.preprocessing.test.Cobol85PreprocessingTest"

  };
  @Override protected String[] testClassNames() {return tests;}
}
