package koopa.core.util;

import java.io.IOException;
import java.io.UncheckedIOException;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public final class Glob {
    private Glob() {}

    public static Iterator<Path> find(String  dir, String pattern) {
        return find(Paths.get(dir),pattern);
    }

    public static Iterator<Path> find(Path dir, String pattern) {
        var matcher = matcher(pattern);
        var files = directoryStream(dir);
        return new Iterator<Path>() {
            Path next;

            @Override
            public boolean hasNext() {
                while (next == null && files.hasNext()) {
                    var f = files.next();
                    if (Files.isRegularFile(f) && matcher.matches(f.getFileName())) {
                        next = f;
                    }
                }
                return next != null;
            }
            @Override
            public Path next() {
                if (hasNext()) {
                    var path = next;
                    next = null;
                    return path;
                }
                throw new NoSuchElementException();
            }
        };
    }

    public static Iterator<Path> walk(String  dir, String pattern) {
        return walk(Paths.get(dir),pattern);
    }

    public static Iterator<Path> walk(Path dir, String pattern) {
        var matcher = matcher(pattern);
        var dirs = tree(dir);
        return new Iterator<Path>() {
            Path next;

            @Override
            public boolean hasNext() {
                while (next == null && !dirs.isEmpty()) {
                    next = nextFile();
                }
                return next != null;
            }
            @Override
            public Path next() {
                if (hasNext()) {
                    var path = next;
                    next = null;
                    return path;
                }
                throw new NoSuchElementException();
            }

            Path nextFile() {
                while (!dirs.isEmpty()) {
                    var dir = dirs.peek();
                    if (dir.hasNext()) {
                        var next = dir.next();
                        if (Files.isRegularFile(next)) {
                            if (matcher.matches(next.getFileName())) {
                                return next;
                            }
                        } else if (Files.isDirectory(next)) {
                            dirs.push(directoryStream(next));
                        }
                    } else {
                        dirs.pop();
                    }
                }
                return null;
            }
        };
    }

    static LinkedList<Iterator<Path>> tree(Path dir) {
        var dirs = new LinkedList<Iterator<Path>>();
        dirs.add(directoryStream(dir));
        return dirs;
    }

    static PathMatcher matcher(String pattern) {
        return FileSystems.getDefault().getPathMatcher("glob:"+pattern);
    }

    @SuppressWarnings("unchecked")
    static Iterator<Path> directoryStream(Path dir) {
        try {
            var dirs = Files.newDirectoryStream(dir);
            return dirs != null ? dirs.iterator() : Collections.EMPTY_LIST.iterator();
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

}
