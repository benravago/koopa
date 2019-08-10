package koopa.core.parsers.combinators;

import java.util.Set;

import koopa.core.parsers.Parse;
import koopa.core.parsers.ParserCombinator;

public class Sequence extends ParserCombinator {

    private final ParserCombinator[] parsers;
    private final int length;

    public Sequence(ParserCombinator[] parsers) {
        assert (parsers != null && parsers.length > 0);
        this.parsers = parsers;
        this.length = parsers.length;
    }

    @Override
    public boolean matches(Parse parse) {
        for (var parser : parsers) {
            if (!parser.accepts(parse)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void addAllKeywordsInScopeTo(Set<String> keywords) {
        for (var parser : parsers) {
            parser.addAllKeywordsInScopeTo(keywords);
        }
    }

    @Override
    public void addAllLeadingKeywordsTo(Set<String> keywords) {
        for (var i = 0; i < length; i++) {
            parsers[i].addAllLeadingKeywordsTo(keywords);
            if (!parsers[i].canMatchEmptyInputs()) {
                break;
            }
        }
    }

    @Override
    public boolean allowsLookahead() {
        for (var i = 0; i < length; i++) {
            if (!parsers[i].canMatchEmptyInputs()) {
                return parsers[i].allowsLookahead();
            } else if (!parsers[i].allowsLookahead()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canMatchEmptyInputs() {
        for (var parser : parsers) {
            if (!parser.canMatchEmptyInputs()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "(...)";
    }

}
