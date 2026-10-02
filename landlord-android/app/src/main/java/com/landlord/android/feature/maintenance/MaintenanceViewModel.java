package com.landlord.android.feature.maintenance;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.landlord.android.feature.properties.UnitEntity;
import java.util.List;

public class MaintenanceViewModel extends AndroidViewModel {

    private final MaintenanceRepository repository;

    public MaintenanceViewModel(@NonNull Application application) {
        super(application);
        repository = new MaintenanceRepository(application);
    }

    public LiveData<List<MaintenanceTicketEntity>> tickets() {
        return repository.observeAll();
    }

    public LiveData<List<UnitEntity>> units() {
        return repository.units();
    }

    public void createTicket(String unitLocalId, String description) {
        repository.createTicket(unitLocalId, description);
    }

    public void resolveTicket(String ticketLocalId, double cost) {
        repository.resolveTicket(ticketLocalId, cost);
    }

    public void attachPhoto(String ticketLocalId, String localFilePath) {
        repository.attachPhoto(ticketLocalId, localFilePath);
    }
}
