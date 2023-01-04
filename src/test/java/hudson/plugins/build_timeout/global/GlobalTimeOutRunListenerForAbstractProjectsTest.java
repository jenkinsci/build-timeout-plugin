package hudson.plugins.build_timeout.global;

import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.concurrent.Executors;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

public class GlobalTimeOutRunListenerForAbstractProjectsTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.LENIENT);
    @Mock
    private TimeOutProvider timeOutProvider;
    @Mock
    private TimeOutStore timeOutStore;
    private GlobalTimeOutRunListener listener;

    @Mock
    private AbstractProject<?, ?> build;
    @Mock
    private Launcher launcher;

    @Mock
    private BuildListener buildListener;

    @Before
    public void setup() {
        listener = new GlobalTimeOutRunListener(
                Executors.newSingleThreadScheduledExecutor(),
                timeOutProvider,
                timeOutStore
        );
    }

    @Test
    public void shouldNotStoreForMatrixProjects() {
        given(timeOutProvider.timeOutFor(build, buildListener)).willReturn(Optional.empty());

        listener.setUpEnvironment(build, launcher, buildListener);

        verifyNoInteractions(timeOutStore);
    }
}
