package hudson.plugins.build_timeout;

import hudson.model.BuildListener;
import jenkins.model.Jenkins;
import hudson.model.AbstractBuild;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Run;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

import java.io.IOException;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public abstract class BuildTimeOutStrategy implements Describable<BuildTimeOutStrategy> {

    public static final long MINUTES = 60*1000L;

    /**
     * Define the delay (in milliseconds) to wait for the build to complete before interrupting.
     * @param run
     * @deprecated override {@link #getTimeOut(hudson.model.AbstractBuild, hudson.model.BuildListener)} instead.
     */
    @Deprecated
    public long getTimeOut(Run run) {
        throw new UnsupportedOperationException("Implementation required");
    }
    
    /**
     * Define the delay (in milliseconds) to wait for the build to complete before interrupting.
     * @param build the build
     * @param listener the build listener
     */
    @SuppressWarnings("deprecation")
    public long getTimeOut(AbstractBuild<?,?> build, BuildListener listener)
            throws InterruptedException, MacroEvaluationException, IOException {
        // call through to the old method.
        return getTimeOut(build);
    }

    /**
     * Called when some output to console.
     * Override this to capture the activity.
     * 
     * @param build
     * @param b output character.
     */
    public void onWrite(AbstractBuild<?,?> build, int b) {}
    
    /**
     * @return
     * @see hudson.model.Describable#getDescriptor()
     */
    @SuppressWarnings("unchecked")
    public Descriptor<BuildTimeOutStrategy> getDescriptor() {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    protected final String expandAll(AbstractBuild<?, ?> build, BuildListener listener, String string)
            throws MacroEvaluationException, IOException, InterruptedException {
        return string.contains("${") ? TokenMacro.expandAll(build, listener, string) : string;
    }

}
