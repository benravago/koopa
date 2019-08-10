package koopa.stage.runtime;

/**
 * This encodes the final test to be run.
 */
public class GrammarTest {

    /**
     * Which stage does this test belong to ?
     */
    final String stage;

    /**
     * Which grammar rule are we exercising ?
     */
    final String target;

    /**
     * Do we expect the sample to be accepted or rejected ?
     */
    final boolean accept;

    /**
     * What is the input we're using as a test ?
     */
    final String sample;

    public GrammarTest(String stage, String target, boolean accept, String sample) {
        this.stage = stage;
        this.target = target;
        this.accept = accept;
        this.sample = sample;
    }

    /**
     * Which stage does this test belong to ?
     */
    public String getStage() {
        return stage;
    }

    /**
     * Which grammar rule are we exercising ?
     */
    public String getTarget() {
        return target;
    }

    /**
     * Do we expect the sample to be accepted or rejected ?
     */
    public boolean shouldAccept() {
        return accept;
    }

    /**
     * What is the input we're using as a test ?
     */
    public String getSample() {
        return sample;
    }

    @Override
    public String toString() {
        return (accept ? "+" : "-") + "[" + sample + "]";
    }

}
