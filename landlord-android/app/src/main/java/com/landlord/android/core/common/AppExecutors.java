package com.landlord.android.core.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppExecutors {
    public static final ExecutorService DB = Executors.newSingleThreadExecutor();
}
