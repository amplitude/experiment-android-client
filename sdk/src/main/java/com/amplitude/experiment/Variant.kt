package com.amplitude.experiment

data class Variant @JvmOverloads constructor(
    /**
     * The value of the variant.
     */
    @JvmField var value: String? = null,
    /**
     * The attached payload, if any.
     */
    @JvmField var payload: Any? = null,
    /**
     * The experiment key. Used to distinguish two experiments associated with the same flag.
     */
    @JvmField var expKey: String? = null,
    /**
     * The key of the variant.
     */
    @JvmField var key: String? = null,
    /**
     * Flag, segment, and variant metadata produced as a result of
     * evaluation for the user. Used for system purposes.
     */
    @JvmField var metadata: Map<String, Any?> = emptyMap()
) {

    /**
     * Useful for comparing a variant's key to a string in java.
     *
     * ```
     * variant.is("on");
     * ```
     *
     * is equivalent to
     *
     * ```
     * "on".equals(variant.key);
     * ```
     *
     * @param value The value to compare with the key of this variant.
     */
    fun `is`(value: String): Boolean = this.key == value
}
