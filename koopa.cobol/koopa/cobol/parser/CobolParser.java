package koopa.cobol.parser;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.logging.Logger;
import static java.util.logging.Level.*;

import koopa.cobol.CobolProject;
import koopa.cobol.CobolTokens;
import koopa.cobol.grammar.CobolGrammar;
import koopa.cobol.sources.LOCCount;
import koopa.core.data.Position;
import koopa.core.data.Token;
import koopa.core.parsers.Parse;
import koopa.core.parsers.Stream;
import koopa.core.streams.BaseStream;
import koopa.core.targets.NullTarget;
import koopa.core.targets.TokenTracker;
import koopa.core.trees.KoopaTreeBuilder;

public class CobolParser {

    private static final Logger LOGGER = Logger.getLogger("parser");

    private boolean keepingTrackOfTokens = false;

    private boolean buildTrees = false;

    private CobolProject project = null;

    public ParseResults parse(Path file) {
        if (LOGGER.isLoggable(INFO)) {
            LOGGER.info("Parsing " + file);
        }
        return parse(file, reader(file));
    }

    public ParseResults parse(Path file, Reader reader) {
        var parse = getParseSetup(file, reader);
        return parse(file, parse);
    }

    public ParseResults parse(Path file, Parse parse) {
        var start = System.currentTimeMillis();
        var results = doParse(file, parse);
        var end = System.currentTimeMillis();
        LOGGER.finer("Parse took: " + (end - start) + " ms");
        results.setTime(end - start);
        return results;
    }

    private ParseResults doParse(Path file, Parse parse) {
        var accepts = project.parserFor(file).accepts(parse);
        var results = new ParseResults(file);
        results.setValidInput(accepts);
        results.setParse(parse);
        var messages = parse.getMessages();
        if (LOGGER.isLoggable(INFO)) {
            LOGGER.info((accepts ? "Valid file: " : "Invalid file: ") + file);
            if (messages.hasWarnings()) {
                LOGGER.info("There were warnings from the grammar:");
                for (var warning : messages.getWarnings()) {
                    LOGGER.info("  " + warning.getFirst() + ": " + warning.getSecond());
                }
            }
        }

        // Lets figure out whether we have seen all program text.
        // This will also push any remaining tokens to the token tracker, if one was set up.
        var tail = new BaseStream(parse.getFlow().getSource(), new NullTarget());
        tail.bookmark();
        var sawMoreProgramText = grabRemainingProgramText(project.getGrammar(), tail, parse.getTarget(TokenTracker.class));

        // Here we check if the parser really consumed all input.
        // If it didn't we try to flag the point of failure as best we can.
        if (accepts && sawMoreProgramText) {
            accepts = false;
            results.setValidInput(false);
            var msg = "Incomplete parse.";
            var finalFrame = parse.getFinalFrame();
            if (finalFrame != null) {
                msg += " Last successful match: " + finalFrame.toTrace() + ".";
            }
            if (LOGGER.isLoggable(FINER)) {
                LOGGER.finer(msg);
            }
            tail.rewind();
            tail.bookmark();
            var finalPosition = parse.getFinalPosition();
            var token = findTokenAt(tail, finalPosition);
            messages.error(token, msg);
        }
        tail.rewind();
        parse.done();
        if (!accepts && messages.getErrorCount() == 0) {
            var msg = "Failed to parse.";
            var finalFrame = parse.getFinalFrame();
            if (finalFrame != null) {
                msg += " Last successful match: " + finalFrame.toTrace() + ".";
            }
            messages.error(null, msg);
        }

        // It is now safe to quit the method if we want/need to.
        if (!accepts) {
            return results;
        }

        // Grab the LOC statistics.
        var loc = parse.getSource(LOCCount.class);
        results.setNumberOfLines(loc.getNumberOfLines());
        results.setNumberOfLinesWithCode(loc.getNumberOfLinesWithCode());
        results.setNumberOfLinesWithComments(loc.getNumberOfLinesWithComments());
        return results;
    }

    public Parse getParseSetup(Path file) {
        return getParseSetup(file, reader(file));
    }

    public Parse getParseSetup(Path file, Reader reader) {
        // Build the tokenisation stage.
        var source = new LOCCount(CobolTokens.getNewSource(file, reader, project));
        var parse = Parse.of(source);

        // Keep track of all tokens passing through here, if so requested.
        TokenTracker tokenTracker = null;
        if (keepingTrackOfTokens) {
            tokenTracker = new TokenTracker();
            parse.to(tokenTracker);
        }
        if (buildTrees) {
            parse.to(new KoopaTreeBuilder(project.getGrammar(), false));
        }
        return parse;
    }

    public void setKeepingTrackOfTokens(boolean keepingTrackOfTokens) {
        this.keepingTrackOfTokens = keepingTrackOfTokens;
    }

    public boolean isKeepingTrackOfTokens() {
        return keepingTrackOfTokens;
    }

    public void setBuildTrees(boolean buildTrees) {
        this.buildTrees = buildTrees;
    }

    public void setProject(CobolProject project) {
        this.project = project;
    }

    private boolean grabRemainingProgramText(CobolGrammar grammar, Stream stream, TokenTracker tracker) {
        var sawMoreProgramText = false;
        for (;;) {
            var d = stream.forward();
            // End-of-input ?
            if (d == null) {
                break;
            }
            // If we're tracking data, push them to the tracker.
            if (tracker != null) {
                tracker.push(d);
            }
            // Have we found more program text ?
            if ( !sawMoreProgramText
                 && d instanceof Token
                 && grammar.isProgramText((Token) d)
                 && !grammar.canBeSkipped((Token) d, null) )
            {
                sawMoreProgramText = true;
            }
            // Stop after we found program text, unless we're tracking all tokens.
            if (sawMoreProgramText && tracker == null) {
                break;
            }
        }
        return sawMoreProgramText;
    }

    private Token findTokenAt(Stream tail, Position best) {
        var d = tail.forward();
        Token first = null;
        for (;;) {
            if (d == null) {
                return first;
            }
            if (d instanceof Token) {
                var token = (Token) d;
                if (first == null) {
                    first = token;
                }
                if (token.getStart().compareTo(best) >= 0) {
                    return token;
                }
            }
            d = tail.forward();
        }
    }

    static Reader reader(Path file) {
    	try { return Files.newBufferedReader(file);	}
    	catch (IOException e) { throw new UncheckedIOException(e); }
    }

}
