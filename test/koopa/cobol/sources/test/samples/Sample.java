package koopa.cobol.sources.test.samples;

import java.nio.file.Files;
import java.nio.file.Path;

import java.io.Reader;
import java.io.StringReader;
import java.io.IOException;
import java.io.UncheckedIOException;

import java.util.ArrayList;
import java.util.List;

import koopa.core.data.Range;
import koopa.core.data.Token;
import koopa.core.sources.LineSplitter;
import koopa.core.sources.Source;
import koopa.core.sources.test.DataValidator;
import koopa.core.trees.Tree;

import static org.junit.jupiter.api.Assertions.*;

public class Sample {

    final String input;
    final List<Range> ranges;
    final List<Annotation> annotations;

    Sample(String input, List<Range> ranges, List<Annotation> annotations) {
        this.input = input;
        this.ranges = ranges;
        this.annotations = annotations;
    }

    public Reader getReader() {
        return new StringReader(input);
    }

    static Sample from(Path sampleFile) {
        List<Block> blocks = allBlocksFrom(sampleFile);
        String input = getInputFrom(blocks);
        List<Range> ranges = getRangesFrom(blocks);
        List<Annotation> annotations = getAnnotationFrom(blocks);
        return new Sample(input, ranges, annotations);
    }

    static List<Block> allBlocksFrom(Path file) {
        try (
            var fileReader = Files.newBufferedReader(file);
        ) {
            var source = new LineSplitter(fileReader);
            var blocks = new ArrayList<Block>();
            for (;;) {
                var block = Block.nextFrom(blocks.size() + 1, source);
                if (block == null) {
                    break;
                } else {
                    blocks.add(block);
                }
            }
            return blocks;
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    static String getInputFrom(List<Block> blocks) {
        var input = new StringBuilder();
        for (var block : blocks) {
            input.append(block.getInput());
        }
        return input.toString();
    }

    static List<Range> getRangesFrom(List<Block> blocks) {
        var ranges = new ArrayList<Range>();
        Range incompleteRange = null;
        for (var block : blocks) {
            var blockRanges = block.getRanges();
            if (blockRanges == null) {
                continue;
            }
            for (var range : blockRanges) {
                var continuesARange = range.getStart() == null;
                var rangeIsContinued = range.getEnd() == null;
                if (continuesARange) {
                    if (incompleteRange == null) {
                        throw new IllegalStateException("Unexpected continuation of a range.");
                    }
                    incompleteRange = new Range(incompleteRange.getStart(),    range.getEnd());
                    if (!rangeIsContinued) {
                        ranges.add(incompleteRange);
                        incompleteRange = null;
                    }
                } else if (rangeIsContinued) {
                    if (incompleteRange != null) {
                        throw new IllegalStateException("Incomplete range.");
                    }
                    incompleteRange = range;
                } else {
                    ranges.add(range);
                }
            }
        }
        return ranges;
    }

    static List<Annotation> getAnnotationFrom(List<Block> blocks) {
        var annotations = new ArrayList<Annotation>();
        for (var block : blocks) {
            if (block.hasAnnotations()) {
                annotations.addAll(block.getAnnotations());
            } else if (block.getRanges() != null) {
                annotations.addAll(emptyAnnotations(block.getRanges().size()));
            }
        }
        return annotations;
    }

    static List<Annotation> emptyAnnotations(int size) {
        var l = new ArrayList<Annotation>(size);
        for (var i = 0; i < size; i++) {
            l.add(null);
        }
        return l;
    }

    /**
     * Verifies that the tokens coming from the given source align with the expected output as defined in the sample.
     */
    void assertOutputIsAsExpected(Source source, DataValidator validator) {
        var i = 0;
        for (;;) {
            if (i >= ranges.size()) {
                assertNull(source.next(), "Was not expecting any more tokens.");
                break;
            }
            var annotation = annotations.get(i);
            var range = ranges.get(i);
            var d = source.next();
            if (d == null) {
                fail("Expected data at line " + range.getStart().getPositionInFile()
                    + ", positions " + range.getStart().getPositionInLine()
                    + "--" + range.getEnd().getPositionInLine() );
            } else if (d instanceof Token) {
                var token = (Token) d;
                if (token.isReplacement()) {
                    continue;
                }
                validate(range, annotation, token, validator);
            } else if (d instanceof Tree) {
                validate(range, annotation, (Tree) d, validator);
            } else {
                // Don't care.
                continue;
            }
            i += 1;
        }
    }

    void validate(Range range, Annotation annotation, Token token, DataValidator validator) {
        var message = "Expected a token at line " + range.getStart().getPositionInFile()
            + ", positions " + range.getStart().getPositionInLine()
            + "--" + range.getEnd().getPositionInLine()
            + ". Found " + token + " instead.";

        assertEquals(range.getStart().getLinenumber(), token.getStart().getLinenumber(), message);
        assertEquals(range.getStart().getPositionInLine(), token.getStart().getPositionInLine(), message);
        assertEquals(range.getEnd().getLinenumber(), token.getEnd().getLinenumber(), message);
        assertEquals(range.getEnd().getPositionInLine(), token.getEnd().getPositionInLine(), message);

        if (annotation != null) {
            var required = annotation.getRequired();
            if (required != null && !required.isEmpty()) {
                for (var category : required) {
                    validator.validate(token, category, true);
                }
            }
            var forbidden = annotation.getForbidden();
            if (forbidden != null && !forbidden.isEmpty()) {
                for (var category : forbidden) {
                    validator.validate(token, category, false);
                }
            }
        }
    }

    void validate(Range range, Annotation annotation, Tree tree, DataValidator validator) {
        var message = "Expected a tree at line " + range.getStart().getPositionInFile()
            + ", positions " + range.getStart().getPositionInLine()
            + "--" + range.getEnd().getPositionInLine()
            + ". Found " + tree + " instead.";

        assertEquals(range.getStart().getLinenumber(), tree.getStartPosition().getLinenumber(), message);
        assertEquals(range.getStart().getPositionInLine(), tree.getStartPosition().getPositionInLine(), message);
        assertEquals(range.getEnd().getLinenumber(), tree.getEndPosition().getLinenumber(), message);
        assertEquals(range.getEnd().getPositionInLine(), tree.getEndPosition().getPositionInLine(), message);

        if (annotation != null) {
            var required = annotation.getRequired();
            if (required != null && !required.isEmpty()) {
                for (var category : required) {
                    validator.validate(tree, category, true);
                }
            }
            var forbidden = annotation.getForbidden();
            if (forbidden != null && !forbidden.isEmpty()) {
                for (var category : forbidden) {
                    validator.validate(tree, category, false);
                }
            }
        }
    }

}
