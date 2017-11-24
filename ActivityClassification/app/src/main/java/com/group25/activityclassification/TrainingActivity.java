package com.group25.activityclassification;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
}

public class TrainingActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

    private static final String TAG = "TrainingActivity";

    // UI Elements
    private TextView       mAccelerometerLiveData;
    private Button         mStartButton;
    private SensorManager  mSensorMgr;
    private TextView       mActivityTextView;
    private TextView       mTimerTextView;

    // State tracking
    private Boolean        mIsRunning;
    private Boolean        mIsAccelerometerRegistered;
    private ActivityType   mActivity;
    private CountDownTimer mCountDownTimer;
    private CountDownTimer mActivityTimer;
    private int            countdownInterval;
    private int            collectionInterval;
    private int            mState;

    // Database
    private DatabaseHelper mDb;

    // Samples (stored in memory before committing to memory)
    private ArrayList<AccelerometerSample> mWalkingSamples;
    private ArrayList<AccelerometerSample> mRunningSamples;
    private ArrayList<AccelerometerSample> mJumpingSamples;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);
        setupActionBar();

        mStartButton = (Button) findViewById(R.id.startButton);
        mStartButton.setOnClickListener(this);

        mAccelerometerLiveData = (TextView) findViewById(R.id.accelerometerLiveData);
        mActivityTextView = (TextView) findViewById(R.id.activityTextView);
        mTimerTextView = (TextView) findViewById(R.id.timerTextView);

        mSensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        mIsAccelerometerRegistered = false;

        mWalkingSamples = new ArrayList<AccelerometerSample>();
        mRunningSamples = new ArrayList<AccelerometerSample>();
        mJumpingSamples = new ArrayList<AccelerometerSample>();

        mState = 0;
        countdownInterval = 5;
        collectionInterval = 105; // 20*5 + 5 seconds

        //----------------

        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        if (mExternalStorageAvailable) {
            Log.d(TAG, "EXTERNAL STORAGE AVAILABLE");
        } else {
            Log.d(TAG, "EXTERNAL STORAGE NOT! AVAILABLE");
        }

        if (mExternalStorageWriteable) {
            Log.d(TAG, "EXTERNAL STORAGE WRITEABLE");
        } else {
            Log.d(TAG, "EXTERNAL STORAGE NOT! WRITEABLE");
        }

        //----------------


        mDb = new DatabaseHelper();

        stop();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle("Training");
        }
    }

    //
    // Button Handler
    //
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.startButton:
                if (mIsRunning) {
                    stop();
                } else {
                    start();
                }
                break;

            default:
                break;
        }
    }

    //
    // Start sample collection
    //
    private void start() {
        mStartButton.setText("Stop");
        mIsRunning = true;
        mState = 0;
        beginActivity();
    }

    //
    // Stop activity state machine
    //
    private void stop() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
        if (mActivityTimer != null) {
            mActivityTimer.cancel();
            mActivityTimer = null;
        }
        stopSampling();
        mIsRunning = false;
        mStartButton.setText("Start");
        mAccelerometerLiveData.setText("Press \"Start\" button to begin collecting samples.");
        mTimerTextView.setText("");
        mActivityTextView.setText("");
    }

    //
    // Activity state machine
    //
    private void beginActivity() {
        switch (mState) {

        // Collect samples for Walking activity
        case 0:
            mActivity = ActivityType.ACTIVITY_WALKING;
            mActivityTextView.setText("Walking");
            break;

        // Collect samples for Running activity
        case 1:
            mActivity = ActivityType.ACTIVITY_RUNNING;
            mActivityTextView.setText("Running");
            break;

        // Collect samples for Jumping Activity
        case 2:
            mActivity = ActivityType.ACTIVITY_JUMPING;
            mActivityTextView.setText("Jumping");
            break;

        // Save samples to database
        case 3:
            stop();
            saveSamplesToDatabase();
            return;
        }

        mState += 1;
        startActivityRecording();
    }

    //
    // Start sampling
    //
    private void startActivityRecording() {
        mAccelerometerLiveData.setText("Please begin activity!");

        // Create a countdown timer to time activity recording
        mActivityTimer = new CountDownTimer(collectionInterval * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                mTimerTextView.setText(String.format("%d seconds remaining", millisUntilFinished / 1000 + 1));
            }

            public void onFinish() {
                mAccelerometerLiveData.setText("");
                mTimerTextView.setText("");
                stopSampling();

                // Begin next activity in state machine
                beginActivity();
            }
        };

        // Create a countdown timer to allow the user to begin activity
        mCountDownTimer = new CountDownTimer(countdownInterval * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                mTimerTextView.setText(String.format("Recording starting in %d", millisUntilFinished / 1000 + 1));
            }

            public void onFinish() {
                mTimerTextView.setText("");
                mActivityTimer.start();
                startSampling();
            }
        }.start();
    }

    //
    // Start sampling
    //
    private void startSampling() {
        if (!mIsAccelerometerRegistered) {
            Sensor accelSensor = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorMgr.registerListener(this, accelSensor, 100000); // Every 100ms (10Hz)
            mIsAccelerometerRegistered = true;
        }
    }

    //
    // Stop sampling
    //
    private void stopSampling() {
        if (mIsAccelerometerRegistered) {
            mSensorMgr.unregisterListener(this);
            mIsAccelerometerRegistered = false;
        }
    }

    //
    // Accelerometer Data Callback
    //
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        switch (mActivity) {
            case ACTIVITY_WALKING:
                mWalkingSamples.add(new AccelerometerSample(x, y, z));
                break;
            case ACTIVITY_RUNNING:
                mRunningSamples.add(new AccelerometerSample(x, y, z));
                break;
            case ACTIVITY_JUMPING:
                mJumpingSamples.add(new AccelerometerSample(x, y, z));
                break;
        }

        // Update accelerometer live text view
        mAccelerometerLiveData.setText(String.format("X: %f, Y: %f, Z: %f", x, y, z));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    //
    // Commit samples to database
    //
    public void saveSamplesToDatabase() {
        mStartButton.setEnabled(false);
        mAccelerometerLiveData.setText("Saving samples to database...");
        new DatabaseSaverTask().execute();
    }

    //
    // Simple async task to write samples to database in the background
    //
    private class DatabaseSaverTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            //    publishProgress((int) ((i / (float) count) * 100));
            //    if (isCancelled()) break;

            // Save 3 (XYZ) * 50 (5 sec, 10Hz) * 20 (collections) * 3 (activities) samples total to database
            for (int i = 0; i < 20; i++) {
                int from = i*50;
                int to = (i+1)*50;
                mDb.addRecordsToDatabase(ActivityType.ACTIVITY_WALKING,
                    new ArrayList<AccelerometerSample>(mWalkingSamples.subList(from, to)));
                mDb.addRecordsToDatabase(ActivityType.ACTIVITY_RUNNING,
                    new ArrayList<AccelerometerSample>(mRunningSamples.subList(from, to)));
                mDb.addRecordsToDatabase(ActivityType.ACTIVITY_JUMPING,
                    new ArrayList<AccelerometerSample>(mJumpingSamples.subList(from, to)));
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mAccelerometerLiveData.setText("Database created!");
        }
    }
}
