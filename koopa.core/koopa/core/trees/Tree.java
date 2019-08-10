package koopa.core.trees;

import static koopa.core.data.tags.AreaTag.COMMENT;
import static koopa.core.data.tags.AreaTag.PROGRAM_TEXT_AREA;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import koopa.core.data.Data;
import koopa.core.data.Position;
import koopa.core.data.Token;
import koopa.core.data.markers.Start;
import koopa.core.data.tags.AreaTag;
import koopa.core.trees.iterators.ChildDataIterator;
import koopa.core.trees.iterators.ChildTokenIterator;
import koopa.core.trees.iterators.ChildTreeIterator;
import koopa.core.trees.iterators.DepthFirstTokenIterator;

public class Tree implements Data {

    // TODO This should really be based on the grammar's logic...
    private static class ProgramTextFilter implements TokenFilter {

        @Override
        public boolean include(Token token) {
            return token.hasTag(PROGRAM_TEXT_AREA);
        }
    }

    private final Data data;
    private final List<Tree> children;
    private int childIndex = -1;
    private Tree parent;

    public Tree(Data data) {
        this.data = data;
        this.children = new ArrayList<>();
        this.parent = null;
    }

    public String getText() {
        if (data instanceof Token) {
            return ((Token) data).getText();
        } else {
            return "";
        }
    }

    public String getName() {
        if (data instanceof Start) {
            return ((Start) data).getName();
        } else {
            return null;
        }
    }

    public String getNamespace() {
        if (data instanceof Start) {
            return ((Start) data).getNamespace();
        } else {
            return null;
        }
    }

    /**
     * Builds up a string representation of the program text being held in this tree.
     * <p>
     * This does not take {@linkplain AreaTag#COMMENT}s into account.
     */
    public String getProgramText() {
        var builder = new StringBuilder();
        for (var t : allTokens(new ProgramTextFilter())) {
            builder.append(t.getText());
        }
        return builder.toString();
    }

    public String getAllText() {
        var b = new StringBuilder();
        for (var t : allTokens()) {
            b.append(t.getText());
        }
        return b.toString();
    }

    public Data getData() {
        return data;
    }

    /**
     * Gives you an {@linkplain Iterable} for all {@linkplain Token}s found anywhere in this tree, in depth-first order.
     */
    public Iterable<Token> allTokens() {
        return allTokens(null);
    }

    /**
     * Gives you an {@linkplain Iterable} for {@linkplain Token}s found anywhere in this tree
     * which pass the given {@linkplain TokenFilter}, in depth-first order.
     */
    public Iterable<Token> allTokens(TokenFilter filter) {
        return () -> new DepthFirstTokenIterator(Tree.this,filter);
    }

    /**
     * Gives you an {@linkplain Iterable} for all child {@linkplain Token}s found in this tree, in original order.
     */
    public Iterable<Token> childTokens() {
        return childTokens(null);
    }

    /**
     * Gives you an {@linkplain Iterable} for child {@linkplain Token}s found in this tree
     * which pass the given {@linkplain TokenFilter}, in original order.
     */
    public Iterable<Token> childTokens(TokenFilter filter) {
        return () -> new ChildTokenIterator(Tree.this,filter);
    }

    /**
     * Gives you an {@linkplain Iterable} for child {@linkplain Data} found in this tree, in original order.
     */
    public Iterable<Data> childData() {
        return () -> new ChildDataIterator(Tree.this);
    }

    /**
     * Gives you an {@linkplain Iterable} for child {@linkplain Tree}s found in this tree, in original order.
     */
    public Iterable<Tree> childTrees() {
        return () -> new ChildTreeIterator(Tree.this);
    }

    /**
     * Searches the tree for the first known start position.
     * <p>
     * This does not take {@linkplain AreaTag#COMMENT}s into account.
     */
    public Position getStartPosition() {
        var start = getStartToken();
        return start == null ? null : start.getStart();
    }

    /**
     * Searches the tree for the first non-{@linkplain AreaTag#COMMENT} token.
     */
    public Token getStartToken() {
        if (data instanceof Token) {
            var token = (Token) data;
            if (!(token.hasTag(COMMENT))) {
                return token;
            }
        }
        for (var i = 0; i < children.size(); i++) {
            var token = children.get(i).getStartToken();
            if (token != null) {
                return token;
            }
        }
        return null;
    }

    /**
     * Searches the tree for the first known token.
     */
    public Token getRawStartToken() {
        if (data instanceof Token) {
            return (Token) data;
        }
        for (var i = 0; i < children.size(); i++) {
            var token = children.get(i).getRawStartToken();
            if (token != null) {
                return token;
            }
        }
        return null;
    }

    /**
     * Searches the tree for the last known end position.
     */
    public Position getRawStart() {
        var t = getRawStartToken();
        return t == null ? null : t.getStart();
    }

    /**
     * Searches the tree for the last known end position.
     * <p>
     * This does not take {@linkplain AreaTag#COMMENT}s into account.
     */
    public Position getEndPosition() {
        var end = getEndToken();
        return end == null ? null : end.getEnd();
    }

    /**
     * Searches the tree for the last known end non-{@linkplain AreaTag#COMMENT} token.
     */
    public Token getEndToken() {
        if (data instanceof Token) {
            var token = (Token) data;
            if (!(token.hasTag(COMMENT))) {
                return token;
            }
        }
        for (var i = children.size() - 1; i >= 0; i--) {
            var token = children.get(i).getEndToken();
            if (token != null) {
                return token;
            }
        }
        return null;
    }

    /**
     * Searches the tree for the last known token.
     */
    // TODO Combine this with getEndToken and TokenFilters.
    public Token getRawEndToken() {
        if (data instanceof Token) {
            return (Token) data;
        }
        for (var i = children.size() - 1; i >= 0; i--) {
            var token = children.get(i).getRawEndToken();
            if (token != null) {
                return token;
            }
        }
        return null;
    }

    /**
     * Searches the tree for the last known end position.
     */
    public Position getRawEnd() {
        var t = getRawEndToken();
        return t == null ? null : t.getEnd();
    }

    public void addChild(Tree child) {
        child.childIndex = children.size();
        children.add(child);
        child.parent = this;
    }

    public void insertChild(int index, Tree child) {
        children.add(index, child);
        child.parent = this;
        for (var i = index; i < children.size(); i++) {
            children.get(i).childIndex = i;
        }
    }

    public void removeChild(int index) {
        var child = children.remove(index);
        child.childIndex = -1;
        child.parent = null;
        for (var i = index; i < children.size(); i++) {
            children.get(i).childIndex = i;
        }
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public int getChildCount() {
        return children.size();
    }

    public Tree getChild(int i) {
        if (i >= 0) {
            return children.get(i);
        } else {
            return children.get(children.size() + i);
        }
    }

    public List<Tree> getChildren() {
        return new ArrayList<>(children);
    }

    public Tree getParent() {
        return parent;
    }

    public int getChildIndex() {
        return childIndex;
    }

    public int getLine() {
        var startPosition = getStartPosition();
        // TODO When would this be null ?
        if (startPosition == null) {
            return 0;
        }
        return startPosition.getLinenumber();
    }

    @Override
    public String toString() {
        return data.toString();
    }

    public Tree getRoot() {
        if (parent == null) {
            return this;
        } else {
            return parent.getRoot();
        }
    }

    public List<Token> getTokens() {
        var tokens = new LinkedList<Token>();
        for (var token : allTokens()) {
            tokens.add(token);
        }
        return tokens;
    }

    public boolean isNode() {
        if (data == null) {
            return false;
        }
        return data instanceof Start;
    }

    public boolean isNode(String name) {
        if (data == null) {
            return false;
        }
        if (!(data instanceof Start)) {
            return false;
        }
        return name.equals(((Start) data).getName());
    }

    public boolean isToken() {
        if (data == null) {
            return false;
        }
        return data instanceof Token;
    }

    /**
     * Return the first child node with the given name.
     */
    public Tree getChild(String name) {
        for (var child : children) {
            if (child.isNode(name)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Does this node have a child with the given name ?
     */
    public boolean hasChild(String name) {
        return getChild(name) != null;
    }

    /**
     * Return a list of all child nodes with the given name.
     */
    public List<Tree> getChildren(String name) {
        var matching = new LinkedList<Tree>();
        for (var child : children) {
            if (child.isNode(name)) {
                matching.add(child);
            }
        }
        return matching;
    }

    public Tree getDescendant(String... names) {
        var current = this;
        for (var name : names) {
            if (current == null) {
                return null;
            }
            current = current.getChild(name);
        }
        return current;
    }

    public Tree getAncestor(String name) {
        var parent = getParent();
        while (parent != null && !parent.isNode(name)) {
            parent = parent.getParent();
        }
        return parent;
    }

    public Tree find(Token token) {
        if (token == null) {
            return null;
        }
        if (data == token) {
            return this;
        }
        for (var child : children) {
            var matching = child.find(token);
            if (matching != null) {
                return matching;
            }
        }
        return null;
    }

    public int getDepth() {
        var d = 0;
        var p = parent;
        while (p != null) {
            d += 1;
            p = p.parent;
        }
        return d;
    }

}
