package koopa.core.util.test;

import static koopa.core.util.test.Util.text;
import static koopa.core.util.test.Util.tree;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import koopa.core.trees.Tree;

// TODO Improve this class. Ideally I'd like tests to be DSL-based files without the Java noise.

public class TreeSample {

    List<Object> taggedWords;
    List<Tree> trees;

    public TreeSample(List<Object> words, List<Tree> trees) {
        this.taggedWords = words;
        this.trees = trees;
    }

    public List<Object> getTaggedWords() {
        return taggedWords;
    }

    public List<Tree> getTrees() {
        return trees;
    }

    public static TreeSample from(String... lines) {
        var roots = new LinkedList<Tree>();
        var trees = new Stack<Tree>();
        var depths = new Stack<Integer>();
        var text = new LinkedList<Object>();
        int len = 0;
        for (var line : lines) {
            var depth = depth(line);
            while (!depths.isEmpty() && depths.peek() >= depth) {
                // System.out.println("popping " + depths.peek() + " : " + trees.peek());
                trees.pop();
                depths.pop();
            }
            var trimmed = line.trim();
            if (trimmed.startsWith("+")) {
                trimmed = trimmed.substring(1).trim();
                var names = trimmed.split("/");
                assert (names.length > 0);
                var parent = trees.isEmpty() ? null : trees.peek();
                for (var name : names) {
                    // System.out.println(name);
                    var tree = tree(name);
                    if (parent != null) {
                        parent.addChild(tree);
                    }
                    parent = tree;
                }
                // System.out.println("pushing " + depth + " : " + parent);
                if (trees.isEmpty()) {
                    roots.add(parent.getRoot());
                }
                trees.push(parent);
                depths.push(depth);
            } else if (trimmed.startsWith(">")) {
                trimmed = trimmed.substring(1).trim();
                // assert (!trees.isEmpty());
                var parent = trees.isEmpty() ? null : trees.peek();
                var words = trimmed.split("\\s+");
                for (var word : words) {
                    var w = text(word, len, len + word.length() - 1);
                    if (parent == null) {
                        roots.add(w);
                    } else {
                        parent.addChild(w);
                    }
                    len += word.length();
                    text.add(word);
                }
            } else {
                throw new InternalError("Don't know how to handle this line: " + line);
            }
        }
        while (trees.size() > 1) {
            trees.pop();
            depths.pop();
        }
        // for (Tree root : roots) {
        //   dump(root, "");
        // }
        // System.out.println();
        var sample = new TreeSample(text, roots);
        return sample;
    }

//  private static void dump(Tree foo, String dent) {
//        if (foo == null) {
//            return;
//        }
//        System.out.println(dent + foo);
//        for (var i = 0; i < foo.getChildCount(); i++) {
//            dump(foo.getChild(i), dent + " ");
//        }
//    }

    private static int depth(String line) {
        for (var i = 0; i < line.length(); i++) {
            if (!Character.isWhitespace(line.charAt(i))) {
                return i;
            }
        }
        return line.length();
    }

}