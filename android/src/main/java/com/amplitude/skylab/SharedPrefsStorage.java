package com.amplitude.skylab;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple SharedPrefs backed storage for caching assigned variant values locally.
 *
 * This is not multi-process safe.
 */
class SharedPrefsStorage implements Storage {

    @NotNull final Context appContext;
    @NotNull final String sharedPrefsKey;
    @NotNull final SharedPreferences sharedPrefs;

    public SharedPrefsStorage(@NotNull Context appContext, @Nullable String instanceName) {
        this.appContext = appContext;
        if (TextUtils.isEmpty(instanceName)) {
            this.sharedPrefsKey = SkylabConfig.SHARED_PREFS_STORAGE_NAME;
        } else {
            this.sharedPrefsKey = SkylabConfig.SHARED_PREFS_STORAGE_NAME + "-" + instanceName;
        }
        sharedPrefs = appContext.getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE);
    }

    @Override
    @NotNull
    public Variant put(@NotNull String key, @NotNull Variant value) {
        String oldValue = sharedPrefs.getString(key, null);
        sharedPrefs.edit().putString(key, value.toJson()).apply();
        return Variant.fromJson(oldValue);
    }

    @Override
    @NotNull
    public Variant get(@NotNull String key) {
        return Variant.fromJson(sharedPrefs.getString(key, null));
    }

    @NotNull
    @Override
    public Map<String, Variant> getAll() {
        Map<String, Variant> all = new HashMap<>();
        for(Map.Entry<String,?> entry : sharedPrefs.getAll().entrySet()){
            if (entry.getValue() instanceof String) {
                all.put(entry.getKey(), Variant.fromJson((String) entry.getValue()));
            }
        }
        return all;
    }

    @Override
    public void clear() {
        sharedPrefs.edit().clear().apply();
    }
}
