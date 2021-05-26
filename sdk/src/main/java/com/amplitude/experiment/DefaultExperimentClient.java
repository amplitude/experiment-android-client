package com.amplitude.experiment;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Default {@link ExperimentClient} implementation. Use the {@link Experiment} class to initialize and
 * access instances.
 */
public class DefaultExperimentClient implements ExperimentClient {

    public static final String LIBRARY = "experiment-android/" + BuildConfig.VERSION_NAME;

    private static final int BASE_64_DEFAULT_FLAGS =
            Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE;
    static final Object STORAGE_LOCK = new Object();

    @NotNull final String instanceName;
    @NotNull final String apiKey;
    @NotNull final ExperimentConfig config;
    @NotNull final HttpUrl serverUrl;

    @Nullable ExperimentUser experimentUser;
    @Nullable ExperimentListener experimentListener;
    @Nullable ContextProvider contextProvider;

    @NotNull final Storage storage;
    @NotNull final OkHttpClient httpClient;
    @NotNull final ScheduledThreadPoolExecutor executorService;
    @Nullable ScheduledFuture<?> pollFuture;

    @NotNull Runnable pollTask = new Runnable() {
        @Override
        public void run() {
            fetchAll();
        }
    };

    @NotNull Callable<ExperimentClient> fetchAllCallable = new Callable<ExperimentClient>() {
        @Override
        public ExperimentClient call() throws Exception {
            return fetchAll().get();
        }
    };

    DefaultExperimentClient(
            @NotNull String apiKey,
            @NotNull ExperimentConfig config,
            @NotNull Storage storage,
            @NotNull OkHttpClient httpClient,
            @NotNull ScheduledThreadPoolExecutor executorService
    ) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            // guarantee that apiKey exists
            throw new IllegalArgumentException("ExperimentClient initialized with null or empty " +
                    "apiKey.");
        }
        this.instanceName = config.getInstanceName();
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.executorService = executorService;
        this.storage = storage;
        this.config = config;
        this.pollFuture = null;
        serverUrl = HttpUrl.parse(config.getServerUrl());
    }

    /**
     * See {@link ExperimentClient#start(ExperimentUser)}
     */
    @Override
    @NotNull
    public Future<ExperimentClient> start(@Nullable ExperimentUser experimentUser) {
        this.experimentUser = experimentUser;
        return executorService.submit(fetchAllCallable);
    }

    /**
     * See {@link ExperimentClient#start(ExperimentUser, long)}
     */
    @Override
    public void start(@Nullable ExperimentUser experimentUser, long timeoutMs) {
        try {
            Future<ExperimentClient> future = start(experimentUser);
            future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            Log.i(Experiment.TAG, "Timeout while initializing client, evaluations may not be " +
                    "ready");
        } catch (Exception e) {
            Log.w(Experiment.TAG, "Exception in client initialization", e);
        }
    }

    /**
     * See {@link ExperimentClient#setUser}
     */
    @Override
    @NotNull
    public Future<ExperimentClient> setUser(@Nullable ExperimentUser experimentUser) {
        if ((this.experimentUser == null && experimentUser == null) || (this.experimentUser != null && this.experimentUser.equals(experimentUser))) {
            // Users are equal, no need to refetch
            final AsyncFuture<ExperimentClient> future = new AsyncFuture<>();
            future.complete(this);
            return future;
        } else {
            this.experimentUser = experimentUser;
            storage.clear();
            return executorService.submit(fetchAllCallable);
        }
    }

    /**
     * See {@link ExperimentClient#getUser}
     * @return
     */
    @Override
    @Nullable
    public ExperimentUser getUser() {
        return this.experimentUser;
    }

    /**
     * See {@link ExperimentClient#getUserWithContext}
     * @return
     */
    @Override
    @NotNull
    public ExperimentUser getUserWithContext() {
        ExperimentUser.Builder builder = ExperimentUser.builder();
        if (contextProvider != null) {
            String deviceId = contextProvider.getDeviceId();
            if (!TextUtils.isEmpty(deviceId)) {
                builder.setDeviceId(deviceId);
            }
            String userId = contextProvider.getUserId();
            if (!TextUtils.isEmpty(userId)) {
                builder.setUserId(userId);
            }
            builder.setPlatform(contextProvider.getPlatform());
            builder.setVersion(contextProvider.getVersion());
            builder.setLanguage(contextProvider.getLanguage());
            builder.setOs(contextProvider.getOs());
            builder.setDeviceBrand(contextProvider.getBrand());
            builder.setDeviceManufacturer(contextProvider.getManufacturer());
            builder.setDeviceModel(contextProvider.getModel());
            builder.setCarrier(contextProvider.getCarrier());
        }
        builder.setLibrary(LIBRARY);
        builder.copyUser(this.experimentUser);
        return builder.build();
    }

    @NotNull
    @Override
    public ExperimentClient startPolling() {
        if (pollFuture == null) {
            Log.d(Experiment.TAG, "Starting polling every " + config.getPollIntervalSecs() + " " +
                    "seconds");
            pollFuture = executorService.scheduleAtFixedRate(pollTask,
                    config.getPollIntervalSecs(), config.getPollIntervalSecs(), TimeUnit.SECONDS);
        }
        return this;
    }

    @NotNull
    @Override
    public ExperimentClient stopPolling() {
        if (pollFuture != null) {
            Log.d(Experiment.TAG, "Stopping polling");
            pollFuture.cancel(false);
            executorService.purge();
            this.pollFuture = null;
        }
        return this;
    }

    @NotNull
    public Future<ExperimentClient> refetchAll() {
        return executorService.submit(fetchAllCallable);
    }

    /**
     * Fetches all variants and returns a future that will complete when the network call is
     * complete.
     *
     * @return
     */
    @NotNull
    Future<ExperimentClient> fetchAll() {
        final AsyncFuture<ExperimentClient> future = new AsyncFuture<>();
        final long start = System.nanoTime();
        final ExperimentUser user = getUserWithContext();
        if (user.userId == null && user.deviceId == null) {
            Log.w(Experiment.TAG, "user id and device id are null; amplitude will not be able to resolve identity");
        }
        final JSONObject jsonContext = user.toJSONObject();
        final String jsonString = jsonContext.toString();
        final byte[] srcData = jsonString.getBytes(Charset.forName("UTF-8"));
        final String base64Encoded = Base64.encodeToString(srcData, BASE_64_DEFAULT_FLAGS);
        final HttpUrl url =
                serverUrl.newBuilder().addPathSegments("sdk/vardata/" + base64Encoded).build();
        Log.d(Experiment.TAG,
                "Requesting variants from " + url.toString() + " for user " + jsonContext.toString());
        Request request = new Request.Builder()
                .get()
                .url(url)
                .addHeader("Authorization", "Api-Key " + this.apiKey)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(Experiment.TAG, e.getMessage(), e);
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String responseString = response.body().string();
                try {
                    if (response.isSuccessful()) {
                        Map<String, Variant> newValues = new HashMap<>();
                        synchronized (STORAGE_LOCK) {
                            storage.clear();
                            JSONObject result = new JSONObject(responseString);
                            Iterator<String> flags = result.keys();
                            while (flags.hasNext()) {
                                String flag = flags.next();
                                JSONObject variantJsonObj = result.getJSONObject(flag);
                                Variant newValue = Variant.fromJsonObject(variantJsonObj);
                                storage.put(flag, newValue);
                                newValues.put(flag, newValue);
                            }
                        }
                        fireOnVariantsReceived(experimentUser, newValues);
                    } else {
                        Log.w(Experiment.TAG, responseString);
                    }
                } catch (JSONException e) {
                    Log.e(Experiment.TAG, "Could not parse JSON: " + responseString);
                    Log.e(Experiment.TAG, e.getMessage(), e);
                } catch (Exception e) {
                    Log.e(Experiment.TAG, "Exception handling response: " + responseString);
                    Log.e(Experiment.TAG, e.getMessage(), e);
                } finally {
                    response.close();
                }
                future.complete(DefaultExperimentClient.this);
                long end = System.nanoTime();
                Log.d(Experiment.TAG, String.format("Fetched all: %s for user %s in %.3f ms",
                        responseString, jsonContext.toString(), (end - start) / 1000000.0));
            }
        });
        return future;
    }

    /**
     * Fetches the variant for the given flagKey from local storage. If no such flag
     * is found, returns the fallback variant set in the {@link ExperimentConfig}.
     *
     * @param flagKey
     * @return
     */
    @Override
    @NotNull
    public Variant getVariant(@NotNull String flagKey) {
        return getVariant(flagKey, config.getFallbackVariant());
    }

    /**
     * Fetches the variant for the given flagKey from local storage. If no such flag
     * is found, returns fallback.
     *
     * @param flagKey
     * @return
     */
    @Override
    @NotNull
    public Variant getVariant(@NotNull String flagKey, @NotNull Variant fallback) {
        Variant variant;
        synchronized (STORAGE_LOCK) {
            variant = storage.get(flagKey);
        }
        if (variant == null || variant.value == null) {
            variant = fallback;
            Log.d(Experiment.TAG, String.format("Variant for %s not found, returning fallback variant" +
                    " %s", flagKey, variant));
        }
        return variant;
    }

    /**
     * Fetches all variants from local storage.
     * @return
     */
    @NotNull
    public Map<String, Variant> getVariants() {
        synchronized (STORAGE_LOCK) {
            return storage.getAll();
        }
    }

    @Override
    @NotNull
    public ExperimentClient setContextProvider(@Nullable ContextProvider provider) {
        this.contextProvider = provider;
        return this;
    }

    @Override
    @NotNull
    public ExperimentClient setListener(@Nullable ExperimentListener experimentListener) {
        this.experimentListener = experimentListener;
        return this;
    }

    private void fireOnVariantsReceived(@Nullable ExperimentUser experimentUser, @NotNull Map<String, Variant> variants) {
        if (this.experimentListener != null) {
            this.experimentListener.onVariantsChanged(experimentUser, variants);
        }
    }
}
