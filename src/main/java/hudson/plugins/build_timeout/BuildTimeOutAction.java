package hudson.plugins.build_timeout;

import hudson.model.InvisibleAction;

/**
 * Use this job action to save the Timeout info.
 *
 */
public class BuildTimeOutAction extends InvisibleAction {

    private final String reason;

    public BuildTimeOutAction(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
