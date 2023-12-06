package hudson.plugins.build_timeout.global;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import hudson.Extension;

import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Extension
@SuppressWarnings("unused")
public class GlobalTimeOutModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(TimeOutProvider.class).to(GlobalTimeOutConfiguration.class);
    }

    @TimeOut
    @Provides
    @Singleton
    ScheduledExecutorService providesScheduler() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder()
                .setNameFormat("timeout-%d")
                .build());
        executor.setRemoveOnCancelPolicy(true);
        return Executors.unconfigurableScheduledExecutorService(executor);
    }

    @Provides
    @Singleton
    TimeOutStore providesTimeOutStore() {
        return new InMemoryTimeOutStore(new HashMap<>());
    }
}
