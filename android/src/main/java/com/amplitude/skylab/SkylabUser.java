package com.amplitude.skylab;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The Skylab user context object. This is an immutable object that can be created using
 * a {@link SkylabUser.Builder}. Example usage:
 *
 * {@code SkylabUser.builder().setLanguage("en").build()}
 */
public class SkylabUser {

    public static final String USER_ID = "user_id";
    public static final String DEVICE_ID = "device_id";
    public static final String COUNTRY = "country";
    public static final String REGION = "region";
    public static final String DMA = "dma";
    public static final String CITY = "city";
    public static final String LANGUAGE = "language";
    public static final String PLATFORM = "platform";
    public static final String VERSION = "version";
    public static final String OS = "os";
    public static final String DEVICE_FAMILY = "device_family";
    public static final String DEVICE_TYPE = "device_type";
    public static final String DEVICE_MANUFACTURER = "device_manufacturer";
    public static final String DEVICE_BRAND = "device_brand";
    public static final String DEVICE_MODEL = "device_model";
    public static final String CARRIER = "carrier";
    public static final String LIBRARY = "library";
    public static final String USER_PROPERTIES = "user_properties";

    @Nullable String userId;
    @Nullable String deviceId;
    @Nullable String country;
    @Nullable String region;
    @Nullable String dma;
    @Nullable String city;
    @Nullable String language;
    @Nullable String platform;
    @Nullable String version;
    @Nullable String os;
    @Nullable String deviceFamily;
    @Nullable String deviceType;
    @Nullable String deviceManufacturer;
    @Nullable String deviceBrand;
    @Nullable String deviceModel;
    @Nullable String carrier;
    @Nullable String library;

    @NotNull JSONObject userProperties;

    private SkylabUser(
            @Nullable String userId,
            @Nullable String deviceId,
            @Nullable String country,
            @Nullable String region,
            @Nullable String dma,
            @Nullable String city,
            @Nullable String language,
            @Nullable String platform,
            @Nullable String version,
            @Nullable String os,
            @Nullable String deviceFamily,
            @Nullable String deviceType,
            @Nullable String deviceManufacturer,
            @Nullable String deviceBrand,
            @Nullable String deviceModel,
            @Nullable String carrier,
            @Nullable String library,
            @NotNull JSONObject userProperties
    ) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.country = country;
        this.region = region;
        this.dma = dma;
        this.city = city;
        this.language = language;
        this.platform = platform;
        this.version = version;
        this.os = os;
        this.deviceFamily = deviceFamily;
        this.deviceType = deviceType;
        this.deviceManufacturer = deviceManufacturer;
        this.deviceBrand = deviceBrand;
        this.deviceModel = deviceModel;
        this.carrier = carrier;
        this.library = library;
        this.userProperties = userProperties;
    }

    @Nullable
    public String getUserId() {
        return userId;
    }

    @Nullable
    public String getDeviceId() {
        return deviceId;
    }

    @Nullable
    public String getCountry() {
        return country;
    }

    @Nullable
    public String getRegion() {
        return region;
    }

    @Nullable
    public String getDma() {
        return dma;
    }

    @Nullable
    public String getCity() {
        return city;
    }

    @Nullable
    public String getLanguage() {
        return language;
    }

    @Nullable
    public String getPlatform() {
        return platform;
    }

    @Nullable
    public String getVersion() {
        return version;
    }

    @Nullable
    public String getOs() { return os; }

    @Nullable
    public String getDeviceFamily() {
        return deviceFamily;
    }

    @Nullable
    public String getDeviceType() {
        return deviceType;
    }

    @Nullable
    public String getDeviceManufacturer() {
        return deviceManufacturer;
    }

    @Nullable
    public String getDeviceBrand() {
        return deviceBrand;
    }

    @Nullable
    public String getDeviceModel() {
        return deviceModel;
    }

    @Nullable
    public String getCarrier() {
        return carrier;
    }

    @Nullable
    public String getLibrary() {
        return library;
    }

    @NotNull
    public JSONObject getUserProperties() {
        return userProperties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SkylabUser that = (SkylabUser) o;

        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (deviceId != null ? !deviceId.equals(that.deviceId) : that.deviceId != null)
            return false;
        if (country != null ? !country.equals(that.country) : that.country != null) return false;
        if (region != null ? !region.equals(that.region) : that.region != null) return false;
        if (dma != null ? !dma.equals(that.dma) : that.dma != null) return false;
        if (city != null ? !city.equals(that.city) : that.city != null) return false;
        if (language != null ? !language.equals(that.language) : that.language != null)
            return false;
        if (platform != null ? !platform.equals(that.platform) : that.platform != null)
            return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;
        if (os != null ? !os.equals(that.os) : that.os != null) return false;
        if (deviceFamily != null ? !deviceFamily.equals(that.deviceFamily) :
                that.deviceFamily != null)
            return false;
        if (deviceType != null ? !deviceType.equals(that.deviceType) : that.deviceType != null)
            return false;
        if (deviceManufacturer != null ? !deviceManufacturer.equals(that.deviceManufacturer) :
                that.deviceManufacturer != null)
            return false;
        if (deviceBrand != null ? !deviceBrand.equals(that.deviceBrand) : that.deviceBrand != null)
            return false;
        if (deviceModel != null ? !deviceModel.equals(that.deviceModel) : that.deviceModel != null)
            return false;
        if (carrier != null ? !carrier.equals(that.carrier) : that.carrier != null) return false;
        if (library != null ? !library.equals(that.library) : that.library != null) return false;
        return userProperties != null ? userProperties.toString().equals(that.userProperties.toString()) :
                that.userProperties == null;
    }

    @NotNull
    @Override
    public String toString() {
        return "SkylabUser{" +
                "userId='" + userId + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", country='" + country + '\'' +
                ", region='" + region + '\'' +
                ", dma='" + dma + '\'' +
                ", city='" + city + '\'' +
                ", language='" + language + '\'' +
                ", platform='" + platform + '\'' +
                ", version='" + version + '\'' +
                ", os='" + os + '\'' +
                ", deviceFamily='" + deviceFamily + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", deviceManufacturer='" + deviceManufacturer + '\'' +
                ", deviceBrand='" + deviceBrand + '\'' +
                ", deviceModel='" + deviceModel + '\'' +
                ", carrier='" + carrier + '\'' +
                ", library='" + library + '\'' +
                ", userProperties=" + userProperties +
                '}';
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (deviceId != null ? deviceId.hashCode() : 0);
        result = 31 * result + (country != null ? country.hashCode() : 0);
        result = 31 * result + (region != null ? region.hashCode() : 0);
        result = 31 * result + (dma != null ? dma.hashCode() : 0);
        result = 31 * result + (city != null ? city.hashCode() : 0);
        result = 31 * result + (language != null ? language.hashCode() : 0);
        result = 31 * result + (platform != null ? platform.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (os != null ? os.hashCode() : 0);
        result = 31 * result + (deviceFamily != null ? deviceFamily.hashCode() : 0);
        result = 31 * result + (deviceType != null ? deviceType.hashCode() : 0);
        result = 31 * result + (deviceManufacturer != null ? deviceManufacturer.hashCode() : 0);
        result = 31 * result + (deviceBrand != null ? deviceBrand.hashCode() : 0);
        result = 31 * result + (deviceModel != null ? deviceModel.hashCode() : 0);
        result = 31 * result + (carrier != null ? carrier.hashCode() : 0);
        result = 31 * result + (library != null ? library.hashCode() : 0);
        result = 31 * result + (userProperties != null ? userProperties.hashCode() : 0);
        return result;
    }

    @NotNull
    public JSONObject toJSONObject() {
        JSONObject object = new JSONObject();
        try {
            object.put(USER_ID, userId);
            object.put(DEVICE_ID, deviceId);
            object.put(USER_PROPERTIES, userProperties);
            object.put(COUNTRY, country);
            object.put(CITY, city);
            object.put(REGION, region);
            object.put(DMA, city);
            object.put(LANGUAGE, language);
            object.put(PLATFORM, platform);
            object.put(VERSION, version);
            object.put(OS, os);
            object.put(DEVICE_FAMILY, deviceFamily);
            object.put(DEVICE_TYPE, deviceType);
            object.put(DEVICE_BRAND, deviceBrand);
            object.put(DEVICE_MANUFACTURER, deviceManufacturer);
            object.put(DEVICE_MODEL, deviceModel);
            object.put(CARRIER, carrier);
            object.put(LIBRARY, library);
        } catch (JSONException e) {
            Log.w(Skylab.TAG, "Error converting SkylabUser to JSONObject", e);
        }
        return object;
    }

    @NotNull
    public static SkylabUser.Builder builder() {
        return new SkylabUser.Builder();
    }

    public static class Builder {
        private String userId;
        private String deviceId;
        private String country;
        private String region;
        private String city;
        private String language;
        private String platform;
        private String version;
        private String os;
        private JSONObject userProperties = new JSONObject();
        private String dma;
        private String deviceFamily;
        private String deviceType;
        private String deviceManufacturer;
        private String deviceBrand;
        private String deviceModel;
        private String carrier;
        private String library;

        /**
         * Sets the user id on the SkylabUser for connecting with Amplitude's identity
         *
         * @param userId The User ID used in Amplitude
         */
        @NotNull
        public Builder setUserId(@Nullable String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Sets the device id on the SkylabUser for connecting with Amplitude's identity
         *
         * @param deviceId The Device ID used in Amplitude
         */
        @NotNull
        public Builder setDeviceId(@Nullable String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        @NotNull
        public Builder setCountry(@Nullable String country) {
            this.country = country;
            return this;
        }

        @NotNull
        public Builder setRegion(@Nullable String region) {
            this.region = region;
            return this;
        }

        @NotNull
        public Builder setCity(@Nullable String city) {
            this.city = city;
            return this;
        }

        @NotNull
        public Builder setLanguage(@Nullable String language) {
            this.language = language;
            return this;
        }

        @NotNull
        public Builder setPlatform(@Nullable String platform) {
            this.platform = platform;
            return this;
        }

        @NotNull
        public Builder setVersion(@Nullable String version) {
            this.version = version;
            return this;
        }

        @NotNull
        public Builder setDma(@Nullable String dma) {
            this.dma = dma;
            return this;
        }

        @NotNull
        public Builder setOs(@Nullable String os) {
            this.os = os;
            return this;
        }

        @NotNull
        public Builder setDeviceFamily(@Nullable String deviceFamily) {
            this.deviceFamily = deviceFamily;
            return this;
        }

        @NotNull
        public Builder setDeviceType(@Nullable String deviceType) {
            this.deviceType = deviceType;
            return this;
        }

        @NotNull
        public Builder setDeviceManufacturer(@Nullable String deviceManufacturer) {
            this.deviceManufacturer = deviceManufacturer;
            return this;
        }

        @NotNull
        public Builder setDeviceBrand(@Nullable String deviceBrand) {
            this.deviceBrand = deviceBrand;
            return this;
        }

        @NotNull
        public Builder setDeviceModel(@Nullable String deviceModel) {
            this.deviceModel = deviceModel;
            return this;
        }

        @NotNull
        public Builder setCarrier(@Nullable String carrier) {
            this.carrier = carrier;
            return this;
        }

        @NotNull
        public Builder setLibrary(@Nullable String library) {
            this.library = library;
            return this;
        }

        @NotNull
        public Builder setUserProperties(@Nullable JSONObject userProperties) {
            this.userProperties = userProperties;
            return this;
        }

        /**
         * Sets a custom user property for use in rule-based targeting
         */
        @NotNull
        public Builder setUserProperty(@NotNull String property, @NotNull Object value) {
            try {
                this.userProperties.put(property, value);
            } catch (JSONException e) {
                Log.e(Skylab.TAG, e.toString());
            }
            return this;
        }

        /**
         * Performs a clone of an existing SkylabUser into the builder,
         * ignoring nulls. This will overwrite all user properties with the copied
         * user's user properties if the copied user contains user properties.
         * @param user
         * @return
         */
        @NotNull
        public Builder copyUser(@Nullable SkylabUser user) {
            if (user == null) {
                return this;
            }
            if (user.userId != null) {
                setUserId(user.userId);
            }
            if (user.deviceId != null) {
                setDeviceId(user.deviceId);
            }
            if (user.country != null) {
                setCountry(user.country);
            }
            if (user.region != null) {
                setRegion(user.region);
            }
            if (user.dma != null) {
                setDma(user.dma);
            }
            if (user.city != null) {
                setCity(user.city);
            }
            if (user.language != null) {
                setLanguage(user.language);
            }
            if (user.platform != null) {
                setPlatform(user.platform);
            }
            if (user.version != null) {
                setVersion(user.version);
            }
            if (user.os != null) {
                setOs(user.os);
            }
            if (user.deviceFamily != null) {
                setDeviceFamily(user.deviceFamily);
            }
            if (user.deviceType != null) {
                setDeviceType(user.deviceType);
            }
            if (user.deviceManufacturer != null) {
                setDeviceManufacturer(user.deviceManufacturer);
            }
            if (user.deviceBrand != null) {
                setDeviceBrand(user.deviceBrand);
            }
            if (user.deviceModel != null) {
                setDeviceModel(user.deviceModel);
            }
            if (user.carrier != null) {
                setCarrier(user.carrier);
            }
            if (user.library != null) {
                setLibrary(user.library);
            }
            if (user.userProperties != null) {
                try {
                    setUserProperties(new JSONObject(user.userProperties.toString()));
                } catch (JSONException e) {
                    // shouldn't happen
                    Log.w(Skylab.TAG, "Could not copy JSON: " + user.userProperties.toString());
                }
            }
            return this;
        }

        @NotNull
        public SkylabUser build() {
            return new SkylabUser(userId, deviceId, country, region, dma, city, language, platform,
                    version, os, deviceFamily, deviceType, deviceManufacturer, deviceBrand,
                    deviceModel, carrier, library, userProperties);
        }
    }

}
