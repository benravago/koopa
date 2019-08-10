package koopa.cobol.sources;

import static koopa.cobol.data.tags.CobolAreaTag.INDICATOR_AREA;
import static koopa.cobol.sources.SourceFormat.FIXED;
import static koopa.cobol.sources.SourceFormat.VARIABLE;
import static koopa.core.data.tags.AreaTag.COMMENT;
import static koopa.core.data.tags.AreaTag.PROGRAM_TEXT_AREA;
import static koopa.core.data.tags.AreaTag.SKIPPED;
import static koopa.core.data.tags.SyntacticTag.INCOMPLETE;
import static koopa.core.data.tags.SyntacticTag.END_OF_LINE;
import static koopa.core.data.tags.SyntacticTag.SEPARATOR;
import static koopa.core.data.tags.SyntacticTag.STRING;

import java.util.LinkedList;

import java.util.logging.Logger;
import static java.util.logging.Level.*;

import koopa.core.data.Data;
import koopa.core.data.Token;
import koopa.core.data.Tokens;
import koopa.core.sources.ChainingSource;
import koopa.core.sources.Source;
import koopa.core.sources.Sources;
import koopa.core.sources.TokenSeparationLogic;

public class ContinuationOfLines extends ChainingSource implements Source {

    private static final Logger LOGGER = Logger.getLogger("source.cobol.continuations");

    /**
     * This is a tag which may be applied to fixed indicators to show that they have been handled.
     *
     * This is needed as in the case of
     * {@linkplain ContinuationOfLines#handleClosedLiteralContinuation(LinkedList, LinkedList)}
     * the continuing line (which has the indicator) will be made pending,
     * but we should take care not to handle the indicator a second time.
     */
    private static enum StatusOfIndicator {
        HANDLED
    };

    private final LinkedList<Data> pending = new LinkedList<>();

    private final LinkedList<LinkedList<Data>> pendingLines = new LinkedList<>();

    public ContinuationOfLines(Source source) {
        super(source);
    }

    @Override
    protected Data nextElement() {
        for (;;) {
            if (!pending.isEmpty()) {
                return withoutInternalStatus(pending.removeFirst());
            }
            var line = getLogicalLine();
            if (line == null) {
                return null;
            }
            pending.addAll(line);
            return withoutInternalStatus(pending.removeFirst());
        }
    }

    /**
     * The data which gets return from this source should not be leaking tags used for internal processing.
     */
    private Data withoutInternalStatus(Data d) {
        if (d == null || !(d instanceof Token)) {
            return d;
        } else {
            return ((Token) d).withoutTags(StatusOfIndicator.HANDLED);
        }
    }

    private LinkedList<Data> getLogicalLine() {
        for (;;) {
            var line = getPendingLine();
            // No more lines ?
            if (line == null) {
                return null;
            }
            // If it's a blank line, we return it as is.
            if (isBlank(line)) {
                return line;
            }
            // Does the line have an incomplete literal, or one with a floating continuation indicator?
            if (hasIncompleteToken(line)) {
                // It does.
                if (LOGGER.isLoggable(FINER)) {
                    LOGGER.finer("Needs continuation: " + line);
                }
                // We need the next (non-empty, non-comment) source line to build the continuation.
                var continuation = grabNextSourceLine();
                if (continuation == null) {
                    // There isn't one ? That's bad, so we just bail out.
                    LOGGER.severe("No next source line, so can not continue " + "(wrong source format ?): " + line);
                    return line;
                }
                // Resolve the continuation, then make the entire line pending for another pass.
                pendingLines.addFirst(handleIncompleteToken(line, continuation));
                continue;
            }
            // The line itself does not say it needs to be continued.
            // But the next (non-empty, non-comment) source line may still choose to continue it.
            var nextSourceLine = grabNextSourceLine();
            var fixedIndicator = findFixedIndicator(nextSourceLine);
            if (isFixedContinuationIndicator(fixedIndicator)) {
                // It does act as a continuation.
                // We will resolve that continuation, then make the entire line pending for another pass.
                pendingLines.addFirst(handleFixedContinuation(fixedIndicator, line, nextSourceLine));
                continue;
            }
            // So the line neither wants to be continued, nor is continued.
            // We make the extra line we read pending.
            pendingLines.add(nextSourceLine);
            // Add we return the line we started with.
            return line;
        }
    }

    private LinkedList<Data> handleIncompleteToken(
        LinkedList<Data> continuedLine, LinkedList<Data> continuingLine)
    {
        var logicalLine = new LinkedList<Data>();
        var incomplete = shiftToIncompleteToken(continuedLine, logicalLine);
        assert (incomplete != null);
        if (LOGGER.isLoggable(FINER)) {
            LOGGER.finer("Incomplete token: " + incomplete);
        }
        if (isLiteralWithFloatingContinuationIndicator(incomplete)) {
            // Floating continuation indicator.
            return handleFloatingContinuationIndicator(logicalLine, incomplete, continuedLine, continuingLine);
        } else {
            // Incomplete string literal.
            return handleIncompleteStringLiteral(logicalLine, incomplete, continuedLine, continuingLine);
        }
    }

    private LinkedList<Data> handleFloatingContinuationIndicator(
        LinkedList<Data> logicalLine, Token incomplete, LinkedList<Data> continuedLine, LinkedList<Data> continuingLine)
    {
        // "In the case of continuation with [a] floating literal continuation indicator,
        //  the next line that is not a comment line or a blank line is the continuation line.
        //  The first nonspace character in the program-text area of the continuation line
        //  shall be a quotation symbol matching the quotation symbol used in the opening delimiter.
        //  The continuation starts with the character immediately after the quotation symbol in the continuation line."

        var skipped = new LinkedList<Data>();
        var firstNonBlank = shiftToFirstNonBlank(continuingLine, skipped);
        if (!firstNonBlank.hasTag(STRING)) {
            // We need a string to continue the floating one...
            // We'll ignore the continuation and try to carry on.
            // Restore the continuingLine...
            continuingLine.addFirst(firstNonBlank);
            while (!skipped.isEmpty()) {
                continuingLine.addFirst(skipped.removeLast());
            }
            pendingLines.add(continuingLine);
            // Restore the logicalLine...
            logicalLine.add(incomplete);
            logicalLine.addAll(continuedLine);
            return logicalLine;
        }
        if (LOGGER.isLoggable(FINER)) {
            LOGGER.finer("Continuing floating literal " + incomplete + " with " + firstNonBlank);
        }
        var positionOfFloatingIndicator = incomplete.getLength() - 2;
        var startOfLiteral =
            Tokens.subtoken(incomplete, 0, positionOfFloatingIndicator).withoutTags(INCOMPLETE);
        var floatingIndicator = // TODO Add floating indicator tag?
            Tokens.subtoken(incomplete, positionOfFloatingIndicator).withoutTags(PROGRAM_TEXT_AREA, INCOMPLETE).withTags(SKIPPED);
        var leadingQuote =
            Tokens.subtoken(firstNonBlank, 0, 1).withTags(SKIPPED).withoutTags(INCOMPLETE, PROGRAM_TEXT_AREA);
        var continuationOfLiteral =
            Tokens.subtoken(firstNonBlank, 1);
        logicalLine.addLast(startOfLiteral);
        logicalLine.addLast(floatingIndicator);
        shiftAllAndSkip(continuedLine, logicalLine);
        shiftAllAndSkip(skipped, logicalLine);
        logicalLine.addLast(leadingQuote);
        logicalLine.addLast(continuationOfLiteral);
        logicalLine.addAll(continuingLine);
        return logicalLine;
    }

    private LinkedList<Data> handleIncompleteStringLiteral(
        LinkedList<Data> logicalLine, Token incomplete, LinkedList<Data> continuedLine, LinkedList<Data> continuingLine)
    {
        // This can only occur in FIXED or VARIABLE format.
        var format = SourceFormat.forToken(incomplete);
        assert (format == FIXED || format == VARIABLE)
             : "Unexpected source format on incomplete string literal: " + incomplete;

        // The continuation line should have a fixed continuation indicator.
        var indicator = findFixedIndicator(continuingLine);
        if (!isFixedContinuationIndicator(indicator)) {
            // No continuation indicator?
            // That's unexpected, but not impossible.
            // The testsuite actually has incomplete string literals inside "pseudo text",
            // which don't require the same treatment as program text.

            // TODO No warning level ?
            if (LOGGER.isLoggable(INFO)) {
                LOGGER.info("Did not find a continuation for incomplete literal: " + incomplete);
            }
            handleMissingContinuation(logicalLine, incomplete, continuedLine, continuingLine);
            return logicalLine;
        }

        // 'The continuation line must contain a hyphen in the indicator area,
        //  and the first nonblank character must be a quotation mark.
        //  The continuation of the literal begins with the character immediately following the quotation mark.'

        // We know the indicator is already there, so we can cross that of the list.
        // Next is to find the first non-blank character.
        var skipped = new LinkedList<Data>();
        var continuation = shiftToFirstNonBlank(continuingLine, skipped);
        if (continuation == null || !continuation.hasTag(STRING)) {
            // Well, this is not according to spec.
            // We'll ignore the continuation and try to carry on.

            // TODO No warning level ?
            if (LOGGER.isLoggable(INFO)) {
                LOGGER.info("Did not find a string continuation for incomplete literal: " + incomplete);
            }
            // First restore the continuing line, then handle the problem.
            if (continuation != null) {
                continuingLine.addFirst(continuation);
            }
            while (!skipped.isEmpty()) {
                continuingLine.addFirst(skipped.removeLast());
            }
            handleMissingContinuation(logicalLine, incomplete, continuedLine, continuingLine);
            return logicalLine;
        }
        composeFullLiteral(format, logicalLine, incomplete, continuedLine, skipped, continuation, continuingLine);
        return logicalLine;
    }

    private void handleMissingContinuation(
        LinkedList<Data> logicalLine, Token incomplete, LinkedList<Data> continuedLine, LinkedList<Data> continuingLine)
    {
        // We will split of the opening quotation character and split the incomplete literal into tokens.
        // Then push everything we have, except for the next source line as the logical line.
        // TODO Verify this case with CobolSourcesValidationTest.

        // So, the leading quote...
        var openingQuote = Tokens.subtoken(incomplete, 0, 1).withTags(SEPARATOR).withoutTags(INCOMPLETE);
        logicalLine.add(openingQuote);
        // The tokens inside the incomplete literal...
        var tokens = TokenSeparationLogic.apply(Tokens.subtoken(incomplete, 1).withoutTags(INCOMPLETE));
        if (tokens != null && !tokens.isEmpty()) {
            logicalLine.addAll(tokens);
        }
        // The remainder of the line.
        logicalLine.addAll(continuedLine);
        continuedLine.clear();
        // The line we expected to have a continuation will be made
        // pending, as it may still contain something to be continued
        // itself.
        pendingLines.add(continuingLine);
    }

    private void composeFullLiteral(
        SourceFormat format, LinkedList<Data> logicalLine, Token incomplete, LinkedList<Data> continuedLine,
        LinkedList<Data> skipped, Token continuation, LinkedList<Data> continuingLine)
    {
        var leadingQuote =
            Tokens.subtoken(continuation, 0, 1).withTags(SKIPPED).withoutTags(INCOMPLETE, PROGRAM_TEXT_AREA);
        var continuationOfLiteral =
            Tokens.subtoken(continuation, 1);
        logicalLine.addLast(incomplete.withoutTags(INCOMPLETE));
        shiftAllAndSkip(continuedLine, logicalLine);
        while (!pendingLines.isEmpty()) {
            logicalLine.addAll(pendingLines.removeFirst());
        }
        shiftAllAndSkip(skipped, logicalLine);
        logicalLine.addLast(leadingQuote);
        logicalLine.addLast(continuationOfLiteral);
        logicalLine.addAll(continuingLine);
    }

    private LinkedList<Data> handleFixedContinuation(Token fixedIndicator,
        LinkedList<Data> continuedLine, LinkedList<Data> continuingLine)
    {
        // 'If there is a hyphen in the indicator area of a line,
        //  the first nonblank character of the continuation line
        //  immediately follows the last nonblank character of
        //  the continued line without an intervening space.'

        // Easy. Except that:
        // 'If an alphanumeric or national literal that is to be continued on
        //  the next line has as its last character a quotation mark in column 72,
        //  the continuation line must start with two consecutive quotation marks.
        //  This will result in a single quotation mark as part of the value of the literal.'

        // (Source: Enterprise COBOL for z/OS, Language Reference, Version 4 Release 2, p.56)

        // So let's check that first.
        var lastToken = findLastProgramTextOtherThenEOLN(continuedLine);
        if (lastToken == null) {
            // Are we continuing an empty line? That shouldn't happen, right?
            // I guess we'll ignore the issue and the continuation then.
            if (LOGGER.isLoggable(INFO)) {
                LOGGER.info("Unexpected continuation of an empty line: " + fixedIndicator);
            }
            pendingLines.add(continuingLine);
            return continuedLine;
        }
        if (!lastToken.hasTag(STRING)) {
            // So we're not in a special case, and just need to handle blanks.
            return handleBasicNonBlankContinuation(continuedLine, continuingLine);
        } else {
            // It is a string literal.
            return handleClosedLiteralContinuation(continuedLine, continuingLine);
        }
    }

    private LinkedList<Data> handleBasicNonBlankContinuation(
        LinkedList<Data> continuedLine, LinkedList<Data> continuingLine)
    {
        var skipped = new LinkedList<Data>();
        var lastNonBlank = unshiftToLastNonBlank(continuedLine, skipped);
        var firstNonBlank = shiftToFirstNonBlank(continuingLine, skipped);
        if (LOGGER.isLoggable(FINER)) {
            LOGGER.finer("Continuing " + lastNonBlank + " with " + firstNonBlank);
        }
        var fullLine = new LinkedList<Data>();
        fullLine.addAll(continuedLine);
        fullLine.add(lastNonBlank);
        shiftAllAndSkip(skipped, fullLine);
        fullLine.add(firstNonBlank);
        fullLine.addAll(continuingLine);
        return fullLine;
    }

    private LinkedList<Data> handleClosedLiteralContinuation(
        LinkedList<Data> continuedLine, LinkedList<Data> continuingLine)
    {
        var endOfContinuedLine = new LinkedList<Data>();
        var closedLiteral = unshiftToLastProgramTextOtherThenEOLN(continuedLine, endOfContinuedLine);

        // Now, the COBOL z/OS spec states:
        // 'If an alphanumeric or national literal that is to be continued on
        // the next line has as its last character a quotation mark in column 72,
        // the continuation line must start with two consecutive quotation marks.
        // This will result in a single quotation mark as part of the value of the literal.'

        // (Source: Enterprise COBOL for z/OS, Language Reference, Version 4 Release 2, p.56)

        // I am going to ignore the condition on column 72 for two reasons:
        // 1. I don't want to bother with the complexity of tabs in this place.
        // 2. Lines may exceed column 72 in VARIABLE formatting.
        // I may get back to this at some point if it proves to be causing problems.
        var startOfContinuingLine = new LinkedList<Data>();
        var firstNonBlank = shiftToFirstNonBlank(continuingLine, startOfContinuingLine);

        // The first non-blank, according to the spec, must start with two consecutive quotation marks.
        // Which means that it should appear here as a string of length two, given how the token separation works.
        if (!firstNonBlank.hasTag(STRING) || firstNonBlank.getLength() != 2) {
            // So it does not adhere to the spec. We'll ignore the continuation and try to carry on.
            if (LOGGER.isLoggable(INFO)) { // TODO No warning level ?
                LOGGER.info("Did not find a string continuation for closed literal: " + closedLiteral);
            }
            continuedLine.add(closedLiteral);
            continuedLine.addAll(endOfContinuedLine);
            // Despite failing to handle the continuation we will still mark the continuation indicator as having been handled,
            // just so we don't try to handle it again.
            markFixedIndicatorAsHandled(startOfContinuingLine);
            continuingLine.addFirst(firstNonBlank);
            while (!startOfContinuingLine.isEmpty()) {
                continuingLine.addFirst(startOfContinuingLine.removeLast());
            }
            pendingLines.add(continuingLine);
            return continuedLine;
        }

        // Now, because the initial quote was meant to be discarded,
        // this means that the entire line has been separated in the wrong way.
        // Which means we have to redo that separation, with the leading quote removed.
        var firstQuote =
            Tokens.subtoken(firstNonBlank, 0, 1).withTags(SKIPPED).withoutTags(INCOMPLETE, PROGRAM_TEXT_AREA);
        var secondQuote =
            Tokens.subtoken(firstNonBlank, 1).withoutTags(PROGRAM_TEXT_AREA);
        var endOfContinuingLine = new LinkedList<Data>();
        var lastText = unshiftToLastProgramTextOtherThenEOLN(continuingLine, endOfContinuingLine);
        if (lastText != null) {
            continuingLine.addLast(lastText);
        }
        continuingLine.addFirst(secondQuote);
        var format = SourceFormat.forToken(firstNonBlank);
        var continuingProgramText =
            Tokens.join(tokens(continuingLine), format, PROGRAM_TEXT_AREA);
        if (LOGGER.isLoggable(FINER)) {
            LOGGER.finer("Retokenizing: " + continuingProgramText);
        }
        var continuingTokens = new LinkedList<Token>();
        continuingTokens.addAll(TokenSeparationLogic.apply(continuingProgramText));
        var furtherLiteral = continuingTokens.removeFirst();
        continuedLine.addLast(closedLiteral);
        shiftAllAndSkip(endOfContinuedLine, continuedLine);
        markFixedIndicatorAsHandled(startOfContinuingLine);
        continuingLine.clear();
        shiftAllAndSkip(startOfContinuingLine, continuingLine);
        continuingLine.add(firstQuote);
        continuingLine.add(furtherLiteral);
        continuingLine.addAll(continuingTokens);
        continuingLine.addAll(endOfContinuingLine);
        pendingLines.add(continuingLine);
        return continuedLine;
    }

    private LinkedList<Token> tokens(LinkedList<Data> data) {
        var tokens = new LinkedList<Token>();
        for (var d : data) {
            if (d instanceof Token) {
                tokens.add((Token) d);
            }
        }
        return tokens;
    }

    private Token shiftToIncompleteToken(LinkedList<Data> line, LinkedList<Data> skipped) {
        for (;;) {
            if (line.isEmpty()) {
                return null;
            }
            var d = line.removeFirst();
            if (isIncompleteToken(d)) {
                return (Token) d;
            } else {
                skipped.add(d);
            }
        }
    }

    private void shiftAllAndSkip(LinkedList<Data> line, LinkedList<Data> skipped) {
        while (!line.isEmpty()) {
            var d = line.removeFirst();
            // TODO I'm removing TEXT from SKIPPED. Seems ok ?
            if (d instanceof Token) { // && ((Token) d).hasTag(PROGRAM_TEXT_AREA))
                skipped.addLast(((Token) d).withTags(SKIPPED).withoutTags(PROGRAM_TEXT_AREA));
            } else {
                skipped.addLast(d);
            }
        }
    }

    private Token shiftToFirstNonBlank(LinkedList<Data> line, LinkedList<Data> skipped) {
        for (;;) {
            if (line.isEmpty()) {
                return null;
            }
            var d = line.removeFirst();
            if (d instanceof Token) {
                var t = (Token) d;
                // TODO Refer to Grammar methods for this.
                // TODO Add a Grammar.isBlank(Token) ?
                // TODO Use WHITESPACE text instead of trim ?
                if (t.hasTag(PROGRAM_TEXT_AREA) && !t.hasTag(COMMENT) && t.getText().trim().length() > 0) {
                    return t;
                }
            }
            skipped.add(d);
        }
    }

    private Token unshiftToLastNonBlank(LinkedList<Data> line, LinkedList<Data> unshifted) {
        for (;;) {
            if (line.isEmpty()) {
                return null;
            }
            var d = line.removeLast();
            if (d instanceof Token) {
                final Token t = (Token) d;
                // TODO Refer to Grammar methods for this.
                // TODO Add a Grammar.isBlank(Token) ?
                // TODO Use WHITESPACE text instead of trim ?
                if (t.hasTag(PROGRAM_TEXT_AREA) && !t.hasTag(COMMENT) && t.getText().trim().length() > 0) {
                    return t;
                }
            }
            unshifted.addFirst(d);
        }
    }

    private Token unshiftToLastProgramTextOtherThenEOLN(LinkedList<Data> line, LinkedList<Data> unshifted) {
        for (;;) {
            if (line.isEmpty()) {
                return null;
            }
            var d = line.removeLast();
            if (d instanceof Token) {
                var t = (Token) d;
                // TODO Refer to Grammar methods for this.
                if (t.hasTag(PROGRAM_TEXT_AREA) && !t.hasAnyTag(COMMENT, END_OF_LINE)) {
                    return t;
                }
            }
            unshifted.addFirst(d);
        }
    }

    private LinkedList<Data> grabNextSourceLine() {
        // Check the pending lines for a source line.
        var i = pendingLines.listIterator();
        while (i.hasNext()) {
            var nextLine = i.next();
            if (nextLine == null || !isBlank(nextLine)) {
                // Hey, we found one. Remove it from the pending lines and return it.
                i.remove();
                return nextLine;
            }
        }
        // If there isn't one in the pending lines, then grab more from the source.
        for (;;) {
            var nextLine = Sources.getLine(source);
            if (nextLine == null || !isBlank(nextLine)) {
                return nextLine;
            }
            pendingLines.addLast(nextLine);
        }
    }

    private LinkedList<Data> getPendingLine() {
        if (pendingLines.isEmpty()) {
            return Sources.getLine(source);
        } else {
            return pendingLines.removeFirst();
        }
    }

    private boolean hasIncompleteToken(LinkedList<Data> line) {
        var i = line.descendingIterator();
        while (i.hasNext()) {
            if (isIncompleteToken(i.next())) {
                return true;
            }
        }
        return false;
    }

    private Token findFixedIndicator(LinkedList<Data> line) {
        if (line == null) {
            return null;
        }
        for (var d : line) {
            if (isFixedIndicator(d)) {
                return (Token) d;
            }
        }
        return null;
    }

    private Token findLastProgramTextOtherThenEOLN(LinkedList<Data> line) {
        var i = line.descendingIterator();
        while (i.hasNext()) {
            var d = i.next();
            if (!(d instanceof Token)) {
                continue;
            }
            var t = (Token) d;
            if (t.hasTag(PROGRAM_TEXT_AREA) && !t.hasAnyTag(COMMENT, END_OF_LINE)) {
                return t;
            }
        }
        return null;
    }

    private boolean isBlank(LinkedList<Data> line) {
        for (var d : line) {
            if (d instanceof Token) {
                var t = (Token) d;
                // TODO Refer to Grammar methods for this.
                // TODO Add a Grammar.isBlank(Token) ?
                if (t.hasTag(PROGRAM_TEXT_AREA) && !t.hasTag(COMMENT) && t.getText().trim().length() > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isIncompleteToken(Data d) {
        if (d == null || !(d instanceof Token)) {
            return false;
        }
        var t = (Token) d;
        // TODO Use Grammar methods.
        return t.hasTags(PROGRAM_TEXT_AREA, INCOMPLETE) && !t.hasTag(COMMENT);
    }

    private boolean isFixedIndicator(Data d) {
        return d != null && d instanceof Token && ((Token) d).hasTag(INDICATOR_AREA);
    }

    /**
     * If the token {@link #isFixedContinuationIndicator(Token)} whose contents is a dash
     * and which has not been {@linkplain StatusOfIndicator#HANDLED}.
     */
    private boolean isFixedContinuationIndicator(final Token t) {
        return isFixedIndicator(t) && !t.hasTag(StatusOfIndicator.HANDLED) && "-".equals(t.getText());
    }

    private boolean isLiteralWithFloatingContinuationIndicator(Token t) {
        if (t == null) return false;
        var n = t.getLength();
        if (n < 2) return false;
        var l1 = t.charAt(n - 1);
        var l2 = t.charAt(n - 2);
        return '-' == l1 && (l2 == '"' || l2 == '\'');
    }

    /**
     * Scan the line for any token which {@link #isFixedContinuationIndicator(Token)}
     * and mark those as {@linkplain StatusOfIndicator#HANDLED}.
     */
    private void markFixedIndicatorAsHandled(LinkedList<Data> line) {
        var tmp = new LinkedList<Data>();
        while (!line.isEmpty()) {
            var d = line.removeFirst();
            if (d instanceof Token && isFixedContinuationIndicator((Token) d)) {
                tmp.addLast(((Token) d).withTags(StatusOfIndicator.HANDLED));
            } else {
                tmp.addLast(d);
            }
        }
        line.addAll(tmp);
    }

}
