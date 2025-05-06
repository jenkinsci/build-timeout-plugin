package hudson.plugins.build_timeout.global;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class InMemoryTimeOutStoreTest {
    private final Map<String, ScheduledFuture<?>> map = new HashMap<>();
    private final TimeOutStore store = new InMemoryTimeOutStore(map);

    @Test
    void shouldKeep() {
        store.scheduled("a", mock(ScheduledFuture.class));

        assertEquals(1, map.size());
        assertTrue(map.containsKey("a"));
    }

    @Test
    void shouldNoOpIfKeyAlreadyExists() {
        store.scheduled("a", mock(ScheduledFuture.class));
        assertEquals(1, map.size());

        store.scheduled("a", mock(ScheduledFuture.class));

        assertEquals(1, map.size());
    }

    @Test
    void shouldRemoveKeyFromMap() {
        store.scheduled("a", mock(ScheduledFuture.class));
        store.scheduled("b", mock(ScheduledFuture.class));

        store.cancel("b");

        assertEquals(1, map.size());
        assertTrue(map.containsKey("a"));
    }

    @Test
    void shouldCancel() {
        ScheduledFuture<?> a = mock(ScheduledFuture.class);
        ScheduledFuture<?> b = mock(ScheduledFuture.class);
        store.scheduled("a", a);
        store.scheduled("b", b);

        store.cancel("b");

        verifyNoInteractions(a);
        verify(b).cancel(false);
    }

    @Test
    void shouldNoOpIfAbsent() {
        store.scheduled("a", mock(ScheduledFuture.class));
        assertEquals(1, map.size());

        store.cancel("c");

        assertEquals(1, map.size());
    }
}
