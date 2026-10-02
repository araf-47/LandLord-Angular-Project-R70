package com.landlord.android.feature.settings;

/** Wire shape for POST /api/v3/user/toggle-2fa - backend's
 *  SingleParamRequest&lt;Boolean&gt;, field confusingly still named "id". */
public class BooleanParamRequest {
    public boolean id;

    public BooleanParamRequest(boolean id) {
        this.id = id;
    }
}
