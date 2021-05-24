package com.amplitude.exampleapp;

import android.app.Application;

import com.amplitude.api.Amplitude;
import com.amplitude.api.AmplitudeClient;
import com.amplitude.api.AmplitudeContextProvider;
import com.amplitude.experiment.Experiment;
import com.amplitude.experiment.ExperimentClient;
import com.amplitude.experiment.ExperimentConfig;
import com.amplitude.experiment.ExperimentListener;
import com.amplitude.experiment.ExperimentUser;
import com.amplitude.experiment.Variant;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ExampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // add the following
        String apiKey = "client-IAxMYws9vVQESrrK88aTcToyqMxiiJoR";
        ExperimentConfig config =
                ExperimentConfig.builder().setServerUrl("https://api.lab.amplitude.com").setPollIntervalSecs(10).build();
        ExperimentClient client = Experiment.init(this, apiKey, config);
        AmplitudeClient amplitude = Amplitude.getInstance();
        amplitude.initialize(this, "a6dd847b9d2f03c816d4f3f8458cdc1d");
        amplitude.setUserId("test-user");
        client.setListener(new ExperimentListener() {
            @Override
            public void onVariantsChanged(ExperimentUser experimentUser, @NotNull Map<String, Variant> variants) {
                // handle variants changed
            }
        });
        client.setContextProvider(new AmplitudeContextProvider(amplitude));
        ExperimentUser experimentUser =
                ExperimentUser.builder().
                        setUserProperty("group", "Group 1").
                        build();
        client.start(experimentUser);
    }

}
