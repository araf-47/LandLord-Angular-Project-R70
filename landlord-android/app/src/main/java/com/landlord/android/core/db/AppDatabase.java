package com.landlord.android.core.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.landlord.android.core.sync.IdMappingDao;
import com.landlord.android.core.sync.IdMappingEntity;
import com.landlord.android.core.sync.PendingOperationDao;
import com.landlord.android.core.sync.PendingOperationEntity;
import com.landlord.android.core.sync.SyncTypeConverters;
import com.landlord.android.feature.properties.PropertyDao;
import com.landlord.android.feature.properties.PropertyEntity;
import com.landlord.android.feature.properties.UnitDao;
import com.landlord.android.feature.properties.UnitEntity;
import com.landlord.android.feature.tenants.TenantDao;
import com.landlord.android.feature.tenants.TenantEntity;
import com.landlord.android.feature.payments.InvoiceDao;
import com.landlord.android.feature.payments.InvoiceEntity;
import com.landlord.android.feature.payments.PaymentDao;
import com.landlord.android.feature.payments.PaymentEntity;
import com.landlord.android.feature.expenses.ExpenseDao;
import com.landlord.android.feature.expenses.ExpenseEntity;
import com.landlord.android.feature.marketplace.MarketplaceRequestDao;
import com.landlord.android.feature.marketplace.MarketplaceRequestEntity;
import com.landlord.android.feature.maintenance.MaintenanceTicketDao;
import com.landlord.android.feature.maintenance.MaintenanceTicketEntity;
import com.landlord.android.feature.messages.ConversationDao;
import com.landlord.android.feature.messages.ConversationEntity;
import com.landlord.android.feature.messages.MessageDao;
import com.landlord.android.feature.messages.MessageEntity;
import com.landlord.android.feature.notifications.NotificationDao;
import com.landlord.android.feature.notifications.NotificationEntity;

@Database(
        entities = {
                PendingOperationEntity.class,
                IdMappingEntity.class,
                PropertyEntity.class,
                UnitEntity.class,
                TenantEntity.class,
                InvoiceEntity.class,
                PaymentEntity.class,
                ExpenseEntity.class,
                MarketplaceRequestEntity.class,
                MaintenanceTicketEntity.class,
                ConversationEntity.class,
                MessageEntity.class,
                NotificationEntity.class
        },
        version = 2,
        exportSchema = false
)
@TypeConverters({SyncTypeConverters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract PendingOperationDao pendingOperationDao();

    public abstract IdMappingDao idMappingDao();

    public abstract PropertyDao propertyDao();

    public abstract UnitDao unitDao();

    public abstract TenantDao tenantDao();

    public abstract InvoiceDao invoiceDao();

    public abstract PaymentDao paymentDao();

    public abstract ExpenseDao expenseDao();

    public abstract MarketplaceRequestDao marketplaceRequestDao();

    public abstract MaintenanceTicketDao maintenanceTicketDao();

    public abstract ConversationDao conversationDao();

    public abstract MessageDao messageDao();

    public abstract NotificationDao notificationDao();

    public static AppDatabase getInstance(Context appContext) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            appContext.getApplicationContext(),
                            AppDatabase.class,
                            "landlord.db"
                    ).fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
