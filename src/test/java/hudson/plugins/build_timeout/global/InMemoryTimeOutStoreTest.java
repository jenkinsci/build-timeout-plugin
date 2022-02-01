package hudson.plugins.build_timeout.global;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class InMemoryTimeOutStoreTest {
    private final Map<String, ScheduledFuture<?>> map = new HashMap<>();
    private final TimeOutStore store = new InMemoryTimeOutStore(map);

    @Test
    public void shouldKeep() {
        store.scheduled("a", mock(ScheduledFuture.class));

        assertEquals(1, map.size());
        assertTrue(map.containsKey("a"));
    }

    @Test
    public void shouldNoOpIfKeyAlreadyExists() {
        store.scheduled("a", mock(ScheduledFuture.class));
        assertEquals(1, map.size());

        store.scheduled("a", mock(ScheduledFuture.class));

        assertEquals(1, map.size());
    }

    @Test
    public void shouldRemoveKeyFromMap() {
        store.scheduled("a", mock(ScheduledFuture.class));
        store.scheduled("b", mock(ScheduledFuture.class));

        store.cancel("b");

        assertEquals(1, map.size());
        assertTrue(map.containsKey("a"));
    }

    @Test
    public void shouldCancel() {
        ScheduledFuture<?> a = mock(ScheduledFuture.class);
        ScheduledFuture<?> b = mock(ScheduledFuture.class);
        store.scheduled("a", a);
        store.scheduled("b", b);

        store.cancel("b");

        verifyZeroInteractions(a);
        verify(b).cancel(false);
    }

    @Test
    public void shouldNoOpIfAbsent() {
        store.scheduled("a", mock(ScheduledFuture.class));
        assertEquals(1, map.size());

        store.cancel("c");

        assertEquals(1, map.size());
    }
}
