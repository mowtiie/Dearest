package com.mowtiie.dearest.crash;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CrashHandlerTest {

    @Test
    public void traceKeepsClassesAndFramesButDropsMessages() {
        Throwable failure = new IllegalStateException("search for 'my secret diary line' failed",
                new java.io.IOException("cause quoting private text"));

        String trace = CrashHandler.traceWithoutMessages(failure);

        assertTrue(trace.startsWith("java.lang.IllegalStateException\n"));
        assertTrue(trace.contains("\tat " + CrashHandlerTest.class.getName()));
        assertTrue(trace.contains("Caused by: java.io.IOException\n"));
        assertFalse(trace.contains("secret"));
        assertFalse(trace.contains("private text"));
    }

    @Test
    public void aCauseThatPointsAtItselfDoesNotLoop() {
        Throwable failure = new RuntimeException("x") {
            @Override public synchronized Throwable getCause() { return this; }
        };

        String trace = CrashHandler.traceWithoutMessages(failure);

        assertFalse(trace.contains("Caused by"));
    }

    @Test
    public void aLongCauseChainIsCut() {
        Throwable failure = new RuntimeException();
        for (int i = 0; i < 50; i++) failure = new RuntimeException(failure);

        String trace = CrashHandler.traceWithoutMessages(failure);

        int causes = trace.split("Caused by: ", -1).length - 1;
        assertTrue(causes <= 10);
    }
}
