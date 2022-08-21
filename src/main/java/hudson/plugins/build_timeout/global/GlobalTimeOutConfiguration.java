package hudson.plugins.build_timeout.global;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.plugins.build_timeout.*;
import hudson.plugins.build_timeout.operations.AbortOperation;
import hudson.tasks.Builder;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.kohsuke.stapler.StaplerRequest;

import javax.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.logging.Level.WARNING;

@Extension
public class GlobalTimeOutConfiguration extends GlobalConfiguration implements TimeOutProvider {
    private static final Logger log = Logger.getLogger(GlobalTimeOutConfiguration.class.getName());
    private transient Jenkins jenkins;
    private BuildTimeOutStrategy strategy;
    private List<BuildTimeOutOperation> operations;
    private boolean overwriteable;

    public GlobalTimeOutConfiguration() {
        load();
    }

    @Inject
    public void setJenkins(Jenkins jenkins) {
        this.jenkins = jenkins;
    }

    /**
     * Unchecking the 'Enable Global Timeout' box sends a null object for
     * timeout.global in the json representation of the form submission.
     *
     * Binding this to the current class leaves the previous values.
     * To get around this binding problem, we explicitly null out these fields
     * before saving.
     *
     * @param req the request object
     * @param json form data as json
     * @return always true
     */
    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
        JSONObject settings = json.getJSONObject("timeout").getJSONObject("global");
        if (settings.isNullObject()) {
            strategy = null;
            operations = null;
            overwriteable = false;
            log.info("global timeout has been cleared");
        } else {
            req.bindJSON(this, settings);
            log.info(() -> String.format("global timeout updated to %s with operations: %s", strategy, describeOperations()));
        }
        save();
        return true;
    }

    @Override
    public synchronized void load() {
        super.load();
        log.info(() -> {
            if (strategy == null) {
                return "global timeout not set";
            }
            return String.format("global timeout loaded as %s with operations: %s", strategy, describeOperations());
        });
    }

    @Override
    public Optional<Duration> timeOutFor(AbstractBuild<?, ?> build, BuildListener listener) {
        try {
            List<Builder> builders = ((Project<?, ?>) build.getProject()).getBuilders();
            Optional<Builder> timeoutBuildStep = builders.stream().filter(builder -> builder instanceof BuildStepWithTimeout).findAny();

            if (strategy == null || (timeoutBuildStep.isPresent() && isOverwriteable())) {
                return Optional.empty();
            }
            return Optional.of(Duration.ofMillis(strategy.getTimeOut(build, listener)));
        } catch (InterruptedException | MacroEvaluationException | IOException e) {
            log.log(WARNING, e, () -> String.format("%s failed to determine time out", build.getExternalizableId()));
            return Optional.empty();
        }
    }

    public boolean isEnabled() {
        return strategy != null;
    }

    public boolean isOverwriteable() {
        return overwriteable;
    }
    
    
    public void setOverwriteable(boolean overwriteable) {
        this.overwriteable = overwriteable;
    }

    @Override
    public List<BuildTimeOutOperation> getOperations() {
        List<BuildTimeOutOperation> nullSafe = operations == null ? new LinkedList<>() : operations;
        if (nullSafe.isEmpty()) {
            nullSafe.add(new AbortOperation());
        }
        return nullSafe;
    }

    public void setOperations(List<BuildTimeOutOperation> operations) {
        this.operations = operations;
    }

    public BuildTimeOutStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(BuildTimeOutStrategy strategy) {
        this.strategy = strategy;
    }

    public List<BuildTimeOutStrategyDescriptor> getAllStrategies() {
        return jenkins.getDescriptorList(BuildTimeOutStrategy.class);
    }

    public List<BuildTimeOutOperationDescriptor> getAllOperations() {
        return jenkins.getDescriptorList(BuildTimeOutOperation.class);
    }

    private String describeOperations() {
        List<BuildTimeOutOperation> nullSafe = operations == null ? Collections.emptyList() : operations;
        if (nullSafe.isEmpty()) {
            return "(none)";
        }
        return nullSafe.stream()
                .map(BuildTimeOutOperation::getClass)
                .map(Class::getSimpleName)
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
