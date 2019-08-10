package koopa.core.parsers.combinators;

import koopa.core.parsers.Parse;
import koopa.core.parsers.ParserCombinator;

/**
 * This {@linkplain ParserCombinator} will attempt to match a given {@linkplain ParserCombinator},
 * while restricting the stream for that one to never pass a positive match for a limiting {@linkplain ParserCombinator}.
 */
public class LimitedTo extends UnaryParserDecorator {

    private final ParserCombinator limiter;

    public LimitedTo(ParserCombinator target, ParserCombinator limiter) {
        super(target);
        this.limiter = limiter;
    }

    @Override
    public boolean matches(Parse parse) {
        var limitedStream = parse.getFlow().getLimitedStream();
        // TODO Only creature closure when really needed.
        var closedLimiter = new Closure(limiter, parse);
        try {
            limitedStream.addLimiter(closedLimiter);
            return parser.accepts(parse);
        } finally {
            limitedStream.removeLimiter(closedLimiter);
        }
    }

    @Override
    public String toString() {
        return "%limit " + parser + " %by " + limiter;
    }

}
