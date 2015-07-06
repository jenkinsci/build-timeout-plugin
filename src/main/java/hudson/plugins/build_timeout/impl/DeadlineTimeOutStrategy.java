package hudson.plugins.build_timeout.impl;

import hudson.Extension;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.plugins.build_timeout.BuildTimeOutStrategy;
import hudson.plugins.build_timeout.BuildTimeOutStrategyDescriptor;
import hudson.util.FormValidation;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * If the build reaches <tt>deadlineTime</tt>, it will be terminated.
 * 
 * @author Fernando Migu�lez Palomo <fernando.miguelez@gmail.com>
 */
public class DeadlineTimeOutStrategy extends BuildTimeOutStrategy {

    public static final int MINIMUM_DEADLINE_TOLERANCE_IN_MINUTES = 1;

    protected static final String DEADLINE_REGEXP = new String("[0-2]?[0-9]:[0-5][0-9](:[0-5][0-9])?");

    protected static final SimpleDateFormat TIME_LONG_FORMAT = new SimpleDateFormat("H:mm:ss");
    protected static final SimpleDateFormat TIME_SHORT_FORMAT = new SimpleDateFormat("H:mm");
    protected static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd H:mm:ss");

    private final String deadlineTime;
    private final int deadlineToleranceInMinutes;

    /**
     * @return deadline time
     */
    public String getDeadlineTime() {
        return deadlineTime;
    }

    /**
     * @return deadline tolerance in minutes
     */
    public int getDeadlineToleranceInMinutes() {
        return deadlineToleranceInMinutes;
    }

    @DataBoundConstructor
    public DeadlineTimeOutStrategy(String deadlineTime, int deadlineToleranceInMinutes) {
        this.deadlineTime = deadlineTime;
        this.deadlineToleranceInMinutes = deadlineToleranceInMinutes <= MINIMUM_DEADLINE_TOLERANCE_IN_MINUTES ? MINIMUM_DEADLINE_TOLERANCE_IN_MINUTES
                : deadlineToleranceInMinutes;
    }

    @Override
    public long getTimeOut(AbstractBuild<?, ?> build, BuildListener listener) throws InterruptedException,
            MacroEvaluationException, IOException, IllegalArgumentException {

        Calendar now = Calendar.getInstance();
        Calendar deadlineTimestamp = Calendar.getInstance();

        String expandedDeadlineTime = expandAll(build, listener, deadlineTime);

        deadlineTimestamp.setTime(parseDeadline(expandedDeadlineTime));
        deadlineTimestamp.set(Calendar.YEAR, now.get(Calendar.YEAR));
        deadlineTimestamp.set(Calendar.MONTH, now.get(Calendar.MONTH));
        deadlineTimestamp.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

        Calendar deadlineTimestampWithTolerance = (Calendar) deadlineTimestamp.clone();
        deadlineTimestampWithTolerance.add(Calendar.MINUTE, deadlineToleranceInMinutes);

        if (deadlineTimestamp.compareTo(now) <= 0 && deadlineTimestampWithTolerance.compareTo(now) > 0) {
            // Deadline time is a past moment but inside tolerance period. Interrupt build immediately. 
            listener.getLogger().println(
                    Messages.DeadlineTimeOutStrategy_ImmediatelyAbort(expandedDeadlineTime,
                            deadlineToleranceInMinutes));
            return 0;
        }

        while (deadlineTimestamp.before(now)) {
            // Deadline time is a past moment. Increment one day till deadline timestamp is in the future.  
            deadlineTimestamp.add(Calendar.DAY_OF_MONTH, 1);
        }

        listener.getLogger().println(
                Messages.DeadlineTimeOutStrategy_NextDeadline(TIMESTAMP_FORMAT.format(deadlineTimestamp
                        .getTime())));

        return deadlineTimestamp.getTimeInMillis() - now.getTimeInMillis();
    }

    private static Date parseDeadline(String deadline) throws IllegalArgumentException {

        if (deadline.matches(DEADLINE_REGEXP)) {
            try {
                if (deadline.length() > 5) {
                    return TIME_LONG_FORMAT.parse(deadline);
                } else {
                    return TIME_SHORT_FORMAT.parse(deadline);
                }
            } catch (ParseException e) {
            }
        }

        throw new IllegalArgumentException(Messages.DeadlineTimeOutStrategy_InvalidDeadlineFormat(deadline));
    }

    @Extension
    public static class DescriptorImpl extends BuildTimeOutStrategyDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.DeadlineTimeOutStrategy_DisplayName();
        }

        public FormValidation doCheckDeadlineTime(@QueryParameter String value) {
            if (hasMacros(value)) {
                return FormValidation.warning(Messages.DeadlineTimeOutStrategy_DeadlineFormatWithMacros());
            } else {
                try {
                    parseDeadline(value);
                    return FormValidation.ok();
                } catch (IllegalArgumentException e) {
                    return FormValidation.error(e.getMessage());
                }
            }
        }
    }
}
