package hudson.plugins.build_timeout.global;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class GlobalTimeOutRunListenerTest {
    @Mock
    private TimeOutProvider timeOutProvider;
    @Mock
    private TimeOutStore timeOutStore;
    private GlobalTimeOutRunListener listener;

    @Mock
    private AbstractBuild<?, ?> build;
    @Mock
    private Launcher launcher;
    @Mock
    private BuildListener buildListener;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        listener = new GlobalTimeOutRunListener(
                Executors.newSingleThreadScheduledExecutor(),
                timeOutProvider,
                timeOutStore
        );
    }

    @Test
    public void shouldStoreIfPresent() throws IOException, InterruptedException {
        given(timeOutProvider.timeOutFor(build, buildListener)).willReturn(Optional.of(Duration.ofMillis(1)));
        given(build.getExternalizableId()).willReturn("a#1");

        listener.setUpEnvironment(build, launcher, buildListener);

        verify(timeOutStore).scheduled(eq("a#1"), any());
    }

    @Test
    public void shouldNotStoreIfAbsent() throws IOException, InterruptedException {
        given(timeOutProvider.timeOutFor(build, buildListener)).willReturn(Optional.empty());

        listener.setUpEnvironment(build, launcher, buildListener);

        verifyZeroInteractions(timeOutStore);
    }
}
