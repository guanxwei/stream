package org.stream.extension.utils;

import static org.testng.Assert.*;

import org.stream.core.exception.GraphLoadException;
import org.stream.core.exception.WorkFlowExecutionException;
import org.stream.extension.utils.actionable.Tellme;
import org.stream.extension.utils.actionable.Value;
import org.stream.extension.utils.actionable.exception.FixException;
import org.testng.annotations.Test;

@Test
public class TestTellme {
    
    @SuppressWarnings("unchecked")
    @Test
    public void testThenFix() {
        var value = Value.of(1);
        Tellme.tryIt(() -> {
            value.change(2);
            throw new RuntimeException();
        }).incase(RuntimeException.class).thenFix(e -> {
            value.change(3);
        }).incase(WorkFlowExecutionException.class).thenFix(e -> {
            value.change(4);
        });
        assertEquals(value.get(Integer.class), 3);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRegardless() {
        var value = Value.of(1);
        try {
            Tellme.tryIt(() -> {
                value.change(2);
                throw new RuntimeException("hello");
            }).incase(RuntimeException.class).thenFix(e -> {
                value.change(3);
                throw new RuntimeException("haha");
            }).incase(WorkFlowExecutionException.class).thenFix(e -> {
                value.change(4);
            }).incase(GraphLoadException.class).thenFix(e -> {
                value.change(5);
            }).reagardless(() -> {
                assertEquals(value.get(Integer.class), 3);
                value.change(8);
            });
        } catch (Throwable t) {
            assertTrue(t instanceof FixException);
            assertTrue(t.getCause() instanceof RuntimeException);
            assertEquals(t.getCause().getMessage(), "haha");
            assertEquals(value.get(Integer.class), 8);
        }
    }
}
