package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * A small glass list of actions next to a tile (hold OK, or the remote's Menu key): the same rows and panel as
 * the Inputs menu. The OK release that follows a long press must not activate the first row, so a row acts only
 * on an OK it saw go down.
 */
final class ActionMenu {
    private final Activity activity;
    private final List<CharSequence> titles = new ArrayList<>();
    private final List<Runnable> actions = new ArrayList<>();

    ActionMenu(Activity activity) {
        this.activity = activity;
    }

    ActionMenu add(int title, Runnable action) {
        return add(activity.getString(title), action);
    }

    ActionMenu add(CharSequence title, Runnable action) {
        titles.add(title);
        actions.add(action);
        return this;
    }

    /** Shows the menu over {@code anchor}, focus on the first row. Returns the window so a caller can close it. */
    PopupWindow show(View anchor) {
        float density = activity.getResources().getDisplayMetrics().density;
        View panel = activity.getLayoutInflater().inflate(R.layout.popup_inputs, null, false);
        LinearLayout rows = panel.findViewById(R.id.rows);
        PopupWindow popup = new PopupWindow(panel, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));   // Back closes it
        popup.setOutsideTouchable(true);
        popup.setAnimationStyle(0);
        popup.setElevation(0f);
        int width = Math.round(220 * density);
        for (int i = 0; i < titles.size(); i++) {
            View row = activity.getLayoutInflater().inflate(R.layout.item_input, rows, false);
            row.findViewById(R.id.icon).setVisibility(View.GONE);
            row.findViewById(R.id.dot).setVisibility(View.GONE);
            row.findViewById(R.id.status).setVisibility(View.GONE);
            TextView title = row.findViewById(R.id.title);
            title.setText(titles.get(i));
            title.setEllipsize(TextUtils.TruncateAt.END);
            width = Math.max(width, (int) Math.ceil(title.getPaint().measureText(titles.get(i).toString()))
                    + Math.round(58 * density));
            final Runnable action = actions.get(i);
            final boolean[] pressed = {false};
            row.setOnClickListener(v -> {
                popup.dismiss();
                action.run();
            });
            final int index = i;
            row.setOnKeyListener((v, code, event) -> {
                boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
                // The list does not wrap, and Left/Right do not leak to the tiles behind.
                if (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT) return true;
                if (code == KeyEvent.KEYCODE_DPAD_UP && index == 0) return true;
                if (code == KeyEvent.KEYCODE_DPAD_DOWN && index == titles.size() - 1) return true;
                if (code != KeyEvent.KEYCODE_DPAD_CENTER && code != KeyEvent.KEYCODE_ENTER
                        && code != KeyEvent.KEYCODE_NUMPAD_ENTER) return false;
                if (down) {
                    if (event.getRepeatCount() == 0) pressed[0] = true;
                    return true;
                }
                boolean activate = pressed[0];
                pressed[0] = false;
                if (activate) {
                    popup.dismiss();
                    action.run();
                }
                return true;
            });
            rows.addView(row);
        }
        int screenW = activity.getWindow().getDecorView().getWidth();
        width = Math.min(width, Math.max(Math.round(160 * density), screenW - Math.round(48 * density)));
        popup.setWidth(width);
        panel.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED);
        // Beside the tile when there is room on its right, otherwise on its left; never off screen.
        int[] at = new int[2];
        anchor.getLocationInWindow(at);
        int screenH = activity.getWindow().getDecorView().getHeight();
        int margin = Math.round(16 * density);
        int x = at[0] + anchor.getWidth() + margin;
        if (x + width > screenW - margin) x = Math.max(margin, at[0] - width - margin);
        int y = Math.max(margin, Math.min(at[1], screenH - panel.getMeasuredHeight() - margin));
        panel.setAlpha(0f);
        popup.showAtLocation(activity.getWindow().getDecorView(), Gravity.TOP | Gravity.START, x, y);
        panel.animate().alpha(1f).setDuration(140).start();
        if (rows.getChildCount() > 0) rows.getChildAt(0).requestFocus();
        return popup;
    }
}
