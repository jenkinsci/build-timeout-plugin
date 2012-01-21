package hudson.plugins.build_timeout;
import static hudson.model.Result.SUCCESS;
import hudson.model.AbstractProject;
import hudson.model.DependencyGraph;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.util.DescribableList;

import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

import org.junit.Assert;
import org.mockito.Mockito;


public class BuildTimeoutWrapperTest extends TestCase {
    

    
    private class Build extends Run {
        int duration;
        Result result;
        public Build(int duration, Result result) throws IOException {
            super(Mockito.mock(FreeStyleProject.class));
            this.duration = duration;
            this.result = result;
        }
        
        @Override
        public long getDuration() {
            return duration;
        }
        
        @Override
        public Result getResult() {
            return result;
        }

    }
    private int timeoutPercentage = 0;
    private int timeoutMilliseconds = 0;
    private int timeoutMillisecondsElasticDefault = 0;
    private Run[] historicalBuilds;
    private String timeoutType = BuildTimeoutWrapper.ABSOLUTE;
    private static final int MINIMUM = 60;


    public void setUp() throws Exception {
        super.setUp();
        BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = MINIMUM;
        this.historicalBuilds = new Build[] {};
        timeoutPercentage = 0;
        timeoutMilliseconds = 0;
        timeoutMillisecondsElasticDefault = 0;
        timeoutType = BuildTimeoutWrapper.ABSOLUTE;
    }
    
    public void testDefaultTimeout() throws Exception {
        assertEffectiveTimeout(MINIMUM, "Default timeout should abort the build.");
        
    }

    public void testShorterThanMinimumLongerThanRequestedMinutes() throws Exception {
        this.timeoutMilliseconds = 5;
        assertEffectiveTimeout(MINIMUM, "Minimum overrides requested timeout");
    }
    
    public void testPercentageWithOneBuild() throws Exception {
        this.timeoutPercentage = 200;
        this.timeoutMillisecondsElasticDefault = 30;
        this.timeoutType = BuildTimeoutWrapper.ELASTIC;
        this.historicalBuilds = new Build[] {new Build(60, SUCCESS)};
        assertEffectiveTimeout(120, "Timeout should be 200% of 60");
    } 
    
    public void testPercentageWithOneBuildLessThanDefault() throws Exception {
        this.timeoutPercentage = 200;
        this.timeoutMillisecondsElasticDefault = 30;
        this.timeoutType = BuildTimeoutWrapper.ELASTIC;
        this.historicalBuilds = new Build[] {new Build(10, SUCCESS)};
        assertEffectiveTimeout(MINIMUM, "Timeout should be minimum");
    } 

    public void testPercentageWithTwoBuilds() throws Exception {
        this.timeoutPercentage = 200;
        this.timeoutType = BuildTimeoutWrapper.ELASTIC;
        this.historicalBuilds = new Build[] {new Build(20, SUCCESS), new Build(40, SUCCESS)};
        assertEffectiveTimeout(60, "Timeout should be 200% of the average of 20 and 40");
    }
    
    public void testPercentageWithNoBuilds() throws Exception {
        this.timeoutPercentage = 200;
        this.timeoutMillisecondsElasticDefault = 90;
        this.timeoutType = BuildTimeoutWrapper.ELASTIC;
        assertEffectiveTimeout(90, "Timeout should be the elastic default.");
    }

    private void assertEffectiveTimeout(long expectedTimeout, String message)
            throws IOException, Exception {
        
        Assert.assertEquals(expectedTimeout, BuildTimeoutWrapper.getEffectiveTimeout(this.timeoutMilliseconds, this.timeoutPercentage, this.timeoutMillisecondsElasticDefault, this.timeoutType, Arrays.asList(historicalBuilds)));
    }
    

}
