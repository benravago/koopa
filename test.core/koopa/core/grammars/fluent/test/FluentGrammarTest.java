package koopa.core.grammars.fluent.test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FluentGrammarTest {

    /**
     * I was getting a {@linkplain StackOverflowError} previously, due to a bit of naive coding.
     */
    @Test
    void testBuildingRecursiveHelperDoesNotOverflowTheStack() {
        FluentTestGrammar grammar = new FluentTestGrammar();
        grammar.defineHelper("recursive").as("recursive");
        assertNotNull(grammar.definitionOf("recursive").asParser());
    }

}
