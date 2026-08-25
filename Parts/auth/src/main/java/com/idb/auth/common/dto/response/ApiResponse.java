package com.idb.auth.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.idb.auth.common.constant.OperationStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private OperationStatus status;
    private String message;
    private T data;
}
