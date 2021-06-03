package com.amplitude.experiment

data class Variant @JvmOverloads constructor(
    @JvmField val value: String,
    @JvmField val payload: Any? = null,
) {

    companion object {
        /**
         * Unwrap an optional variant's value.
         * @param variant The variant to unwrap.
         * @return The value if the variant is not-null, null otherwise.
         */
        @JvmStatic
        fun value(variant: Variant?): String? = variant?.value

        /**
         * Unwrap an optional variant's value.
         * @param variant The variant to unwrap.
         * @return The value if the variant is not-null, null otherwise.
         */
        @JvmStatic
        fun payload(variant: Variant?): Any? = variant?.payload
    }
}
