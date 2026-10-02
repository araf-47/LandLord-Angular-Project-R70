package com.landlord.android.feature.reports;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.FileProvider;
import com.landlord.android.core.common.AppExecutors;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/** Same pattern as ReceiptDownloader - binary, server-generated, online-only,
 *  no offline/outbox angle. Generic over PDF/XLSX export calls across all
 *  report types instead of one class per type. */
public class ReportFileDownloader {

    public interface Callback {
        void onSuccess(Uri uri, String mimeType);
        void onError(String message);
    }

    public static void download(Context context, Call<ResponseBody> call, String fileName,
                                 String mimeType, Callback callback) {
        Context appContext = context.getApplicationContext();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        AppExecutors.DB.execute(() -> {
            try {
                Response<ResponseBody> response = call.execute();
                if (!response.isSuccessful() || response.body() == null) {
                    mainHandler.post(() -> callback.onError("HTTP " + response.code()));
                    return;
                }

                File dir = new File(appContext.getCacheDir(), "reports");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, fileName);

                try (InputStream in = response.body().byteStream();
                     FileOutputStream out = new FileOutputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }

                Uri uri = FileProvider.getUriForFile(appContext,
                        appContext.getPackageName() + ".fileprovider", file);

                mainHandler.post(() -> callback.onSuccess(uri, mimeType));
            } catch (IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Download failed"));
            }
        });
    }

    public static void openFile(Context context, Uri uri, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(intent, "Open report"));
    }
}
