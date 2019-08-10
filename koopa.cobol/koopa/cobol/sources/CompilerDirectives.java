package koopa.cobol.sources;

import static koopa.cobol.data.tags.CobolTag.SOURCE_FORMAT_DIRECTIVE;
import static koopa.cobol.data.tags.CobolTag.SOURCE_LISTING_DIRECTIVE;
import static koopa.core.data.tags.AreaTag.COMPILER_DIRECTIVE;
import static koopa.core.data.tags.AreaTag.PROGRAM_TEXT_AREA;

import java.util.LinkedList;
import java.util.List;

import java.util.logging.Logger;
import static java.util.logging.Level.*;

import koopa.cobol.grammar.directives.DirectivesGrammar;
import koopa.core.data.Data;
import koopa.core.data.Token;
import koopa.core.data.tags.SyntacticTag;
import koopa.core.parsers.Parse;
import koopa.core.sources.ChainingSource;
import koopa.core.sources.ListSource;
import koopa.core.sources.Source;
import koopa.core.sources.Sources;
import koopa.core.sources.TagAll;
import koopa.core.sources.TokenSeparator;
import koopa.core.trees.KoopaTreeBuilder;
import koopa.core.trees.Tree;

public class CompilerDirectives extends ChainingSource implements Source {

    private static final Logger LOGGER = Logger.getLogger("source.cobol.compiler_directives");

    private final DirectivesGrammar grammar;

    private SourceFormat format;

    private final LinkedList<Data> pending = new LinkedList<>();

    private final List<Tree> handled = new LinkedList<>();

    public CompilerDirectives(Source source, SourceFormat initialFormat) {
        super(source);
        this.format = initialFormat;
        this.grammar = DirectivesGrammar.instance();
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
            // Check if it contains a compiler directive.
            var directive = tryToParseCompilerDirective(line);
            if (directive == null) {
                // If not, mark it all with the current active source format, and start returning that.
                return nextFrom(tagged(line, format));
            } else {
                // If there is one, handle it, and start returning the result.
                if (LOGGER.isLoggable(FINER)) {
                    LOGGER.finer("Found a compiler directive in: " + line);
                }
                return nextFrom(handleCompilerDirective(directive, line));
            }
        }
    }

    private Tree tryToParseCompilerDirective(LinkedList<Data> line) {
        // TODO Speed: can we reuse the sources?
        var lineSource = new ListSource(line);
        // TODO Program-Area splits ?
        // TODO Make TagAll (work on) a Source of Data ?
        var tagAll = new TagAll(lineSource, format, PROGRAM_TEXT_AREA);
        var tokenSeparator = new TokenSeparator(tagAll);
        // var source = tokenSeparator;
        var directive = grammar.directive();
        var treeBuilder = new KoopaTreeBuilder(grammar);
        var parse = Parse.of(tokenSeparator).to(treeBuilder);
        var accepts = directive.accepts(parse);
        return accepts ? treeBuilder.getTree() : null;
    }

    private LinkedList<Data> handleCompilerDirective(Tree directive, LinkedList<Data> line) {
        handled.add(directive);
        var isoSourceFormat = directive.getDescendant("iso", "instruction", "source", "format");
        if (isoSourceFormat != null) {
            var value = isoSourceFormat.getAllText().toUpperCase();
            var declaredFormat = SourceFormat.fromName(value);
            if (LOGGER.isLoggable(FINER)) {
                LOGGER.finer("ISO SOURCE FORMAT " + declaredFormat);
            }
            if (declaredFormat != null) {
                return sourceformatDirective(line, declaredFormat);
            } else {
                return compilerDirective(line);
            }
        }
        var mfSet = directive.getDescendant("mf", "set");
        if (mfSet != null) {
            SourceFormat newFormat = null;
            var sourceformats = mfSet.getChildren("sourceformat");
            for (var t : sourceformats) {
                var rawValue = t.getChild("parameter").getAllText();
                var value = rawValue.substring(1, rawValue.length() - 1).toUpperCase();
                var declaredFormat = SourceFormat.fromName(value);
                if (declaredFormat != null) {
                    newFormat = declaredFormat;
                }
            }
            if (LOGGER.isLoggable(FINER)) {
                LOGGER.finer("MF SET SOURCEFORMAT " + newFormat);
            }
            if (newFormat != null) {
                return sourceformatDirective(line, newFormat);
            } else {
                return compilerDirective(line);
            }
        }
        if (directive.hasChild("listing")) {
            return sourceListingDirective(line);
        }
        return compilerDirective(line);
    }

    private LinkedList<Data> sourceformatDirective(LinkedList<Data> line, SourceFormat newFormat) {
        return tagged(line, newFormat, COMPILER_DIRECTIVE, SOURCE_FORMAT_DIRECTIVE);
    }

    private LinkedList<Data> compilerDirective(LinkedList<Data> line) {
        return tagged(line, format, COMPILER_DIRECTIVE);
    }

    private LinkedList<Data> sourceListingDirective(LinkedList<Data> line) {
        return tagged(line, format, COMPILER_DIRECTIVE, SOURCE_LISTING_DIRECTIVE);
    }

    private LinkedList<Data> tagged(LinkedList<Data> data, SourceFormat newFormat, Object... tags) {
        var tagged = new LinkedList<Data>();
        for (var d : data) {
            if (d instanceof Token) {
                var t = (Token) d;
                if (t.hasTag(SyntacticTag.END_OF_LINE)) {
                    tagged.add(t.withTags(newFormat));
                } else {
                    tagged.add(t.withTags(format).withTags(tags));
                }
            } else {
                tagged.add(d);
            }
        }
        format = newFormat;
        return tagged;
    }

    private Data nextFrom(LinkedList<Data> data) {
        pending.addAll(data);
        return pending.removeFirst();
    }

    public List<Tree> getHandledDirectives() {
        return handled;
    }

}
