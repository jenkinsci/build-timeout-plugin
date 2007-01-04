package hudson.plugins.build_timeout;

import hudson.Plugin;
import hudson.tasks.BuildWrappers;

/**
 * @plugin
 * @author Kohsuke Kawaguchi
 */
public class PluginImpl extends Plugin {
    public void start() {
        BuildWrappers.WRAPPERS.add(BuildTimeoutWrapper.DESCRIPTOR);
    }
}
