package hudson.plugins.build_timeout;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.Run;

import java.io.IOException;

import junit.framework.TestCase;

import org.junit.Assert;


public class BuildTimeoutWrapperTest extends TestCase {
    
    private int timeoutPercentage = 0;
    private int timeoutMilliseconds = 0;
    private int timeoutMillisecondsElasticDefault = 0;
    private Run<?,?> build;
    private String timeoutType = BuildTimeoutWrapper.ABSOLUTE;
    private static final int MINIMUM = 60;


    public void setUp() throws Exception {
        super.setUp();
        BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = MINIMUM;
        this.build = mock(Run.class);
        when(build.getEstimatedDuration()).thenReturn(0L);
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
        when(this.build.getEstimatedDuration()).thenReturn(60L);
        assertEffectiveTimeout(120, "Timeout should be 200% of 60");
    } 
    
    public void testPercentageWithOneBuildLessThanDefault() throws Exception {
        this.timeoutPercentage = 200;
        this.timeoutMillisecondsElasticDefault = 30;
        this.timeoutType = BuildTimeoutWrapper.ELASTIC;
        when(this.build.getEstimatedDuration()).thenReturn(10L);
        assertEffectiveTimeout(MINIMUM, "Timeout should be minimum");
    } 

    public void testPercentageWithNoBuilds() throws Exception {
        this.timeoutPercentage = 200;
        this.timeoutMillisecondsElasticDefault = 90;
        this.timeoutType = BuildTimeoutWrapper.ELASTIC;
        when(this.build.getEstimatedDuration()).thenReturn(-1L);
        assertEffectiveTimeout(90, "Timeout should be the elastic default.");
    }

    private void assertEffectiveTimeout(long expectedTimeout, String message)
            throws IOException, Exception {
        
        Assert.assertEquals(expectedTimeout, BuildTimeoutWrapper.getEffectiveTimeout(this.timeoutMilliseconds, this.timeoutPercentage, this.timeoutMillisecondsElasticDefault, this.timeoutType, this.build));
    }
    

}
