package com.amplitude.experiment

import java.util.concurrent.Future

/**
 * An experiment client manages a set of experiments and flags for a given user.
 */
interface ExperimentClient {

    /**
     * Get the user for the experiment client. The user can be set by calling
     * [fetch] with a user argument, or by explicitly setting the user via
     * [setUser].
     * @return This client's user.
     */
    fun getUser(): ExperimentUser

    /**
     * Set the user within the client. This user will be used to to fetch
     * variants if the user passed into [fetch] is null or missing.
     */
    fun setUser(user: ExperimentUser)

    /**
     * Assign the given user to the SDK and asynchronously fetch all variants
     * from the server. Subsequent calls may omit the user from the argument to
     * use the user from the previous call, or set previously using [setUser].
     *
     * If an [ExperimentUserProvider] has been set, the argument user will
     * be merged with the provider user, preferring user fields from the
     * argument user and falling back on the provider for fields which are null
     * or undefined.
     *
     * @param user The user to fetch variants for.
     * @returns Future that resolves when the request for variants completes.
     * @see ExperimentUser
     * @see ExperimentUserProvider
     */
    fun fetch(user: ExperimentUser? = null): Future<ExperimentClient>

    /**
     * Returns the variant for the provided key.
     *
     * Fetches [all] variants, falling back given [fallback] then the
     * configured fallbackVariant.
     *
     * @param key
     * @param fallback The highest priority fallback.
     * @see ExperimentConfig
     */
    fun variant(key: String, fallback: Variant? = null): Variant?

    /**
     * Returns all variants for the user.
     *
     * The primary source of variants is based on the [Source] configured in
     * the [ExperimentConfig].
     *
     * @see Source
     * @see ExperimentConfig
     */
    fun all(): Map<String, Variant>

    /**
     * Sets a user provider that will inject identity information into the user
     * for [fetch] requests. The user provider will only set user fields
     * in outgoing requests which are null or undefined.
     *
     * @param provider
     * @see ExperimentUserProvider
     */
    fun setUserProvider(provider: ExperimentUserProvider?): ExperimentClient
}
