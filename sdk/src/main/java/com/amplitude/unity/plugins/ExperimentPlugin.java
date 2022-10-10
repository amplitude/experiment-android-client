package com.amplitude.unity.plugins;

import android.app.Application;
import com.amplitude.analytics.connector.util.JSONUtil;
import com.amplitude.experiment.Experiment;
import com.amplitude.experiment.ExperimentClient;
import com.amplitude.experiment.ExperimentConfig;
import com.amplitude.experiment.ExperimentUser;
import com.amplitude.experiment.Variant;
import com.amplitude.experiment.util.ConfigKt;
import com.amplitude.experiment.util.UserKt;
import com.amplitude.experiment.util.VariantKt;

import java.util.Map;

public class ExperimentPlugin {

    public static void initialize(
            Application application,
            String apiKey,
            String experimentConfigJson
    ) {
        ExperimentConfig config = null;
        if (experimentConfigJson != null) {
            config = ConfigKt.toExperimentConfig(experimentConfigJson);
        }
        if (config == null) {
            config = new ExperimentConfig();
        }
        Experiment.initialize(application, apiKey, config);
    }

    public static void initializeWithAmplitudeAnalytics(
            Application application,
            String apiKey,
            String experimentConfigJson
    ) {
        ExperimentConfig config = null;
        if (experimentConfigJson != null) {
            config = ConfigKt.toExperimentConfig(experimentConfigJson);
        }
        if (config == null) {
            config = new ExperimentConfig();
        }
        Experiment.initializeWithAmplitudeAnalytics(application, apiKey, config);
    }

    public static void fetch(String instanceName, String apiKey, String experimentUserJson) {
        ExperimentClient client = Experiment.getInstance(instanceName, apiKey);
        if (client == null) {
            return;
        }
        ExperimentUser user = null;
        if (experimentUserJson != null) {
            user = UserKt.toExperimentUser(experimentUserJson);
        }
        try {
            client.fetch(user).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String variant(String instanceName, String apiKey, String key, String fallbackVariantJson) {
        ExperimentClient client = Experiment.getInstance(instanceName, apiKey);
        if (client == null) {
            return null;
        }
        Variant fallbackVariant = null;
        if (fallbackVariantJson != null) {
            fallbackVariant = VariantKt.toVariant(fallbackVariantJson);
        }
        Variant variant = client.variant(key, fallbackVariant);
        return VariantKt.toJson(variant);
    }

    public static String all(String instanceName, String apiKey) {
        ExperimentClient client = Experiment.getInstance(instanceName, apiKey);
        if (client == null) {
            return null;
        }
        Map<String, Variant> variants = client.all();
        try {
            return JSONUtil.toJSONObject(variants).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static void exposure(String instanceName, String apiKey, String key) {
        ExperimentClient client = Experiment.getInstance(instanceName, apiKey);
        if (client == null) {
            return;
        }
        client.exposure(key);
    }


    public static void setUser(String instanceName, String apiKey, String experimentUserJson) {
        ExperimentClient client = Experiment.getInstance(instanceName, apiKey);
        if (client == null || experimentUserJson == null) {
            return;
        }
        ExperimentUser user = UserKt.toExperimentUser(experimentUserJson);
        if (user == null) {
            return;
        }
        client.setUser(user);
    }


    public static String getUser(String instanceName, String apiKey) {
        ExperimentClient client = Experiment.getInstance(instanceName, apiKey);
        if (client == null) {
            return null;
        }
        ExperimentUser user = client.getUser();
        if (user == null) {
            return null;
        }
        return UserKt.toJson(user);
    }
}
