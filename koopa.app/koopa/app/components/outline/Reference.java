package koopa.app.components.outline;

import koopa.core.trees.Tree;

public class Reference {

    public final String id;
    public final Tree item;

    public Reference(String id, Tree item) {
        this.id=id; this.item=item;
    }

    public Reference dup(String id) {
        return new Reference(id,item);
    }

    public Reference dup(Tree item) {
        return new Reference(id,item);
    }
    
    public Tree root() {
        Tree tree = item, parent;
        while ((parent = tree.getParent()) != null) tree = parent;
        return tree;
    }

    @Override
    public String toString() {
        return id;
    }

}
