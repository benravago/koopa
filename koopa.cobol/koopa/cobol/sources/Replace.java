package koopa.cobol.sources;

import static koopa.core.data.tags.AreaTag.COMMENT;

import java.util.LinkedList;
import java.util.List;

import java.util.logging.Logger;
import static java.util.logging.Level.*;

import koopa.cobol.grammar.preprocessing.CobolPreprocessingGrammar;
import koopa.cobol.parser.preprocessing.replacing.Replacing;
import koopa.cobol.parser.preprocessing.replacing.ReplacingPhrase;
import koopa.core.data.Data;
import koopa.core.data.Token;
import koopa.core.data.tags.SyntacticTag;
import koopa.core.parsers.Parse;
import koopa.core.sources.ChainingSource;
import koopa.core.sources.ListSource;
import koopa.core.sources.Source;
import koopa.core.sources.Sources;
import koopa.core.trees.KoopaTreeBuilder;
import koopa.core.trees.Tree;
import koopa.core.trees.Trees;

// TODO Extract common stuff with CopyInclude ?
public class Replace extends ChainingSource implements Source {

    private static final Logger LOGGER = Logger.getLogger("source.cobol.replace");

    private final CobolPreprocessingGrammar grammar;

    private final LinkedList<Data> pending = new LinkedList<>();

    /**
     * All REPLACE statements which got handled are tracked here, in syntax tree form.
     */
    private final List<Tree> handledReplaceStatements = new LinkedList<>();

    public Replace(Source source, CobolPreprocessingGrammar grammar) {
        super(source);
        this.grammar = grammar;
    }

    @Override
    protected Data nextElement() {
        for (;;) {
            if (!pending.isEmpty()) {
                return pending.removeFirst();
            }
            // Grab line from source.
            final LinkedList<Data> line = Sources.getLine(source);
            if (line == null) {
                return null;
            }
            // pending = line, up to first 'REPLACE'.
            anythingUpToReplaceBecomesPending(line);
            if (line.isEmpty()) {
                // No REPLACE was found.
                return pending.removeFirst();
            } else {
                // We have a REPLACE statement. For sure?
                // Let's grab all the data for the REPLACE statement.
                var replaceStatement = getReplaceStatement(line);
                if (replaceStatement == null) {
                    // We didn't get any data, which means the REPLACE statement couldn't be detected in full.
                    // So we just make all data seen so far pending, and carry on.
                    pending.addAll(line);
                    return pending.removeFirst();
                }
                // We should have a REPLACE statement, right?
                // Let's try getting the syntax tree.
                var replace = parseReplaceStatement(replaceStatement);
                if (replace == null) {
                    // We didn't get a syntax tree for some reason.
                    // The REPLACE statement was probably mal-formed.
                    // So we just make all data seen so far pending, and carry on.
                    pending.addAll(line);
                    return pending.removeFirst();
                }
                // We now definitely have a REPLACE statement.
                // Let's try setting everything up.
                var handlingIt = handleReplaceStatement(replace, replaceStatement, line);
                if (!handlingIt) {
                    // For some reason the REPLACE statement could not be handled.
                    // So we just make all data seen so far pending, and carry on.
                    pending.addAll(line);
                    return pending.removeFirst();
                }
            }
        }
    }

    private void anythingUpToReplaceBecomesPending(LinkedList<Data> line) {
        // "A REPLACE statement shall be preceded by a space except when it is the first statement in a compilation group."
        var canStartReplaceHere = true;
        for (;;) {
            if (line.isEmpty()) {
                return;
            }
            var d = line.getFirst();
            if (d instanceof Token) {
                var t = (Token) d;
                // Are we at a REPLACE ?
                if (canStartReplaceHere && "replace".equalsIgnoreCase(t.getText())) {
                    // Are we really ? E.g. no REPLACE100, or REPLACE-FOO ?
                    if (isSpace(line, 1)) {
                        if (LOGGER.isLoggable(FINER)) {
                            LOGGER.finer("Possible start of a REPLACE statement: " + t);
                        }
                        return;
                    }
                }
                // Are we at a space ?
                canStartReplaceHere = (t.getText().trim().length() == 0);
            }
            line.removeFirst();
            pending.add(d);
        }
    }

    private LinkedList<Data> getReplaceStatement(LinkedList<Data> line) {
        var replace = new LinkedList<Data>();
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
                    // Nope. So the REPLACE statement is incomplete.
                    // We rewind all tokens by adding them back to the line.
                    while (!replace.isEmpty()) {
                        line.addFirst(replace.removeLast());
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
                    // The entire pseudo-literal becomes part of the REPLACE statement.
                    replace.addAll(pseudoLiteral);
                } else {
                    // Woops. Seems we couldn't grab the full pseudo-literal.
                    // Which leaves the REPLACE statement incomplete.
                    // We rewind all tokens by adding them back to the line.
                    while (!replace.isEmpty()) {
                        line.addFirst(replace.removeLast());
                    }
                    // And return null.
                    return null;
                }
            } else {
                // We're not inside a pseudo-literal.
                // So we can take the next item and add it to the REPLACE statement.
                var d = line.removeFirst();
                replace.add(d);
                // Once we find a dot, we're done.
                if (d instanceof Token && ".".equals(((Token) d).getText())) {
                    return replace;
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

    private Tree parseReplaceStatement(LinkedList<Data> replaceStatement) {
        var replaceStatementSource = new ListSource(replaceStatement);
        var treeBuilder = new KoopaTreeBuilder(grammar);
        var parse = Parse.of(replaceStatementSource).to(treeBuilder);
        var accepts = grammar.replaceStatement().accepts(parse);
        return accepts ? treeBuilder.getTree() : null;
    }

    /**
     * Tries to set up handling of the REPLACE statement.
     * Tells whether it succeeded in doing so, or not.
     */
    private boolean handleReplaceStatement(Tree replace, LinkedList<Data> replaceStatement, LinkedList<Data> line) {
        if (replace.hasChild("off")) {
            var last = (replace.getDescendant("off", "last") != null);
            if (LOGGER.isLoggable(FINE)) {
                LOGGER.fine("Processing a REPLACE " + (last ? "LAST OFF" : "OFF") + " statement ...");
            }
            var data = new ReplacementData(false, !last, null);
            pending.add(replace);
            pending.add(data);
            if (!line.isEmpty()) {
                pending.addAll(line);
            }
            handledReplaceStatements.add(replace);
            return true;
        } else if (replace.hasChild("replacing")) {
            var also = (replace.getDescendant("replacing", "also") != null);
            if (LOGGER.isLoggable(FINE)) {
                LOGGER.fine("Processing a REPLACE " + (also ? "ALSO " : "") + "statement ...");
            }
            // Look for any REPLACING instructions which need to be activated.
            var replacements = getReplacements(replace);
            var data = new ReplacementData(true, !also, replacements);
            pending.add(replace);
            pending.add(data);
            if (!line.isEmpty()) {
                pending.addAll(line);
            }
            handledReplaceStatements.add(replace);
            return true;

        } else {
            // Something strange is afoot. Like someone messing with the grammar...
            LOGGER.severe("Processing an unkown REPLACE statement: " + replace.getAllText());
            return false;
        }
    }

    private List<ReplacingPhrase> getReplacements(Tree replace) {
        var instructions = Trees.getMatches(replace, "replacing","replacementInstruction" );
        if (instructions == null || instructions.isEmpty()) {
            return null;
        }
        var phrases = Replacing.allPhrasesFrom(instructions);
        if (LOGGER.isLoggable(FINE)) {
            LOGGER.fine("Replace defines replacements: " + phrases);
        }
        return phrases;
    }

    public List<Tree> getHandledDirectives() {
        return handledReplaceStatements;
    }

}
