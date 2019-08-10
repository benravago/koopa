package koopa.core.util.test;

import java.nio.file.Path;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;

public abstract class FileBasedTestSuite extends TestShell {

    @Override
    protected Iterator<Entry<String,Runnable>> runnableTests() {
        return getRunners(this);
    }

    protected abstract Iterator<Path> getFiles();
    protected abstract void testFile(Path source);

    static Iterator<Entry<String,Runnable>> getRunners(FileBasedTestSuite suite) {
        var files = suite.getFiles();
        return new Iterator<Entry<String,Runnable>>() {
            @Override
            public boolean hasNext() {
                return files.hasNext();
            }
            @Override
            public Entry<String, Runnable> next() {
                if (hasNext()) {
                    var file = files.next();
                    var name = fileName(file);
                    return new SimpleEntry<String,Runnable>( name, () -> suite.testFile(file) );
                }
                throw new NoSuchElementException();
            }
        };
    }

    static String fileName(Path source) {
        var s = source.getFileName().toString();
        var p = s.lastIndexOf('.');
        return (p < 0) ? s : s.substring(0, p);
    }

}
