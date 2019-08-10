package koopa.cobol.parser.test;

import java.io.IOException;
import java.io.UncheckedIOException;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import koopa.core.util.test.CSVReader;
import koopa.core.util.test.CSVWriter;
import koopa.core.util.Tuple;
import koopa.core.data.Token;
import koopa.cobol.parser.Metrics;
import koopa.cobol.parser.ParseResults;
import koopa.cobol.sources.CopyInclude;
import koopa.cobol.sources.Replace;

class TestResult {

    /**
     * Just a quick override, useful when working on the sources.
     */
    static final boolean TEST_TOKEN_COUNT = true;

    /**
     * Just a quick override, useful when working on the sources.
     */
    static final boolean TEST_COVERAGE = true;

    String name = null;
    boolean valid = false;
    int tokenCount = 0;
    float coverage = 0;

    int errorCount = 0;
    List<Tuple<Token, String>> errors = null;

    int warningCount = 0;
    List<Tuple<Token, String>> warnings = null;

    int preprocessedDirectivesCount = 0;

    static TestResult from(ParseResults parseResults) {
        var result = new TestResult();
        var parse = parseResults.getParse();
        result.name = parseResults.getFile().getFileName().toString();
        result.valid = parseResults.isValidInput();
        result.tokenCount = Metrics.getSignificantTokenCount(parseResults);
        result.coverage = Metrics.getCoverage(parseResults);
        result.errorCount = parse.getMessages().getErrorCount();
        result.warningCount = parse.getMessages().getWarningCount();
        result.errors = parse.getMessages().getErrors();
        result.warnings = parse.getMessages().getWarnings();
        var copyInclude = parse.getSource(CopyInclude.class);
        if (copyInclude != null) {
            var handledDirectives = copyInclude.getHandledDirectives();
            result.preprocessedDirectivesCount += handledDirectives.size();
        }
        var replace = parse.getSource(Replace.class);
        if (replace != null) {
            var handledDirectives = replace.getHandledDirectives();
            result.preprocessedDirectivesCount += handledDirectives.size();
        }
        return result;
    }

    String getName() {
        return name;
    }
    void setName(String name) {
        this.name = name;
    }
    boolean isValid() {
        return valid;
    }
    void setValid(boolean valid) {
        this.valid = valid;
    }
    int getTokenCount() {
        return tokenCount;
    }
    void setTokenCount(int tokenCount) {
        this.tokenCount = tokenCount;
    }
    float getCoverage() {
        return coverage;
    }
    void setCoverage(float coverage) {
        this.coverage = coverage;
    }
    int getErrorCount() {
        return errorCount;
    }
    void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }
    int getWarningCount() {
        return warningCount;
    }
    void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }
    int getPreprocessedDirectivesCount() {
        return preprocessedDirectivesCount;
    }
    void setPreprocessedDirectivesCount(int preprocessedDirectivesCount) {
        this.preprocessedDirectivesCount = preprocessedDirectivesCount;
    }
    List<String> getComparison(TestResult actual) {
        var messages = new ArrayList<String>();
        if (actual.valid != this.valid) {
            if (this.valid) {
                messages.add("- This file used to parse. It no longer does.");
            } else {
                messages.add("+ This file used to fail parsing. It is now valid.");
            }
        }
        if (TEST_TOKEN_COUNT && actual.tokenCount != this.tokenCount) {
            // Positive case: when valid and count went up ?
            if (actual.tokenCount < this.tokenCount) {
                messages.add("  Number of tokens went down from " + this.tokenCount + " to " + actual.tokenCount + ".");
            } else {
                messages.add("  Number of tokens went up from " + this.tokenCount + " to " + actual.tokenCount + ".");
            }
        }
        if (TEST_COVERAGE && actual.coverage != this.coverage) {
            if (actual.coverage < this.coverage) {
                messages.add("- Coverage went down from " + this.coverage + " to " + actual.coverage + ".");
            } else {
                messages.add("+ Coverage went up from " + this.coverage + " to " + actual.coverage + ".");
            }
        }
        if (actual.errorCount != this.errorCount) {
            if (actual.errorCount < this.errorCount) {
                messages.add("+ Error count went down from " + this.errorCount + " to " + actual.errorCount + ".");
            } else {
                messages.add("- Error count went up from " + this.errorCount + " to " + actual.errorCount + ".");
            }
        }
        // TODO Errors.
        if (actual.warningCount != this.warningCount) {
            if (actual.warningCount < this.warningCount) {
                messages.add("+ Warning count went down from " + this.warningCount + " to " + actual.warningCount + ".");
            } else {
                messages.add("- Warning count went up from " + this.warningCount + " to " + actual.warningCount + ".");
            }
        }
        // TODO Warnings.
        if (actual.preprocessedDirectivesCount != this.preprocessedDirectivesCount) {
            if (actual.preprocessedDirectivesCount < this.preprocessedDirectivesCount) {
                messages.add("+ Preprocessed directives count went down from " + this.preprocessedDirectivesCount + " to " + actual.preprocessedDirectivesCount + ".");
            } else {
                messages.add("- Preprocessed directives count went up from " + this.preprocessedDirectivesCount + " to " + actual.preprocessedDirectivesCount + ".");
            }
        }
        return messages;
    }

    static Map<String, TestResult> newResultMap() {
        return new ConcurrentHashMap<String, TestResult>();
    }

    static Map<String, TestResult> loadFromFile(Path expectedFile) {
        try (
            var reader = new CSVReader(Files.newBufferedReader(expectedFile))
        ) {
            String[] entries = null;
            // Header.
            if ((entries = reader.readNext()) == null) {
                return null;
            }
            var targets = newResultMap();
            // Entries.
            while ((entries = reader.readNext()) != null) {
                var name = entries[0];
                var valid = entries[1];
                var tokenCount = entries[2];
                var coverage = entries[3];
                var errorCount = entries[4];
                //  errors = entries[5];
                var warningCount = entries[6];
                //  warnings = entries[7];
                var preprocessedDirectivesCount = entries[8];
                var results = new TestResult();
                results.setName(name);
                results.setValid("true".equalsIgnoreCase(valid));
                results.setTokenCount(Integer.parseInt(tokenCount));
                results.setCoverage(Float.parseFloat(coverage));
                results.setErrorCount(Integer.parseInt(errorCount));
                // TODO List of errors.
                results.setWarningCount(Integer.parseInt(warningCount));
                // TODO List of warnings.
                results.setPreprocessedDirectivesCount(Integer.parseInt(preprocessedDirectivesCount));
                targets.put(name, results);
            }
            return targets;
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    static void saveToFile(Map<String, TestResult> results, Path targetFile) {
        try (
            var writer = new CSVWriter(Files.newBufferedWriter(targetFile));
        ) {
            // Write out the header for the CSV.
            writeResultsHeader(writer);
            var keys = results.keySet();
            var sortedKeys = new ArrayList<String>(keys);
            Collections.sort(sortedKeys);
            for (var key : sortedKeys) {
                writeNextResult(writer, key, results.get(key));
            }
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    static void writeResultsHeader(CSVWriter writer) throws IOException {
        var header = new String[] {
            "File", "Valid",
            "Number of tokens", "Coverage",
            "Number of errors", "Errors",
            "Number of warnings", "Warnings",
            "Number of preprocessed directives" };
        writer.writeNext(header);
        writer.flush();
    }

    private static void writeNextResult(CSVWriter writer, String name, TestResult results) throws IOException {
        var entries = new String[9];
        var errors = "";
        for (var error : results.errors) {
            if (errors.length() > 0) {
                errors += "\n";
            }
            errors += error.getFirst() + " " + error.getSecond();
        }
        var warnings = "";
        for (var warning : results.warnings) {
            if (warnings.length() > 0) {
                warnings += "\n";
            }
            warnings += warning.getFirst() + " " + warning.getSecond();
        }
        entries[0] = name;
        entries[1] = "" + results.valid;
        entries[2] = "" + results.tokenCount;
        entries[3] = "" + results.coverage;
        entries[4] = "" + results.errorCount;
        entries[5] = "" + errors;
        entries[6] = "" + results.warningCount;
        entries[7] = "" + warnings;
        entries[8] = "" + results.preprocessedDirectivesCount;
        writer.writeNext(entries);
        writer.flush();
    }

}
