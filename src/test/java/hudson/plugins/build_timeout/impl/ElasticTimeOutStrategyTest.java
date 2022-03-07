package hudson.plugins.build_timeout.impl;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.build_timeout.BuildTimeOutStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static hudson.model.Result.SUCCESS;
import static hudson.plugins.build_timeout.BuildTimeOutStrategy.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ElasticTimeOutStrategyTest {

    @TempDir
    Path directory;

    @Test
    public void percentageWithOneBuild() throws Exception {
        BuildTimeOutStrategy strategy = new ElasticTimeOutStrategy(200, 60, 3);

        Build b = new Build(new Build(60 * MINUTES, SUCCESS));

        assertEquals(120 * MINUTES, strategy.getTimeOut(b,null),"Timeout should be 200% of 60");
    }

    @Test
    public void percentageWithTwoBuilds() throws Exception {
        BuildTimeOutStrategy strategy = new ElasticTimeOutStrategy(200, 60, 3);

        Build b = new Build(new Build(20 * MINUTES, SUCCESS, new Build(40 * MINUTES, SUCCESS)));

        assertEquals(60 * MINUTES, strategy.getTimeOut(b,null),"Timeout should be 200% of the average of 20 and 40");
    }

    @Test
    public void percentageWithNoBuilds() throws Exception {
        BuildTimeOutStrategy strategy = new ElasticTimeOutStrategy(200, 90, 3);

        Build b = new Build(null);
        assertEquals(90 * MINUTES, strategy.getTimeOut(b,null),"Timeout should be the elastic default.");
    }

    @Test
    public void failSafeTimeoutDurationWithOneBuild() throws Exception {
        BuildTimeOutStrategy strategy = new ElasticTimeOutStrategy("200", "60", "3", true);

        Build b = new Build(new Build(20 * MINUTES, SUCCESS));

        assertEquals(60 * MINUTES, strategy.getTimeOut(b,null),"Timeout should be the elastic default.");
    }

    @Test
    public void failSafeTimeoutDurationWithTwoBuilds() throws Exception {
        BuildTimeOutStrategy strategy = new ElasticTimeOutStrategy("200", "60", "3", true);

        Build b = new Build(new Build(20 * MINUTES, SUCCESS, new Build(5 * MINUTES, SUCCESS)));

        assertEquals(60 * MINUTES, strategy.getTimeOut(b,null),"Timeout should be the elastic default.");
    }

    @Test
    public void failSafeTimeoutIsNotUsed() throws Exception {
        BuildTimeOutStrategy strategy = new ElasticTimeOutStrategy("200", "60", "3", true);

        Build b = new Build(
                new Build(120 * MINUTES, SUCCESS,
                        new Build(150 * MINUTES, SUCCESS)
                )
        );

        // Timeout should be 200 % of average of builds.
        assertEquals(
                270 * MINUTES,
                strategy.getTimeOut(b,null),"Timeout should not be the elastic default.");

    }

    private class Build extends FreeStyleBuild {
        Build previous;
        long duration;
        Result result;

        Build(Build previous) throws IOException {
            super(Mockito.mock(FreeStyleProject.class));
            this.previous = previous;
        }

        Build(long duration, Result result, Build previous) throws IOException {
            this(previous);
            this.duration = duration;
            this.result = result;
        }

        Build(long duration, Result result) throws IOException {
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
        }
        
        @Override
        public File getRootDir() {
            return new File(String.valueOf(directory.getRoot()), getId());
        }
    }
}
