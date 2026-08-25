package com.idb.auth.common.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SingleParamRequest<T> {
    @JsonProperty("id")
    @NotNull(message = "ID is required")
    private T id;
}
