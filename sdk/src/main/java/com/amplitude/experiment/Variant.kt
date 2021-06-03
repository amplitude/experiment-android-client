package com.amplitude.experiment

data class Variant @JvmOverloads constructor(
    @JvmField val value: String? = null,
    @JvmField val payload: Any? = null,
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
