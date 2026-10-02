package com.landlord.android.feature.tenants;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.landlord.android.core.sync.SyncMetadata;

@Entity(tableName = "tenants")
public class TenantEntity {

    @PrimaryKey
    @NonNull
    public String localId = "";

    /** Local UUID of the owning UnitEntity - resolved to a server id only
     *  at outgoing-request serialization time, same pattern as Unit->Property. */
    public String unitLocalId;

    public String name;
    public String phone;
    public String email;
    public String nationalId;
    public String status = "active";
    public long createdAt;

    @Embedded
    public SyncMetadata sync = new SyncMetadata();
}
