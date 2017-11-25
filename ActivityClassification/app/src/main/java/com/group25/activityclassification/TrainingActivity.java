package com.group25.activityclassification;

import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;

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
    private int            mCountdownInterval;
    private int            mCollectionInterval;
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
        mCountdownInterval = 5;
        mCollectionInterval = 105; // 20*5 + 5 seconds

        mDb = new DatabaseHelper();

        if (mDb.exists()) {
            // DB already exists? Want to run training on existing data?
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Training database already exists! Would you like to DELETE the existing training data and collect new data?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Delete existing training data and re-train
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Keep existing training data and exit activity
                    Intent resultIntent = new Intent();
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                }
            });
            builder.show();
        }

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
        mActivityTimer = new CountDownTimer(mCollectionInterval * 1000, 1000) {
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
        mCountDownTimer = new CountDownTimer(mCountdownInterval * 1000, 1000) {
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
            mDb.reinitDatabase();

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
