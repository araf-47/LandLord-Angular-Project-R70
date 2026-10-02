package com.landlord.android.feature.messages;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.landlord.android.core.sync.SyncMetadata;

@Entity(tableName = "messages")
public class MessageEntity {

    @PrimaryKey
    @NonNull
    public String localId = "";

    public String conversationLocalId;
    public String senderRole = "landlord";
    public String text;
    public boolean read;
    public long sentAt;

    @Embedded
    public SyncMetadata sync = new SyncMetadata();
}
