package org.stream.extension.circuit;

import java.util.HashSet;
import java.util.Set;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;

/**
 * Default implement of {@link CircuitBreaker}.
 *
 * DefaultCircuitBreaker will try to trace the execution step by saving execution pair information in the thread local,
 * where a execution pair is constructed by a previous node's name & a next node's name.
 * If the breaker find that a execution pair equals with the incoming pair, true will be returned by method {@link #outOfControl(Node, Node, ActivityResult)}
 * otherwise false will be returned.
 * @author 魏冠雄
 *
 */
public class DefaultCircuitBreaker implements CircuitBreaker {

    private ThreadLocal<Set<String>> pairs = new ThreadLocal<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen(final Node previous, final Node next, final ActivityResult activityResult) {
        String first = previous == null ? "NULL" : previous.getNodeName();
        String second = next == null ? "NULL" : next.getNodeName();
        String pair = first + second + activityResult.name();
        if (pairs.get() == null) {
            pairs.set(new HashSet<>());
        }
        if (pairs.get().contains(pair)) {
            pairs.get().clear();
            return true;
        }

        pairs.get().add(pair);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Node open(final Graph graph, final Node previous) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        pairs.get().clear();
    }

}