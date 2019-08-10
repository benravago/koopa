package koopa.stage.runtime.model;

import java.nio.file.Path;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A collection of {@linkplain Stage}s.
 */
public class SuiteOfStages {

    final Map<String, Stage> stages;

    public SuiteOfStages(Iterable<Path> sources) {
        stages = new LinkedHashMap<>();
        for (var source : sources) {
            var stage = new Stage(this,source);
            stages.put(stage.getName(), stage);
        }
    }

    public Collection<Stage> getStages() {
        return stages.values();
    }

    public Stage getStage(String name) {
        return stages.get(name);
    }

    public int size() {
        return stages.size();
    }

}
