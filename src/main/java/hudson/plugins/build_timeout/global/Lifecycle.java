package hudson.plugins.build_timeout.global;

import hudson.Extension;
import hudson.init.Terminator;

import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

@Extension
@SuppressWarnings("unused")
public class Lifecycle {
    private static final Logger log = Logger.getLogger(Lifecycle.class.getName());
    private final ScheduledExecutorService scheduler;

    /**
     * Unused - required by sezpoz
     */
    public Lifecycle() {
        this(null);
    }

    @Inject
    public Lifecycle(@TimeOut ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    @Terminator
    public void shutdown() {
        log.fine(() -> "Shutting down Global TimeOut ScheduledExecutorService...");
        List<Runnable> timeOuts = scheduler.shutdownNow();
        log.info(() -> String.format("Shutdown complete - Global TimeOut ScheduledExecutorService had %d tasks pending", timeOuts.size()));
    }
}
