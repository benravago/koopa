package koopa.cobol;

import java.nio.file.Path;
import java.io.Reader;

import koopa.cobol.sources.CompilerDirectives;
import koopa.cobol.sources.ContinuationOfLines;
import koopa.cobol.sources.CopyInclude;
import koopa.cobol.sources.InlineComments;
import koopa.cobol.sources.ProgramArea;
import koopa.cobol.sources.Replace;
import koopa.cobol.sources.Replacing;
import koopa.core.sources.LineSplitter;
import koopa.core.sources.Source;
import koopa.core.sources.StackOfSources;
import koopa.core.sources.TokenSeparator;
import koopa.core.util.LineEndings;

public final class CobolTokens {
    private CobolTokens() {}

    // TODO Expect project to be non-null ?
    public static Source getNewSource(Path file, Reader reader, CobolProject project) {

        var grammar = project.getGrammar();

        // Note: The logical unit is a source line, I think.
        // You should never ask for the next line if you haven't resolved the current one.
        var inputStack = new StackOfSources();

        var lineEndings = LineEndings.getChoices();
        var lineSplitter = new LineSplitter(file, reader, lineEndings);

        inputStack.push(lineSplitter);

        // * Detect compiler directives, source format and source listing statements.
        //   Apply source format switches.
        var format = project.getDefaultFormat();
        var compilerDirectives = new CompilerDirectives(inputStack, format);

        // * Split lines according to the source format.
        var tabLength = project.getTabLength(file);
        var programArea = new ProgramArea(compilerDirectives, tabLength);

        // * Split program text into tokens.
        var tokenSeparator = new TokenSeparator(programArea);

        // * Inline comments.
        var inlineComments = new InlineComments(tokenSeparator);

        Source optionalCopybookExpansion;

        var preprocessing = project.isPreprocessing(file);
        if (!preprocessing) {
            // COPY statements will be left alone.
            optionalCopybookExpansion = inlineComments;
        } else {
            // REPLACE statements will be expanded and applied.

            // * Handle COPY includes.
            var copyInclude = new CopyInclude(inlineComments, grammar, project, inputStack);
            // * Handle COPY REPLACING.
            var copyReplacing = new Replacing(copyInclude);
            // This stage is tested in koopa.cobol.parser.preprocessing.test.PreprocessingSourceTest2
            optionalCopybookExpansion = copyReplacing;
        }

        // * Line continuations.
        var continuationOfLines = new ContinuationOfLines(optionalCopybookExpansion);

        // We may or may not be activating replacements.
        // This variable will just track what we chose in the end.
        Source optionalReplaceStatements;

        // We don't need actual copybook paths to implement REPLACE statements.
        // But we do use it as a marker to see whether we should actually be expanding them.
        if (!preprocessing) {
            // REPLACE statements will be left alone.
            optionalReplaceStatements = continuationOfLines;
        } else {
            // REPLACE statements will be expanded and applied.

            // * Handle REPLACE statements.
            var replace = new Replace(continuationOfLines, grammar);
            // * Handle actual replacements.
            var replacing = new Replacing(replace);
            // This stage is tested in koopa.cobol.parser.preprocessing.test.PreprocessingSourceTest
            optionalReplaceStatements = replacing;
        }
        return optionalReplaceStatements;
    }

    public static Source getNewSource(Reader reader, CobolProject project) {
        return getNewSource(null, reader, project);
    }

}
