package hudson.plugins.build_timeout;

import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import hudson.model.AbstractBuild;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Run;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public abstract class BuildTimeOutStrategy implements Describable<BuildTimeOutStrategy> {

    public static final long MINUTES = 60*1000L;
    public static final Logger LOG = Logger.getLogger(BuildTimeOutStrategy.class.getName());

    /**
     * Define the delay (in milliseconds) to wait for the build to complete before interrupting.
     * @param run
     */
    public abstract long getTimeOut(Run run);
    
    /**
     * Called when some output to console.
     * Override this to capture the activity.
     * 
     * @param build
     * @param b output character.
     */
    public void onWrite(AbstractBuild<?,?> build, int b) {}
    
    /**
     * Decides whether to call {@link BuildTimeOutStrategy.onWrite}
     * 
     * For performance reason, {@link BuildTimeOutStrategy.onWrite} is called
     * only when subclass overrides it.
     * 
     * @return true to call {@link BuildTimeOutStrategy.onWrite}
     */
    public boolean wantsCaptureLog() {
        try {
            Class<?> classOfOnWrite = getClass().getMethod("onWrite", AbstractBuild.class, int.class).getDeclaringClass();
            return !BuildTimeOutStrategy.class.equals(classOfOnWrite);
        } catch(SecurityException e) {
            LOG.log(Level.WARNING, "Unexpected exception in build-timeout-plugin", e);
            return false;
        } catch(NoSuchMethodException e) {
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
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }
}
