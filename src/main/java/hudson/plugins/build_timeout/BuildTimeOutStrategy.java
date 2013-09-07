package hudson.plugins.build_timeout;

import hudson.model.AbstractBuild;
import hudson.model.Describable;
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
}
