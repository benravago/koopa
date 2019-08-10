package koopa.core.trees.test;

import static java.util.Arrays.asList;
import java.util.LinkedList;
import java.util.List;

import static koopa.core.util.test.Util.t;
import static koopa.core.util.test.Util.tree;
import koopa.core.data.Data;
import koopa.core.data.Token;
import koopa.core.trees.Tree;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TreeIteratorsTest {

    static Token rear = t("Rear");
    static Token admiral = t("Admiral");
    static Tree rank = tree("rank", rear, admiral);
    static Token m = t("M");
    static Token dot = t(".");
    static Tree initials = tree("initials", m, dot);
    static Token grace = t("Grace");
    static Token hopper = t("Hopper");
    static Tree name = tree("name", grace, initials, hopper);
    static Tree subject = tree("subject", rank, name);

    @Test
    void allTokens() {
        assertEquals( asList(new Token[] { rear, admiral, grace, m, dot, hopper }), collect(subject.allTokens()) );
        assertEquals( asList(new Token[] { rear, admiral }), collect(rank.allTokens()) );
        assertEquals( asList(new Token[] { m, dot }), collect(initials.allTokens()) );
        assertEquals( asList(new Token[] { grace, m, dot, hopper }), collect(name.allTokens()) );
    }

    @Test
    void childTokens() {
        assertEquals( asList(new Token[] {}), collect(subject.childTokens()) );
        assertEquals( asList(new Token[] { rear, admiral }), collect(rank.childTokens()) );
        assertEquals( asList(new Token[] { m, dot }), collect(initials.childTokens()) );
        assertEquals( asList(new Token[] { grace, hopper }), collect(name.childTokens()) );
    }

    @Test
    void childData() {
        assertEquals( asList(new Data[] { rank.getData(), name.getData() }), collect(subject.childData()) );
        assertEquals( asList(new Data[] { rear, admiral }), collect(rank.childData()) );
        assertEquals( asList(new Data[] { m, dot }), collect(initials.childData()) );
        assertEquals( asList(new Data[] { grace, initials.getData(), hopper }), collect(name.childData()) );
    }

    @Test
    void childTrees() {
        assertEquals( asList(new Tree[] { rank, name }), collect(subject.childTrees()) );
        assertEquals( asList(new Tree[] {}), collect(rank.childTrees()) );
        assertEquals( asList(new Tree[] {}), collect(initials.childTrees()) );
        assertEquals( asList(new Tree[] { initials }), collect(name.childTrees()) );
    }

    <T> List<T> collect(Iterable<T> iterable) {
        var list = new LinkedList<T>();
        for (var t : iterable) {
            list.add(t);
        }
        return list;
    }

}
