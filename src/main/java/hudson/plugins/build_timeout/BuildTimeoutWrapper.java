package hudson.plugins.build_timeout;

import static hudson.util.TimeUnit2.MILLISECONDS;
import static hudson.util.TimeUnit2.MINUTES;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.queue.Executables;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tasks.Mailer;
import hudson.triggers.SafeTimerTask;
import hudson.triggers.Trigger;
import hudson.util.TimeUnit2;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;
import java.util.StringTokenizer;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import jenkins.model.CauseOfInterruption;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;


/**
 * {@link BuildWrapper} that terminates a build if it's taking too long.
 *
 * @author Kohsuke Kawaguchi
 */
@SuppressWarnings("rawtypes")
public class BuildTimeoutWrapper extends BuildWrapper {
    
    private static int MINIMUM_TIMEOUT_MINUTES_DEFAULT = 3;
    
    public static long MINIMUM_TIMEOUT_MILLISECONDS = Long.getLong(BuildTimeoutWrapper.class.getName()+ ".MINIMUM_TIMEOUT_MILLISECONDS",
            MINIMUM_TIMEOUT_MINUTES_DEFAULT * 60 * 1000);

    
    public static final String ABSOLUTE = "absolute";
    public static final String ELASTIC = "elastic";
    public static final String STUCK = "likelyStuck";
    
    /**
     * If the build took longer than this amount of minutes,
     * it will be terminated.
     */
    public int timeoutMinutes;

    /**
     * Fail the build.
     */
    public boolean failBuild;
    
    /**
     * Abort the build.
     */
    public Boolean abortBuild;
    
    
    public SendMail sendMail;
    
    /**
     * Writing the build description when timeout occurred.
     */
    public boolean writingDescription;

    /**
     * The percentage of the mean of the duration of the last n successful builds
     * to wait before killing the build.
     * 
     * IE, if the last n successful builds averaged a 10 minute duration,
     * then 200% of that would be 20 minutes.
     */
    public int timeoutPercentage;
    
    /**
     * Values can be "elastic" or "absolute"
     */
    public String timeoutType;
    
    /**
     * The timeout to use if there are no valid builds in the build 
     * history (ie, no successful or unstable builds)
     */
    public Integer timeoutMinutesElasticDefault;
    
    @DataBoundConstructor
    public BuildTimeoutWrapper(int timeoutMinutes, boolean failBuild, boolean abortBuild, boolean writingDescription,
                               int timeoutPercentage, int timeoutMinutesElasticDefault, String timeoutType,
                               SendMail sendMail) {
        this.timeoutMinutes = Math.max(MINIMUM_TIMEOUT_MINUTES_DEFAULT,timeoutMinutes);
        this.failBuild = failBuild;
        this.abortBuild = abortBuild;
        this.writingDescription = writingDescription;
        this.timeoutPercentage = timeoutPercentage;
        this.timeoutMinutesElasticDefault = Math.max(MINIMUM_TIMEOUT_MINUTES_DEFAULT, timeoutMinutesElasticDefault);
        this.timeoutType = timeoutType;
        this.sendMail = sendMail;
    }
    
    @Override
    public Environment setUp(final AbstractBuild build, Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
        class EnvironmentImpl extends Environment {
            final class TimeoutTimerTask extends SafeTimerTask {
                private final AbstractBuild build;
                private final BuildListener listener;
                //Did the task timeout?
                public boolean timeout= false;

                private TimeoutTimerTask(AbstractBuild build, BuildListener listener) {
                    this.build = build;
                    this.listener = listener;
                }

                public void doRun() {
                    // timed out
                    timeout=true;
                    
                    long effectiveTimeoutMinutes = MINUTES.convert(effectiveTimeout,MILLISECONDS);
                    final String msg;
                    if (failBuild) {
                        msg = Messages.Timeout_Message(effectiveTimeoutMinutes, Messages.Timeout_Failed());
                    } else if (abortBuild){
                        msg = Messages.Timeout_Message(effectiveTimeoutMinutes, Messages.Timeout_Aborted());
                    } else {
                        msg = Messages.Timeout_Message2(effectiveTimeoutMinutes);
                    }

                    listener.getLogger().println(msg);
                    if (writingDescription) {
                        try {
                            String description = build.getDescription();
                            description = description != null ? description + "<br/>" + msg : msg;
                            build.setDescription(description);
                        } catch (IOException e) {
                            listener.getLogger().println("failed to write to the build description!");
                        }
                    }

                    
                    if (sendMail != null) {
                        sendTimeoutMail(effectiveTimeoutMinutes);
                    }
                    
                    Executor e = build.getExecutor();
                    if (e != null) {
                        if (failBuild) e.interrupt(Result.FAILURE, new TimeoutInterruption(msg));
                        if (abortBuild) e.interrupt(Result.ABORTED, new TimeoutInterruption(msg));
                    }
                }

                private void sendTimeoutMail(long effectiveTimeoutMinutes) {
                    try {
                        MimeMessage mimeMsg = new MimeMessage(Mailer.descriptor().createSession());
                        mimeMsg.setSubject("Build " + build.getFullDisplayName() + ": timeout");
                        
                        @SuppressWarnings("deprecation")
                        String body = "Build " + build.getFullDisplayName() + " timed out after "
                                + effectiveTimeoutMinutes + " minutes.\n"
                                        + "See " + build.getAbsoluteUrl();
                        
                        mimeMsg.setText(body);
                        mimeMsg.setFrom(new InternetAddress(Mailer.descriptor().getAdminAddress()));
                        
                        StringTokenizer tokens = new StringTokenizer(sendMail.recipients);
                        while(tokens.hasMoreTokens()) {
                            Address address = new InternetAddress(tokens.nextToken());
                            mimeMsg.setRecipient(RecipientType.TO, address);
                        }
                        
                        Transport.send(mimeMsg);
                        listener.getLogger().println("Sent timeout mail to "+sendMail.recipients);
                    } catch (MessagingException e) {
                        listener.error("BuildTimeoutPlugin: failed to send mail:" + e.getMessage());
                    }
                }
            }

            private final TimeoutTimerTask task;
            
            private final long effectiveTimeout;
            
            public EnvironmentImpl() {
                long timeout;
                if (ELASTIC.equals(timeoutType)) {
                    timeout = getEffectiveTimeout(timeoutMinutes * 60L * 1000L, timeoutPercentage,
                            timeoutMinutesElasticDefault * 60*1000, timeoutType, build);
                } else if (STUCK.equals(timeoutType)) {
                    timeout = getLikelyStuckTime();
                } else {
                    timeout = timeoutMinutes * 60L * 1000L;
                }

                this.effectiveTimeout = timeout;
                task = new TimeoutTimerTask(build, listener);
                Trigger.timer.schedule(task, timeout);
            }

            /**
             * Get the time considered it stuck.
             * 
             * @return 10 times as much as eta if eta is available, else 24 hours.
             * @see Executor#isLikelyStuck()
             */
            private long getLikelyStuckTime() {
                Executor executor = build.getExecutor();
                if (executor == null) {
                    return TimeUnit2.HOURS.toMillis(24);
                }

                Queue.Executable executable = executor.getCurrentExecutable();
                if (executable == null) {
                    return TimeUnit2.HOURS.toMillis(24);
                }

                long eta = Executables.getEstimatedDurationFor(executable);
                if (eta >= 0) {
                    return eta * 10;
                } else {
                    return TimeUnit2.HOURS.toMillis(24);
                }
            }

            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                task.cancel();
                return (!task.timeout ||!failBuild);
            }
        }

        return new EnvironmentImpl();
    }

    static long getEffectiveTimeout(long timeoutMilliseconds, int timeoutPercentage, int timeoutMillsecondsElasticDefault,
            String timeoutType, Run run) {
        
        if (ELASTIC.equals(timeoutType)) {
            double elasticTimeout = getElasticTimeout(timeoutPercentage, run);
            if (elasticTimeout == 0) {
                return Math.max(MINIMUM_TIMEOUT_MILLISECONDS, timeoutMillsecondsElasticDefault);
            } else {
                return (long) Math.max(MINIMUM_TIMEOUT_MILLISECONDS, elasticTimeout);    
            }
        } else {
            return (long) Math.max(MINIMUM_TIMEOUT_MILLISECONDS, timeoutMilliseconds);    
        }
    }
    
    private static double getElasticTimeout(int timeoutPercentage, Run run) {
        long averageDuration = run.getEstimatedDuration();
        if (averageDuration <= 0) return 0;
        
        return timeoutPercentage * averageDuration / 100;
    }

    protected Object readResolve() {
        if (timeoutType == null)  {
            timeoutType = ABSOLUTE;
        }
        
        if (abortBuild == null) {
            abortBuild = !failBuild;
        }
        
        return this;
    }
    
    @Override
    public Descriptor<BuildWrapper> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        DescriptorImpl() {
            super(BuildTimeoutWrapper.class);
        }

        public String getDisplayName() {
            return Messages.Descriptor_DisplayName();
        }

        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        public int[] getPercentages() {
            return new int[] {150,200,250,300,350,400};
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public BuildWrapper newInstance(StaplerRequest req, JSONObject formData)
                throws hudson.model.Descriptor.FormException {
            JSONObject timeoutObject = formData.getJSONObject("timeoutType");

            // we would ideally do this on the form itself (to show the default)
            //but there is a show/hide bug when using radioOptions inside an optionBlock
            if (timeoutObject.isNullObject() || timeoutObject.isEmpty()) {
                formData.put("timeoutType", ABSOLUTE);
            } else {
                // Jenkins 1.427
                // {"timeoutType": {
                //   "value": "elastic", "timeoutPercentage": "150", 
                //   "timeoutMinutesElasticDefault": "3333333"}}
                // Jenkins 1.420
                // {"timeoutMinutes": "3", 
                //  "timeoutType": {"value": "elastic"}, 
                //  "timeoutPercentage": "150", "timeoutMinutesElasticDefault": "3333333", 
                // "failBuild": false, "writingDescription": false}
                // => to keep comaptibility
                // "timeoutType": "elastic",  "timeoutPercentage": "150", 
                // "timeoutMinutesElasticDefault": "3333333"... 
                String timeoutType = timeoutObject.getString("value");
                timeoutObject.remove("value");
                for (String key : (Set<String>) timeoutObject.keySet()) {
                    formData.put(key, timeoutObject.get(key));
                }
                formData.put("timeoutType", timeoutType);
            }        

            return super.newInstance(req, formData);
        }

        public ListBoxModel doFillTimeoutPercentageItems() {
            ListBoxModel m = new ListBoxModel();
            for (int option : getPercentages()) {
                String s = String.valueOf(option);
                m.add(s + "%", s);
            }
            return m;
        }
    }
    
    public static class SendMail implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String recipients;

        @DataBoundConstructor
        public SendMail(String recipients) {
            this.recipients = recipients;
        }

        public String getRecipients() {
            return recipients;
        }
    }
    
    public static class TimeoutInterruption extends CauseOfInterruption {

        private String description;
        
        public TimeoutInterruption(String description) {
            this.description = description;
        }
        
        @Override
        public String getShortDescription() {
            return this.description;
        }
        
    }
}
