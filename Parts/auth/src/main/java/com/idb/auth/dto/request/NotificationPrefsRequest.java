package com.idb.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationPrefsRequest {
    @JsonProperty("notifyRentDueEmail")
    private boolean notifyRentDueEmail;

    @JsonProperty("notifyRentDueSms")
    private boolean notifyRentDueSms;

    @JsonProperty("notifyPaymentReceivedEmail")
    private boolean notifyPaymentReceivedEmail;

    @JsonProperty("notifyMaintenanceEmail")
    private boolean notifyMaintenanceEmail;
}
