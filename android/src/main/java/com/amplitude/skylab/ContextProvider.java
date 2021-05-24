package com.amplitude.skylab;

import org.jetbrains.annotations.Nullable;

/**
 *  Classes can implement this interface to provide a User ID and Device ID to the
 *  {@link SkylabUser} context object. This allows for connecting with Amplitude's
 *  identity system.
 *
 *  Also see {@link SkylabClient#setContextProvider(ContextProvider)}
 */
public interface ContextProvider {

    @Nullable
    String getUserId();

    @Nullable
    String getDeviceId();

    @Nullable
    String getPlatform();

    @Nullable
    String getVersion();

    @Nullable
    String getLanguage();

    @Nullable
    String getOs();

    @Nullable
    String getBrand();

    @Nullable
    String getManufacturer();

    @Nullable
    String getModel();

    @Nullable
    String getCarrier();
}
