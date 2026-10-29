package app.orchard.tv.remote;

import android.app.Application;
import android.content.Context;

public class RemoteApp extends Application {
    public static volatile Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }
}
