package de.foxstream.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.ui.PlayerView;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// Live-TV der oeffentlich-rechtlichen Sender direkt in der App abspielen.
// Nach oben wischen = naechster Sender, nach unten = vorheriger.
@CapacitorPlugin(name = "Tv")
public class TvPlugin extends Plugin {

    private static int dp(Activity a, int v) {
        return (int) (v * a.getResources().getDisplayMetrics().density + 0.5f);
    }

    @SuppressLint("UnsafeOptInUsageError")
    @PluginMethod
    public void play(final PluginCall call) {
        final List<JSONObject> items = new ArrayList<>();
        try {
            JSArray arr = call.getArray("items", null);
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    items.add(arr.getJSONObject(i));
                }
            }
        } catch (Exception e) {
            call.reject("Senderliste nicht lesbar");
            return;
        }
        if (items.isEmpty()) {
            call.reject("Keine Sender");
            return;
        }
        final int start = Math.max(0, Math.min(items.size() - 1, call.getInt("start", 0)));
        final Activity act = getActivity();
        if (act == null) {
            call.reject("Keine Ansicht");
            return;
        }
        act.runOnUiThread(() -> {
            try {
                final int n = items.size();
                final int[] idx = {start};

                LinearLayout root = new LinearLayout(act);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setBackgroundColor(Color.BLACK);

                LinearLayout bar = new LinearLayout(act);
                bar.setOrientation(LinearLayout.HORIZONTAL);
                bar.setGravity(Gravity.CENTER_VERTICAL);
                bar.setPadding(dp(act, 6), dp(act, 8), dp(act, 12), dp(act, 8));
                Button close = new Button(act);
                close.setText("✕");
                close.setTextColor(Color.WHITE);
                close.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                close.setBackgroundColor(Color.TRANSPARENT);
                bar.addView(close, new LinearLayout.LayoutParams(dp(act, 52), dp(act, 48)));
                final TextView live = new TextView(act);
                live.setText("● LIVE ");
                live.setTextColor(Color.rgb(255, 90, 70));
                live.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                bar.addView(live);
                final TextView t = new TextView(act);
                t.setTextColor(Color.WHITE);
                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                t.setMaxLines(1);
                t.setEllipsize(android.text.TextUtils.TruncateAt.END);
                bar.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                final TextView cnt = new TextView(act);
                cnt.setTextColor(Color.rgb(170, 170, 170));
                cnt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                bar.addView(cnt);
                root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                DefaultHttpDataSource.Factory hf = new DefaultHttpDataSource.Factory()
                        .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Mobile Safari/537.36")
                        .setAllowCrossProtocolRedirects(true)
                        .setConnectTimeoutMs(15000)
                        .setReadTimeoutMs(20000);
                final HlsMediaSource.Factory f = new HlsMediaSource.Factory(hf);
                final ExoPlayer player = new ExoPlayer.Builder(act).build();
                final PlayerView pv = new PlayerView(act);
                pv.setPlayer(player);
                pv.setKeepScreenOn(true);
                root.addView(pv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

                final TextView info = new TextView(act);
                info.setTextColor(Color.rgb(200, 200, 200));
                info.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                info.setGravity(Gravity.CENTER);
                info.setPadding(dp(act, 16), dp(act, 8), dp(act, 16), dp(act, 14));
                root.addView(info, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                final Runnable tune = () -> {
                    JSONObject it = items.get(idx[0]);
                    t.setText(it.optString("name", "Sender"));
                    cnt.setText(n > 1 ? (idx[0] + 1) + " / " + n : "");
                    info.setTextColor(Color.rgb(200, 200, 200));
                    info.setText(n > 1 ? "Nach oben wischen = nächster Sender · nach unten = vorheriger" : "");
                    MediaItem mi = new MediaItem.Builder().setUri(it.optString("url", "")).setMimeType(MimeTypes.APPLICATION_M3U8).build();
                    MediaSource src = f.createMediaSource(mi);
                    player.setMediaSource(src);
                    player.prepare();
                    player.setPlayWhenReady(true);
                    JSObject o = new JSObject();
                    o.put("index", idx[0]);
                    notifyListeners("tuned", o);
                };
                player.addListener(new Player.Listener() {
                    @Override
                    public void onPlayerError(PlaybackException e) {
                        info.setTextColor(Color.rgb(255, 170, 150));
                        info.setText("Dieser Sender lädt gerade nicht (" + e.getErrorCodeName() + "). Manche Sender gibt es nur in Deutschland. Wisch zum nächsten.");
                    }
                });

                final GestureDetector gd = new GestureDetector(act, new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                        if (e1 == null || n < 2) {
                            return false;
                        }
                        float dy = e2.getY() - e1.getY();
                        float dx = e2.getX() - e1.getX();
                        if (Math.abs(dy) < dp(act, 70) || Math.abs(dy) < Math.abs(dx) * 1.3f || Math.abs(vy) < 500) {
                            return false;
                        }
                        idx[0] = (idx[0] + (dy < 0 ? 1 : -1) + n) % n;
                        tune.run();
                        return true;
                    }
                });
                final Dialog d = new Dialog(act, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
                    @Override
                    public boolean dispatchTouchEvent(MotionEvent ev) {
                        if (gd.onTouchEvent(ev)) {
                            MotionEvent c = MotionEvent.obtain(ev);
                            c.setAction(MotionEvent.ACTION_CANCEL);
                            super.dispatchTouchEvent(c);
                            c.recycle();
                            return true;
                        }
                        return super.dispatchTouchEvent(ev);
                    }
                };
                close.setOnClickListener(v -> d.dismiss());
                d.setOnDismissListener(di -> {
                    try {
                        pv.setPlayer(null);
                        player.release();
                    } catch (Exception ignored) {
                        // egal
                    }
                    JSObject o = new JSObject();
                    o.put("index", idx[0]);
                    call.resolve(o);
                });
                d.setContentView(root);
                d.show();
                tune.run();
            } catch (Throwable e) {
                call.reject(String.valueOf(e.getMessage()));
            }
        });
    }
}
