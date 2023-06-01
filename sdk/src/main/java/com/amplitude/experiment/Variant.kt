package com.amplitude.experiment

data class Variant @JvmOverloads constructor(
    /**
     * The value of the variant.
     */
    @JvmField val value: String? = null,
    /**
     * The attached payload, if any.
     */
    @JvmField val payload: Any? = null,
    /**
     * The experiment key. Used to distinguish two experiments associated with the same flag.
     */
    @JvmField val expKey: String? = null,
) {

    /**
     * Useful for comparing a variant's value to a string in java.
     *
     * ```
     * variant.is("on");
     * ```
     *
     * is equivalent to
     *
     * ```
     * "on".equals(variant.value);
     * ```
     *
     * @param value The value to compare with the value of this variant.
     */
    fun `is`(value: String): Boolean = this.value == value
}
