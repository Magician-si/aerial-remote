package app.orchard.tv.remote;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RemoteInfoActivity extends Activity {
    @Override
    protected void onDestroy() {
        if (RemoteServer.onFirstAuth == dismissOnConnect) RemoteServer.onFirstAuth = null;
        super.onDestroy();
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private TextView panelText(String text, float sp, int color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setTextColor(color);
        tv.setGravity(Gravity.CENTER);
        tv.setLineSpacing(0, 1.25f);
        return tv;
    }

    private void addTo(LinearLayout panel, View v, int topMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) dp(topMarginDp);
        panel.addView(v, lp);
    }

    private GradientDrawable roundedBg(int fill, int stroke, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(fill);
        d.setStroke((int) dp(1), stroke);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private TextView panelButton(String text) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        btn.setTextColor(0xFFFFFFFF);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding((int) dp(20), (int) dp(10), (int) dp(20), (int) dp(10));
        btn.setFocusable(true);
        btn.setClickable(true);
        GradientDrawable focused = roundedBg(0x26FFFFFF, 0x59FFFFFF, 14);
        btn.setOnFocusChangeListener((v, focusedNow) -> v.setBackground(focusedNow ? focused : null));
        return btn;
    }

    private final Runnable dismissOnConnect = () -> runOnUiThread(this::finish);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RemoteServer.onFirstAuth = dismissOnConnect;
        FrameLayout root = new FrameLayout(this);
        View scrim = new View(this);
        scrim.setBackgroundColor(0x99000000);
        scrim.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrim.setOnClickListener(v -> finish());
        root.addView(scrim);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(roundedBg(0xD9141416, 0x33FFFFFF, 24));
        panel.setClickable(true);
        panel.setPadding((int) dp(30), (int) dp(24), (int) dp(30), (int) dp(22));
        panel.setLayoutParams(new FrameLayout.LayoutParams((int) dp(520),
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        root.addView(panel);

        boolean running = TvAccessibilityService.get() != null;
        String url = RemoteServer.pairingUrl(this);

        TextView title = panelText("手机遥控", 19, 0xFFFFFFFF);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        panel.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (url == null) {
            addTo(panel, panelText("电视还没联网 — 先连上 Wi-Fi 或网线", 14, 0xFFF2C169), 14);
        } else {
            addTo(panel, panelText("用手机相机扫码，或在手机浏览器里输入这个地址：", 13, 0xB3FFFFFF), 10);
            Bitmap qrBitmap = QrCode.toBitmap(QrCode.encode(url), (int) dp(196));
            ImageView qrView = new ImageView(this);
            qrView.setImageBitmap(qrBitmap);
            LinearLayout.LayoutParams qrLp = new LinearLayout.LayoutParams((int) dp(196), (int) dp(196));
            qrLp.gravity = Gravity.CENTER_HORIZONTAL;
            qrLp.topMargin = (int) dp(14);
            panel.addView(qrView, qrLp);
            addTo(panel, panelText(url, 16.5f, 0xFFFFFFFF), 12);
            addTo(panel, panelText("配对码：" + RemoteServer.pin(this) + "（页面要求输入时填它）",
                    13.5f, 0xD9FFFFFF), 6);
            addTo(panel, panelText("手机和电视要连同一个 Wi-Fi", 12, 0x8CFFFFFF), 4);
        }

        addTo(panel, panelText(running ? "无障碍服务已开启 — 在手机上打开页面就能用了"
                : "还差一步：开启无障碍服务，手机才能移动指针、滚动和打字",
                13.5f, running ? 0xFF8FE3B0 : 0xFFF2C169), 16);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams buttonsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonsLp.topMargin = (int) dp(16);
        panel.addView(buttons, buttonsLp);

        if (!running) {
            TextView openA11y = panelButton("去开启无障碍");
            openA11y.setOnClickListener(v -> {
                finish();
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            });
            buttons.addView(openA11y);
        }
        TextView close = panelButton("关闭");
        if (!running) {
            LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            closeLp.leftMargin = (int) dp(10);
            close.setLayoutParams(closeLp);
        }
        close.setOnClickListener(v -> finish());
        buttons.addView(close);
        close.requestFocus();

        setContentView(root);
    }
}
