package com.landlord.android.core.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class ConnectivityObserver {

    private static ConnectivityObserver instance;

    private final MutableLiveData<Boolean> isOnline = new MutableLiveData<>(false);

    private ConnectivityObserver() {
    }

    public static synchronized ConnectivityObserver getInstance() {
        if (instance == null) {
            instance = new ConnectivityObserver();
        }
        return instance;
    }

    public void start(Context appContext) {
        ConnectivityManager cm = (ConnectivityManager)
                appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                isOnline.postValue(true);
                SyncScheduler.requestImmediateSync(appContext);
            }

            @Override
            public void onLost(Network network) {
                isOnline.postValue(false);
            }
        });
    }

    public LiveData<Boolean> isOnline() {
        return isOnline;
    }
}
