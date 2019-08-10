package koopa.stage.runtime.generation;

import java.util.LinkedList;

public abstract class TestBuilder {

    LinkedList<String> stack = new LinkedList<>();

    public void push(String fragment) {
        stack.addLast(fragment);
    }

    public void pop() {
        stack.removeLast();
    }

    public void commit() {
        var builder = new StringBuilder();
        for (var fragment : stack) {
            builder.append(fragment);
        }
        ready(builder.toString());
    }

    protected abstract void ready(String text);

}
