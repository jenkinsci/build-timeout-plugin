package hudson.plugins.build_timeout;

import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.model.*;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public abstract class BuildTimeOutStrategy implements Describable<BuildTimeOutStrategy> {

    public static final long MINUTES = 60*1000L;
    public static final Logger LOG = Logger.getLogger(BuildTimeOutStrategy.class.getName());

    /**
     * Define the delay (in milliseconds) to wait for the build to complete before interrupting.
     * @param run
     * @deprecated override {@link #getTimeOut(hudson.model.AbstractBuild, hudson.model.BuildListener)} instead.
     */
    @Deprecated
    public long getTimeOut(@NonNull Run run) {
        throw new UnsupportedOperationException("Implementation required");
    }
    
    /**
     * Define the delay (in milliseconds) to wait for the build to complete before interrupting.
     * @param build the build
     * @param listener the build listener
     */
    @SuppressWarnings("deprecation")
    public long getTimeOut(@NonNull AbstractBuild<?,?> build, @NonNull BuildListener listener)
            throws InterruptedException, MacroEvaluationException, IOException {
        // call through to the old method.
        return getTimeOut(build);
    }

    public long getTimeOut(@NonNull Run build, @NonNull BuildListener listener)
        throws InterruptedException, MacroEvaluationException, IOException {
        return getTimeOut(build);
    }

    /**
     * Called when some output to console.
     * Override this to capture the activity.
     * 
     * @param build
     * @param b output character.
     * 
     * @deprecated use {@link #onWrite(AbstractBuild, byte[], int)}
     * 
     */
    @Deprecated
    public void onWrite(AbstractBuild<?,?> build, int b) {}
    
    /**
     * Called when some output to console.
     * Override this to capture the activity.
     * 
     * @param build
     * @param b output characters.
     * @param length length of b to output
     * 
     */
    public void onWrite(AbstractBuild<?,?> build, byte b[], int length) {
        for(int i = 0; i < length; ++i) {
            onWrite(build, b[i]);
        }
    }
    
    /**
     * Decides whether to call {@link #onWrite(AbstractBuild, byte[], int)}
     * 
     * For performance reason, {@link #onWrite(AbstractBuild, byte[], int)} is called
     * only when subclass overrides it.
     * 
     * @return true to call {@link #onWrite(AbstractBuild, byte[], int)}
     */
    public boolean wantsCaptureLog() {
        try {
            Class<?> classOfOnWrite = getClass().getMethod("onWrite", AbstractBuild.class, int.class).getDeclaringClass();
            Class<?> classOfNewOnWrite = getClass().getMethod("onWrite", AbstractBuild.class, byte[].class, int.class).getDeclaringClass();
            return !BuildTimeOutStrategy.class.equals(classOfOnWrite) || !BuildTimeOutStrategy.class.equals(classOfNewOnWrite);
        } catch(SecurityException|NoSuchMethodException e) {
            LOG.log(Level.WARNING, "Unexpected exception in build-timeout-plugin", e);
            return false;
        }
    }
    
    /**
     * @return
     * @see hudson.model.Describable#getDescriptor()
     */
    @SuppressWarnings("unchecked")
    public Descriptor<BuildTimeOutStrategy> getDescriptor() {
        return Jenkins.getActiveInstance().getDescriptorOrDie(getClass());
    }

    protected final String expandAll(@NonNull AbstractBuild<?, ?> build, @NonNull BuildListener listener, @NonNull String string)
            throws MacroEvaluationException, IOException, InterruptedException {
        return hasMacros(string) ? TokenMacro.expandAll(build, listener, string) : string;
    }

    protected final static boolean hasMacros(@NonNull String value) {
        return value.contains("${");
    }
}
