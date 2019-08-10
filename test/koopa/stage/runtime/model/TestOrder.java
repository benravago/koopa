package koopa.stage.runtime.model;

import java.util.Random;

class TestOrder {

    static final boolean RANDOMIZE_TESTS;
    static final Random RANDOMIZER;
    static final int RANDOMIZER_LIMIT;

    static {
        RANDOMIZE_TESTS = Boolean.getBoolean("koopa.tests.random");
        var seed = System.getProperty("koopa.tests.random.seed");
        if (!RANDOMIZE_TESTS) {
            RANDOMIZER = null;
        } else if (seed == null) {
            RANDOMIZER = new Random();
        } else {
            RANDOMIZER = new Random(seed.hashCode());
        }
        var limit = System.getProperty("koopa.tests.random.limit");
        if (!RANDOMIZE_TESTS) {
            RANDOMIZER_LIMIT = 1;
        } else if (limit == null) {
            RANDOMIZER_LIMIT = 2;
        } else {
            RANDOMIZER_LIMIT = Integer.parseInt(limit);
        }
        if (RANDOMIZE_TESTS) {
            System.out.println("Randomizing grammar tests, up to " + RANDOMIZER_LIMIT + " per test definition.");
        }
    }

}
