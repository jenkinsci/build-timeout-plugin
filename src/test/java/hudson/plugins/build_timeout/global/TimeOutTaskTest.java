package hudson.plugins.build_timeout.global;

import com.google.common.collect.Lists;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.build_timeout.BuildTimeOutOperation;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import javax.annotation.Nonnull;
import java.time.Duration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;

public class TimeOutTaskTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);
    @Mock
    private TimeOutProvider timeOutProvider;
    @Mock
    private AbstractBuild<?, ?> build;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BuildListener listener;
    private TimeOutTask task;

    @Before
    public void setup() {
        task = TimeOutTask.create(timeOutProvider, build, listener, Duration.ofMillis(1));
    }

    @Test
    public void shouldCallAllOperations() {
        TestOp one = new TestOp();
        TestOp two = new TestOp();
        given(timeOutProvider.getOperations()).willReturn(Lists.newArrayList(one, two));

        task.run();

        assertTrue(one.performed);
        assertTrue(two.performed);
    }

    @Test
    public void shouldStopAtFirstFailure() {
        TestOp one = new TestOp();
        FailsOp two = new FailsOp();
        TestOp three = new TestOp();
        given(timeOutProvider.getOperations()).willReturn(Lists.newArrayList(one, two, three));

        task.run();

        assertTrue(one.performed);
        assertTrue(two.performed);
        assertFalse(three.performed);
    }

    @Test
    public void shouldStopAtFirstException() {
        TestOp one = new TestOp();
        ThrowsOp two = new ThrowsOp();
        TestOp three = new TestOp();
        given(timeOutProvider.getOperations()).willReturn(Lists.newArrayList(one, two, three));

        task.run();

        assertTrue(one.performed);
        assertTrue(two.performed);
        assertFalse(three.performed);
    }

    private static class TestOp extends BuildTimeOutOperation {
        boolean performed = false;

        @Override
        public boolean perform(@Nonnull AbstractBuild<?, ?> build, @Nonnull BuildListener listener, long effectiveTimeout) {
            performed = true;
            return true;
        }
    }

    private static class FailsOp extends BuildTimeOutOperation {
        boolean performed = false;

        @Override
        public boolean perform(@Nonnull AbstractBuild<?, ?> build, @Nonnull BuildListener listener, long effectiveTimeout) {
            performed = true;
            return false;
        }
    }

    private static class ThrowsOp extends BuildTimeOutOperation {
        boolean performed = false;

        @Override
        public boolean perform(@Nonnull AbstractBuild<?, ?> build, @Nonnull BuildListener listener, long effectiveTimeout) {
            performed = true;
            throw new RuntimeException("bad");
        }
    }
}
