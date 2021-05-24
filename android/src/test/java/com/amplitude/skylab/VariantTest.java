package com.amplitude.skylab;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class VariantTest {

    @Test
    public void fromJsonObject() throws JSONException {
        {
            JSONObject jsonObject = new JSONObject();
            Variant variant = Variant.fromJsonObject(jsonObject);
            Assert.assertNull(variant.value);
            Assert.assertNull(variant.payload);
        }

        {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("value", "value");
            jsonObject.put("payload", "payload");
            Variant variant = Variant.fromJsonObject(jsonObject);
            Assert.assertEquals("value", variant.value);
            Assert.assertEquals("payload", variant.payload);
        }
    }

    @Test
    public void fromJsonObjectDeprecatedKeyField() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", "value");
        Variant variant = Variant.fromJsonObject(jsonObject);
        Assert.assertEquals("value", variant.value);
        Assert.assertNull(variant.payload);
    }

    @Test
    public void toJson() throws JSONException {
        {
            Variant variant = new Variant("value");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("value", "value");
            Assert.assertEquals(jsonObject.toString(), variant.toJson());
        }

        {
            Variant variant = new Variant(null);
            Assert.assertEquals("{}", variant.toJson());
            Assert.assertNull(Variant.fromJson(variant.toJson()).value);
        }
    }

    @Test
    public void fromJson() {
        JSONObject jsonObject = new JSONObject();
        Assert.assertEquals(new Variant(null), Variant.fromJson(jsonObject.toString()));
    }
}
