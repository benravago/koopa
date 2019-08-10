package koopa.core.parsers.combinators;

import koopa.core.parsers.Parse;
import koopa.core.parsers.ParserCombinator;

/**
 * This {@linkplain ParserCombinator} will attempt to match a given {@linkplain ParserCombinator},
 * while restricting the stream for that one to never pass a positive match for a limiting {@linkplain ParserCombinator}.
 * 
 * The overall match will only succeed if the given parser matches, and we have hit the limiter at that point.
 * <p>
 * The limited will be put inside a {@linkplain Closure} when this parser starts matching.
 */
public class UpTo extends UnaryParserDecorator {
    // TODO Extract common base class with LimitedTo ?

    private final ParserCombinator limiter;

    public UpTo(ParserCombinator target, ParserCombinator limiter) {
        super(target);
        this.limiter = limiter;
    }

    @Override
    public boolean matches(Parse parse) {
        var limitedStream = parse.getFlow().getLimitedStream();
        // TODO Only create closure when really needed.
        var closedLimiter = new Closure(limiter, parse);
        if (parse.getTrace().isEnabled()) {
            parse.getTrace().indent(toString() + " ?");
        }
        limitedStream.addLimiter(closedLimiter);
        var accepts = parser.accepts(parse);
        limitedStream.removeLimiter(closedLimiter);
        if (!accepts) {
            if (parse.getTrace().isEnabled()) {
                parse.getTrace().dedent(toString() + " : no, no match");
            }
            return false;
        }
        var stream = parse.getStream();
        if (stream.peek() == null) {
            if (parse.getTrace().isEnabled()) {
                parse.getTrace().dedent(toString() + " : yes, at end");
            }
            return true;
        }
        if (parse.getTrace().isEnabled()) {
            parse.getTrace().indent(toString() + ", at limit ?");
        }
        stream.bookmark();
        var atLimiter = limiter.accepts(parse);
        stream.rewind();
        if (parse.getTrace().isEnabled()) {
            parse.getTrace().dedent(toString() + ", at limit : " + (atLimiter ? "yes" : "no"));
        }
        if (parse.getTrace().isEnabled()) {
            parse.getTrace().dedent(toString() + " : " + (atLimiter ? "yes" : "no"));
        }
        return atLimiter;
    }

    @Override
    public String toString() {
        return "%match (" + parser.toString() + ") %upto ...";
    }

}
