package hudson.plugins.build_timeout.global;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.listeners.RunListener;

import edu.umd.cs.findbugs.annotations.NonNull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Extension
@Singleton
@SuppressWarnings("unused")
public class GlobalTimeOutRunListener extends RunListener<Run<?, ?>> {
    private final ScheduledExecutorService scheduler;
    private final TimeOutProvider timeOutProvider;
    private final TimeOutStore store;

    /**
     * Unused - required by sezpoz
     */
    public GlobalTimeOutRunListener() {
        this(null, null, null);
    }

    @Inject
    public GlobalTimeOutRunListener(@TimeOut ScheduledExecutorService scheduler, TimeOutProvider timeOutProvider, TimeOutStore store) {
        this.scheduler = scheduler;
        this.timeOutProvider = timeOutProvider;
        this.store = store;
    }

    @Override
    public Environment setUpEnvironment(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, Run.RunnerAbortedException {
        timeOutProvider.timeOutFor(build, listener)
                .map(duration -> scheduler.schedule(TimeOutTask.create(timeOutProvider, build, listener, duration),
                        duration.toMillis(),
                        TimeUnit.MILLISECONDS))
                .ifPresent(future -> store.scheduled(build.getExternalizableId(), future));
        return super.setUpEnvironment(build, launcher, listener);
    }

    @Override
    public void onCompleted(Run<?, ?> run, @NonNull TaskListener listener) {
        store.cancel(run.getExternalizableId());
    }
}
