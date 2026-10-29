package app.orchard.tv.remote;

import android.content.Context;
import android.content.Intent;

public final class RemoteTile {
    private RemoteTile() {
    }

    public static void onClick() {
        Context ctx = RemoteApp.appContext;
        if (ctx != null) {
            Intent i = new Intent(ctx, RemoteInfoActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        }
    }
}
