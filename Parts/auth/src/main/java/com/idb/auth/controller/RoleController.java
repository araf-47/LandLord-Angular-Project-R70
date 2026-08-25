package com.idb.auth.controller;

import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_LIST;
import static com.idb.auth.common.constant.OperationStatus.SUCCESS;
import static com.idb.auth.constant.AuthConstants.URL_ROLE_CONTROLLER;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.service.RoleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(URL_ROLE_CONTROLLER)
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping(value = ENDPOINT_LIST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<String>>> list() {
        return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                .status(SUCCESS)
                .data(roleService.findByActive())
                .build());
    }
}
