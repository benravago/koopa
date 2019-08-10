package koopa.stage.runtime.generation;

import java.util.LinkedList;

import koopa.core.trees.Tree;
import koopa.stage.runtime.model.GrammarTestDefinition;

/**
 * This is a reference to another {@linkplain GrammarTestDefinition}.
 * The effect is that any formative example for the referenced definition may be included in its place.
 */
public class Reference extends Part {

    GrammarTestDefinition test;
    String stageName;
    String targetName;

    public Reference(GrammarTestDefinition test, Tree definition) {
        this.test = test;
        if (definition.hasChild("stageName")) {
            this.stageName = definition.getChild("stageName").getAllText();
        } else {
            this.stageName = test.getTarget().getStage().getName();
        }
        this.targetName = definition.getChild("targetName").getAllText();
    }

    @Override
    public void includeInTests(TestBuilder builder, LinkedList<Part> furtherParts) {
        if (nextPart != null) {
            furtherParts.addFirst(nextPart);
        }
        var stage = test.getSuite().getStage(stageName);
        if (stage == null) {
            throw new InternalError("Unkown stage: " + stageName);
        }
        var target = stage.getTarget(targetName);
        if (target == null) {
            throw new InternalError("Unkown target: " + stageName + ":" + targetName);
        }
        target.includeInTests(builder, furtherParts);
        if (nextPart != null) {
            furtherParts.removeFirst();
        }
    }

}
