package hudson.plugins.build_timeout.global;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

public class InMemoryTimeOutStore implements TimeOutStore {
    private static final Logger log = Logger.getLogger(InMemoryTimeOutStore.class.getName());
    private final Map<String, ScheduledFuture<?>> map;

    public InMemoryTimeOutStore(Map<String, ScheduledFuture<?>> map) {
        this.map = map;
    }

    @Override
    public void scheduled(String key, ScheduledFuture<?> timeOut) {
        ScheduledFuture<?> previous = map.putIfAbsent(key, timeOut);
        boolean added = previous == null;
        if (added) {
            log.fine(() -> String.format("%s time out stored", key));
        } else {
            log.fine(() -> String.format("%s time out already present - skipping", key));
        }
    }

    @Override
    public void cancel(String key) {
        ScheduledFuture<?> future = map.remove(key);
        if (future == null) {
            log.fine(() -> String.format("%s time out not found - skipping", key));
            return;
        }
        boolean cancelled = future.cancel(false);
        if (cancelled) {
            log.fine(() -> String.format("%s time out cancellation succeeded", key));
        }
        log.fine(() -> String.format("tracking %d global time out(s)", map.size()));
    }
}
