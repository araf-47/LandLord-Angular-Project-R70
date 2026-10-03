package com.landlord.android.core.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.landlord.android.R;

/**
 * Modern slide-up replacement for MaterialAlertDialogBuilder on form-heavy
 * add/create dialogs. Wraps an already-inflated form layout with a title and
 * a cancel/confirm button row, drag-to-dismiss included for free.
 */
public final class FormBottomSheet {

    private FormBottomSheet() {
    }

    public interface OnConfirm {
        void onConfirm();
    }

    public static void show(Context context, String title, String confirmText,
                             View content, OnConfirm onConfirm) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_form, null);

        ((TextView) sheetView.findViewById(R.id.sheet_title)).setText(title);
        ((FrameLayout) sheetView.findViewById(R.id.sheet_content)).addView(content);

        sheetView.findViewById(R.id.sheet_cancel).setOnClickListener(v -> dialog.dismiss());

        TextView confirmButton = sheetView.findViewById(R.id.sheet_confirm);
        confirmButton.setText(confirmText);
        confirmButton.setOnClickListener(v -> {
            onConfirm.onConfirm();
            dialog.dismiss();
        });

        dialog.setContentView(sheetView);
        dialog.show();
    }
}
