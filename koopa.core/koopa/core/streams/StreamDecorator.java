package koopa.core.streams;

import java.util.Iterator;

import koopa.core.data.Data;
import koopa.core.data.Marker;
import koopa.core.parsers.Parse;
import koopa.core.parsers.Stream;

public abstract class StreamDecorator implements Stream {

    protected final Stream stream;

    public StreamDecorator(Stream stream) {
        assert (stream != null);
        this.stream = stream;
    }

    @Override
    public Data forward() {
        return stream.forward();
    }

    @Override
    public Data skip() {
        return stream.skip();
    }

    @Override
    public void insert(Marker marker) {
        stream.insert(marker);
    }

    @Override
    public void rewind(Data d) {
        stream.rewind(d);
    }

    @Override
    public Data peek() {
        return stream.peek();
    }

    @Override
    public String peekMore() {
        return stream.peekMore();
    }

    @Override
    public void bookmark() {
        stream.bookmark();
    }

    @Override
    public void rewind() {
        stream.rewind();
    }

    @Override
    public void commit() {
        stream.commit();
    }

    @Override
    public Parse getParse() {
        return stream.getParse();
    }

    @Override
    public void setParse(Parse parse) {
        stream.setParse(parse);
    }

    @Override
    public Iterator<Data> backToBookmarkIterator() {
        return stream.backToBookmarkIterator();
    }

    @Override
    public Iterator<Data> fromBookmarkIterator() {
        return stream.fromBookmarkIterator();
    }

    @Override
    public BaseStream getBaseStream() {
        return stream.getBaseStream();
    }

}
