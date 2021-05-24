package com.amplitude.skylab;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class InMemoryStorage implements Storage {

    private final Map<String, Variant> data = new ConcurrentHashMap<>();

    @Override
    @NotNull
    public Variant put(@NotNull String key, @NotNull Variant value) {
        if (key != null) {
            return data.put(key, value);
        }
        return new Variant(null, null);
    }

    @Override
    @NotNull
    public Variant get(@NotNull String key) {
        if (key != null) {
            return data.get(key);
        }
        return new Variant(null, null);
    }

    @Override
    @NotNull
    public Map<String, Variant> getAll() {
        return new HashMap<>(data);
    }

    @Override
    public void clear() {
        data.clear();
    }
}
