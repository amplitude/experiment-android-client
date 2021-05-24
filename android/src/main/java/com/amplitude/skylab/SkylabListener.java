package com.amplitude.skylab;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Classes can implement this interface to handle changes in assigned variants for a user.
 * The {@link #onVariantsChanged(SkylabUser, Map)} method is called when a
 * {@link SkylabClient} fetches variant values that are different from previously fetched
 * values.
 */
public interface SkylabListener {
    void onVariantsChanged(@Nullable SkylabUser skylabUser, @NotNull Map<String, Variant> variants);
}
