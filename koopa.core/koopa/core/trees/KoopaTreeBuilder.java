package koopa.core.trees;

import java.util.LinkedList;
import java.util.List;

import koopa.core.data.Token;
import koopa.core.data.markers.End;
import koopa.core.data.markers.InWater;
import koopa.core.data.markers.Start;
import koopa.core.grammars.Grammar;

public class KoopaTreeBuilder extends TreeBuildingTarget {

    /**
     * Any leaves preceding the first tree get tracked here.
     * They will be added to the first tree once we get it.
     */
    private LinkedList<Tree> leading = new LinkedList<>();

    /**
     * Tree being built.
     */
    private LinkedList<Tree> treeparts = new LinkedList<>();

    /**
     * Trees which have been built.
     */
    private List<Tree> trees = new LinkedList<>();

    public KoopaTreeBuilder(Grammar grammar) {
        this(grammar, false);
    }

    public KoopaTreeBuilder(Grammar grammar, boolean hideWater) {
        super(hideWater);
    }

    public List<Tree> getTrees() {
        return this.trees;
    }

    public Tree getTree() {
        if (trees.isEmpty()) {
            return null;
        } else {
            return trees.get(0);
        }
    }

    @Override
    public void down(Start start) {
        var tree = new Tree(start);
        if (!this.treeparts.isEmpty()) {
            this.treeparts.peek().addChild(tree);
        } else {
            while (!leading.isEmpty()) {
                tree.addChild(leading.removeFirst());
            }
        }
        this.treeparts.push(tree);
    }

    @Override
    public void leaf(Token token) {
        var tree = new Tree(token);
        if (!this.treeparts.isEmpty()) {
            this.treeparts.peek().addChild(tree);
        } else {
            leading.add(tree);
        }
    }

    @Override
    public void up(End end) {
        assert (!this.treeparts.isEmpty());
        var tree = this.treeparts.pop();
        if (this.treeparts.isEmpty()) {
            this.trees.add(tree);
        }
    }

    @Override
    public void water(InWater water) {
        var tree = new Tree(water);
        if (!this.treeparts.isEmpty()) {
            this.treeparts.peek().addChild(tree);
        }
        this.treeparts.push(tree);
    }

    @Override
    public void land() {
        assert (!this.treeparts.isEmpty());
        var tree = this.treeparts.pop();
        if (this.treeparts.isEmpty()) {
            this.trees.add(tree);
        }
    }

}
