package com.landlord.android.feature.properties;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class PropertyDetailViewModel extends AndroidViewModel {

    private final UnitRepository unitRepository;
    private final PropertyDao propertyDao;

    public PropertyDetailViewModel(@NonNull Application application) {
        super(application);
        unitRepository = new UnitRepository(application);
        propertyDao = com.landlord.android.core.db.AppDatabase.getInstance(application).propertyDao();
    }

    public LiveData<List<UnitEntity>> units(String propertyLocalId) {
        return unitRepository.observeByProperty(propertyLocalId);
    }

    public LiveData<PropertyEntity> property(String propertyLocalId) {
        return propertyDao.observeByLocalId(propertyLocalId);
    }

    public void addUnit(String propertyLocalId, String unitNumber, double rent) {
        unitRepository.createUnit(propertyLocalId, unitNumber, rent);
    }
}
