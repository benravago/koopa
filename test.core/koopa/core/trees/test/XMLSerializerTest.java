package koopa.core.trees.test;

import java.io.IOException;

import static koopa.core.util.test.Util.token;
import static koopa.core.util.test.Util.tree;

import koopa.core.trees.XMLSerializer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class XMLSerializerTest {

    @Test
    void testSerializeNull() throws IOException {
        var expected = "<?xml version='1.0' encoding='UTF-8'?>\n" + "<koopa>\n" + "</koopa>\n";
        var actual = XMLSerializer.serialize(null);
        assertEquals(expected, actual);
    }

    @Test
    void testSerializeAToken() throws IOException {
        var expected = "<?xml version='1.0' encoding='UTF-8'?>\n" + "<koopa>\n" + "  <t><![CDATA[COBOL]]></t>\n" + "</koopa>\n";
        var actual = XMLSerializer.serialize(token("COBOL"));
        assertEquals(expected, actual);
    }

    @Test
    void testSerializeATree() throws IOException {
        var expected =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
            + "<koopa>\n" + "  <quote>\n" + "    <t><![CDATA[Stop]]></t>\n"
            + "    <t><![CDATA[bashing]]></t>\n"
            + "    <t><![CDATA[Cobol]]></t>\n" + "  </quote>\n"
            + "</koopa>\n";
        var actual = XMLSerializer.serialize(tree("quote", "Stop", "bashing", "Cobol"));
        assertEquals(expected, actual);
    }

    @Test
    void testEscapingOfCDATAEndMarker() throws IOException {
        var expected =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
            + "<koopa>\n" + "  <escaped>\n"
            + "    <t><![CDATA[DISPLAY]]></t>\n"
            + "    <t><![CDATA[']]]]><![CDATA[>']]></t>\n"
            + "  </escaped>\n" + "</koopa>\n";
        var actual = XMLSerializer.serialize(tree("escaped", "DISPLAY", "']]>'"));
        assertEquals(expected, actual);
    }

}
