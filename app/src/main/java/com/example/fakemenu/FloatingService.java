package com.example.fakemenu;

import android.animation.ValueAnimator;
import android.app.Service;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Locale;

/** JelhzHax - fake floating menu. Pure UI: nothing here touches any game or app. */
@SuppressWarnings("deprecation")
public class FloatingService extends Service {
    static { System.loadLibrary("fakemenu"); }
    private native String nativeTitle();
    private native String nativeHandle();
    private native String nativeVersion();
    private native String nativeFooter();

    private static final int MATCH = -1, WRAP = -2;
    private static final int RED = 0xFFFF3030, BG = 0xFF0B0B0C, SIDE = 0xFF0F0F11, ROW = 0xFF121214;
    private static final int TEXT = 0xFFF2F2F2, MUTED = 0xFF8A8A92, DIM = 0xFF55555C;

    private static final String[] TABS = {"Combat", "Visuals", "Memory", "Movement", "Skins", "Settings"};
    private static final String[] GLYPH = {"\u271B", "\u25C9", "\u25A6", "\u27A4", "\u2726", "\u2731"};
    private static final String[] SEC_SMALL = {"Targeting", "Overlay", "Game Memory", "Motion", "Cosmetics", "Menu"};
    private static final String[] SEC_TITLE = {"Combat Assist", "Visual Overlay", "Memory Tweaks",
            "Movement Tweaks", "Skin Options", "Menu Settings"};

    private final int[] total = new int[TABS.length];
    private final int[] active = new int[TABS.length];
    private final LinearLayout[] pages = new LinearLayout[TABS.length];
    private final LinearLayout[] tabItem = new LinearLayout[TABS.length];
    private final TextView[] tabTitle = new TextView[TABS.length];
    private final TextView[] tabIcon = new TextView[TABS.length];
    private final TextView[] tabSub = new TextView[TABS.length];
    private TextView bCat, bTitle, bSub, bGlyph, uptime;
    private ScrollView contentScroll;
    private int current;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private long startMs;
    private boolean placed;

    private WindowManager wm;
    private View icon, panel;
    private WindowManager.LayoutParams iconLp, panelLp;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (uptime != null) {
                long s = (System.currentTimeMillis() - startMs) / 1000;
                uptime.setText(String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60));
            }
            ui.postDelayed(this, 1000);
        }
    };

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (icon == null) {
            wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            startMs = System.currentTimeMillis();
            buildIcon();
            buildPanel();
            ui.post(tick);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ui.removeCallbacks(tick);
        if (icon != null) wm.removeView(icon);
        if (panel != null) wm.removeView(panel);
        icon = null;
        panel = null;
    }

    // ================= Floating icon =================
    private void buildIcon() {
        icon = new JCrownView(this);
        iconLp = params(dp(58), dp(58));
        iconLp.x = dp(6);
        iconLp.y = dp(120);
        icon.setOnTouchListener(new Drag(icon, iconLp, true,
                () -> showPanel(panel.getVisibility() != View.VISIBLE)));
        wm.addView(icon, iconLp);
    }

    private void showPanel(boolean show) {
        if (panel == null) return;
        panel.animate().cancel();
        if (show) {
            fitPanel();
            panel.setAlpha(0f);
            panel.setScaleX(0.94f);
            panel.setScaleY(0.94f);
            panel.setVisibility(View.VISIBLE);
            panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(170).start();
        } else {
            panel.animate().alpha(0f).scaleX(0.94f).scaleY(0.94f).setDuration(120)
                    .withEndAction(() -> {
                        if (panel != null && panel.getAlpha() < 0.05f) panel.setVisibility(View.GONE);
                    }).start();
        }
    }

    // Re-fit to the current screen (works after rotating into landscape)
    private void fitPanel() {
        DisplayMetrics m = getResources().getDisplayMetrics();
        panelLp.width = Math.min(dp(470), m.widthPixels - dp(12));
        panelLp.height = Math.min(dp(320), m.heightPixels - dp(24));
        if (!placed) {
            panelLp.x = (m.widthPixels - panelLp.width) / 2;
            panelLp.y = (m.heightPixels - panelLp.height) / 2;
            placed = true;
        }
        panelLp.x = Math.max(0, Math.min(panelLp.x, m.widthPixels - panelLp.width));
        panelLp.y = Math.max(0, Math.min(panelLp.y, m.heightPixels - panelLp.height));
        wm.updateViewLayout(panel, panelLp);
    }

    // ================= Menu panel =================
    private void buildPanel() {
        buildPages();

        LinearLayout root = new LinearLayout(this);
        root.setBackground(box(BG, dp(12), 0xFF3A1212));
        root.setClipToOutline(true);

        // ---------- Sidebar ----------
        LinearLayout side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setPadding(dp(8), dp(8), dp(8), dp(8));

        LinearLayout logo = new LinearLayout(this);
        logo.setGravity(Gravity.CENTER_VERTICAL);
        logo.addView(new JCrownView(this), new LinearLayout.LayoutParams(dp(32), dp(32)));
        String name = nativeTitle();
        SpannableStringBuilder brandText = new SpannableStringBuilder(name);
        brandText.setSpan(new ForegroundColorSpan(RED), Math.max(0, name.length() - 3),
                name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView brand = label("", 13, TEXT, true);
        brand.setText(brandText);
        brand.setPadding(dp(6), 0, 0, 0);
        logo.addView(brand);
        side.addView(logo);

        TextView handle = label(nativeHandle(), 10, MUTED, false);
        handle.setGravity(Gravity.CENTER);
        handle.setPadding(0, dp(8), 0, dp(8));
        side.addView(handle, new LinearLayout.LayoutParams(MATCH, WRAP));
        side.addView(divider(), new LinearLayout.LayoutParams(MATCH, dp(1)));

        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            LinearLayout item = new LinearLayout(this);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(5), dp(5), dp(5), dp(5));

            TextView ic = label(GLYPH[i], 13, RED, true);
            ic.setGravity(Gravity.CENTER);
            item.addView(ic, new LinearLayout.LayoutParams(dp(26), dp(26)));

            LinearLayout tx = new LinearLayout(this);
            tx.setOrientation(LinearLayout.VERTICAL);
            tx.setPadding(dp(8), 0, 0, 0);
            TextView tt = label(TABS[i], 12, TEXT, true);
            TextView ts = label("0 enabled", 9, MUTED, false);
            tx.addView(tt);
            tx.addView(ts);
            item.addView(tx);
            item.setOnClickListener(v -> selectTab(idx));

            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(MATCH, WRAP);
            ilp.topMargin = dp(4);
            side.addView(item, ilp);
            tabItem[i] = item;
            tabTitle[i] = tt;
            tabIcon[i] = ic;
            tabSub[i] = ts;
        }

        int totalFeatures = 0;
        for (int t : total) totalFeatures += t;
        TextView infoHead = label("MENU INFORMATION", 9, MUTED, true);
        infoHead.setLetterSpacing(0.15f);
        infoHead.setPadding(dp(2), dp(14), 0, dp(6));
        side.addView(infoHead);
        uptime = infoRow(side, "Uptime", "00:00:00", TEXT);
        infoRow(side, "Variant", "UI Demo", TEXT);
        infoRow(side, "Features", String.valueOf(totalFeatures), TEXT);
        infoRow(side, "Version", nativeVersion(), RED);

        ScrollView sideScroll = new ScrollView(this);
        sideScroll.setBackgroundColor(SIDE);
        sideScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        sideScroll.setVerticalScrollBarEnabled(false);
        sideScroll.addView(side);
        root.addView(sideScroll, new LinearLayout.LayoutParams(dp(124), MATCH));

        // ---------- Content column ----------
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        top.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF2A0000, 0xFF100000}));
        TextView close = label("\u2715", 15, TEXT, true);
        close.setGravity(Gravity.CENTER);
        close.setPadding(dp(14), 0, dp(14), 0);
        close.setOnClickListener(v -> showPanel(false));
        top.addView(close, new LinearLayout.LayoutParams(WRAP, MATCH));
        col.addView(top, new LinearLayout.LayoutParams(MATCH, dp(30)));

        LinearLayout banner = new LinearLayout(this);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPadding(dp(10), dp(8), dp(10), dp(8));
        banner.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF3A0808, BG}));
        bGlyph = label("", 20, 0xFFFFFFFF, true);
        bGlyph.setGravity(Gravity.CENTER);
        bGlyph.setBackground(redBox(dp(10)));
        banner.addView(bGlyph, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout bt = new LinearLayout(this);
        bt.setOrientation(LinearLayout.VERTICAL);
        bt.setPadding(dp(10), 0, 0, 0);
        bCat = label("", 9, MUTED, true);
        bCat.setLetterSpacing(0.2f);
        bTitle = label("", 20, TEXT, true);
        bSub = label("", 11, MUTED, false);
        bt.addView(bCat);
        bt.addView(bTitle);
        bt.addView(bSub);
        banner.addView(bt);
        col.addView(banner, new LinearLayout.LayoutParams(MATCH, WRAP));

        FrameLayout holder = new FrameLayout(this);
        for (LinearLayout p : pages) holder.addView(p);
        contentScroll = new ScrollView(this);
        contentScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        contentScroll.setVerticalScrollBarEnabled(false);
        contentScroll.addView(holder);
        col.addView(contentScroll, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        TextView foot = label(nativeFooter(), 9, DIM, false);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, dp(4), 0, dp(5));
        col.addView(foot, new LinearLayout.LayoutParams(MATCH, WRAP));

        root.addView(col, new LinearLayout.LayoutParams(0, MATCH, 1f));

        // ---------- Window ----------
        panel = root;
        panelLp = params(dp(470), dp(320));
        top.setOnTouchListener(new Drag(panel, panelLp, false, null));
        logo.setOnTouchListener(new Drag(panel, panelLp, false, null));
        panel.setVisibility(View.GONE);
        wm.addView(panel, panelLp);
        selectTab(0);
    }

    private void selectTab(int idx) {
        current = idx;
        for (int j = 0; j < TABS.length; j++) {
            boolean on = j == idx;
            pages[j].setVisibility(on ? View.VISIBLE : View.GONE);
            tabItem[j].setBackground(on ? box(0xFF1B1B1E, dp(9), 0xFF2A2A30) : null);
            tabTitle[j].setTextColor(on ? RED : TEXT);
            tabIcon[j].setTextColor(on ? 0xFFFFFFFF : RED);
            tabIcon[j].setBackground(on ? redBox(dp(8)) : box(0xFF2A0808, dp(8), 0x00000000));
        }
        bCat.setText(TABS[idx].toUpperCase(Locale.US));
        bTitle.setText(TABS[idx]);
        bGlyph.setText(GLYPH[idx]);
        contentScroll.scrollTo(0, 0);
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < TABS.length; i++) {
            if (tabSub[i] != null) tabSub[i].setText(active[i] + " enabled");
        }
        if (bSub != null) {
            bSub.setText(total[current] + " features \u00D7 " + active[current] + " active");
        }
    }

    private void bump(int tab, int d) {
        active[tab] += d;
        refresh();
    }

    // ================= Fake features (31, all UI only) =================
    private void buildPages() {
        for (int i = 0; i < TABS.length; i++) {
            LinearLayout p = new LinearLayout(this);
            p.setOrientation(LinearLayout.VERTICAL);
            p.setPadding(dp(10), dp(8), dp(10), dp(8));
            TextView small = label(SEC_SMALL[i], 10, RED, false);
            TextView title = label(SEC_TITLE[i], 15, TEXT, true);
            title.setPadding(0, dp(1), 0, dp(6));
            p.addView(small);
            p.addView(title);
            pages[i] = p;
        }

        // Combat (6)
        feat(0, "Aim Assist", "Smooth target tracking on nearby enemies");
        slider(0, "Aim Smoothness", "Set how strongly the aim locks on", 100);
        feat(0, "Auto Skill Cast", "Cast skills automatically when ready");
        feat(0, "Auto Basic Attack", "Attack the nearest target automatically");
        feat(0, "Fast Cast", "Remove the skill cast delay");
        slider(0, "Aim Range", "Maximum distance for target lock", 100);

        // Visuals (6)
        feat(1, "ESP Line", "Draw a line to every visible player");
        feat(1, "ESP Box", "Draw a box around every visible player");
        feat(1, "ESP Name", "Show player names above their heads");
        feat(1, "ESP Health Bar", "Show a health bar next to each player");
        feat(1, "Show Cooldowns", "Display skill cooldown timers");
        feat(1, "Map Awareness", "Show extra markers on the minimap");

        // Memory (5)
        feat(2, "Cooldown Reducer", "Shorten skill cooldown timers");
        feat(2, "Mana Saver", "Skills cost less energy to use");
        slider(2, "Range Booster", "Extend the basic attack range", 100);
        slider(2, "Drone View", "Widen the camera view", 10);
        feat(2, "Anti Flash", "Prevent flash effects from blocking the view");

        // Movement (5)
        slider(3, "Speed Boost", "Increase the movement speed", 100);
        feat(3, "Auto Dash", "Dash toward the aim point automatically");
        feat(3, "No Slow", "Ignore slowing effects");
        feat(3, "Smooth Camera", "Keep the camera steady while moving");
        feat(3, "Anti Stun", "Shorten the stun duration");

        // Skins (5)
        feat(4, "Unlock All Skins", "Show every skin in the locker");
        feat(4, "Recall Effects", "Use custom recall animations");
        feat(4, "Elimination Effects", "Use custom elimination effects");
        feat(4, "Battle Emotes", "Unlock all battle emotes");
        feat(4, "Custom Frames", "Use any profile frame");

        // Settings (4 + 2 buttons)
        slider(5, "Menu Opacity", "Adjust how see-through the menu is", 100);
        feat(5, "Icon Glow", "Pulse the floating icon glow");
        feat(5, "Haptic Feedback", "Vibrate lightly on every tap");
        feat(5, "Lock Position", "Stop the menu from being dragged");
        button(5, "Reset All", true, () -> reset(panel));
        button(5, "Exit Menu", false, this::stopSelf);
    }

    private LinearLayout row(int tab, String name, String desc) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setBackground(box(ROW, dp(8), 0xFF1C1C20));
        r.setPadding(dp(8), dp(7), dp(10), dp(7));

        TextView idx = label(String.valueOf(total[tab]++), 11, DIM, false);
        idx.setGravity(Gravity.CENTER);
        r.addView(idx, new LinearLayout.LayoutParams(dp(22), WRAP));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(4), 0, dp(6), 0);
        col.addView(label(name, 13, TEXT, true));
        col.addView(label(desc, 10, MUTED, false));
        r.addView(col, new LinearLayout.LayoutParams(0, WRAP, 1f));
        return r;
    }

    private void feat(int tab, String name, String desc) {
        LinearLayout r = row(tab, name, desc);
        Switch sw = new Switch(this);
        sw.setThumbTintList(ColorStateList.valueOf(0xFFFFFFFF));
        sw.setTrackTintList(tint(RED, 0xFF3A3A40));
        sw.setOnCheckedChangeListener((b, on) -> bump(tab, on ? 1 : -1));
        r.addView(sw);
        pages[tab].addView(r, rowLp());
    }

    private void slider(int tab, String name, String desc, int max) {
        LinearLayout r = row(tab, name, desc);
        SeekBar sb = new SeekBar(this);
        sb.setMax(max);
        sb.setProgressTintList(ColorStateList.valueOf(RED));
        sb.setThumbTintList(ColorStateList.valueOf(0xFFFFFFFF));
        sb.setProgressBackgroundTintList(ColorStateList.valueOf(0x33FFFFFF));
        TextView val = label("0", 11, MUTED, true);
        val.setGravity(Gravity.END);
        final boolean[] was = {false};
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int p, boolean user) {
                val.setText(String.valueOf(p));
                boolean now = p > 0;
                if (now != was[0]) {
                    was[0] = now;
                    bump(tab, now ? 1 : -1);
                }
            }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        });
        r.addView(sb, new LinearLayout.LayoutParams(dp(90), WRAP));
        r.addView(val, new LinearLayout.LayoutParams(dp(26), WRAP));
        pages[tab].addView(r, rowLp());
    }

    private void button(int tab, String s, boolean primary, Runnable r) {
        TextView b = label(s, 13, primary ? 0xFFFFFFFF : 0xFFFF7A7A, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(primary ? redBox(dp(10)) : box(ROW, dp(10), 0xFF3A1A1A));
        b.setPadding(0, dp(10), 0, dp(10));
        b.setOnClickListener(v -> r.run());
        LinearLayout.LayoutParams lp = rowLp();
        lp.topMargin = dp(4);
        pages[tab].addView(b, lp);
    }

    private TextView infoRow(LinearLayout parent, String k, String v, int valueColor) {
        LinearLayout r = new LinearLayout(this);
        r.setPadding(dp(2), dp(2), dp(2), dp(2));
        r.addView(label(k, 10, MUTED, false), new LinearLayout.LayoutParams(0, WRAP, 1f));
        TextView val = label(v, 10, valueColor, true);
        r.addView(val);
        parent.addView(r, new LinearLayout.LayoutParams(MATCH, WRAP));
        return val;
    }

    // Resets every switch/slider to off/0 (UI state only)
    private void reset(View v) {
        if (v instanceof Switch) ((Switch) v).setChecked(false);
        else if (v instanceof SeekBar) ((SeekBar) v).setProgress(0);
        else if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) reset(g.getChildAt(i));
        }
    }

    // ================= Helpers =================
    private TextView label(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private GradientDrawable box(int fill, int radius, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius);
        g.setStroke(dp(1), stroke);
        return g;
    }

    private GradientDrawable redBox(int radius) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFFFF4D4D, 0xFF8A0000});
        g.setCornerRadius(radius);
        return g;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(0xFF1E1E22);
        return v;
    }

    private ColorStateList tint(int on, int off) {
        return new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{on, off});
    }

    private LinearLayout.LayoutParams rowLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.bottomMargin = dp(6);
        return lp;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private WindowManager.LayoutParams params(int w, int h) {
        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(w, h, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        return lp;
    }

    // Drag a floating view; tap runs onTap; optional edge snap on release
    private class Drag implements View.OnTouchListener {
        final View view;
        final WindowManager.LayoutParams lp;
        final boolean snap;
        final Runnable onTap;
        int sx, sy;
        float tx, ty;
        boolean moved;

        Drag(View view, WindowManager.LayoutParams lp, boolean snap, Runnable onTap) {
            this.view = view;
            this.lp = lp;
            this.snap = snap;
            this.onTap = onTap;
        }

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    sx = lp.x; sy = lp.y;
                    tx = e.getRawX(); ty = e.getRawY();
                    moved = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = e.getRawX() - tx, dy = e.getRawY() - ty;
                    if (Math.abs(dx) > dp(4) || Math.abs(dy) > dp(4)) moved = true;
                    if (moved) {
                        lp.x = sx + (int) dx;
                        lp.y = sy + (int) dy;
                        wm.updateViewLayout(view, lp);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!moved) { if (onTap != null) onTap.run(); }
                    else if (snap) snapToEdge();
                    return true;
            }
            return false;
        }

        private void snapToEdge() {
            int screenW = getResources().getDisplayMetrics().widthPixels;
            int end = (lp.x + view.getWidth() / 2 < screenW / 2)
                    ? dp(6) : screenW - view.getWidth() - dp(6);
            ValueAnimator va = ValueAnimator.ofInt(lp.x, end);
            va.setDuration(200);
            va.addUpdateListener(a -> {
                if (!view.isAttachedToWindow()) return;
                lp.x = (int) a.getAnimatedValue();
                wm.updateViewLayout(view, lp);
            });
            va.start();
        }
    }
}
