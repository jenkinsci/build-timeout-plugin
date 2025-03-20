package hudson.plugins.build_timeout.global;

import com.google.common.collect.Lists;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.build_timeout.BuildTimeOutOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TimeOutTaskTest {
    @Mock
    private TimeOutProvider timeOutProvider;
    @Mock
    private AbstractBuild<?, ?> build;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BuildListener listener;
    private TimeOutTask task;

    @BeforeEach
    void setup() {
        task = TimeOutTask.create(timeOutProvider, build, listener, Duration.ofMillis(1));
    }

    @Test
    void shouldCallAllOperations() {
        TestOp one = new TestOp();
        TestOp two = new TestOp();
        given(timeOutProvider.getOperations()).willReturn(Lists.newArrayList(one, two));

        task.run();

        assertTrue(one.performed);
        assertTrue(two.performed);
    }

    @Test
    void shouldStopAtFirstFailure() {
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
    void shouldStopAtFirstException() {
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
        public boolean perform(@NonNull AbstractBuild<?, ?> build, @NonNull BuildListener listener, long effectiveTimeout) {
            performed = true;
            return true;
        }
    }

    private static class FailsOp extends BuildTimeOutOperation {
        boolean performed = false;

        @Override
        public boolean perform(@NonNull AbstractBuild<?, ?> build, @NonNull BuildListener listener, long effectiveTimeout) {
            performed = true;
            return false;
        }
    }

    private static class ThrowsOp extends BuildTimeOutOperation {
        boolean performed = false;

        @Override
        public boolean perform(@NonNull AbstractBuild<?, ?> build, @NonNull BuildListener listener, long effectiveTimeout) {
            performed = true;
            throw new RuntimeException("bad");
        }
    }
}
