package koopa.cobol.sources;

import static koopa.core.data.tags.AreaTag.COMMENT;

import java.nio.file.Files;

import java.io.Reader;
import java.io.IOException;

import java.util.List;
import java.util.LinkedList;

import java.util.logging.Logger;
import static java.util.logging.Level.*;

import koopa.cobol.CobolProject;
import koopa.cobol.grammar.preprocessing.CobolPreprocessingGrammar;
import koopa.cobol.parser.preprocessing.replacing.Replacing;
import koopa.cobol.parser.preprocessing.replacing.ReplacingPhrase;
import koopa.core.data.Data;
import koopa.core.data.Replaced;
import koopa.core.data.Token;
import koopa.core.data.tags.AreaTag;
import koopa.core.data.tags.SyntacticTag;
import koopa.core.parsers.Parse;
import koopa.core.sources.AsReplacing;
import koopa.core.sources.ChainingSource;
import koopa.core.sources.LineSplitter;
import koopa.core.sources.ListSource;
import koopa.core.sources.Source;
import koopa.core.sources.Sources;
import koopa.core.sources.StackOfSources;
import koopa.core.trees.KoopaTreeBuilder;
import koopa.core.trees.Tree;
import koopa.core.trees.Trees;
import koopa.core.util.LineEndings;

public class CopyInclude extends ChainingSource implements Source {

    private static final Logger LOGGER = Logger.getLogger("source.cobol.copy_include");

    private final CobolPreprocessingGrammar grammar;
    private final CobolProject project;
    private final StackOfSources inputStack;

    private final LinkedList<Data> pending = new LinkedList<>();

    /**
     * All COPY statements which got handled are tracked here, in syntax tree form.
     */
    private final List<Tree> handledCopyStatements = new LinkedList<>();

    public CopyInclude(Source source, CobolPreprocessingGrammar grammar, CobolProject project, StackOfSources stack) {
        super(source);
        this.project = project;
        this.inputStack = stack;
        this.grammar = grammar;
    }

    @Override
    protected Data nextElement() {
        for (;;) {
            if (!pending.isEmpty()) {
                return pending.removeFirst();
            }
            // Grab line from source.
            var line = Sources.getLine(source);
            if (line == null) {
                return null;
            }
            // pending = line, up to first 'COPY'.
            anythingUpToCopyBecomesPending(line);
            if (line.isEmpty()) {
                // No COPY was found.
                return pending.removeFirst();
            } else {
                // We have a COPY statement. For sure?
                // Let's grab all the data for the COPY statement.
                var copyStatement = getCopyStatement(line);
                if (copyStatement == null) {
                    // We didn't get any data, which means the COPY statement couldn't be detected in full.
                    // So we just make all data seen so far pending, and carry on.
                    pending.addAll(line);
                    return pending.removeFirst();
                }
                // TODO Resolve any continuations.
                // We should have a COPY statement, right?
                // Let's try getting the syntax tree.
                var copy = parseCopyStatement(copyStatement);
                if (copy == null) {
                    // We didn't get a syntax tree for some reason.
                    // The COPY statement was probably mal-formed.
                    // So we just make all data seen so far pending, and carry on.
                    pending.addAll(line);
                    return pending.removeFirst();
                }
                // We now definitely have a COPY statement.
                // Let's try setting everything up.
                var handlingIt = handleCopyStatement(copy, copyStatement, line);
                if (!handlingIt) {
                    // For some reason the COPY statement could not be handled.
                    // So we just make all data seen so far pending, and carry on.
                    pending.addAll(copyStatement);
                    pending.addAll(line);
                    return pending.removeFirst();
                } else {
                    pending.add(copy);
                }
            }
        }
    }

    private void anythingUpToCopyBecomesPending(LinkedList<Data> line) {
        // "A COPY statement shall be preceded by a space except when it is the first statement in a compilation group."
        var canStartCopyHere = true;
        for (;;) {
            if (line.isEmpty()) {
                return;
            }
            var d = line.getFirst();
            if (d instanceof Token) {
                var t = (Token) d;
                // Did we find a COPY in the program text area, which is not part of a bigger word?
                if (canStartCopyHere && t.hasTag(AreaTag.PROGRAM_TEXT_AREA) && "copy".equalsIgnoreCase(t.getText())) {
                    // Did we really? E.g. no COPY100, or COPY-FOO?
                    if (isSpace(line, 1)) {
                        if (LOGGER.isLoggable(FINER)) {
                            LOGGER.finer("Possible start of a COPY statement: " + t);
                        }
                        // OK then!
                        return;
                    }
                }
                // Are we at a space ?
                canStartCopyHere = (t.getText().trim().length() == 0);
            }
            line.removeFirst();
            pending.add(d);
        }
    }

    private LinkedList<Data> getCopyStatement(LinkedList<Data> line) {
        var copy = new LinkedList<Data>();
        for (;;) {
            // Do we need to read another line ?
            if (line.isEmpty()) {
                // Yes, we do.
                var nextLine = Sources.getLine(source);
                // Did we get another line ?
                if (nextLine != null) {
                    // Yes, we did.
                    line.addAll(nextLine);
                } else {
                    // Nope. So the COPY statement is incomplete.
                    // We rewind all tokens by adding them back to the line.
                    while (!copy.isEmpty()) {
                        line.addFirst(copy.removeLast());
                    }
                    // And return null.
                    return null;
                }
            }
            // Are we at the start of a pseudo-literal ?
            if (atPseudoLiteral(line)) {
                // Yes, we are. Let's grab all data for that pseudo-literal.
                var pseudoLiteral = getPseudoLiteral(line);
                if (pseudoLiteral != null) {
                    // The entire pseudo-literal becomes part of the COPY statement.
                    copy.addAll(pseudoLiteral);
                } else {
                    // Woops. Seems we couldn't grab the full pseudo-literal.
                    // Which leaves the COPY statement incomplete.
                    // We rewind all tokens by adding them back to the line.
                    while (!copy.isEmpty()) {
                        line.addFirst(copy.removeLast());
                    }
                    // And return null.
                    return null;
                }
            } else {
                // We're not inside a pseudo-literal.
                // So we can take the next item and add it to the copy statement.
                var d = line.removeFirst();
                copy.add(d);
                // Once we find a dot, we're done.
                if (d instanceof Token && ".".equals(((Token) d).getText())) {
                    return copy;
                }
            }
        }
    }

    private boolean atPseudoLiteral(LinkedList<Data> line) {
        return isEqualsSign(line, 0) && isEqualsSign(line, 1);
    }

    private LinkedList<Data> getPseudoLiteral(LinkedList<Data> line) {
        var pseudoLiteral = new LinkedList<Data>();
        pseudoLiteral.add(line.removeFirst());
        pseudoLiteral.add(line.removeFirst());
        for (;;) {
            // Do we need more data ?
            if (line.isEmpty()) {
                // Yes, we do.
                var nextLine = Sources.getLine(source);
                // Did we get another line ?
                if (nextLine != null) {
                    // Yes, we did.
                    line.addAll(nextLine);
                } else {
                    // Nope. So the pseudo-literal is incomplete.
                    // We rewind all tokens by adding them back to the line.
                    while (!pseudoLiteral.isEmpty()) {
                        line.addFirst(pseudoLiteral.removeLast());
                    }
                    // And return null.
                    return null;
                }
            }
            if (atEndOfPseudoLiteral(line)) {
                pseudoLiteral.add(line.removeFirst());
                pseudoLiteral.add(line.removeFirst());
                return pseudoLiteral;
            }
            pseudoLiteral.add(line.removeFirst());
        }
    }

    private boolean atEndOfPseudoLiteral(LinkedList<Data> line) {
        return isEqualsSign(line, 0) && isEqualsSign(line, 1) && !isEqualsSign(line, 2);
    }

    private boolean isEqualsSign(LinkedList<Data> line, int index) {
        if (index >= line.size()) {
            return false;
        }
        var a = line.get(index);
        return a != null && a instanceof Token && !((Token) a).hasTag(COMMENT) && ((Token) a).getText().equals("=");
    }

    private boolean isSpace(LinkedList<Data> line, int index) {
        while (index < line.size()) {
            var d = line.get(index);
            if (d instanceof Token) {
                return ((Token) d).hasTag(SyntacticTag.WHITESPACE);
            }
            index += 1;
        }

        return true;
    }

    private Tree parseCopyStatement(LinkedList<Data> copyStatement) {
        var copyStatementSource = new ListSource(copyStatement);
        // There may be continuations which need to be resolved before we can parse it.
        var continuationOfLines = new ContinuationOfLines(copyStatementSource);
        var treeBuilder = new KoopaTreeBuilder(grammar);
        var parse = Parse.of(continuationOfLines).to(treeBuilder);
        var accepts = grammar.copyStatement().accepts(parse);
        return accepts ? treeBuilder.getTree() : null;
    }

    /**
     * Tries to set up handling of the COPY statement.
     * Tells whether it succeeded in doing so, or not.
     *
     * @param line
     */
    private boolean handleCopyStatement(Tree copy, LinkedList<Data> copyStatement, LinkedList<Data> line) {
        if (LOGGER.isLoggable(FINE)) {
            LOGGER.fine("Processing a COPY statement");
        }
        var textName = Trees.getAllText(copy,"textName");
        var libraryName = Trees.getAllText(copy,"libraryName");
        var splitter = inputStack.getSource(LineSplitter.class);
        var file = (splitter == null) ? null : splitter.getFile();
        if (LOGGER.isLoggable(FINE)) {
            if (libraryName == null) {
                if (file == null) {
                    LOGGER.fine("Looking for copybook " + textName);
                } else {
                    LOGGER.fine("Looking for copybook " + textName + " relative to " + file);
                }
            } else {
                if (file == null) {
                    LOGGER.fine("Looking for copybook " + textName + " in library " + libraryName);
                } else {
                    LOGGER.fine("Looking for copybook " + textName + " in library " + libraryName + " relative to " + file);
                }
            }
        }
        var copybook = project.locateCopybook(textName, libraryName, file);
        if (copybook == null) {
            LOGGER.severe("Missing copybook " + textName + " in " + libraryName);
            return false;
        }
        if (LOGGER.isLoggable(FINE)) {
            LOGGER.fine("Found copybook at " + copybook);
        }
        Reader copybookReader;
        try {
            copybookReader = Files.newBufferedReader(copybook);
        } catch (IOException e) {
            LOGGER.severe("IOException while opening copybook " + copy);
            return false;
        }

        // Look for any REPLACING instructions which need to be activated.
        var replacements = getReplacements(copy);

        // We now have to set up the sources, such that the copybook gets included correctly.
        // We have three sets of data hanging around:
        // * The list of pending tokens.
        // * The tokens in the copybook.
        // * The remainder of the line following the COPY statement.

        // Given that we're using a stack of sources as the main input
        // we have to push these sources in reverse order
        // so that the tokens will appear in the right order.

        // We can leave the pending tokens out though,
        // as they be next in line to be read from here anyway.

        // If there are replacements to be activated, we have to make sure
        // we mark where they start and stop in the token stream.

        // This sets up the remainder of the line.
        if (!line.isEmpty()) {
            var remainderOfLine = new ListSource(line);
            inputStack.push(remainderOfLine);
        }
        // Mark the end of any REPLACING instructions.
        if (replacements != null) {
            var turnOffReplacements = new ListSource(new ReplacementData(false, false, replacements));
            inputStack.push(turnOffReplacements);
        }
        // This sets up the copybook as a source.
        var lineEndings = LineEndings.getChoices();
        var lineSplitter = new LineSplitter(copybook, copybookReader, lineEndings);
        // This marks all tokens coming from the copybook as being replacements for the COPY statement.
        var asReplacing = new AsReplacing(lineSplitter, replaced(copy));
        inputStack.push(asReplacing);
        // Mark the start of any REPLACING instructions.
        if (replacements != null) {
            var turnOnReplacements = new ListSource(new ReplacementData(true, false, replacements));
            inputStack.push(turnOnReplacements);
        }
        handledCopyStatements.add(copy);
        if (LOGGER.isLoggable(FINE)) {
            LOGGER.fine("Set up expansion of " + copybook);
        }
        return true;
    }

    private Replaced replaced(Tree copy) {
        var start = copy.getStartPosition();
        assert (start != null);
        var end = copy.getEndPosition();
        assert (end != null);
        var context = copy.getStartToken().getReplaced();
        return new Replaced(start, end, context);
    }

    private List<ReplacingPhrase> getReplacements(Tree copy) {
        var instructions = Trees.getMatches(copy, "replacing","replacementInstruction" );
        if (instructions == null || instructions.isEmpty()) {
            return null;
        }
        var phrases = Replacing.allPhrasesFrom(instructions);
        if (LOGGER.isLoggable(FINE)) {
            LOGGER.fine("Copy defines replacements: " + phrases);
        }
        return phrases;
    }

    public List<Tree> getHandledDirectives() {
        return handledCopyStatements;
    }

}
