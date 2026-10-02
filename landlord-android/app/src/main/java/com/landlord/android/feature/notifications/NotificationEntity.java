package com.landlord.android.feature.notifications;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.landlord.android.core.sync.SyncMetadata;

/** Landlord-audience notifications only pull from the server (GET
 *  ?audience=landlord) - the app never creates one, only marks read. */
@Entity(tableName = "notifications")
public class NotificationEntity {

    @PrimaryKey
    @NonNull
    public String localId = "";

    public String type;
    public String title;
    public String body;
    public boolean read;
    public long createdAt;

    @Embedded
    public SyncMetadata sync = new SyncMetadata();
}
