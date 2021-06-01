package com.amplitude.exampleapp;

import android.app.Application;

import com.amplitude.api.Amplitude;
import com.amplitude.api.AmplitudeClient;
import com.amplitude.api.AmplitudeUserProvider;
import com.amplitude.experiment.Experiment;
import com.amplitude.experiment.ExperimentClient;
import com.amplitude.experiment.ExperimentConfig;
import com.amplitude.experiment.ExperimentUser;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ExampleApplication extends Application {

    public static ExperimentClient experimentClient;

    @Override
    public void onCreate() {
        super.onCreate();
        // add the following
        String apiKey = "client-IAxMYws9vVQESrrK88aTcToyqMxiiJoR";
        ExperimentConfig config = ExperimentConfig.builder()
            .serverUrl("https://api.lab.amplitude.com")
            .build();
        experimentClient = Experiment.initialize(this, apiKey, config);

        AmplitudeClient amplitude = Amplitude.getInstance();
        amplitude.initialize(this, "a6dd847b9d2f03c816d4f3f8458cdc1d");
        amplitude.setUserId("test-user");
        experimentClient.setUserProvider(new AmplitudeUserProvider(amplitude));
        ExperimentUser experimentUser =
                ExperimentUser.builder().
                        userProperty("group", "Group 1").
                        build();
        try {
            experimentClient.fetch(experimentUser).get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
