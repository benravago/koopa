package koopa.stage.runtime.model;

import java.nio.file.Path;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import koopa.stage.util.StageUtil;

/**
 * A collection of grammar tests for one or more {@linkplain Target}s.
 * Corresponds to a single ".stage" file.
 */
public class Stage {

    /**
     * The overall suite this this stage belongs to.
     */
    final SuiteOfStages suite;

    /**
     * The source file this stage was loaded from.
     */
    final Path source;

    /**
     * The name of this stage. Which is the name of the {@link #source} file.
     */
    final String name;

    /**
     * Collects all grammar tests and groups them with the grammar rules they were defined for.
     */
    final Map<String, Target> targets = new LinkedHashMap<>();

    public Stage(SuiteOfStages suite, Path source) {
        this.suite = suite;
        this.source = source;
        this.name = stageName(source);
        load();
    }

    public static String stageName(Path source) {
        var s = source.getFileName().toString();
        var p = s.indexOf('.');
        return (p < 0) ? s : s.substring(0, p);
    }

    private void load() {
        var ast = StageUtil.getAST(source, false);
        if (ast == null) {
            throw new IllegalArgumentException("Failed to parse stage: " + source);
        }
        for (var targetDefinition : ast.getChildren("target")) {
            var target = new Target(this, targetDefinition);
            targets.put(target.getName(), target);
        }
    }

    public Path getSource() {
        return source;
    }

    public Collection<Target> getTargets() {
        return targets.values();
    }

    public Target getTarget(String name) {
        return targets.get(name);
    }

    public String getName() {
        return name;
    }

    public SuiteOfStages getSuite() {
        return suite;
    }

}
