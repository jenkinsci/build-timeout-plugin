package hudson.plugins.build_timeout.global;

import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

public class GlobalTimeOutRunListenerForAbstractProjectsTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.LENIENT);
    @Mock
    private TimeOutProvider timeOutProvider;
    @Mock
    private TimeOutStore timeOutStore;

    @Mock
    private AbstractProject<?, ?> build;

    @Mock
    private BuildListener buildListener;

    @Test
    public void shouldNotStoreForMatrixProjects() {
        given(timeOutProvider.timeOutFor(build, buildListener)).willReturn(Optional.empty());

        verifyNoInteractions(timeOutStore);
    }
}
