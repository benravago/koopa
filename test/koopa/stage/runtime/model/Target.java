package koopa.stage.runtime.model;

import java.util.LinkedList;
import java.util.List;

import koopa.core.trees.Tree;
import koopa.stage.runtime.GrammarTest;
import koopa.stage.runtime.generation.Part;
import koopa.stage.runtime.generation.TestBuilder;

/**
 * A collection of tests (through {@linkplain GrammarTestDefinition}s) for a specific grammar rule.
 */
public class Target {

    /**
     * The {@linkplain Stage} this collection of tests was defined in.
     */
    final Stage stage;

    /**
     * The name of the grammar rule we're testing.
     */
    final String name;

    /**
     * All tests defined for this grammar rule.
     */
    final List<GrammarTestDefinition> testDefinitions;

    /**
     * The subset of {@link #testDefinitions} which are formative.
     *
     * (cfr.{@linkplain} {@link GrammarTestDefinition#isFormative()})
     */
    final List<GrammarTestDefinition> formativeDefinitions;

    /**
     * When generating actual tests, this variable helps cycle through all definitions.
     */
    int nextDefinitionToBeUsedWhenReferenced = 0;

    public Target(Stage stage, Tree targetDefinition) {
        this.stage = stage;
        this.name = targetDefinition.getChild("identifier").getAllText();
        this.testDefinitions = new LinkedList<>();
        this.formativeDefinitions = new LinkedList<>();
        for (var testDefinition : targetDefinition.getChildren("test")) {
            var def = new GrammarTestDefinition(this, testDefinition);
            this.testDefinitions.add(def);
            if (def.isFormative()) {
                this.formativeDefinitions.add(def);
            }
        }
    }

    public Stage getStage() {
        return stage;
    }

    public SuiteOfStages getSuite() {
        return stage.getSuite();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public List<GrammarTest> getTests() {
        var tests = new LinkedList<GrammarTest>();
        for (GrammarTestDefinition def : testDefinitions) {
            tests.addAll(def.getTests());
        }
        return tests;
    }

    public void includeInTests(TestBuilder builder, LinkedList<Part> next) {
        if (formativeDefinitions.isEmpty()) {
            // no-op
        } else if (!TestOrder.RANDOMIZE_TESTS) {
            // When not in random mode we cycle through the formative definitions when asked for a sample to be included in the tests.
            var index = nextDefinitionToBeUsedWhenReferenced;
            nextDefinitionToBeUsedWhenReferenced = (nextDefinitionToBeUsedWhenReferenced + 1) % formativeDefinitions.size();
            formativeDefinitions.get(index).includeInTests(builder, next);
        } else {
            // In random mode we just pick a random formative definition to provide the sample to be included in the tests.
            var random = TestOrder.RANDOMIZER.nextInt(formativeDefinitions.size());
            formativeDefinitions.get(random).includeInTests(builder, next);
        }
    }

}
