package com.amplitude.skylab;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

public class Variant {

    @Nullable public final String value;
    @Nullable public final Object payload;

    public Variant(@Nullable String value) {
        this(value, null);
    }

    public Variant(@Nullable String value, @Nullable Object payload) {
        this.value = value;
        this.payload = payload;
    }

    @Nullable
    @Deprecated
    public String value() {
        return this.value;
    }

    @Nullable
    @Deprecated
    public Object payload() {
        return this.payload;
    }

    @Override
    @NotNull
    public String toString() {
        return "Variant{" +
                "value='" + value + '\'' +
                ", payload=" + payload +
                '}';
    }

    @NotNull
    public static Variant fromJsonObject(@NotNull JSONObject variantJsonObj) throws JSONException {
        String value;

        if (variantJsonObj.has("value")) {
            value = variantJsonObj.getString("value");
        } else if (variantJsonObj.has("key")) {
            value = variantJsonObj.getString("key");
        } else {
            return new Variant(null);
        }

        Object payload = null;
        if (variantJsonObj.has("payload")) {
            payload = variantJsonObj.get("payload");
        }
        return new Variant(value, payload);
    }

    @NotNull
    public String toJson() {
        // create a JSONObject and then serialize it
        JSONObject jsonObj = new JSONObject();
        try {
            if (value != null) {
                jsonObj.put("value", value);
            }
            if (payload != null) {
                jsonObj.put("payload", payload);
            }
        } catch (JSONException e) {
            Log.w(Skylab.TAG, "Error converting Variant to json string", e);
        }

        return jsonObj.toString();
    }

    @NotNull
    public static Variant fromJson(@Nullable String json) {
        // deserialize into a JSONObject and then create a Variant
        if (json == null) {
            return new Variant(null, null);
        }

        try {
            JSONObject jsonObj = new JSONObject(json);
            return Variant.fromJsonObject(jsonObj);
        } catch (JSONException e) {
            // values persisted in older versions would throw a JSONException
            return new Variant(json);
        }
    }

    /**
     * See {@code #equals(Object)}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    /**
     * Variants are equal if their keys are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Variant other = (Variant) obj;
        if (value == null) {
            return other.value == null;
        } else {
            return value.equals(other.value);
        }
    }
}
