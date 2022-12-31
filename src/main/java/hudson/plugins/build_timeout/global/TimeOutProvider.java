package hudson.plugins.build_timeout.global;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.plugins.build_timeout.BuildTimeOutOperation;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface TimeOutProvider {
    Optional<Duration> timeOutFor(AbstractBuild<?,?> build, BuildListener listener);

    Optional<Duration> timeOutFor(AbstractProject<?,?> build, BuildListener listener);

    List<BuildTimeOutOperation> getOperations();
}
