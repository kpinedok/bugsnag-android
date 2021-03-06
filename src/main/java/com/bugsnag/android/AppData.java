package com.bugsnag.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

/**
 * Information about the running Android app which doesn't change over time,
 * including app name, version and release stage.
 * <p/>
 * App information in this class is cached during construction for faster
 * subsequent lookups and to reduce GC overhead.
 */
class AppData implements JsonStream.Streamable {
    private final Configuration config;

    private final String packageName;
    private final String appName;
    private final Integer versionCode;
    private final String versionName;
    private final String guessedReleaseStage;

    AppData(@NonNull Context appContext, @NonNull Configuration config) {
        this.config = config;

        packageName = getPackageName(appContext);
        appName = getAppName(appContext);
        versionCode = getVersionCode(appContext);
        versionName = getVersionName(appContext);
        guessedReleaseStage = guessReleaseStage(appContext);
    }

    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();

        writer.name("id").value(packageName);
        writer.name("name").value(appName);
        writer.name("packageName").value(packageName);
        writer.name("versionName").value(versionName);
        writer.name("versionCode").value(versionCode);

        if (config.buildUUID != null)
            writer.name("buildUUID").value(config.buildUUID);

        // Prefer user-configured appVersion + releaseStage
        if (getAppVersion() != null)
            writer.name("version").value(getAppVersion());
        writer.name("releaseStage").value(getReleaseStage());

        writer.endObject();
    }

    @NonNull
    public String getReleaseStage() {
        if (config.releaseStage != null) {
            return config.releaseStage;
        } else {
            return guessedReleaseStage;
        }
    }

    @Nullable
    public String getAppVersion() {
        if (config.appVersion != null) {
            return config.appVersion;
        } else {
            return versionName;
        }
    }

    /**
     * The package name of the running Android app, eg: com.example.myapp
     */
    @NonNull
    private static String getPackageName(Context appContext) {
        return appContext.getPackageName();
    }

    /**
     * The name of the running Android app, from android:label in
     * AndroidManifest.xml
     */
    @Nullable
    private static String getAppName(Context appContext) {
        try {
            PackageManager packageManager = appContext.getPackageManager();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(appContext.getPackageName(), 0);

            return (String) packageManager.getApplicationLabel(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.warn("Could not get app name");
        }
        return null;
    }

    /**
     * The version code of the running Android app, from android:versionCode
     * in AndroidManifest.xml
     */
    @Nullable
    private static Integer getVersionCode(Context appContext) {
        try {
            return appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.warn("Could not get versionCode");
        }
        return null;
    }

    /**
     * The version code of the running Android app, from android:versionName
     * in AndroidManifest.xml
     */
    @Nullable
    private static String getVersionName(Context appContext) {
        try {
            return appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.warn("Could not get versionName");
        }
        return null;
    }

    /**
     * Guess the release stage of the running Android app by checking the
     * android:debuggable flag from AndroidManifest.xml
     */
    @NonNull
    private static String guessReleaseStage(Context appContext) {
        try {
            int appFlags = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(), 0).flags;
            if ((appFlags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                return "development";
            }
        } catch (PackageManager.NameNotFoundException e) {
            Logger.warn("Could not get releaseStage");
        }
        return "production";
    }
}
