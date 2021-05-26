package com.amplitude.experiment;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class ExperimentUserTest {

    @Test
    public void toJSONObject() throws JSONException {
        ExperimentUser user =
                ExperimentUser.builder().setUserId("user_id").setDeviceId("device_id").setCountry(
                        "country").setVersion(null).setUserProperty("userPropertyKey", "value").build();
        JSONObject expected = new JSONObject();
        expected.put("user_id", "user_id");
        expected.put("device_id", "device_id");
        expected.put("country", "country");
        JSONObject expectedUserProperties = new JSONObject();
        expectedUserProperties.put("userPropertyKey", "value");
        expected.put("user_properties", expectedUserProperties);

        Assert.assertEquals(expected.toString(), user.toJSONObject().toString());
    }

    @Test
    public void builder_copy() {
        ExperimentUser.Builder builder =
                ExperimentUser.builder()
                        .setUserId("user_id")
                        .setDeviceId("device_id")
                        .setCountry("country")
                        .setCity("test")
                        .setRegion("test")
                        .setDma("test")
                        .setLanguage("test")
                        .setPlatform("test")
                        .setOs("test")
                        .setLibrary("test")
                        .setDeviceFamily("test")
                        .setDeviceType("test")
                        .setDeviceBrand("test")
                        .setDeviceManufacturer("test")
                        .setDeviceModel("test")
                        .setCarrier("test")
                        .setUserProperty("userPropertyKey", "value");

        ExperimentUser user2 =
                ExperimentUser.builder()
                        .setCountry("newCountry")
                        .setVersion("newVersion")
                        .setUserProperty("userPropertyKey2", "value2")
                        .build();

        ExperimentUser user = builder.copyUser(user2).build();
        ExperimentUser expected =
                ExperimentUser.builder()
                        .setUserId("user_id")
                        .setDeviceId("device_id")
                        .setCountry("newCountry") // overwrites value
                        .setVersion("newVersion") // overwrites null
                        .setLanguage("test")
                        .setCity("test")
                        .setRegion("test")
                        .setDma("test")
                        .setLanguage("test")
                        .setPlatform("test")
                        .setOs("test")
                        .setLibrary("test")
                        .setDeviceFamily("test")
                        .setDeviceType("test")
                        .setDeviceBrand("test")
                        .setDeviceManufacturer("test")
                        .setDeviceModel("test")
                        .setCarrier("test")
                        .setUserProperty("userPropertyKey2", "value2")
                        .build();
        Assert.assertEquals(expected, user);
    }

}
