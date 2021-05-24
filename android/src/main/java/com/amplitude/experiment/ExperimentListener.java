package com.amplitude.experiment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Classes can implement this interface to handle changes in assigned variants for a user.
 * The {@link #onVariantsChanged(ExperimentUser, Map)} method is called when a
 * {@link ExperimentClient} fetches variant values that are different from previously fetched
 * values.
 */
public interface ExperimentListener {
    void onVariantsChanged(@Nullable ExperimentUser experimentUser, @NotNull Map<String, Variant> variants);
}
