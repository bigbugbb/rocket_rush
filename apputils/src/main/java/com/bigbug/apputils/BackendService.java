package com.bigbug.apputils;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import android.text.format.DateUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class BackendService extends IntentService {

    // Action names that describe backend tasks
    private static final String ACTION_INDEX   = "com.bigbug.apputils.action.index";
    private static final String ACTION_SHOW    = "com.bigbug.apputils.action.show";
    private static final String ACTION_CREATE  = "com.bigbug.apputils.action.create";
    private static final String ACTION_UPDATE  = "com.bigbug.apputils.action.update";
    private static final String ACTION_DESTROY = "com.bigbug.apputils.action.destroy";

    // Key for identify request entity
    private static final String REQUEST = "com.bigbug.apputils.extra.request";

    /**
     * Starts this service to perform action index. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionIndex(Context context, RequestEntity request) {
        startActionGeneral(context, ACTION_INDEX, request);
    }

    /**
     * Starts this service to perform action show. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionShow(Context context, RequestEntity request) {
        startActionGeneral(context, ACTION_SHOW, request);
    }

    /**
     * Starts this service to perform action create. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionCreate(Context context, RequestEntity request) {
        startActionGeneral(context, ACTION_CREATE, request);
    }

    /**
     * Starts this service to perform action update. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionUpdate(Context context, RequestEntity request) {
        startActionGeneral(context, ACTION_UPDATE, request);
    }

    /**
     * Starts this service to perform action destroy. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionDestroy(Context context, RequestEntity request) {
        startActionGeneral(context, ACTION_DESTROY, request);
    }

    private static void startActionGeneral(Context context, String action, RequestEntity request) {
        Intent intent = new Intent(context, BackendService.class);
        intent.setAction(action);
        intent.putExtra(REQUEST, request);
        context.startService(intent);
    }

    public BackendService() {
        super("BackendService");

        if (PackageManager.PERMISSION_DENIED == getPackageManager().checkPermission("android.permission.WAKE_LOCK", getPackageName())) {
            if (!Constants.IS_EXCEPTION_SUPPRESSION_ENABLED) {
                throw new RuntimeException("Application requires the WAKE_LOCK permission!");
            }
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = null;

        try {
            // Acquire a new wakelock
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());

            final String action = intent.getAction();
            final RequestEntity request = (RequestEntity) intent.getSerializableExtra(REQUEST);

            if (ACTION_INDEX.equals(action)) {
                handleActionIndex(request.mURL, request.mJSON, request.mCallbacks);
            } else if (ACTION_SHOW.equals(action)) {
                handleActionShow(request.mURL, request.mJSON, request.mCallbacks);
            } else if (ACTION_CREATE.equals(action)) {
                handleActionCreate(request.mURL, request.mJSON, request.mCallbacks);
            } else if (ACTION_UPDATE.equals(action)) {
                handleActionUpdate(request.mURL, request.mJSON, request.mCallbacks);
            } else if (ACTION_DESTROY.equals(action)) {
                handleActionDestroy(request.mURL, request.mJSON, request.mCallbacks);
            }
        } catch (Exception e) {
            if (Constants.IS_LOGGABLE) {
                Log.e(Constants.LOG_TAG, "DrawerHandler threw an uncaught exception", e); //$NON-NLS-1$
            }

            if (!Constants.IS_EXCEPTION_SUPPRESSION_ENABLED) {
                throw new RuntimeException(e);
            }
        } finally {
            // Release the wakelock
            if (null != wl && wl.isHeld()) {
                wl.release();
            }
        }
    }

    protected void handleActionIndex(final String url, final String json, final ResponseCallbacks callbacks) {
        uploadData("GET", url, json, false, callbacks);
    }

    protected void handleActionShow(final String url, final String json, final ResponseCallbacks callbacks) {
        uploadData("GET", url, json, false, callbacks);
    }

    protected void handleActionCreate(final String url, final String json, final ResponseCallbacks callbacks) {
        uploadData("POST", url, json, false, callbacks);
    }

    protected void handleActionUpdate(final String url, final String json, final ResponseCallbacks callbacks) {
        uploadData("PUT", url, json, false, callbacks);
    }

    protected void handleActionDestroy(final String url, final String json, final ResponseCallbacks callbacks) {
        uploadData("DELETE", url, json, false, callbacks);
    }

    protected boolean uploadData(final String method, final String url, final String json, final boolean isZipped, final ResponseCallbacks callbacks) {

        if (Constants.IS_PARAMETER_CHECKING_ENABLED) {
            if (null == url) {
                throw new IllegalArgumentException("url cannot be null"); //$NON-NLS-1$
            }

            if (null == json) {
                throw new IllegalArgumentException("body cannot be null"); //$NON-NLS-1$
            }
        }

        if (Constants.IS_LOGGABLE) {
            Log.v(Constants.LOG_TAG, String.format("Upload body before compression is: %s", json)); //$NON-NLS-1$
        }

        /*
         * GZIP the data to upload
         */
        byte[] data;
        {
            GZIPOutputStream gos = null;
            try {
                final byte[] originalBytes = json.getBytes("UTF-8"); //$NON-NLS-1$
                final ByteArrayOutputStream baos = new ByteArrayOutputStream(originalBytes.length);
                gos = new GZIPOutputStream(baos);
                gos.write(originalBytes);
                gos.finish();

                /*
                 * KitKat throws an exception when you call flush
                 * https://code.google.com/p/android/issues/detail?id=62589
                 */
                if (DatapointHelper.getApiLevel() < 19) {
                    gos.flush();
                }

                data = baos.toByteArray();
            } catch (final UnsupportedEncodingException e) {
                if (Constants.IS_LOGGABLE) {
                    Log.w(Constants.LOG_TAG, "UnsupportedEncodingException", e); //$NON-NLS-1$
                }
                return false;
            } catch (final IOException e) {
                if (Constants.IS_LOGGABLE) {
                    Log.w(Constants.LOG_TAG, "IOException", e); //$NON-NLS-1$
                }
                return false;
            } finally {
                if (null != gos) {
                    try {
                        gos.close();
                    } catch (final IOException e) {
                        if (Constants.IS_LOGGABLE) {
                            Log.w(Constants.LOG_TAG, "Caught exception", e); //$NON-NLS-1$
                        }

                        return false;
                    }
                }
            }
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();

            connection.setDoOutput(true); // sets POST method implicitly
            connection.setRequestMethod(method);
            connection.setConnectTimeout((int) DateUtils.MINUTE_IN_MILLIS);
            connection.setReadTimeout((int) DateUtils.MINUTE_IN_MILLIS);
            connection.setRequestProperty("Content-Type", "application/x-gzip"); //$NON-NLS-1$//$NON-NLS-2$
            connection.setRequestProperty("Content-Encoding", "gzip"); //$NON-NLS-1$//$NON-NLS-2$
            connection.setRequestProperty("x-upload-time",
                    Long.toString(Math.round((double) System.currentTimeMillis() / DateUtils.SECOND_IN_MILLIS))); //$NON-NLS-1$//$NON-NLS-2$
            connection.setFixedLengthStreamingMode(data.length);

            OutputStream stream = null;
            try {
                stream = connection.getOutputStream();
                stream.write(data);
            } finally {
                if (null != stream) {
                    stream.flush();
                    stream.close();
                }
            }

            final int responseCode = connection.getResponseCode();
            if (Constants.IS_LOGGABLE) {
                Log.v(Constants.LOG_TAG, String.format("Upload complete with status %d", Integer.valueOf(responseCode))); //$NON-NLS-1$
            }

            /*
             * 5xx status codes indicate a server error, so upload should be reattempted
             */
            if (responseCode >= 500 && responseCode <= 599) {
                throw new IOException(String.format("Server error %d", responseCode));
            }

            /*
             * Retrives the HTTP response as a string if available
             */
            final String response = retriveHttpResponse(connection.getInputStream());
            if (null != callbacks) {
                callbacks.onComplete(response);
            }
        } catch (final IOException e) {
            if (Constants.IS_LOGGABLE) {
                Log.w(Constants.LOG_TAG, "ClientProtocolException", e); //$NON-NLS-1$
            }

            if (null != callbacks) {
                callbacks.onError(e);
            }

            return false;
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
        }

        return true;
    }

    /**
     * Retrives the HTTP response body as a string and trigger onUploadResponded
     *
     * @param input InputStream from which the HTTP response body can be fetched. Cannot be null.
     */
	/* package */private String retriveHttpResponse(final InputStream input) throws IOException
    {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        final StringBuilder builder = new StringBuilder();

        String line = null;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        final String response = builder.toString();

        reader.close();

        return response;
    }
}
