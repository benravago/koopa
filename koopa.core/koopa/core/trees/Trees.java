package koopa.core.trees;

import java.util.ArrayList;
import java.util.List;

import koopa.core.data.Data;
import koopa.core.grammars.Grammar;

public class Trees {

    public static Tree getTree(Grammar grammar, List<Data> data) {
        var builder = new KoopaTreeBuilder(grammar, true);
        for (var d : data) {
            builder.push(d);
        }
        var trees = builder.getTrees();
        return trees == null || trees.size() != 1 ? null : trees.get(0);
    }

    public static Tree find(Tree root, String name) {
        if (root.isNode(name)) {
            return root;
        }
        for (var child : root.getChildren()) {
            var tree = find(child,name);
            if (tree != null) {
                return tree;
            }
        }
        return null;
    }

    public static String getAllText(Tree root, String name) {
        var tree = find(root,name);
        return tree != null ? tree.getAllText() : null;
    }

    public static Tree getMatch(Tree root, String... path) {
        return getMatch(root,1,path);
    }

    public static Tree getMatch(Tree root, int i, String... path) {
        if (i > 0) {
            var list = getMatches(root,path);
            if (list != null && --i < list.size()) {
                return list.get(i);
            }
        }
        return null;
    }

    public static List<Tree> getMatches(Tree root, String... path) {
        if (path == null || path.length < 1) {
            return null;
        } else {
            if (path.length < 2) {
                return root.getChildren(path[0]);
            } else {
                var name = shift(path,root.getName());
                var list = new ArrayList<Tree>();
                match(list,root,0,path,name);
                return list;
            }
        }
    }

    static void match(List<Tree> list, Tree root, int i, String[] path, String name) {
        if (i < path.length) {
            var parent = path[i];
            if (root.isNode(parent)) {
                for (var child : root.getChildren()) {
                    match(list,child,i+1,path,name);
                }
            }
        } else {
            if (root.isNode(name)) {
                list.add(root);
            }
        }
    }

    static String shift(String[] path, String first) {
        var i = path.length - 1;
        var last = path[i];
        while (i > 0) {
            path[i] = path[--i];
        }
        path[i] = first;
        return last;
    }

}
