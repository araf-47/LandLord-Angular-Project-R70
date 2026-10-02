package com.landlord.android.core.sync;

import androidx.room.TypeConverter;

public class SyncTypeConverters {

    @TypeConverter
    public static String fromSyncState(SyncState state) {
        return state == null ? null : state.name();
    }

    @TypeConverter
    public static SyncState toSyncState(String value) {
        return value == null ? null : SyncState.valueOf(value);
    }

    @TypeConverter
    public static String fromOpType(OpType type) {
        return type == null ? null : type.name();
    }

    @TypeConverter
    public static OpType toOpType(String value) {
        return value == null ? null : OpType.valueOf(value);
    }
}
