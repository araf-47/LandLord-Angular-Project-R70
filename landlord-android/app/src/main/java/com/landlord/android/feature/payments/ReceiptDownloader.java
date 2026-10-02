package com.landlord.android.feature.payments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.FileProvider;
import com.landlord.android.core.common.AppExecutors;
import com.landlord.android.core.network.NetworkModule;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.ResponseBody;
import retrofit2.Response;

/** Receipts are a binary PDF generated server-side - inherently online-only,
 *  no offline/outbox angle here. Downloads to app cache and opens via a
 *  FileProvider URI + the system's PDF viewer/share sheet. */
public class ReceiptDownloader {

    public interface Callback {
        void onSuccess(Uri uri);
        void onError(String message);
    }

    public static void downloadAndOpen(Context context, long paymentServerId, Callback callback) {
        Context appContext = context.getApplicationContext();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        AppExecutors.DB.execute(() -> {
            try {
                PaymentApiService api = NetworkModule.getRetrofit(appContext).create(PaymentApiService.class);
                Response<ResponseBody> response = api.receipt(paymentServerId).execute();

                if (!response.isSuccessful() || response.body() == null) {
                    mainHandler.post(() -> callback.onError("HTTP " + response.code()));
                    return;
                }

                File dir = new File(appContext.getCacheDir(), "receipts");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, "receipt-" + paymentServerId + ".pdf");

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

                mainHandler.post(() -> callback.onSuccess(uri));
            } catch (IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Download failed"));
            }
        });
    }

    public static void openPdf(Context context, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(intent, "Open receipt"));
    }
}
