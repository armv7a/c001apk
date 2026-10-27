package cn.shuzilm.core;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.hardware.SensorEvent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.text.TextUtils;
import android.util.Log;

import com.example.c001apk.compose.logic.smid.SmidConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DUHelper extends PhoneStateListener {
    public static final int MAIN_DU_SYNCHRONOUS = 0;
    public static final int MAIN_DU_ASYNCHRONOUS = 1;

    private static final String SO_LIB = "du";
    private static final String LOCK_FILE = "du.lock";
    private static final String PREF_SUFFIX = "_dna";
    private static final String PREF_DEVICE_ID = "device_id";
    private static final String CONFIG_SWITCH = "l_o";
    private static final String CONFIG_API_KEY = "apiKey";
    private static final String CONFIG_URL = "url";
    private static final String CONFIG_STORE = "store";
    private static final String CONFIG_CUSTOM = "custom";
    private static final String VALID = "valid";
    private static final String TAG = "[shuzilm]";
    private static final String DEBUG_TAG = "SMID_DEBUG";

    private static final DUHelper INSTANCE = new DUHelper();
    private static final Lock QUERY_LOCK = new ReentrantLock();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final JSONObject CUSTOM_DATA = new JSONObject();
    private static final JSONObject CONFIG_DATA = new JSONObject();

    public static Context mContext;
    public static int mPopu;
    public static int mPort;

    private static JSONObject assetConfig;
    private static String apiKey;
    private static String cachedQueryId;
    private static volatile boolean aiStarted;
    private static volatile boolean libraryReady;

    private int queryType = 2;
    private boolean loadFinished;
    private long lastSupplementalRequestTs;

    private DUHelper() {
    }

    public static void init(Context context, String key, boolean ignored) {
        Context appContext = appContext(context);
        mContext = appContext;
        apiKey = key;
        FileOutputStream outputStream = null;
        FileLock fileLock = null;
        try {
            outputStream = appContext.openFileOutput(LOCK_FILE, Context.MODE_PRIVATE);
            FileChannel channel = outputStream.getChannel();
            fileLock = channel.tryLock();
            if (fileLock == null || !fileLock.isValid()) {
                return;
            }
            startAi(1);
            initPort(appContext);
            EXECUTOR.execute(new LibraryLoader());
        } catch (Exception | UnsatisfiedLinkError throwable) {
            throwable.printStackTrace();
        } finally {
            try {
                if (fileLock != null) {
                    fileLock.release();
                }
            } catch (IOException ignored2) {
            }
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException ignored2) {
            }
        }
    }

    public static String getQueryID(Context context, String storeValue, String customValue, boolean legacyMode) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        getQueryID(context, storeValue, customValue, legacyMode, MAIN_DU_ASYNCHRONOUS, new BlockingListener(result, latch), 2);
        try {
            latch.await(30000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        return result.get();
    }

    public static Map<String, String> getQueryID(Context context, String storeValue, String customValue,
                                                 boolean legacyMode, int mode, Listener listener, int type) {
        return getQueryID(context, storeValue, customValue, legacyMode, mode, listener, type, null);
    }

    public static Map<String, String> getQueryID(Context context, String storeValue, String customValue,
                                                 boolean legacyMode, int mode, Listener listener, int type, String url) {
        String disabled = "0";
        String enabled = "1";
        if (legacyMode) {
            startAi(2);
            setConfig(CONFIG_SWITCH, disabled);
        } else {
            setConfig(CONFIG_SWITCH, enabled);
        }

        try {
            HashMap<String, String> result = new HashMap<>();
            Context appContext = appContext(context);
            INSTANCE.queryType = type;
            if (mode == MAIN_DU_ASYNCHRONOUS) {
                INSTANCE.requestQueryAsync(appContext, storeValue, customValue, listener, type, url);
                return null;
            }

            if (!QUERY_LOCK.tryLock()) {
                String fallback = INSTANCE.readCachedQueryId(appContext);
                if (fallback == null) {
                    INSTANCE.requestQueryAsync(appContext, storeValue, customValue, listener, type, url);
                }
                result.put(PREF_DEVICE_ID, fallback);
                result.put(VALID, disabled);
                return result;
            }

            try {
                String queryId = INSTANCE.queryIdInternal(appContext, storeValue, customValue, type, url);
                if (queryId != null) {
                    cachedQueryId = queryId;
                    disabled = enabled;
                }
                if (queryId == null) {
                    queryId = cachedQueryId != null ? cachedQueryId : INSTANCE.readCachedQueryId(appContext);
                }
                result.put(PREF_DEVICE_ID, queryId);
                result.put(VALID, disabled);
                return result;
            } finally {
                QUERY_LOCK.unlock();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static int setConfig(String key, String value) {
        if (key == null || value == null) {
            return -1;
        }
        try {
            INSTANCE.put(CONFIG_DATA, key, value);
        } catch (Throwable ignored) {
        }
        return 0;
    }

    public static int setData(String key, String value) {
        try {
            INSTANCE.put(CUSTOM_DATA, key, value);
        } catch (Throwable ignored) {
        }
        return 0;
    }

    public static void report(Context context, String eventCode, String value) {
        try {
            reportRun(appContext(context), eventCode, value);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static String debugCachedDeviceId(Context context) {
        return INSTANCE.readCachedQueryId(appContext(context));
    }

    public static boolean isLibraryReady() {
        return libraryReady;
    }

    public static String debugConfigJson(Context context, String storeValue) {
        try {
            JSONObject jsonObject = new JSONObject(CONFIG_DATA.toString());
            if (!TextUtils.isEmpty(apiKey)) {
                jsonObject.put(CONFIG_API_KEY, apiKey);
            }
            INSTANCE.fillBaseConfig(appContext(context), jsonObject, storeValue);
            return jsonObject.toString();
        } catch (Throwable throwable) {
            return "config_error=" + throwable;
        }
    }

    public static String debugCustomJson(String customValue) {
        try {
            JSONObject jsonObject = new JSONObject(CUSTOM_DATA.toString());
            INSTANCE.putCustom(jsonObject, customValue);
            return jsonObject.toString();
        } catch (Throwable throwable) {
            return "custom_error=" + throwable;
        }
    }

    public static String debugQueryRaw(Context context, String storeValue, String customValue, int type) {
        try {
            Context appContext = appContext(context);
            setConfig(CONFIG_API_KEY, apiKey);
            INSTANCE.fillBaseConfig(appContext, CONFIG_DATA, storeValue);
            INSTANCE.putCustom(CUSTOM_DATA, customValue);
            JSONObject queryConfig = new JSONObject(CONFIG_DATA.toString());
            normalizeEndpoint(queryConfig);
            String configJson = queryConfig.toString();
            String customJson = CUSTOM_DATA.toString();
            String raw = query(appContext, configJson, customJson, type);



            return raw;
        } catch (Throwable throwable) {
            Log.e(DEBUG_TAG, "debugQueryRaw failed", throwable);
            return null;
        }
    }

    public static int unResListener() {
        return 0;
    }

    @Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
        super.onSignalStrengthsChanged(signalStrength);
        try {
            onSSChanged(mContext, signalStrength);
        } catch (UnsatisfiedLinkError throwable) {
            throwable.printStackTrace();
        }
    }

    private void requestQueryAsync(final Context context, final String storeValue, final String customValue,
                                   final Listener listener, final int type, final String url) {
        try {
            EXECUTOR.execute(new AsyncQueryTask(this, context, storeValue, customValue, type, listener, url));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private String queryIdInternal(Context context, String storeValue, String customValue, int type, String url) {
        try {
            if (!isNetworkAvailable(context)) {
                Log.e(TAG, "network is unavailable.");
                return null;
            }
            setConfig(CONFIG_API_KEY, apiKey);
            fillBaseConfig(context, CONFIG_DATA, storeValue);
            putCustom(CUSTOM_DATA, customValue);
            // Snapshot the endpoint per queued query; a late primary query must not adopt
            // the fallback URL, and subsequent attempts must retain the SDK's default.
            JSONObject queryConfig = new JSONObject(CONFIG_DATA.toString());
            if (url != null) queryConfig.put(CONFIG_URL, url);
            normalizeEndpoint(queryConfig);
            String configJson = queryConfig.toString();
            String customJson = CUSTOM_DATA.toString();
            String queryId = query(context, configJson, customJson, type);



            if (queryId != null && !queryId.isEmpty()) {
                String locationValue = CONFIG_DATA.optString("location");
                if (enabled(locationValue)) {
                    mPopu = 10001;
                }
                cacheQueryId(context, queryId);
            }
            return queryId;
        } catch (Exception | UnsatisfiedLinkError throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    private static void normalizeEndpoint(JSONObject config) throws JSONException {
        String endpoint = config.optString(CONFIG_URL, null);
        if (endpoint != null) config.put(CONFIG_URL, DigitalUnionEndpoint.toNativeAuthority(endpoint));
    }

    private void fillBaseConfig(Context context, JSONObject jsonObject, String storeValue) {
        try {
            if (jsonObject.isNull(CONFIG_STORE)) {
                if (TextUtils.isEmpty(storeValue)) {
                    Object store = getAssetConfigValue(context, CONFIG_STORE);
                    storeValue = store instanceof String ? (String) store : null;
                }
                if (storeValue != null) {
                    jsonObject.put(CONFIG_STORE, storeValue);
                }
            }
            if (jsonObject.isNull(CONFIG_URL)) {
                Object urlValue = getAssetConfigValue(context, CONFIG_URL);
                if (urlValue instanceof String) {
                    jsonObject.put(CONFIG_URL, urlValue);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void putCustom(JSONObject jsonObject, String customValue) {
        put(jsonObject, CONFIG_CUSTOM, customValue);
    }

    private void put(JSONObject jsonObject, String key, String value) {
        try {
            jsonObject.put(key, value);
        } catch (JSONException exception) {
            throw new RuntimeException(exception);
        }
    }

    private Object getAssetConfigValue(Context context, String key) {
        try {
            if (assetConfig == null) {
                assetConfig = readAssetConfig(context);
            }
            return assetConfig != null ? assetConfig.opt(key) : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private JSONObject readAssetConfig(Context context) throws JSONException {
        String content = readAssetText(context, "cn.shuzilm.config.json");
        return content != null ? new JSONObject(content) : null;
    }

    private String readAssetText(Context context, String name) {
        try {
            InputStream inputStream = context.getAssets().open(name);
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            inputStream.close();
            return builder.toString();
        } catch (IOException ignored) {
            return null;
        }
    }

    private String readCachedQueryId(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(context.getPackageName() + PREF_SUFFIX, Context.MODE_PRIVATE);
        return preferences != null ? preferences.getString(PREF_DEVICE_ID, null) : null;
    }

    private void cacheQueryId(Context context, String queryId) {
        context.getSharedPreferences(context.getPackageName() + PREF_SUFFIX, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_DEVICE_ID, queryId)
                .apply();
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            try {
                NetworkInfo info = connectivityManager.getActiveNetworkInfo();
                if (info != null && info.isConnected() && info.getState() == NetworkInfo.State.CONNECTED) {
                    return true;
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        return false;
    }

    private static void startAi(int mode) {
        if (aiStarted) {
            return;
        }
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                }
            }).start();
            if (mode == 2) {
                aiStarted = true;
            }
        } catch (Throwable ignored) {
        }
    }

    private static int initPort(Context context) {
        String packageName = context.getPackageName();
        try {
            int installPart = (int) (context.getPackageManager().getPackageInfo(packageName, 0).firstInstallTime % 10000L);
            String seed = packageName + android.os.Build.MODEL;
            int hash = Math.abs(seed.hashCode()) % 5000;
            mPort = 12000 + hash + installPart;
            return 0;
        } catch (Exception exception) {
            mPort = 17835;
            exception.printStackTrace();
            return -1;
        }
    }

    private static Context appContext(Context context) {
        try {
            Context app = context.getApplicationContext();
            Context base = app != null ? app : context;
            return new PackageLockedContext(base, SmidConfig.DIGITAL_UNION_PACKAGE_NAME);
        } catch (Exception exception) {
            exception.printStackTrace();
            return new PackageLockedContext(context, SmidConfig.DIGITAL_UNION_PACKAGE_NAME);
        }
    }

    public static final class PackageLockedContext extends ContextWrapper {
        private final String packageName;

        public PackageLockedContext(Context base, String packageName) {
            super(base);
            this.packageName = packageName;
        }

        @Override
        public Context getApplicationContext() {
            Context base = super.getApplicationContext();
            return base == null ? this : new PackageLockedContext(base, packageName);
        }

        @Override
        public String getPackageName() {
            return packageName;
        }

        @Override
        public android.content.pm.PackageManager getPackageManager() {
            return com.example.c001apk.compose.logic.smid.OfficialPackageManager.create(
                    getBaseContext().getPackageManager(), getBaseContext().getPackageName());
        }

        @Override
        public ApplicationInfo getApplicationInfo() {
            ApplicationInfo info = new ApplicationInfo(super.getApplicationInfo());
            info.packageName = packageName;
            return info;
        }
    }

    private static boolean enabled(String value) {
        return "1".equals(value);
    }

    private static final class LibraryLoader implements Runnable {
        @Override
        public void run() {
            try {
                System.loadLibrary(SO_LIB);
                if (!INSTANCE.loadFinished) {
                    if (Looper.myLooper() == null) {
                        Looper.prepare();
                    }
                    Looper.myLooper();
                }
                INSTANCE.loadFinished = true;
                libraryReady = true;
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    private static final class BlockingListener implements Listener {
        private final AtomicReference<String> reference;
        private final CountDownLatch latch;

        private BlockingListener(AtomicReference<String> reference, CountDownLatch latch) {
            this.reference = reference;
            this.latch = latch;
        }

        @Override
        public void handler(String value) {
            reference.set(value);
            latch.countDown();
        }
    }

    private static final class AsyncQueryTask implements Runnable {
        private final DUHelper helper;
        private final Context context;
        private final String storeValue;
        private final String customValue;
        private final int type;
        private final Listener listener;
        private final String url;

        private AsyncQueryTask(DUHelper helper, Context context, String storeValue, String customValue, int type, Listener listener, String url) {
            this.helper = helper;
            this.context = context;
            this.storeValue = storeValue;
            this.customValue = customValue;
            this.type = type;
            this.listener = listener;
            this.url = url;
        }

        @Override
        public void run() {
            try {
                String queryId = helper.queryIdInternal(context, storeValue, customValue, type, url);
                if (listener != null) {
                    listener.handler(queryId);
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private static native void aXZlZWNl(Context context, Intent intent);
    private static native String c6M2YmYQ(Context context, int type);
    private static native String c6M3YmYQ(Context context, int type, String value);
    private static native void dGZvcmRQ(Context context, String eventCode, String value);
    private static native String ntERIJNQ(Context context, int type, String value);
    private static native String onEvent(Context context, String eventCode, String mainEvent, String payload);
    private static native String onIEvent(Context context, String eventCode, String mainEvent, String payload);
    private static native void onSSChanged(Context context, SignalStrength signalStrength);
    private static native void onSensorChanged(Context context, SensorEvent sensorEvent);
    private static native void oxlbmV0d(Context context, Object object, int type);
    private static native String query(Context context, String config, String custom, int type);
    private static native String reportRun(Context context, String eventCode, String value);
    private static native String run(Context context, String data, String extra);
    private static native String ttERIJNQ(Context context, String eventCode, String value);
    private static native String zZVTFJRA(Context context, String value);
}
