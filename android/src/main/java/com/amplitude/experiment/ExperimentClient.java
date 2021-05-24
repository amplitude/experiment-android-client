package com.amplitude.experiment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.Future;

/**
 * Experiment client interface
 */
public interface ExperimentClient {

    /**
     * Fetches evaluations with the given {@link ExperimentUser}. If experimentUser is null, will fetch with an empty
     * {@link ExperimentUser}.
     *
     * @return A future that resolves when the evaluations have been returned by the server
     */
    @NotNull
    Future<ExperimentClient> start(@Nullable ExperimentUser experimentUser);

    /**
     * Calls `start(experimentUser)` and blocks for timeoutMs
     *
     * @param experimentUser
     * @param timeoutMs
     */
    void start(@Nullable ExperimentUser experimentUser, long timeoutMs);

    /**
     * Sets the evaluation {@link ExperimentUser}. Clears the local cache if the {@link ExperimentUser} has changed and
     * refetches evaluations.
     *
     * @param experimentUser
     * @return A future that resolves when the evaluations have been returned by the server
     */
    Future<ExperimentClient> setUser(@Nullable ExperimentUser experimentUser);

    /**
     * @return The current ExperimentUser, *without* any properties added by the ContextProvider
     */
    @Nullable
    ExperimentUser getUser();

    /**
     * @return The current ExperimentUser, *including* properties added by thee ContextProvider
     */
    @NotNull
    ExperimentUser getUserWithContext();

    /**
     * Asynchronously refetches evaluations with the stored {@link ExperimentUser}.
     *
     * @return A future that resolves when the evaluations have been returned by the server
     */
    @NotNull
    Future<ExperimentClient> refetchAll();

    @NotNull
    ExperimentClient startPolling();

    @NotNull
    ExperimentClient stopPolling();

    /**
     * Fetches the {@link Variant} for the given flagKey from local storage
     *
     * @param flagKey
     * @return
     */
    @NotNull
    Variant getVariant(@NotNull String flagKey);

    /**
     * Fetches the {@link Variant} for the given flagKey from local storage.
     * If the variant has not been fetched before, returns fallback.
     *
     * @param flagKey
     * @return
     */
    @NotNull
    Variant getVariant(@NotNull String flagKey, @NotNull Variant fallback);

    /**
     * Fetches all {@link Variant}s in a Map of flagKey to Variant.
     * @return
     */
    @NotNull
    Map<String, Variant> getVariants();

    /**
     * Sets an identity provider that enriches the {@link ExperimentUser} with a user_id and device_id
     * when fetching flags. The enrichment happens at the time a new network request is made.
     * This is used to connect Experiment to Amplitude identities. See {@link ContextProvider}.
     */
    @NotNull
    ExperimentClient setContextProvider(@Nullable ContextProvider provider);

    /**
     * Sets a listener for assigned variant change events. See {@link ExperimentListener}
     */
    @NotNull
    ExperimentClient setListener(@Nullable ExperimentListener experimentListener);
}
