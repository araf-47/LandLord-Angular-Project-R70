package com.idb.auth.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheConfigParams {
    private long ttlMinutes = 60;
    private long maxSize = 100;
    private boolean recordStats = true;
}
