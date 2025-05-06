package hudson.plugins.build_timeout.global;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GlobalTimeOutRunListenerTest {
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

    @BeforeEach
    void setup() {
        listener = new GlobalTimeOutRunListener(
                Executors.newSingleThreadScheduledExecutor(),
                timeOutProvider,
                timeOutStore
        );
    }

    @Test
    void shouldStoreIfPresent() throws IOException, InterruptedException {
        given(timeOutProvider.timeOutFor(build, buildListener)).willReturn(Optional.of(Duration.ofMillis(1)));
        given(build.getExternalizableId()).willReturn("a#1");

        listener.setUpEnvironment(build, launcher, buildListener);

        verify(timeOutStore).scheduled(eq("a#1"), any());
    }

    @Test
    void shouldNotStoreIfAbsent() throws IOException, InterruptedException {
        given(timeOutProvider.timeOutFor(build, buildListener)).willReturn(Optional.empty());

        listener.setUpEnvironment(build, launcher, buildListener);

        verifyNoInteractions(timeOutStore);
    }
}
