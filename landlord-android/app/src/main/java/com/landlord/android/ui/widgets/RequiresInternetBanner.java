package com.landlord.android.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.lifecycle.LifecycleOwner;
import com.landlord.android.core.sync.ConnectivityObserver;

/** Shared across Reports/AI Insights - screens that are deliberately
 *  online-only (server-computed content, no offline/outbox angle), per the
 *  app's offline-first architecture decision. Shows/hides itself based on
 *  live connectivity state. */
public class RequiresInternetBanner extends FrameLayout {

    private final TextView label;

    public RequiresInternetBanner(Context context, AttributeSet attrs) {
        super(context, attrs);
        label = new TextView(context);
        label.setText("Requires an internet connection");
        label.setPadding(32, 24, 32, 24);
        label.setBackgroundColor(0xFFFDECEA);
        label.setTextColor(0xFFB3261E);
        addView(label);
        setVisibility(GONE);
    }

    public void observe(LifecycleOwner owner) {
        ConnectivityObserver.getInstance().isOnline().observe(owner,
                online -> setVisibility(Boolean.TRUE.equals(online) ? GONE : VISIBLE));
    }
}
