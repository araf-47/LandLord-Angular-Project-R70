package com.landlord.android.feature.properties;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class PropertiesViewModel extends AndroidViewModel {

    private final PropertyRepository repository;

    public PropertiesViewModel(@NonNull Application application) {
        super(application);
        repository = new PropertyRepository(application);
    }

    public LiveData<List<PropertyEntity>> properties() {
        return repository.observeAll();
    }

    public void createProperty(String name, String address, String district,
                                String area, String propertyType) {
        repository.createProperty(name, address, district, area, propertyType);
    }
}
