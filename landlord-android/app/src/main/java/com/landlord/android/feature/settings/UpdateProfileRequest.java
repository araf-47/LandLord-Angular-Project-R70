package com.landlord.android.feature.settings;

/** Wire shape for POST /api/v3/user/update - UserController's
 *  UserRegistrationRequest, only id/email/phone populated here. */
public class UpdateProfileRequest {
    public Long id;
    public String email;
    public String phone;
}
