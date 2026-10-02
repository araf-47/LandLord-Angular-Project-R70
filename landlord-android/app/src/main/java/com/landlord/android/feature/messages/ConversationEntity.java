package com.landlord.android.feature.messages;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.landlord.android.core.sync.SyncMetadata;

@Entity(tableName = "conversations")
public class ConversationEntity {

    @PrimaryKey
    @NonNull
    public String localId = "";

    /** Nullable - a conversation isn't required to be tied to a tenant. */
    public String tenantLocalId;

    public String withName;
    public long createdAt;

    @Embedded
    public SyncMetadata sync = new SyncMetadata();
}
