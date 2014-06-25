package com.bigbug.rocketrush.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.util.Log;

import com.bigbug.rocketrush.Constants;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Implements the storage mechanism for the Localytics library. The interface and implementation are similar to a ContentProvider
 * but modified to be better suited to a library. The interface is table-oriented, rather than Uri-oriented.
 * <p>
 * This is not a public API.
 */
public class RocketRushProvider
{
    /**
     * Name of the RocketRush database, stored in the host application's {@link Context#getDatabasePath(String)}.
     * <p>
     * This is not a public API.
     */
    /*
     * This field is made package-accessible for unit testing. While the exact file name is arbitrary, this name was chosen to
     * avoid collisions with app developers because it is sufficiently long and uses the Localytics package namespace.
     */
    /* package */static final String DATABASE_FILE = "com.bigbug.rocketrush.%d.sqlite"; //$NON-NLS-1$

    /**
     * Version of the database.
     * <p>
     * Version history:
     * <ol>
     * <li>1: Initial version</li>
     * </ol>
     */
    private static final int DATABASE_VERSION = 1;

    /**
     * Singleton instance of the {@link RocketRushProvider}. Lazily initialized via {@link #getInstance(Context)}.
     */
    private static RocketRushProvider sProvider = null;

    /**
     * Projection map for {@link BaseColumns#_COUNT}.
     */
    private static final Map<String, String> sCountProjectionMap = Collections.unmodifiableMap(getCountProjectionMap());

    /**
     * Unmodifiable set of valid table names.
     */
    private static final Set<String> sValidTables = Collections.unmodifiableSet(getValidTables());

    /**
     * SQLite database owned by the provider.
     */
    private final SQLiteDatabase mDb;

    /**
     * Obtains an instance of the Localytics Provider. Since the provider is a singleton object, only a single instance will be
     * returned.
     * <p>
     * Note: if {@code context} is an instance of {@link android.test.RenamingDelegatingContext}, then a new object will be
     * returned every time. This is not a "public" API, but is documented here as it aids unit testing.
     *
     * @param context Application context. Cannot be null.
     * @return An instance of {@link RocketRushProvider}.
     * @throws IllegalArgumentException if {@code context} is null
     */
    public static synchronized RocketRushProvider getInstance(final Context context) {
        /*
         * Note: Don't call getApplicationContext() on the context, as that would return a different context and defeat useful
         * contexts such as RenamingDelegatingContext.
         */

        if (Constants.IS_PARAMETER_CHECKING_ENABLED) {
            if (null == context) {
                throw new IllegalArgumentException("context cannot be null"); //$NON-NLS-1$
            }
        }

        if (null == sProvider) {
            sProvider = new RocketRushProvider(context);
        }

        return sProvider;
    }

    /**
     * Constructs a new Localytics Provider.
     * <p>
     * Note: this method may perform disk operations.
     *
     * @param context application context. Cannot be null.
     */
    protected RocketRushProvider(final Context context) {
        mDb = new DatabaseHelper(context, String.format(DATABASE_FILE, DATABASE_VERSION), DATABASE_VERSION).getWritableDatabase();
    }

    /**
     * Inserts a new record.
     * <p>
     * Note: this method may perform disk operations.
     *
     * @param tableName name of the table operate on. Must be one of the recognized tables. Cannot be null.
     * @param values ContentValues to insert. Cannot be null.
     * @return the {@link BaseColumns#_ID} of the inserted row or -1 if an error occurred.
     * @throws IllegalArgumentException if tableName is null or not a valid table name.
     * @throws IllegalArgumentException if values are null.
     */
    public long insert(final String tableName, final ContentValues values)
    {
        if (Constants.IS_PARAMETER_CHECKING_ENABLED)
        {
            if (!isValidTable(tableName))
            {
                throw new IllegalArgumentException(String.format("tableName %s is invalid", tableName)); //$NON-NLS-1$
            }

            if (null == values)
            {
                throw new IllegalArgumentException("values cannot be null"); //$NON-NLS-1$
            }
        }

        if (Constants.IS_LOGGABLE)
        {
            Log.v(Constants.LOG_TAG, String.format("Insert table: %s, values: %s", tableName, values.toString())); //$NON-NLS-1$
        }

        final long result = mDb.insertOrThrow(tableName, null, values);

        if (Constants.IS_LOGGABLE)
        {
            Log.v(Constants.LOG_TAG, String.format("Inserted row with new id %d", Long.valueOf(result))); //$NON-NLS-1$
        }

        return result;
    }

    /**
     * Performs a query.
     * <p>
     * Note: this method may perform disk operations.
     *
     * @param tableName name of the table operate on. Must be one of the recognized tables. Cannot be null.
     * @param projection The list of columns to include. If null, then all columns are included by default.
     * @param selection A filter to apply to all rows, like the SQLite WHERE clause. Passing null will query all rows. This param
     *            may contain ? symbols, which will be replaced by values from the {@code selectionArgs} param.
     * @param selectionArgs An optional string array of replacements for ? symbols in {@code selection}. May be null.
     * @param sortOrder How the rows in the cursor should be sorted. If null, then the sort order is undefined.
     * @return Cursor for the query. To the receiver: Don't forget to call .close() on the cursor when finished with it.
     * @throws IllegalArgumentException if tableName is null or not a valid table name.
     */
    public Cursor query(final String tableName, final String[] projection, final String selection, final String[] selectionArgs, final String sortOrder)
    {
        if (Constants.IS_PARAMETER_CHECKING_ENABLED)
        {
            if (!isValidTable(tableName))
            {
                throw new IllegalArgumentException(String.format("tableName %s is invalid", tableName)); //$NON-NLS-1$
            }
        }

        if (Constants.IS_LOGGABLE)
        {
            Log.v(Constants.LOG_TAG, String.format("Query table: %s, projection: %s, selection: %s, selectionArgs: %s", tableName, Arrays.toString(projection), selection, Arrays.toString(selectionArgs))); //$NON-NLS-1$
        }

        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(tableName);

        if (projection != null && 1 == projection.length && BaseColumns._COUNT.equals(projection[0]))
        {
            qb.setProjectionMap(sCountProjectionMap);
        }

        final Cursor result = qb.query(mDb, projection, selection, selectionArgs, null, null, sortOrder);

        if (Constants.IS_LOGGABLE)
        {
            Log.v(Constants.LOG_TAG, "Query result is: " + DatabaseUtils.dumpCursorToString(result)); //$NON-NLS-1$
        }

        return result;
    }

    /**
     * Updates row(s).
     * <p>
     * Note: this method may perform disk operations.
     *
     * @param tableName name of the table operate on. Must be one of the recognized tables. Cannot be null.
     * @param values A ContentValues mapping from column names (see the associated BaseColumns class for the table) to new column
     *            values.
     * @param selection A filter to limit which rows are updated, like the SQLite WHERE clause. Passing null implies all rows.
     *            This param may contain ? symbols, which will be replaced by values from the {@code selectionArgs} param.
     * @param selectionArgs An optional string array of replacements for ? symbols in {@code selection}. May be null.
     * @return int representing the number of rows modified, which is in the range from 0 to the number of items in the table.
     * @throws IllegalArgumentException if tableName is null or not a valid table name.
     */
    public int update(final String tableName, final ContentValues values, final String selection, final String[] selectionArgs)
    {
        if (Constants.IS_PARAMETER_CHECKING_ENABLED)
        {
            if (!isValidTable(tableName))
            {
                throw new IllegalArgumentException(String.format("tableName %s is invalid", tableName)); //$NON-NLS-1$
            }
        }

        if (Constants.IS_LOGGABLE)
        {
            Log.v(Constants.LOG_TAG, String.format("Update table: %s, values: %s, selection: %s, selectionArgs: %s", tableName, values.toString(), selection, Arrays.toString(selectionArgs))); //$NON-NLS-1$
        }

        return mDb.update(tableName, values, selection, selectionArgs);
    }

    /**
     * Deletes row(s).
     *
     * WORKAROUND for ART verifier bug in KitKat: Changed method name from delete to remove
     *
     * <p>
     * Note: this method may perform disk operations.
     *
     * @param tableName name of the table operate on. Must be one of the recognized tables. Cannot be null.
     * @param selection A filter to limit which rows are deleted, like the SQLite WHERE clause. Passing null implies all rows.
     *            This param may contain ? symbols, which will be replaced by values from the {@code selectionArgs} param.
     * @param selectionArgs An optional string array of replacements for ? symbols in {@code selection}. May be null.
     * @return The number of rows affected, which is in the range from 0 to the number of items in the table.
     * @throws IllegalArgumentException if tableName is null or not a valid table name.
     */
    public int remove(final String tableName, final String selection, final String[] selectionArgs)
    {
        if (Constants.IS_PARAMETER_CHECKING_ENABLED)
        {
            if (!isValidTable(tableName))
            {
                throw new IllegalArgumentException(String.format("tableName %s is invalid", tableName)); //$NON-NLS-1$
            }
        }

        if (Constants.IS_LOGGABLE)
        {
            Log.v(Constants.LOG_TAG, String.format("Delete table: %s, selection: %s, selectionArgs: %s", tableName, selection, Arrays.toString(selectionArgs))); //$NON-NLS-1$
        }

        final int count;
        if (null == selection)
        {
            count = mDb.delete(tableName, "1", null); //$NON-NLS-1$
        }
        else
        {
            count = mDb.delete(tableName, selection, selectionArgs);
        }

        if (Constants.IS_LOGGABLE)
        {
            Log.v(Constants.LOG_TAG, String.format("Deleted %d rows", Integer.valueOf(count))); //$NON-NLS-1$
        }

        return count;
    }

    /**
     * Executes an arbitrary runnable with exclusive access to the database, essentially allowing an atomic transaction.
     *
     * @param runnable Runnable to execute. Cannot be null.
     * @throws IllegalArgumentException if {@code runnable} is null
     */
    /*
     * This implementation is sort of a hack. In the future, it would be better model this after applyBatch() with a list of
     * ContentProviderOperation objects. But that API isn't available until Android 2.0.
     *
     * An alternative implementation would have been to expose the begin/end transaction methods on the Provider object. While
     * that would work, it makes it harder to transition to a ContentProviderOperation model in the future.
     */
    public void runBatchTransaction(final Runnable runnable) {
        if (Constants.IS_PARAMETER_CHECKING_ENABLED) {
            if (null == runnable) {
                throw new IllegalArgumentException("runnable cannot be null"); //$NON-NLS-1$
            }
        }

        mDb.beginTransaction();
        try {
            runnable.run();
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
    }

    /**
     * Executes an arbitrary runnable with exclusive access to the database, essentially allowing an atomic transaction.
     *
     * @param callable Callable to execute. Cannot be null.
     * @throws IllegalArgumentException if {@code callable} is null
     */
    /*
     * This implementation is sort of a hack. In the future, it would be better model this after applyBatch() with a list of
     * ContentProviderOperation objects. But that API isn't available until Android 2.0.
     *
     * An alternative implementation would have been to expose the begin/end transaction methods on the Provider object. While
     * that would work, it makes it harder to transition to a ContentProviderOperation model in the future.
     */
    public Object runBatchTransaction(final Callable<Object> callable) {
        if (Constants.IS_PARAMETER_CHECKING_ENABLED) {
            if (null == callable) {
                throw new IllegalArgumentException("callable cannot be null"); //$NON-NLS-1$
            }
        }

        mDb.beginTransaction();
        try {
            Object result = callable.call();
            mDb.setTransactionSuccessful();
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Database batch transaction failed"); //$NON-NLS-1$
        } finally {
            mDb.endTransaction();
        }
    }

    /**
     * Closes the LocalyticsProvider object. Normally the provider is a long-lived object and should not be closed during normal
     * application use. This method is intended for unit testing purposes only, where a lot of temporary provider objects are
     * created and should be closed.
     */
    /* package */void close()
    {
        mDb.close();
    }

    /**
     * Private helper to test whether a given table name is valid
     *
     * @param table name of a table to check. This param may be null.
     * @return true if the table is valid, false if the table is invalid. If {@code table} is null, returns false.
     */
    private static boolean isValidTable(final String table) {
        if (null == table) {
            return false;
        }

        return sValidTables.contains(table);
    }

    /**
     * Private helper that knows all the tables that {@link RocketRushProvider} can operate on.
     *
     * @return returns a set of the valid tables.
     */
    private static Set<String> getValidTables() {
        final HashSet<String> tables = new HashSet<String>();

        tables.add(IdentityDbColumns.TABLE_NAME);
        tables.add(UsersDbColumns.TABLE_NAME);
        tables.add(PlaysDbColumns.TABLE_NAME);

        return tables;
    }

    /**
     * @return Projection map for {@link BaseColumns#_COUNT}.
     */
    private static HashMap<String, String> getCountProjectionMap() {
        final HashMap<String, String> temp = new HashMap<String, String>();
        temp.put(BaseColumns._COUNT, "COUNT(*)"); //$NON-NLS-1$

        return temp;
    }

    /**
     * Private helper that deletes files from older versions of the RocketRush library.
     * <p>
     * Note: This is a private method that is only made package-accessible for unit testing.
     *
     * @param context application context
     * @throws IllegalArgumentException if {@code context} is null
     */
    /* package */static void deleteOldFiles(final Context context) {
        if (Constants.IS_PARAMETER_CHECKING_ENABLED) {
            if (null == context) {
                throw new IllegalArgumentException("context cannot be null"); //$NON-NLS-1$
            }
        }

        deleteDirectory(new File(context.getFilesDir(), "rocket_rush")); //$NON-NLS-1$
    }

    /**
     * Private helper to delete a directory, regardless of whether the directory is empty.
     *
     * @param directory Directory or file to delete. Cannot be null.
     * @return true if deletion was successful. False if deletion failed.
     */
    private static boolean deleteDirectory(final File directory) {
        if (directory.exists() && directory.isDirectory()) {
            for (final String child : directory.list()) {
                if (!deleteDirectory(new File(directory, child))) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return directory.delete();
    }

    /**
     * A private helper class to open and create the Localytics SQLite database.
     */
    private static final class DatabaseHelper extends SQLiteOpenHelper {
        /**
         * Constant representing the SQLite value for true
         */
        private static final String SQLITE_BOOLEAN_TRUE = "1"; //$NON-NLS-1$

        /**
         * Constant representing the SQLite value for false
         */
        private static final String SQLITE_BOOLEAN_FALSE = "0"; //$NON-NLS-1$

        /**
         * Application context
         */
        private final Context mContext;

        /**
         * @param context Application context. Cannot be null.
         * @param name File name of the database. Cannot be null or empty. A database with this name will be opened in
         *            {@link Context#getDatabasePath(String)}.
         * @param version version of the database.
         */
        public DatabaseHelper(final Context context, final String name, final int version) {
            super(context, name, null, version);

            mContext = context;
        }

        /**
         * Initializes the tables of the database.
         * <p>
         * If an error occurs during initialization and an exception is thrown, {@link SQLiteDatabase#close()} will not be called
         * by this method. That responsibility is left to the caller.
         *
         * @param db The database to perform post-creation processing on. db cannot not be null
         * @throws IllegalArgumentException if db is null
         */
        @Override
        public void onCreate(final SQLiteDatabase db) {
            if (null == db) {
                throw new IllegalArgumentException("db cannot be null"); //$NON-NLS-1$
            }

            // user table
            db.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT NOT NULL, %s TEXT UNIQUE NOT NULL, %s TEXT, %s INTEGER CHECK (%s >= 0));", UsersDbColumns.TABLE_NAME, UsersDbColumns._ID, UsersDbColumns.NAME, UsersDbColumns.EMAIL, UsersDbColumns.IMAGE_URL, UsersDbColumns.SCORE, UsersDbColumns.SCORE)); //$NON-NLS-1$

            // identity table
            db.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT NOT NULL, %s TEXT NOT NULL, %s TEXT UNIQUE, %s TEXT NOT NULL, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT);", IdentityDbColumns.TABLE_NAME, IdentityDbColumns._ID, IdentityDbColumns.APP_VERSION, IdentityDbColumns.ANDROID_VERSION, IdentityDbColumns.REGISTRATION_ID, IdentityDbColumns.DEVICE_MODEL, IdentityDbColumns.DEVICE_IMEI, IdentityDbColumns.DEVICE_WIFI_MAC_HASH, IdentityDbColumns.LOCALE_LANGUAGE, IdentityDbColumns.LOCALE_COUNTRY, IdentityDbColumns.DEVICE_COUNTRY, IdentityDbColumns.NETWORK_CARRIER, IdentityDbColumns.NETWORK_COUNTRY, IdentityDbColumns.NETWORK_TYPE)); //$NON-NLS-1$

            // play table
            db.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s INTEGER REFERENCES %s(%s) NOT NULL, %s TEXT UNIQUE NOT NULL, %s DATETIME DEFAULT CURRENT_TIMESTAMP, %s DATETIME DEFAULT CURRENT_TIMESTAMP, %s INTEGER NOT NULL CHECK (%s >= 0), %s TEXT NOT NULL, %s INTEGER NOT NULL CHECK (%s >= 0), %s INTEGER NOT NULL CHECK (%s >= 0), %s TEXT NOT NULL, %s REAL NOT NULL, %s REAL NOT NULL);", PlaysDbColumns.TABLE_NAME, PlaysDbColumns._ID, PlaysDbColumns.USER_KEY_REF, UsersDbColumns.TABLE_NAME, UsersDbColumns._ID, PlaysDbColumns.UUID, PlaysDbColumns.START_TIME, PlaysDbColumns.STOP_TIME, PlaysDbColumns.TOTAL_TIME, PlaysDbColumns.TOTAL_TIME, PlaysDbColumns.GAME_MODE, PlaysDbColumns.SCORE, PlaysDbColumns.SCORE, PlaysDbColumns.LEVEL, PlaysDbColumns.LEVEL, PlaysDbColumns.RFE, PlaysDbColumns.LATITUDE, PlaysDbColumns.LONGITUDE)); //$NON-NLS-1$
        }

        @Override
        public void onOpen(final SQLiteDatabase db) {
            super.onOpen(db);

            if (Constants.IS_LOGGABLE) {
                Log.v(Constants.LOG_TAG, String.format("SQLite library version is: %s", DatabaseUtils.stringForQuery(db, "select sqlite_version()", null))); //$NON-NLS-1$//$NON-NLS-2$
            }

            if (!db.isReadOnly()) {
                /*
                 * Enable foreign key support
                 */
                db.execSQL("PRAGMA foreign_keys = ON;"); //$NON-NLS-1$
            }
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion)
        {
        }

        // @Override
        // public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
        // {
        // }
    }

    /**
     * Database table for the info data.
     * <p>
     * This is not a public API.
     */
    public static final class IdentityDbColumns implements BaseColumns {
        /**
         * Private constructor prevents instantiation
         *
         * @throws UnsupportedOperationException because this class cannot be instantiated.
         */
        private IdentityDbColumns() {
            throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
        }

        /**
         * SQLite table name
         */
        public static final String TABLE_NAME = "identity"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the app's versionName
         * <p>
         * Constraints: This cannot be null.
         */
        public static final String APP_VERSION = "app_version"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the android version of user's device
         * <p>
         * Constraints: This cannot be null.
         */
        public static final String ANDROID_VERSION = "android_version"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the gcm registration id for this app
         * <p>
         */
        public static final String REGISTRATION_ID = "registration_id"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the user's device model
         * <p>
         * Constraints: This cannot be null.
         */
        public static final String DEVICE_MODEL = "device_model"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the user's device IMEI
         * <p>
         * Constraints: This cannot be null.
         */
        public static final String DEVICE_IMEI = "device_imei"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing a hash of the Wi-Fi MAC address of the device. May be null if Wi-Fi isn't available or is disabled.
         * <p>
         * Constraints: None
         */
        public static final String DEVICE_WIFI_MAC_HASH = "device_wifi_mac_hash"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * Represents the locale language of the device.
         * <p>
         * Constraints: Cannot be null.
         */
        public static final String LOCALE_LANGUAGE = "locale_language"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * Represents the locale country of the device.
         * <p>
         * Constraints: Cannot be null.
         */
        public static final String LOCALE_COUNTRY = "locale_country"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * Represents the locale country of the device, according to the SIM card.
         * <p>
         * Constraints: Cannot be null.
         */
        public static final String DEVICE_COUNTRY = "device_country"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * Represents the network carrier of the device. May be null for non-telephony devices.
         * <p>
         * Constraints: None
         */
        public static final String NETWORK_CARRIER = "network_carrier"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * Represents the network country of the device. May be null for non-telephony devices.
         * <p>
         * Constraints: None
         */
        public static final String NETWORK_COUNTRY = "network_country"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * Represents the primary network connection type for the device. This could be any type, including Wi-Fi, various cell
         * networks, Ethernet, etc.
         * <p>
         * Constraints: None
         *
         * @see android.telephony.TelephonyManager
         */
        public static final String NETWORK_TYPE = "network_type"; //$NON-NLS-1$
    }

    /**
     * Database table for the user data.
     * <p>
     * This is not a public API.
     */
    public static final class UsersDbColumns implements BaseColumns {
        /**
         * Private constructor prevents instantiation
         *
         * @throws UnsupportedOperationException because this class cannot be instantiated.
         */
        private UsersDbColumns() {
            throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
        }

        /**
         * SQLite table name
         */
        public static final String TABLE_NAME = "user"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the app's versionName
         * <p>
         * Constraints: This cannot be null.
         */
        public static final String NAME = "name"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the version of Android
         * <p>
         * Constraints: This cannot be null.
         */
        public static final String EMAIL = "email"; //$NON-NLS-1$

        /**
         * TYPE: {@code int}
         * <p>
         * Integer the Android SDK
         * <p>
         * Constraints: Must be an integer and cannot be null.
         *
         * @see android.os.Build.VERSION#SDK
         */
        public static final String IMAGE_URL = "img_url"; //$NON-NLS-1$

        /**
         * TYPE: {@code int}
         * <p>
         * Represents the highest score the user got.
         * <p>
         * Constraints: Must be an integer and cannot be null.
         */
        public static final String SCORE = "score"; //$NON-NLS-1$
    }

    /**
     * Database table for the play data. Each time a user plays the game, a new row is added.
     * <p>
     * This is not a public API.
     */
    public static final class PlaysDbColumns implements BaseColumns {
        /**
         * Private constructor prevents instantiation
         *
         * @throws UnsupportedOperationException because this class cannot be instantiated.
         */
        private PlaysDbColumns() {
            throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
        }

        /**
         * SQLite table name
         */
        public static final String TABLE_NAME = "plays"; //$NON-NLS-1$

        /**
         * TYPE: {@code long}
         * <p>
         * A one-to-many relationship with {@link UsersDbColumns#_ID}.
         * <p>
         * Constraints: This is a foreign key with the {@link UsersDbColumns#_ID} column. This cannot be null.
         */
        public static final String USER_KEY_REF = "user_key_ref"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * Unique ID of the play, as generated from {@link java.util.UUID}.
         * <p>
         * Constraints: This is unique and cannot be null.
         */
        public static final String UUID = "uuid"; //$NON-NLS-1$

        /**
         * TYPE: {@code long}
         * <p>
         * The time when the play started.
         * <p>
         * Constraints: This column must be >=0. This column cannot be null.
         */
        public static final String START_TIME = "start_time"; //$NON-NLS-1$

        /**
         * TYPE: {@code long}
         * <p>
         * The time when the play stopped.
         * <p>
         * Constraints: This column must larger than play_start_time. This column cannot be null.
         */
        public static final String STOP_TIME = "stop_time"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * The total duration of the game from the play start time.
         * <p>
         * Constraints: This column must be larger than play_stop_time - play_start_time. This column cannot be null.
         */
        public static final String TOTAL_TIME = "total_time"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the game mode. The value should be either 'single' or 'multiple'
         * <p>
         * Constraints: None
         */
        public static final String GAME_MODE = "game_mode"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the game score.
         * <p>
         * Constraints: This column must be >=0. This column cannot be null.
         */
        public static final String SCORE = "score"; // $NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the level on which the game ends.
         * <p>
         * Constraints: This column must be >=1. This column cannot be null.
         */
        public static final String LEVEL = "level"; // $NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the reason for which the game ends.
         * <p>
         * Constraints: This column cannot be null.
         */
        public static final String RFE = "rfe"; // $NON-NLS-1$

        /**
         * TYPE: {@code double}
         * <p>
         * Represents the latitude of the device. May be null if no longitude is known.
         * <p>
         * Constraints: None
         */
        public static final String LATITUDE = "latitude"; //$NON-NLS-1$

        /**
         * TYPE: {@code double}
         * <p>
         * Represents the longitude of the device. May be null if no longitude is known.
         * <p>
         * Constraints: None
         */
        public static final String LONGITUDE = "longitude"; //$NON-NLS-1$
    }
}
