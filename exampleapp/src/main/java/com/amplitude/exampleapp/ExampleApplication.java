package com.amplitude.exampleapp;

import android.app.Application;

import com.amplitude.api.Amplitude;
import com.amplitude.api.AmplitudeClient;
import com.amplitude.api.AmplitudeContextProvider;
import com.amplitude.skylab.Skylab;
import com.amplitude.skylab.SkylabClient;
import com.amplitude.skylab.SkylabConfig;
import com.amplitude.skylab.SkylabListener;
import com.amplitude.skylab.SkylabUser;
import com.amplitude.skylab.Variant;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ExampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // add the following
        String apiKey = "client-IAxMYws9vVQESrrK88aTcToyqMxiiJoR";
        SkylabConfig config =
                SkylabConfig.builder().setServerUrl("https://api.lab.amplitude.com").setPollIntervalSecs(10).build();
        SkylabClient client = Skylab.init(this, apiKey, config);
        AmplitudeClient amplitude = Amplitude.getInstance();
        amplitude.initialize(this, "a6dd847b9d2f03c816d4f3f8458cdc1d");
        amplitude.setUserId("test-user");
        client.setListener(new SkylabListener() {
            @Override
            public void onVariantsChanged(SkylabUser skylabUser, @NotNull Map<String, Variant> variants) {
                // handle variants changed
            }
        });
        client.setContextProvider(new AmplitudeContextProvider(amplitude));
        SkylabUser skylabUser =
                SkylabUser.builder().
                        setUserProperty("group", "Group 1").
                        build();
        client.start(skylabUser);
    }

}
