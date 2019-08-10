package koopa.stage.runtime.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import koopa.core.trees.Tree;
import koopa.stage.runtime.GrammarTest;
import koopa.stage.runtime.generation.Fragment;
import koopa.stage.runtime.generation.Mark;
import koopa.stage.runtime.generation.Part;
import koopa.stage.runtime.generation.Reference;
import koopa.stage.runtime.generation.TestBuilder;

/**
 * This defines a single grammar test encoded in a {@linkplain Stage} for a specific {@linkplain Target}.
 */
public class GrammarTestDefinition {

    /**
     * The {@linkplain Target} rule this defines as a test for.
     */
    final Target target;

    /**
     * If we expect the target to accept this sample, or reject it.
     */
    final boolean accept;

    /**
     * If this definition reference others. See {@linkplain Reference}.
     */
    final boolean hasReference;

    /**
     * Whether or not this definition includes a {@linkplain Mark}.
     */
    final boolean hasMark;

    /**
     * The different {@linkplain Part}s which make up the definition.
     */
    final List<Part> parts;

    public GrammarTestDefinition(Target target, Tree definition) {
        this.target = target;
        this.accept = definition.hasChild("accept");
        var sample = definition.getChild("sample");
        this.parts = new ArrayList<Part>();
        var sawMark = false;
        var sawReference = false;
        for (var i = 0; i < sample.getChildCount(); i++) {
            var part = sample.getChild(i);
            if (part.isToken()) {
                continue;
            } else if (part.isNode("fragment")) {
                parts.add(new Fragment(part));
            } else if (part.isNode("reference")) {
                parts.add(new Reference(this, part));
                sawReference = true;
            } else if (part.isNode("mark")) {
                parts.add(new Mark());
                sawMark = true;
            } else {
                throw new InternalError("Unexpected part of a definition: " + part.getName());
            }
        }
        for (var i = 0; i < this.parts.size() - 1; i++) {
            parts.get(i).setNextPart(parts.get(i + 1));
        }
        this.hasMark = sawMark;
        this.hasReference = sawReference;
    }

    public Target getTarget() {
        return target;
    }

    public List<GrammarTest> getTests() {
        var tests = new LinkedList<GrammarTest>();
        buildTests(new TestBuilder() {
            @Override
            protected void ready(String text) {
                tests.add(new GrammarTest(
                    target.getStage().getName(),
                    target.getName(),
                    accept,
                    text )
                );
            }
        });
        return tests;
    }

    public boolean shouldAccept() {
        return accept;
    }

    /**
     * A {@linkplain GrammarTestDefinition} is "formative" for a {@linkplain Target}
     * when it is a positive match for that target.
     *
     * (i.e. {@linkplain GrammarTestDefinition#shouldAccept()} is <code>true</code>)
     * and the target matches it in full (i.e. there is no marker in the input).
     */
    public boolean isFormative() {
        return accept && !hasMark;
    }

    public SuiteOfStages getSuite() {
        return target.getSuite();
    }

    public void buildTests(TestBuilder builder) {
        parts.get(0).buildTests(builder);
        if (TestOrder.RANDOMIZE_TESTS && hasReference) {
            for (var i = 1; i < TestOrder.RANDOMIZER_LIMIT; i++) {
                parts.get(0).buildTests(builder);
            }
        }
    }

    public void includeInTests(TestBuilder builder, LinkedList<Part> next) {
        parts.get(0).includeInTests(builder, next);
    }

}
