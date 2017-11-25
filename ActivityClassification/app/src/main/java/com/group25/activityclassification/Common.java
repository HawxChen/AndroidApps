package com.group25.activityclassification;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

enum ActivityType {
    ACTIVITY_WALKING,
    ACTIVITY_RUNNING,
    ACTIVITY_JUMPING,
    ACTIVITY_UNKNOWN
};

class AccelerometerSample {
    float x, y, z;

    public AccelerometerSample(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}

class UserActivity {
    private ArrayList<AccelerometerSample> mSamples;
    private ActivityType                   mActivityType;

    public UserActivity(ArrayList<AccelerometerSample> samples) {
        mSamples      = samples;
        mActivityType = ActivityType.ACTIVITY_UNKNOWN;
    }

    public UserActivity(ArrayList<AccelerometerSample> samples, ActivityType activityType) {
        mSamples      = samples;
        mActivityType = activityType;
    }

    public String getSvmFormat() {
        StringBuilder stringBuilder = new StringBuilder();
        int classifierId;
        switch (mActivityType) {
            case ACTIVITY_WALKING: classifierId = -1; break;
            case ACTIVITY_RUNNING: classifierId =  0; break;
            case ACTIVITY_JUMPING: classifierId =  1; break;
            default: classifierId = -1; break; // Default to walking
        }
        stringBuilder.append(String.format("%d ", classifierId));
        for (int i = 0; i < 50; i++) {
            AccelerometerSample sample = mSamples.get(i);
            stringBuilder.append(String.format("%d:%f ", i*3+1, sample.x/10.0));
            stringBuilder.append(String.format("%d:%f ", i*3+2, sample.y/10.0));
            stringBuilder.append(String.format("%d:%f ", i*3+3, sample.z/10.0));
        }
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }
}

class DatabaseHelper {

    private static final String TAG = "DatabaseHelper";

    private SQLiteDatabase db;
    private String         dbDir;
    private String         dbName;
    private String         dbPath;

    //
    // Open the database for writing
    //
    public DatabaseHelper() {
        // Path to database in the filesystem
        dbDir = Environment.getExternalStorageDirectory().getPath() + File.separator + "Cse535_Group25";
        dbName = "A3.db";
        dbPath = dbDir + File.separator + dbName;
    }

    //
    // Get DB directory
    //
    public String getDbDir() {
        return dbDir;
    }

    //
    // Get DB path
    //
    public String getDbPath() {
        return dbPath;
    }

    //
    // Check to see if the database already exists
    //
    public Boolean exists() {
        return (new File(dbPath)).exists();
    }

    //
    // Reinit the database
    //
    public void reinitDatabase() {
        // Delete database if it exists
        File f = new File(dbPath);
        if (f.exists()) {
            f.delete();
        }

        initDatabase();
    }

    //
    // Init the database
    //
    public void initDatabase() {
        try
        {
            // Open the database (create if it does not exist)
            Log.e(TAG,"Database path: " + dbPath);
            new File(dbDir).mkdirs(); // Make sure parent directories exist!
            db = SQLiteDatabase.openOrCreateDatabase(dbPath, null);
            if (db == null) {
                Log.e(TAG, "openOrCreateDatabase returned null!");
            } else {
                Log.e(TAG, "Successfully opened database");
            }
            createTable();
        }
        catch (SQLiteException ex)
        {
            Log.e(TAG, "error -- " + ex.getMessage(), ex);
        }
    }

    //
    // Create samples table in database
    //
    public void createTable() {
        // Based on assignment specification, the DB schema is as follows:
        //
        // +----+-------+-------+-------+-----+--------+--------+--------+----------+
        // | ID | Accel | Accel | Accel | ... | Accel  | Accel  | Accel  | Activity |
        // |    | X 1st | Y 1st | Z 1st | ... | X 50th | Y 50th | Z 50th | Label    |
        // +----+-------+-------+-------+-----+--------+--------+--------+----------+

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("CREATE TABLE IF NOT EXISTS `samples` (" +
                "`ID` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE, ");
        for (int i = 1; i <= 50; i++) {
            queryBuilder.append(String.format("`ACCEL%d_X` REAL NOT NULL, " +
                    "`ACCEL%d_Y` REAL NOT NULL, " +
                    "`ACCEL%d_Z` REAL NOT NULL, ", i, i, i));

        }
        queryBuilder.append("`ACTIVITY`	TEXT NOT NULL)");
        String query = queryBuilder.toString();

        Log.e(TAG, "SQL Query: " + query);
        db.beginTransaction();
        db.execSQL(query);
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public String activityTypeToString(ActivityType activityType) {
        switch (activityType) {
            case ACTIVITY_WALKING: return "Walking";
            case ACTIVITY_RUNNING: return "Running";
            case ACTIVITY_JUMPING: return "Jumping";
            default:               return "Unknown";
        }
    }

    public ActivityType stringToActivityType(String str) {
        if (str.equals("Walking")) return ActivityType.ACTIVITY_WALKING;
        if (str.equals("Running")) return ActivityType.ACTIVITY_RUNNING;
        if (str.equals("Jumping")) return ActivityType.ACTIVITY_JUMPING;
        return ActivityType.ACTIVITY_UNKNOWN;
    }

    public void addRecordsToDatabase(ActivityType activityType, ArrayList<AccelerometerSample> samples) {
        ContentValues cv = new ContentValues();
        for(int i = 1; i <= 50; i++) {
            AccelerometerSample sample = samples.get(i-1);
            cv.put(String.format("ACCEL%d_X", i), sample.x);
            cv.put(String.format("ACCEL%d_Y", i), sample.y);
            cv.put(String.format("ACCEL%d_Z", i), sample.z);
            cv.put("ACTIVITY", activityTypeToString(activityType));
        }

        db.beginTransaction();
        db.insert("samples", null, cv);
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public ArrayList<UserActivity> getActivitiesFromDatabase() {
        ArrayList<UserActivity> activities = new ArrayList<UserActivity>();
        Cursor cursor = db.rawQuery("SELECT * FROM `samples`", null);
        cursor.moveToFirst();
        for (int i = 0; i < cursor.getCount(); i++) {
            ArrayList<AccelerometerSample> samples = new ArrayList<AccelerometerSample>();
            for (int j = 0; j < 50; j++) {
                float x = cursor.getFloat(1 + j*3 + 0);
                float y = cursor.getFloat(1 + j*3 + 1);
                float z = cursor.getFloat(1 + j*3 + 2);
                samples.add(new AccelerometerSample(x, y, z));
            }
            String label = cursor.getString(cursor.getColumnCount()-1);
            activities.add(new UserActivity(samples, stringToActivityType(label)));
            cursor.moveToNext();
        }
        return activities;
    }
}
