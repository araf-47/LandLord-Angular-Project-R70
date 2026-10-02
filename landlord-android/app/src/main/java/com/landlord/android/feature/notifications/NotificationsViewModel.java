package com.landlord.android.feature.notifications;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class NotificationsViewModel extends AndroidViewModel {

    private final NotificationRepository repository;

    public NotificationsViewModel(@NonNull Application application) {
        super(application);
        repository = new NotificationRepository(application);
    }

    public LiveData<List<NotificationEntity>> notifications() {
        return repository.observeAll();
    }

    public void markRead(String localId) {
        repository.markRead(localId);
    }
}
