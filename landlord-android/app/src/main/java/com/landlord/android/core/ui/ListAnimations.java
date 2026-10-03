package com.landlord.android.core.ui;

import android.view.View;
import android.view.animation.AnimationUtils;
import com.landlord.android.R;

/**
 * Fade+slide entrance for RecyclerView rows on first bind, skipped on scroll-back.
 * One instance per adapter - construct a field, call animate() from onBindViewHolder.
 */
public final class ListAnimations {

    private int lastAnimatedPosition = -1;

    public void animate(View itemView, int position) {
        if (position <= lastAnimatedPosition) {
            return;
        }
        lastAnimatedPosition = position;
        itemView.clearAnimation();
        itemView.startAnimation(AnimationUtils.loadAnimation(itemView.getContext(), R.anim.item_fade_slide_in));
    }
}
