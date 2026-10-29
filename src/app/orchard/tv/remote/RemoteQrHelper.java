package app.orchard.tv.remote;

/**
 * Returns the remote pairing URL (the same string the remote panel QR encodes),
 * or null when no local IP is available. The wallpaper screen uses this so its
 * QR shows the remote URL instead of the 8790 upload URL; on null the caller
 * keeps the original address so the QR never breaks.
 */
public final class RemoteQrHelper {
    private RemoteQrHelper() {
    }

    public static String remoteUrl() {
        try {
            android.content.Context ctx = RemoteApp.appContext;
            if (ctx == null) return null;
            return RemoteServer.pairingUrl(ctx);
        } catch (Exception ignored) {
            return null;
        }
    }
}
