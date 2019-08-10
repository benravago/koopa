package koopa.cobol.grammar.directives.test;

import java.io.StringReader;

import koopa.cobol.grammar.directives.DirectivesGrammar;
import koopa.cobol.sources.SourceFormat;
import koopa.core.grammars.Grammar;
import koopa.core.sources.BasicTokens;
import koopa.core.sources.Source;
import koopa.core.sources.TagAll;
import koopa.stage.runtime.GrammarTestSuite;

public abstract class DirectivesTests extends GrammarTestSuite {

    public Grammar getGrammar() {
        return DirectivesGrammar.instance();
    }

    public Source getSourceForSample(String sample, Grammar grammar) {
        var reader = new StringReader(sample);
        var basicTokens = BasicTokens.getNewSource(null, reader);
        var freeTokens = new TagAll(basicTokens, getSourceFormat());
        return freeTokens;
    }

    protected abstract SourceFormat getSourceFormat();

}
