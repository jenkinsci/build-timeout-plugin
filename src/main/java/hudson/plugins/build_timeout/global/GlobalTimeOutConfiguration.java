package hudson.plugins.build_timeout.global;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.build_timeout.BuildTimeOutOperation;
import hudson.plugins.build_timeout.BuildTimeOutOperationDescriptor;
import hudson.plugins.build_timeout.BuildTimeOutStrategy;
import hudson.plugins.build_timeout.BuildTimeOutStrategyDescriptor;
import hudson.plugins.build_timeout.operations.AbortOperation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.kohsuke.stapler.StaplerRequest;

import javax.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;

@Extension
public class GlobalTimeOutConfiguration extends GlobalConfiguration implements TimeOutProvider {
    private static final Logger log = Logger.getLogger(GlobalTimeOutConfiguration.class.getName());
    private final transient Jenkins jenkins;
    private BuildTimeOutStrategy strategy;
    private List<BuildTimeOutOperation> operations;

    /**
     * Unused - required by sezpoz
     */
    @SuppressWarnings("unused")
    public GlobalTimeOutConfiguration() {
        this(Jenkins.get());
    }

    @Inject
    public GlobalTimeOutConfiguration(Jenkins jenkins) {
        this.jenkins = jenkins;
        load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
        JSONObject settings = json.getJSONObject("timeout").getJSONObject("global");
        req.bindJSON(this, settings);
        save();
        return true;
    }

    @Override
    public Optional<Duration> timeOutFor(AbstractBuild<?, ?> build, BuildListener listener) {
        try {
            if (strategy == null) {
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
}
