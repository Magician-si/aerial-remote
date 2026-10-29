package app.orchard.tv.remote;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * The hands of the phone remote. Once the user enables this service (Settings, Accessibility),
 * it can move an on-screen pointer, tap and scroll anywhere via gestures, press Back/Home
 * globally, move D-pad focus through the accessibility tree, and type into the focused field.
 */
public class TvAccessibilityService extends AccessibilityService {

    private static volatile TvAccessibilityService instance;

    public static TvAccessibilityService get() {
        return instance;
    }

    public static boolean isRunning() {
        return instance != null;
    }

    private WindowManager wm;
    private PointerView pointer;
    private WindowManager.LayoutParams pointerParams;
    private boolean pointerShown;
    private float px, py;
    private int screenW, screenH;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hidePointer = this::removePointer;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getRealSize(size);
        screenW = size.x;
        screenH = size.y;
        px = screenW / 2f;
        py = screenH / 2f;
        RemoteServer.start(this);
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        removePointer();
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Events are only the binding's heartbeat; control comes from the phone.
    }

    @Override
    public void onInterrupt() {
    }

    // ---- pointer overlay ---------------------------------------------------------------------

    private void showPointer() {
        if (pointer == null) {
            pointer = new PointerView(this);
            float d = getResources().getDisplayMetrics().density;
            pointerParams = new WindowManager.LayoutParams(
                    (int) (30 * d), (int) (30 * d),
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            pointerParams.gravity = Gravity.TOP | Gravity.START;
        }
        pointerParams.x = (int) px;
        pointerParams.y = (int) py;
        try {
            if (!pointerShown) {
                wm.addView(pointer, pointerParams);
                pointerShown = true;
            } else {
                wm.updateViewLayout(pointer, pointerParams);
            }
        } catch (Exception ignored) {
        }
        handler.removeCallbacks(hidePointer);
        handler.postDelayed(hidePointer, 6000);
    }

    private void removePointer() {
        if (pointerShown && pointer != null) {
            try {
                wm.removeView(pointer);
            } catch (Exception ignored) {
            }
            pointerShown = false;
        }
    }


    // ---- remote actions (called from the server thread) ----------------------------------------

    public void movePointer(float dx, float dy) {
        handler.post(() -> {
            double mag = Math.hypot(dx, dy);
            double gain = 1.25 + Math.min(2.1, mag / 34.0);
            px = clamp(px + (float) (dx * gain), 0, screenW - 1);
            py = clamp(py + (float) (dy * gain), 0, screenH - 1);
            showPointer();
        });
    }

    public void tapPointer() {
        handler.post(() -> {
            showPointer();
            // Prefer a real node click when a small clickable widget sits under the pointer
            // (deterministic in native UI); fall back to an injected tap for web content.
            if (clickNodeAt((int) px, (int) py)) return;
            // TV tiles are often focus-driven, not clickable: focus the tile under the
            // pointer (visible highlight) and click it if it accepts clicks.
            if (focusAndClickNodeAt((int) px, (int) py)) return;
            // TV UIs often drive by focus: if the pointer lookup missed, activate the
            // currently focused item, which is what a remote OK press would do.
            if (clickFocusedNode()) return;
            // A zero-length, very short tap gets swallowed on some TVs: give the stroke a
            // couple of pixels of travel and a longer press.
            int x2 = (int) clamp(px + 2, 0, screenW - 1);
            int y2 = (int) clamp(py + 2, 0, screenH - 1);
            gesture((int) px, (int) py, x2, y2, 120);
        });
    }

    /** Focuses the deepest focusable node under the pointer, then clicks it and
     *  each ancestor until one accepts the click. Returns true if focusing or
     *  any click worked. */
    private boolean focusAndClickNodeAt(int x, int y) {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return false;
            AccessibilityNodeInfo node = findFocusableAt(root, x, y);
            if (node == null) return false;
            boolean focused = node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            boolean clicked = clickNodeAndAncestors(node);
            return focused || clicked;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Tries ACTION_CLICK on the node and each ancestor until one accepts it:
     *  TV tiles sometimes expose the click on a parent of the focused child. */
    private boolean clickNodeAndAncestors(AccessibilityNodeInfo node) {
        try {
            for (AccessibilityNodeInfo n = node; n != null; n = n.getParent()) {
                if (n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private AccessibilityNodeInfo findFocusableAt(AccessibilityNodeInfo node, int x, int y) {
        Rect r = new Rect();
        node.getBoundsInScreen(r);
        if (!r.contains(x, y)) return null;
        AccessibilityNodeInfo found = null;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            AccessibilityNodeInfo hit = findFocusableAt(child, x, y);
            if (hit != null) found = hit;
        }
        if (found != null) return found;
        return node.isFocusable() ? node : null;
    }

    /** Clicks the accessibility-focused node, if it is clickable. */
    private boolean clickFocusedNode() {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return false;
            AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
            if (focused == null) return false;
            return focused.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Deepest clickable node containing the point, or null. Refuses full-screen containers
     *  and the WebView root itself, where a positional gesture is the honest click. */
    private boolean clickNodeAt(int x, int y) {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return false;
            AccessibilityNodeInfo hit = findClickableAt(root, x, y);
            if (hit == null) return false;
            Rect r = new Rect();
            hit.getBoundsInScreen(r);
            long area = (long) r.width() * r.height();
            String cls = hit.getClassName() != null ? hit.getClassName().toString() : "";
            if (cls.contains("WebView") || area > (long) screenW * screenH * 19 / 20) return false;
            return hit.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } catch (Exception ignored) {
            return false;
        }
    }

    private AccessibilityNodeInfo findClickableAt(AccessibilityNodeInfo node, int x, int y) {
        Rect r = new Rect();
        node.getBoundsInScreen(r);
        if (!r.contains(x, y)) return null;
        AccessibilityNodeInfo found = null;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            AccessibilityNodeInfo hit = findClickableAt(child, x, y);
            if (hit != null) found = hit;
        }
        if (found != null) return found;
        return node.isClickable() ? node : null;
    }

    public void scrollPointer(float dx, float dy) {
        handler.post(() -> {
            showPointer();
            boolean vertical = Math.abs(dy) >= Math.abs(dx);
            float total = vertical ? dy : dx;
            // Fingers moving up/left (negative travel) mean "show me more": scroll forward.
            boolean forward = total < 0;
            int steps = (int) Math.min(3, Math.max(1, Math.round(Math.abs(total) / 140f)));
            if (nodeScroll(forward, steps)) return;
            if (scrollFocusedNode(forward, steps)) return;
            // TV lists scroll by moving focus: focus the next item in the swipe
            // direction and the list follows it, even when nothing is scrollable.
            if (focusMove(forward, vertical)) return;
            // Fallback for screens with no scrollable accessibility node: one accumulated swipe.
            int x2 = (int) clamp(px + dx * 2, 0, screenW - 1);
            int y2 = (int) clamp(py + dy * 2, 0, screenH - 1);
            gesture((int) px, (int) py, x2, y2, 220);
        });
    }

    /** Scrolls the scrollable node under the pointer (or the first one on screen).
     *  Injected gestures are unreliable on some TVs; node scroll actions are not. */
    private boolean nodeScroll(boolean forward, int steps) {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return false;
            AccessibilityNodeInfo target = findScrollableAt(root, (int) px, (int) py);
            if (target == null) target = findScrollable(root);
            if (target == null) return false;
            int action = forward ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                    : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;
            boolean any = false;
            for (int i = 0; i < steps; i++) any |= target.performAction(action);
            return any;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Scrolls the list containing the focused item, when the pointer is not over one. */
    private boolean scrollFocusedNode(boolean forward, int steps) {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return false;
            AccessibilityNodeInfo node = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
            while (node != null && !node.isScrollable()) node = node.getParent();
            if (node == null) return false;
            int action = forward ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                    : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;
            boolean any = false;
            for (int i = 0; i < steps; i++) any |= node.performAction(action);
            return any;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Moves TV focus to the nearest focusable item in the swipe direction
     *  (swipe up reveals below, so focus goes down, and vice versa). Finds the
     *  currently focused item first — accessibility focus, else any node
     *  reporting isFocused — then picks the nearest focusable item beyond it
     *  by on-screen position. On focus-driven TV UIs the list scrolls to
     *  follow the newly focused item. */
    private boolean focusMove(boolean forward, boolean vertical) {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return false;
            AccessibilityNodeInfo current = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
            if (current == null) current = findFocusedNode(root);
            if (current == null) {
                AccessibilityNodeInfo atPointer = findFocusableAt(root, (int) px, (int) py);
                if (atPointer == null) return false;
                return atPointer.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            }
            Rect cr = new Rect();
            current.getBoundsInScreen(cr);
            int cx = cr.centerX(), cy = cr.centerY();
            java.util.List<AccessibilityNodeInfo> all = new java.util.ArrayList<>();
            collectFocusable(root, all);
            AccessibilityNodeInfo best = null;
            int bestPrimary = Integer.MAX_VALUE, bestSecondary = Integer.MAX_VALUE;
            for (AccessibilityNodeInfo n : all) {
                if (n == current) continue;
                Rect r = new Rect();
                n.getBoundsInScreen(r);
                int nx = r.centerX(), ny = r.centerY();
                int primary, secondary;
                if (vertical) {
                    if (forward) {
                        if (ny <= cy) continue;
                        primary = ny - cy;
                    } else {
                        if (ny >= cy) continue;
                        primary = cy - ny;
                    }
                    secondary = Math.abs(nx - cx);
                } else {
                    if (forward) {
                        if (nx <= cx) continue;
                        primary = nx - cx;
                    } else {
                        if (nx >= cx) continue;
                        primary = cx - nx;
                    }
                    secondary = Math.abs(ny - cy);
                }
                if (primary < bestPrimary
                        || (primary == bestPrimary && secondary < bestSecondary)) {
                    best = n;
                    bestPrimary = primary;
                    bestSecondary = secondary;
                }
            }
            if (best == null) return false;
            return best.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        } catch (Exception ignored) {
            return false;
        }
    }

    private AccessibilityNodeInfo findFocusedNode(AccessibilityNodeInfo node) {
        if (node.isFocused()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            AccessibilityNodeInfo hit = findFocusedNode(child);
            if (hit != null) return hit;
        }
        return null;
    }

    private void collectFocusable(AccessibilityNodeInfo node,
            java.util.List<AccessibilityNodeInfo> out) {
        if (out.size() > 5000) return;
        if (node.isFocusable()) out.add(node);
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) collectFocusable(child, out);
        }
    }

    private AccessibilityNodeInfo findScrollableAt(AccessibilityNodeInfo node, int x, int y) {
        Rect r = new Rect();
        node.getBoundsInScreen(r);
        if (!r.contains(x, y)) return null;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            AccessibilityNodeInfo hit = findScrollableAt(child, x, y);
            if (hit != null) return hit;
        }
        return node.isScrollable() ? node : null;
    }

    private AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo node) {
        if (node.isScrollable()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            AccessibilityNodeInfo hit = findScrollable(child);
            if (hit != null) return hit;
        }
        return null;
    }

    /** Power key: puts the TV to sleep. (Waking it again needs the physical remote —
     *  Android gives apps no way to power a TV back on.) */
    public void sleep() {
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        }
    }

    private void gesture(int x1, int y1, int x2, int y2, int durationMs) {
        try {
            Path path = new Path();
            path.moveTo(x1, y1);
            path.lineTo(x2, y2);
            GestureDescription gesture = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path, 0, durationMs))
                    .build();
            dispatchGesture(gesture, null, null);
        } catch (Exception ignored) {
        }
    }

    public void pressBack() {
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    public void pressHome() {
        performGlobalAction(GLOBAL_ACTION_HOME);
    }

    /** D-pad keys jump one item at a time: focus moves to the nearest focusable
     *  item in the pressed direction and the pointer follows it, so one press
     *  lands on the next item instead of stepping a fixed distance. Falls back
     *  to a 150dp pointer nudge when nothing focusable lies in that direction. */
    public void pressDpad(int direction) {
        handler.post(() -> {
            if (!dpadMove(direction)) {
                float step = 150 * getResources().getDisplayMetrics().density;
                if (direction == View.FOCUS_LEFT) px = clamp(px - step, 0, screenW - 1);
                else if (direction == View.FOCUS_RIGHT) px = clamp(px + step, 0, screenW - 1);
                else if (direction == View.FOCUS_UP) py = clamp(py - step, 0, screenH - 1);
                else if (direction == View.FOCUS_DOWN) py = clamp(py + step, 0, screenH - 1);
                showPointer();
            }
        });
    }

    /** Moves focus to the nearest focusable item in the given direction and puts
     *  the pointer on it. One press steps through any intermediate focus stops
     *  inside the starting item (icons, labels that show no highlight of their
     *  own) until focus lands on a node whose center lies outside the starting
     *  item, so one press always means one visible item. For LEFT/RIGHT, only
     *  candidates whose vertical range overlaps the starting item's vertical
     *  band (same row) are considered, so rows below/above (volume, brightness,
     *  menu rows) can never hijack a horizontal move. Returns false when no
     *  focusable item lies that way. */
    private boolean dpadMove(int direction) {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return false;
            AccessibilityNodeInfo current = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
            if (current == null) current = findFocusedNode(root);
            if (current == null) {
                AccessibilityNodeInfo atPointer = findFocusableAt(root, (int) px, (int) py);
                if (atPointer == null) return false;
                boolean ok = atPointer.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                if (ok) {
                    Rect r = new Rect();
                    atPointer.getBoundsInScreen(r);
                    px = r.exactCenterX();
                    py = r.exactCenterY();
                    showPointer();
                }
                return ok;
            }
            // Starting item region: the focused node plus its focusable parent
            // (the tile container), so inner stops inside the tile are skipped.
            Rect region = new Rect();
            current.getBoundsInScreen(region);
            AccessibilityNodeInfo parent = current.getParent();
            if (parent != null && parent.isFocusable()) {
                Rect pr = new Rect();
                parent.getBoundsInScreen(pr);
                region.union(pr);
            }
            boolean moved = false;
            for (int step = 0; step < 10; step++) {
                Rect cr = new Rect();
                current.getBoundsInScreen(cr);
                int cx = cr.centerX(), cy = cr.centerY();
                java.util.List<AccessibilityNodeInfo> all = new java.util.ArrayList<>();
                collectFocusable(root, all);
                AccessibilityNodeInfo best = null;
                int bestPrimary = Integer.MAX_VALUE, bestSecondary = Integer.MAX_VALUE;
                for (AccessibilityNodeInfo n : all) {
                    if (n == current) continue;
                    Rect r = new Rect();
                    n.getBoundsInScreen(r);
                    int nx = r.centerX(), ny = r.centerY();
                    // Horizontal beam: for LEFT/RIGHT, only candidates in the
                    // same row count (vertical range overlaps the starting
                    // item's band). Without this, a full-width row below whose
                    // center is only slightly further sideways wins on primary
                    // distance and hijacks the move, so one press lands on the
                    // wrong row instead of the next tile.
                    if ((direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT)
                            && (r.bottom <= region.top || r.top >= region.bottom)) {
                        continue;
                    }
                    int primary, secondary;
                    if (direction == View.FOCUS_LEFT) {
                        if (nx >= cx) continue;
                        primary = cx - nx;
                        secondary = Math.abs(ny - cy);
                    } else if (direction == View.FOCUS_RIGHT) {
                        if (nx <= cx) continue;
                        primary = nx - cx;
                        secondary = Math.abs(ny - cy);
                    } else if (direction == View.FOCUS_UP) {
                        if (ny >= cy) continue;
                        primary = cy - ny;
                        secondary = Math.abs(nx - cx);
                    } else if (direction == View.FOCUS_DOWN) {
                        if (ny <= cy) continue;
                        primary = ny - cy;
                        secondary = Math.abs(nx - cx);
                    } else {
                        return false;
                    }
                    if (primary < bestPrimary
                            || (primary == bestPrimary && secondary < bestSecondary)) {
                        best = n;
                        bestPrimary = primary;
                        bestSecondary = secondary;
                    }
                }
                if (best == null) break;
                boolean ok = best.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                if (!ok) break;
                moved = true;
                current = best;
                Rect br = new Rect();
                best.getBoundsInScreen(br);
                boolean outside;
                if (direction == View.FOCUS_LEFT) outside = br.centerX() < region.left;
                else if (direction == View.FOCUS_RIGHT) outside = br.centerX() > region.right;
                else if (direction == View.FOCUS_UP) outside = br.centerY() < region.top;
                else outside = br.centerY() > region.bottom;
                if (outside) break;
            }
            if (!moved) return false;
            Rect r = new Rect();
            current.getBoundsInScreen(r);
            px = r.exactCenterX();
            py = r.exactCenterY();
            showPointer();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public void pressOk() {
        tapPointer();
    }

    /** Replaces the text of the currently focused input field with what the phone typed. */
    public void setText(String text) {
        handler.post(() -> {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return;
            try {
                AccessibilityNodeInfo input = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                if (input != null && input.isEditable()) {
                    Bundle args = new Bundle();
                    args.putCharSequence(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                    input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
                }
            } catch (Exception ignored) {
            }
        });
    }

    /** Current text of the focused input, so the phone can backspace accurately. */
    public String focusedText() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return null;
        try {
            AccessibilityNodeInfo input = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (input != null && input.isEditable() && input.getText() != null) {
                return input.getText().toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }
}
