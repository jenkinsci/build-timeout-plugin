package hudson.plugins.build_timeout;

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
     * @return
     * @see hudson.model.Describable#getDescriptor()
     */
    @SuppressWarnings("unchecked")
    public Descriptor<BuildTimeOutStrategy> getDescriptor() {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }
}
