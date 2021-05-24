package com.amplitude.skylab;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

interface Storage {

    @NotNull
    Variant put(@NotNull String key, @NotNull Variant variant);

    @NotNull
    Variant get(@NotNull String key);

    @NotNull
    Map<String, Variant> getAll();

    void clear();
}
