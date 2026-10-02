package com.landlord.android.feature.settings;

/** Wire shape for POST /api/v3/auth/otp - SingleParamRequest&lt;String&gt;, {"id": "username"}. */
public class StringParamRequest {
    public String id;

    public StringParamRequest(String id) {
        this.id = id;
    }
}
