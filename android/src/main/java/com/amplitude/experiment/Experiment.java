package com.amplitude.experiment;

import android.app.Application;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import okhttp3.OkHttpClient;

/**
 * Factory class for Experiment. Manages a pool of singleton instances of {@link ExperimentClient} identified by
 * name. {@link ExperimentClient} names are normalized to be lowercase. The name "$default_instance" is
 * reserved for the default {@link ExperimentClient} instance, and is provided for convenience.
 * <p>
 * All {@link ExperimentClient} share the same executor service and http client.
 */
public class Experiment {

    public static final String TAG = "Experiment";

    private static final ThreadFactory DAEMON_THREAD_FACTORY = new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        }
    };
    private static final ScheduledThreadPoolExecutor EXECUTOR_SERVICE =
            new ScheduledThreadPoolExecutor(0
            , DAEMON_THREAD_FACTORY);
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final Map<String, DefaultExperimentClient> INSTANCES = new ConcurrentHashMap<String,
            DefaultExperimentClient>();

    // Public API

    /**
     * Returns the default {@link ExperimentClient} instance.
     */
    @Nullable
    public static ExperimentClient getInstance() {
        return getInstance(ExperimentConfig.Defaults.INSTANCE_NAME);
    }

    /**
     * Returns the {@link ExperimentClient} instance associated with the provided name. If no Client
     * was initialized with the given name, returns null.
     */
    @Nullable
    public static ExperimentClient getInstance(@Nullable String name) {
        String normalizedName = ExperimentConfig.normalizeInstanceName(name);
        return INSTANCES.get(normalizedName);
    }

    /**
     * Returns the default {@link ExperimentClient} instance associated with the provided name.
     * @throws IllegalStateException If the default client has not been initialized.
     */
    @NotNull
    public static ExperimentClient getInstanceOrThrow() {
        ExperimentClient c = getInstance(ExperimentConfig.Defaults.INSTANCE_NAME);
        if (c == null)
            throw new IllegalStateException("The default ExperimentClient must be initialized before accessing the default instance");
        return getInstance(ExperimentConfig.Defaults.INSTANCE_NAME);
    }

    /**
     * Returns the {@link ExperimentClient} instance.
     * @throws IllegalStateException If the default client has not been initialized.
     */
    @NotNull
    public static ExperimentClient getInstanceOrThrow(@Nullable String name) {
        ExperimentClient c = getInstance(ExperimentConfig.Defaults.INSTANCE_NAME);
        if (c == null)
            throw new IllegalStateException("The \""+name+"\" ExperimentClient must be initialized before accessing the default instance");
        return getInstance(name);
    }

    /**
     * Initializes a {@link ExperimentClient} with the provided api key and {@link ExperimentConfig}.
     * If a {@link ExperimentClient} already exists with the instanceName set by the {@link ExperimentConfig},
     * returns that instance instead.
     *
     * @param application The Android Application context
     * @param apiKey  The API key. This can be found in the Experiment settings and should not be null or empty.
     * @param config see {@link ExperimentConfig} for configuration options
     */
    @NotNull
    public static synchronized ExperimentClient init(
            @NotNull Application application,
            @NotNull String apiKey,
            @NotNull ExperimentConfig config
    ) {
        String instanceName = config.getInstanceName();
        DefaultExperimentClient client = INSTANCES.get(instanceName);
        if (client == null) {
            client = new DefaultExperimentClient(apiKey, config,
                    new SharedPrefsStorage(application, instanceName), HTTP_CLIENT, EXECUTOR_SERVICE);
            INSTANCES.put(instanceName, client);
        }
        return client;
    }

    /**
     * Shuts down all {@link ExperimentClient} instances.
     */
    public static void shutdown() {
        HTTP_CLIENT.dispatcher().executorService().shutdown();
    }
}
