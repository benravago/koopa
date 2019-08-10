package koopa.core.grammars.combinators;

import java.util.Map;
import java.util.Set;

import koopa.core.grammars.Grammar;
import koopa.core.parsers.Parse;
import koopa.core.parsers.ParserCombinator;

/**
 * This is kind of an optimizing {@linkplain ParserCombinator}
 * which can choose directly between a selection of many {@linkplain ParserCombinator}s
 * by looking at the very next {@linkplain Grammar#keyword()} in the stream.
 * 
 * I.e. it dispatches to a specific parser based on the value of the upcoming keyword.
 */
public class Dispatched extends WithKeyword {

    private static final String SYMBOL = "$(...|...)";

    private final Map<String, ParserCombinator> lookupTable;

    /**
     * Note: this constructor assumes that the keys in the lookup table
     * have been passed through {@linkplain Grammar#comparableText(String)}.
     */
    public Dispatched(Grammar grammar, Map<String, ParserCombinator> lookupTable) {
        super(grammar);
        this.lookupTable = lookupTable;
    }

    @Override
    protected boolean matchesAtKeyword(Parse parse, String keyword) {
        if (!lookupTable.containsKey(keyword)) {
            if (parse.getTrace().isEnabled()) {
                parse.getTrace().add(SYMBOL + " : no, did not find " + keyword);
            }
            return false;
        }

        if (parse.getTrace().isEnabled()) {
            parse.getTrace().indent(SYMBOL + " ? at " + keyword);
        }

        var parser = lookupTable.get(keyword);
        var accepted = parser.accepts(parse);

        if (parse.getTrace().isEnabled()) {
            parse.getTrace().dedent(SYMBOL + " : " + (accepted ? "yes" : "no"));
        }

        return accepted;
    }

    @Override
    public void addAllKeywordsInScopeTo(Set<String> keywords) {
        for (var parser : lookupTable.values()) {
            parser.addAllKeywordsInScopeTo(keywords);
        }
    }

    @Override
    public void addAllLeadingKeywordsTo(Set<String> keywords) {
        for (var key : lookupTable.keySet()) {
            keywords.add(key);
        }
    }

    @Override
    public boolean allowsLookahead() {
        return true;
    }

    @Override
    public String toString() {
        return SYMBOL;
    }

}
