package koopa.cobol.sources.test.samples;

import java.nio.file.Path;
import java.util.Iterator;

import java.util.Map;
import java.util.HashMap;

import koopa.core.data.Token;
import koopa.core.sources.Source;
import koopa.core.sources.test.DataValidator;
import koopa.core.trees.Tree;
import koopa.core.util.Glob;
import koopa.core.util.test.FileBasedTestSuite;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class provides the infrastructure for testing the different sources.
 * It looks for ".sample" files, and runs each one it finds through a JUnit test.
 */
public abstract class SourcesValidationTest extends FileBasedTestSuite implements DataValidator {

    protected abstract Path getFolder();

    @Override
    protected Iterator<Path> getFiles() {
        return Glob.find(getFolder(),"*.sample");
    }

    @Override
    protected void testFile(Path source) {
        testSampleValidates(source);
    }

    // Test
    void testSampleValidates(Path file) {
        var sample = Sample.from(file);
        var source = getSource(file, sample);
        sample.assertOutputIsAsExpected(source, this);
    }

    protected abstract Source getSource(Path file, Sample sample);

    Map<String, Tags> TOKEN_CATEGORIES;
    Map<String, NodeType> NODE_CATEGORIES;

    // BeforeEach
    protected void initialize() {
        TOKEN_CATEGORIES = new HashMap<>();
        NODE_CATEGORIES = new HashMap<>();
    }

    protected void addTokenCategory(String category, Object[] required) {
        addTokenCategory(category, required, null);
    }

    protected void addTokenCategory(String category, Object[] required, Object[] forbidden) {
        assert (!TOKEN_CATEGORIES.containsKey(category));
        TOKEN_CATEGORIES.put(category, new Tags(required, forbidden));
    }

    protected void addNodeCategory(String category, String namespace, String name) {
        assert (!NODE_CATEGORIES.containsKey(category));
        NODE_CATEGORIES.put(category, new NodeType(namespace, name));
    }

    @Override
    public void validate(Token token, String category, boolean required) {
        if ("-".equalsIgnoreCase(category)) {
            // A "don't care". For when you don't care.
            return;
        }
        if ("?".equalsIgnoreCase(category)) {
            // This one is used for debugging.
            System.out.println(token);
            return;
        }
        if (TOKEN_CATEGORIES.containsKey(category)) {
            var tags = TOKEN_CATEGORIES.get(category);
            if (required) {
                assertRequiredCategory(token, category, tags);
            } else {
                assertForbiddenCategory(token, category, tags);
            }
        } else {
            System.out.println("Warning! Unknown category '" + category + "' on " + token);
        }
    }

    @Override
    public void validate(Tree tree, String category, boolean required) {
        if ("-".equalsIgnoreCase(category)) {
            // A "don't care". For when you don't care.
            return;
        }
        if ("?".equalsIgnoreCase(category)) {
            // This one is used for debugging.
            System.out.println(tree);
            return;
        }
        if (NODE_CATEGORIES.containsKey(category)) {
            var type = NODE_CATEGORIES.get(category);
            var msg = required
                    ? "Category " + category + ": requires " + type + ". Got: " + tree
                    : "Category " + category + ": forbids" + type + ". Got: " + tree ;
            assertTrue(type.name.equals(tree.getName()) == required, msg);
            if (type.namespace != null) {
                assertTrue(type.namespace.equals(tree.getNamespace()) == required, msg);
            }
        } else {
            System.out.println("Warning! Unknown category '" + category + "' on " + tree);
        }
    }

    static void assertRequiredCategory(Token token, String category, Tags tags) {
        if (tags.required != null) {
            for (var tag : tags.required) {
                assertTrue(token.hasTag(tag), "Category " + category + ": requires " + tag + " on " + token + ".");
            }
        }
        if (tags.forbidden != null) {
            for (var tag : tags.forbidden) {
                assertFalse(token.hasTag(tag), "Category " + category + ": forbids " + tag + " on " + token + "." );
            }
        }
    }

    static void assertForbiddenCategory(Token token, String category, Tags tags) {
        if (tags.required != null) {
            for (var tag : tags.required) {
                assertFalse(token.hasTag(tag), "Category !" + category + ": forbids " + tag + " on " + token + "." );
            }
        }
        // We don't assume that the negation of a category suddenly makes its forbidden elements required.
    }

    static class Tags {
        final Object[] required;
        final Object[] forbidden;
        Tags(Object[] required, Object[] forbidden) {
            this.required = required;
            this.forbidden = forbidden;
        }
    }

    static class NodeType {
        final String namespace;
        final String name;
        NodeType(String namespace, String name) {
            this.namespace = namespace;
            this.name = name;
        }
        @Override
        public String toString() {
            return (namespace == null)
                 ? "<" + name + ">"
                 : "<" + namespace + ":" + name + ">";
        }
    }

}
