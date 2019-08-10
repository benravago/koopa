package koopa.core.parsers.combinators;

import java.util.Set;

import koopa.core.parsers.Parse;
import koopa.core.parsers.ParserCombinator;
import koopa.core.trees.Tree;
import koopa.core.trees.TreeSource;

/**
 * ...
 */
public class MatchTree extends UnaryParserDecorator {

    private final String namespace;
    private final String name;

    public MatchTree(String namespace, String name, ParserCombinator parser) {
        super(parser);
        this.namespace = namespace;
        assert (name != null);
        this.name = name;
    }

    @Override
    public boolean matches(Parse parse) {
        var stream = parse.getStream();
        var d = stream.forward();
        if (d == null || !(d instanceof Tree)) {
            if (parse.getTrace().isEnabled()) {
                parse.getTrace().add(toString() + " ? no; not a Tree: " + d);
            }
            return false;
        }
        var t = (Tree) d;
        if (!t.isNode()) {
            if (parse.getTrace().isEnabled()) {
                parse.getTrace().add(toString() + " ? no; not a Tree node: " + t);
            }
            return false;
        }
        if (!name.equals(t.getName())) {
            if (parse.getTrace().isEnabled()) {
                parse.getTrace().add(toString() + " ? no; wrong name: " + t);
            }
            return false;
        }
        if (namespace != null && !namespace.equals(t.getNamespace())) {
            if (parse.getTrace().isEnabled()) {
                parse.getTrace().add(toString() + " ? no; wrong namespace: " + t);
            }
            return false;
        }
        if (parser != null) {
            var source = new TreeSource(t);
            var treeParse = parse.ofNested(source);
            if (parse.getTrace().isEnabled()) {
                parse.getTrace().indent(toString() + " ?");
            }
            var accepts = parser.accepts(treeParse);
            if (!accepts) {
                if (parse.getTrace().isEnabled()) {
                    parse.getTrace().dedent(toString() + " ? no");
                }
                return false;
            }
            var treeStream = treeParse.getStream();
            // No need to bookmark/forward/rewind here, as we're done streaming.
            // A simple forward should be okay.
            var atEnd = (treeStream.forward() == null);
            if (!atEnd) {
                if (parse.getTrace().isEnabled()) {
                    parse.getTrace().dedent(toString() + " ? no, not at end of tree");
                }
                return false;
            }
            if (parse.getTrace().isEnabled()) {
                parse.getTrace().dedent(toString() + " ? yes");
            }
            return true;
        }
        if (parse.getTrace().isEnabled()) {
            parse.getTrace().add(toString() + " ? yes; was: " + t);
        }
        return true;
    }

    /**
     * This parser ..., and so does not contribute any keywords.
     */
    @Override
    public void addAllKeywordsInScopeTo(Set<String> keywords) {}

    /**
     * This parser ..., and so does not contribute any keywords.
     */
    @Override
    public void addAllLeadingKeywordsTo(Set<String> keywords) {}

    /**
     * This parser ..., and so does not contribute any keywords.
     */
    @Override
    public boolean allowsLookahead() {
        return false;
    }

    /**
     * This parser just tests the context of the stream it is in, and so never actually consumes any input.
     */
    @Override
    public boolean canMatchEmptyInputs() {
        return true;
    }

    @Override
    public String toString() {
        var tag = (namespace == null ? name : namespace + ":" + name);
        if (parser == null) {
            return "<" + tag + ">";
        } else {
            return "<" + tag + ">" + parser.toString() + "</" + tag + ">";
        }
    }

}
