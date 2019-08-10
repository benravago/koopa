package koopa.core.trees.test;

import static koopa.core.util.test.Util.token;
import static koopa.core.util.test.Util.tree;
import koopa.core.trees.TreeWalker;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TreeWalkerTest {

    @Test
    void canWalkDepthFirst() {
        var calvin = token("Calvin");
        var hobbes = token("Hobbes");
        var susie = token("Susie");
        var rosalyn = token("Rosalyn");
        var mainCharacters = tree("main", calvin, hobbes);
        var supportingCharacters = tree("supporting", susie, rosalyn);
        var characters = tree("characters", mainCharacters,    supportingCharacters);
        var walker = new TreeWalker(characters);
        assertSame(characters, walker.next());
        assertSame(characters, walker.getCurrent());
        assertSame(mainCharacters, walker.next());
        assertSame(mainCharacters, walker.getCurrent());
        assertSame(calvin, walker.next());
        assertSame(calvin, walker.getCurrent());
        assertSame(hobbes, walker.next());
        assertSame(hobbes, walker.getCurrent());
        assertSame(supportingCharacters, walker.next());
        assertSame(supportingCharacters, walker.getCurrent());
        assertSame(susie, walker.next());
        assertSame(susie, walker.getCurrent());
        assertSame(rosalyn, walker.next());
        assertSame(rosalyn, walker.getCurrent());
        assertSame(null, walker.next());
        assertSame(null, walker.getCurrent());
    }

    @Test
    void willScopeCorrectly() {
        var calvin = token("Calvin");
        var hobbes = token("Hobbes");
        var susie = token("Susie");
        var rosalyn = token("Rosalyn");
        var mainCharacters = tree("main", calvin, hobbes);
        var supportingCharacters = tree("supporting", susie, rosalyn);
        tree("characters", mainCharacters, supportingCharacters);
        var walker = new TreeWalker(mainCharacters);
        assertSame(mainCharacters, walker.next());
        assertSame(calvin, walker.next());
        assertSame(hobbes, walker.next());
        assertSame(null, walker.next());
    }

    @Test
    void willSkipCorrectly() {
        var calvin = token("Calvin");
        var hobbes = token("Hobbes");
        var susie = token("Susie");
        var rosalyn = token("Rosalyn");
        var mainCharacters = tree("main", calvin, hobbes);
        var supportingCharacters = tree("supporting", susie, rosalyn);
        var characters = tree("characters", mainCharacters, supportingCharacters);
        var walker = new TreeWalker(characters);
        assertSame(characters, walker.next());
        assertSame(mainCharacters, walker.next());
        assertSame(calvin, walker.next());
        walker.skipRemainderOfTree(mainCharacters);
        assertSame(supportingCharacters, walker.next());
        walker.skipRemainderOfTree(supportingCharacters);
        assertSame(null, walker.next());
    }

    @Test
    void willSaveAndRestoreState() {
        var calvin = token("Calvin");
        var hobbes = token("Hobbes");
        var susie = token("Susie");
        var rosalyn = token("Rosalyn");
        var mainCharacters = tree("main", calvin, hobbes);
        var supportingCharacters = tree("supporting", susie, rosalyn);
        var characters = tree("characters", mainCharacters, supportingCharacters);
        var walker = new TreeWalker(characters);
        assertSame(characters, walker.next());
        TreeWalker.State state = walker.getState();
        assertSame(mainCharacters, walker.next());
        assertSame(calvin, walker.next());
        walker.setState(state);
        assertSame(mainCharacters, walker.next());
        assertSame(calvin, walker.next());
    }

}
