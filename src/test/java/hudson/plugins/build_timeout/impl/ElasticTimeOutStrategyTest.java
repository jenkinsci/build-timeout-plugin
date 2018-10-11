package hudson.plugins.build_timeout.impl;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.build_timeout.BuildTimeOutStrategy;
import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.mockito.Mockito;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;

import static hudson.model.Result.SUCCESS;
import static hudson.plugins.build_timeout.BuildTimeOutStrategy.MINUTES;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ElasticTimeOutStrategyTest extends TestCase {

    private File tempDir;

    private synchronized File getTempDir() {
        if (tempDir == null) {
            tempDir = Files.createTempDir();
        }
        return tempDir;
    }

    private synchronized void deleteTempDir() throws IOException {
        if (tempDir != null) {
            FileUtils.deleteDirectory(tempDir);
            tempDir = null;
        }
    }


    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        deleteTempDir();
    }

    public void testPercentageWithOneBuild() throws Exception {
        BuildTimeOutStrategy strategy = new ElasticTimeOutStrategy(200, 60, 3);

        Build b = new Build(new Build(60 * MINUTES, SUCCESS));

        assertEquals("Timeout should be 200% of 60", 120 * MINUTES, strategy.getTimeOut(b,null));
    }

    public void testPercentageWithTwoBuilds() throws Exception {
        BuildTimeOutStrategy strategy = new ElasticTimeOutStrategy(200, 60, 3);

        Build b = new Build(new Build(20 * MINUTES, SUCCESS, new Build(40 * MINUTES, SUCCESS)));

        assertEquals("Timeout should be 200% of the average of 20 and 40", 60 * MINUTES, strategy.getTimeOut(b,null));
    }

    public void testPercentageWithNoBuilds() throws Exception {
        BuildTimeOutStrategy strategy = new ElasticTimeOutStrategy(200, 90, 3);

        Build b = new Build(null);
        assertEquals("Timeout should be the elastic default.", 90 * MINUTES, strategy.getTimeOut(b,null));
    }

    public void testFailSafeTimeoutDurationWithOneBuild() throws Exception {
        BuildTimeOutStrategy strategy = new ElasticTimeOutStrategy("200", "60", "3", true);

        Build b = new Build(new Build(20 * MINUTES, SUCCESS));

        assertEquals("Timeout should be the elastic default.", 60 * MINUTES, strategy.getTimeOut(b,null));
    }

    public void testFailSafeTimeoutDurationWithTwoBuilds() throws Exception {
        BuildTimeOutStrategy strategy = new ElasticTimeOutStrategy("200", "60", "3", true);

        Build b = new Build(new Build(20 * MINUTES, SUCCESS, new Build(5 * MINUTES, SUCCESS)));

        assertEquals("Timeout should be the elastic default.", 60 * MINUTES, strategy.getTimeOut(b,null));
    }

    public void testFailSafeTimeoutIsNotUsed() throws Exception {
        BuildTimeOutStrategy strategy = new ElasticTimeOutStrategy("200", "60", "3", true);

        Build b = new Build(
                new Build(120 * MINUTES, SUCCESS,
                        new Build(150 * MINUTES, SUCCESS)
                )
        );

        // Timeout should be 200 % of average of builds.
        assertEquals(
                "Timeout should not be the elastic default.",
                270 * MINUTES,
                strategy.getTimeOut(b,null)
        );
    }

    private class Build extends FreeStyleBuild {
        Build previous;
        long duration;
        Result result;

        public Build(Build previous) throws IOException {
            super(Mockito.mock(FreeStyleProject.class));
            this.previous = previous;
        }

        public Build(long duration, Result result, Build previous) throws IOException {
            this(previous);
            this.duration = duration;
            this.result = result;
        }

        public Build(long duration, Result result) throws IOException {
            this(duration, result, null);
        }

        @Override
        public long getDuration() {
            return duration;
        }

        @Override
        public Result getResult() {
            return result;
        }

        @Override
        public FreeStyleBuild getPreviousBuild() {
            return previous;
        }

        @Override
        public void run() {
            //To change body of implemented methods use File | Settings | File Templates.
        }
        
        @Override
        public File getRootDir() {
            // this directory is created automatically
            // when `Run` is instantiated.
            // Use the temporary directory to be removed.
            return new File(getTempDir(), getId());
        }
    }

}
