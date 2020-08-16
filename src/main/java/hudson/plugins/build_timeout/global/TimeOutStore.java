package hudson.plugins.build_timeout.global;

import java.util.concurrent.ScheduledFuture;

public interface TimeOutStore {
    void scheduled(String key, ScheduledFuture<?> timeOut);
    void cancel(String key);
}
