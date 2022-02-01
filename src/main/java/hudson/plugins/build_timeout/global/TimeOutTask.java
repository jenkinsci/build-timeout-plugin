package hudson.plugins.build_timeout.global;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.build_timeout.BuildTimeOutOperation;

import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TimeOutTask implements Runnable {
    private static final Logger log = Logger.getLogger(TimeOutTask.class.getName());
    private final TimeOutProvider timeOutProvider;
    private final AbstractBuild<?, ?> build;
    private final BuildListener listener;
    private final Duration duration;

    private TimeOutTask(TimeOutProvider timeOutProvider, AbstractBuild<?, ?> build, BuildListener listener, Duration duration) {
        this.timeOutProvider = timeOutProvider;
        this.build = build;
        this.listener = listener;
        this.duration = duration;
    }

    @Override
    public void run() {
        List<BuildTimeOutOperation> operations = timeOutProvider.getOperations();
        for (BuildTimeOutOperation operation : operations) {
            try {
                boolean succeeded = operation.perform(build, listener, duration.toMillis());
                if (!succeeded) {
                    log.info(() -> String.format(
                            "%s failed to perform global time out %s after %d minutes - no further operations will be run",
                            build.getExternalizableId(),
                            operation.getClass().getSimpleName(),
                            duration.toMinutes()
                    ));
                    return;
                }
                listener.getLogger().println("[build-timeout] Global time out activated");
                log.fine(() -> String.format(
                        "%s successfully performed global time out %s after %d minutes",
                        build.getExternalizableId(),
                        operation.getClass().getSimpleName(),
                        duration.toMinutes()));
            } catch (RuntimeException e) {
                log.log(Level.WARNING, e, () -> String.format(
                        "%s failed to perform global time out %s after %d minutes - no further operations will be run",
                        build.getExternalizableId(),
                        operation.getClass().getSimpleName(),
                        duration.toMinutes()));
                return;
            }
        }
    }

    public static TimeOutTask create(TimeOutProvider timeOutProvider, AbstractBuild<?, ?> build, BuildListener listener, Duration duration) {
        return new TimeOutTask(timeOutProvider, build, listener, duration);
    }
}
