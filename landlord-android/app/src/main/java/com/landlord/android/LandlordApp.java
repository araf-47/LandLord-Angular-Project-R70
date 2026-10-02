package com.landlord.android;

import android.app.Application;
import com.landlord.android.core.auth.TokenManager;
import com.landlord.android.core.sync.ConnectivityObserver;
import com.landlord.android.core.sync.SyncHandlerRegistry;
import com.landlord.android.core.sync.SyncScheduler;
import com.landlord.android.feature.properties.PropertySyncHandler;
import com.landlord.android.feature.properties.UnitSyncHandler;
import com.landlord.android.feature.tenants.TenantSyncHandler;
import com.landlord.android.feature.payments.InvoiceSyncHandler;
import com.landlord.android.feature.payments.PaymentSyncHandler;
import com.landlord.android.feature.expenses.ExpenseSyncHandler;
import com.landlord.android.feature.marketplace.MarketplaceSyncHandler;
import com.landlord.android.feature.maintenance.MaintenanceSyncHandler;
import com.landlord.android.feature.maintenance.TicketPhotoSyncHandler;
import com.landlord.android.feature.messages.ConversationSyncHandler;
import com.landlord.android.feature.messages.MessageSyncHandler;
import com.landlord.android.feature.notifications.NotificationSyncHandler;

public class LandlordApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        SyncHandlerRegistry.register(new PropertySyncHandler(this));
        SyncHandlerRegistry.register(new UnitSyncHandler(this));
        SyncHandlerRegistry.register(new TenantSyncHandler(this));
        SyncHandlerRegistry.register(new InvoiceSyncHandler(this));
        SyncHandlerRegistry.register(new PaymentSyncHandler(this));
        SyncHandlerRegistry.register(new ExpenseSyncHandler(this));
        SyncHandlerRegistry.register(new MarketplaceSyncHandler(this));
        SyncHandlerRegistry.register(new MaintenanceSyncHandler(this));
        SyncHandlerRegistry.register(new TicketPhotoSyncHandler(this));
        SyncHandlerRegistry.register(new ConversationSyncHandler(this));
        SyncHandlerRegistry.register(new MessageSyncHandler(this));
        SyncHandlerRegistry.register(new NotificationSyncHandler(this));

        ConnectivityObserver.getInstance().start(this);

        if (TokenManager.getInstance(this).hasTokens()) {
            SyncScheduler.ensurePeriodicSyncScheduled(this);
        }
    }
}
