package cn.shuzilm.core;

import android.content.Context;
import android.os.Looper;

public final class Main {
    private static String storeValue;

    private Main() {
    }

    public static String getQueryID(Context context) {
        return getQueryID(context, null, null, true);
    }

    public static String getQueryID(Context context, String store, String custom) {
        return getQueryID(context, store, custom, true);
    }

    public static String getQueryID(Context context, String store, String custom, boolean legacyMode) {
        try {
            if (Looper.myLooper() == null) {
                Looper.prepare();
            }
            storeValue = store;
            return DUHelper.getQueryID(context, store, custom, legacyMode);
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static void getQueryID(Context context, String store, String custom, int mode, Listener listener) {
        storeValue = store;
        DUHelper.getQueryID(context, store, custom, true, mode, listener, 2);
    }

    public static void getQueryID(Context context, String store, String custom, boolean legacyMode, Listener listener) {
        storeValue = store;
        DUHelper.getQueryID(context, store, custom, legacyMode, DUHelper.MAIN_DU_ASYNCHRONOUS, listener, 2);
    }

    public static void init(Context context, String apiKey) {
        init(context, apiKey, true);
    }

    public static void init(Context context, String apiKey, boolean preload) {
        DUHelper.init(context, apiKey, preload);
    }

    public static int setConfig(String key, String value) {
        return DUHelper.setConfig(key, value);
    }

    public static int setData(String key, String value) {
        return DUHelper.setData(key, value);
    }

    public static void report(Context context, String eventCode, String value) {
        DUHelper.report(context, eventCode, value);
    }

    public static String debugQueryRaw(Context context, String store, String custom, int type) {
        storeValue = store;
        return DUHelper.debugQueryRaw(context, store, custom, type);
    }

    public static String debugConfigJson(Context context, String store) {
        storeValue = store;
        return DUHelper.debugConfigJson(context, store);
    }

    public static String debugCustomJson(String custom) {
        return DUHelper.debugCustomJson(custom);
    }

    public static boolean isLibraryReady() {
        return DUHelper.isLibraryReady();
    }

    public static String debugCachedDeviceId(Context context) {
        return DUHelper.debugCachedDeviceId(context);
    }

    public static int unResListener() {
        return DUHelper.unResListener();
    }

    public static String getVersion() {
        return "v8.4.2";
    }
}
