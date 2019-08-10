package koopa.core.targets;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import koopa.core.data.Data;
import koopa.core.data.Token;

/**
 * This is a {@linkplain Target} which will refrain from forwarding its {@linkplain Data}
 * to another target until instructed to do so via {@link #shiftAllToNextTarget()}).
 * <p>
 * In addition to the delay, you can also retract the data which was pushed
 * up to the point of the last {@link #shiftAllToNextTarget()}.
 */
public class HoldingTarget implements Target {

    /**
     * The recipient of our {@linkplain Data}.
     */
    private final Target target;

    /**
     * The {@linkplain Data} we're holding on to.
     */
    private final LinkedList<Data> queue;

    private final List<RawObserver> rawObservers;

    /**
     * Who wants to be notified about incoming {@linkplain Data} ?
     */
    private final List<Observer> observers;

    private boolean notificationsInProgress = false;

    public HoldingTarget(Target target) {
        assert (target != null);
        this.target = target;
        this.queue = new LinkedList<>();
        this.rawObservers = new LinkedList<>();
        this.observers = new LinkedList<>();
    }

    @Override
    public void push(Data data) {
        queue.addLast(data);
        for (var observer : rawObservers) {
            observer.pushed(data);
        }
        if (!(data instanceof Token)) {
            return;
        }
        var token = (Token) data;
        if (token.isSkipped()) {
            return;
        }
        if (!notificationsInProgress) {
            notificationsInProgress = true;
            for (var observer : observers) {
                observer.pushed(this, token);
            }
            assert (queue.getLast() == data);
            notificationsInProgress = false;
        }
    }

    public Token peekAtLastToken() {
        var it = queue.listIterator(queue.size());
        while (it.hasPrevious()) {
            var d = it.previous();
            if (d instanceof Token) {
                final Token t = (Token) d;
                return t;
            }
        }
        return null;
    }

    @Override
    public void done() {}

    /**
     * Undoes the latest {@link #push(Data)}, returning the data which was pushed.
     */
    public Data pop() {
        assert (!queue.isEmpty());
        var last = queue.removeLast();
        for (var observer : rawObservers) {
            observer.popping(last);
        }
        if (!notificationsInProgress && last instanceof Token) {
            var token = (Token) last;
            if (!token.isSkipped()) {
                notificationsInProgress = true;
                // final Token data = peekAtLastToken();
                for (var observer : observers) {
                    observer.popping(this,token);
                }
                // assert (peekAtLastToken() == data);
                notificationsInProgress = false;
            }
        }
        return last;
    }

    /**
     * This will {@linkplain Target#push(Data)} all data which is being held {@link TokenTracker} the {@link #target}.
     */
    public void shiftAllToNextTarget() {
        while (!queue.isEmpty()) {
            var data = queue.removeFirst();
            target.push(data);
        }
    }

    /**
     * Whether or not we're holding on to {@linkplain Data}.
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * How much {@linkplain Data} we're holding on to.
     */
    public int size() {
        return queue.size();
    }

    /**
     * Get an {@linkplain Iterator} over all {@linkplain Data} being held, in reverse order.
     */
    public Iterator<Data> descendingIterator() {
        return queue.descendingIterator();
    }

    /**
     * Get an {@linkplain Iterator} over all {@linkplain Data} being held, starting at the given index.
     */
    public Iterator<Data> listIterator(int index) {
        return queue.listIterator(index);
    }

    public interface RawObserver {
        void pushed(Data data);
        void popping(Data last);
    }

    public interface Observer {
        void start(HoldingTarget holdingTarget, Token last);
        void pushed(HoldingTarget holdingTarget, Token data);
        void popping(HoldingTarget holdingTarget, Token last);
    }

    public void addObserver(RawObserver observer) {
        rawObservers.add(observer);
    }

    public void removeObserver(RawObserver observer) {
        rawObservers.remove(observer);
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
        if (!notificationsInProgress) {
            notificationsInProgress = true;
            var token = peekAtLastToken();
            observer.start(this, token);
            assert (peekAtLastToken() == token);
            notificationsInProgress = false;
        }
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public String toString() {
        return queue.toString();
    }

}
