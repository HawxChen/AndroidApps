package com.group25.activityclassification;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
    private ActivityType   mActivityType;
    private CountDownTimer mCountDownTimer;
    private int            mCountdownInterval;
    private int            mActivitiesGathered; // Number of activities of a certain type gathered
    private int            mActivitiesRequired; // Number of activities of a certain type required
    private int            mState;

    // Database
    private DatabaseHelper mDb;

    // Samples (stored in memory before committing to memory)
    private ArrayList<AccelerometerSample> mSamples;
    private ArrayList<UserActivity>        mActivities;

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

        mSamples = new ArrayList<AccelerometerSample>();
        mActivities = new ArrayList<UserActivity>();

        mState              = 0;
        mCountdownInterval  = 3;
        mActivitiesRequired = 20;

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
        beginActivityCollection();
    }

    //
    // Stop activity state machine
    //
    private void stop() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
        stopSampling();
        mIsRunning = false;
        mStartButton.setText("Start");
        mAccelerometerLiveData.setText("Press \"Start\" button to begin collecting samples.");
        mTimerTextView.setText("");
        mActivityTextView.setText("");

        mSamples.clear();
        mActivities.clear();
    }

    //
    // Activity state machine
    //
    private void beginActivityCollection() {
        switch (mState) {

        // Collect samples for Walking activity
        case 0:
            mActivityType = ActivityType.ACTIVITY_WALKING;
            mActivityTextView.setText("Walking");
            break;

        // Collect samples for Running activity
        case 1:
            mActivityType = ActivityType.ACTIVITY_RUNNING;
            mActivityTextView.setText("Running");
            break;

        // Collect samples for Jumping Activity
        case 2:
            mActivityType = ActivityType.ACTIVITY_JUMPING;
            mActivityTextView.setText("Jumping");
            break;

        // Save samples to database
        case 3:
            saveSamplesToDatabase();
            return;
        }

        mState += 1;
        mActivitiesGathered = 0;
        startActivityRecording();
    }

    //
    // Start sampling
    //
    private void startActivityRecording() {
        mAccelerometerLiveData.setText("Please begin activity!");

        // Create a countdown timer to allow the user to begin activity
        mCountDownTimer = new CountDownTimer(mCountdownInterval * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                mTimerTextView.setText(String.format("Recording starting in %d", millisUntilFinished / 1000 + 1));
            }

            public void onFinish() {
                mTimerTextView.setText("");
                startSampling();
            }
        }.start();
    }

    private void updateGatheredSamplesView() {
        mTimerTextView.setText(String.format("Gathered %d of %d activity recordings", mActivitiesGathered, mActivitiesRequired));
    }

    //
    // Start sampling
    //
    private void startSampling() {
        if (!mIsAccelerometerRegistered) {
            updateGatheredSamplesView();
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
        mAccelerometerLiveData.setText("");
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

        mSamples.add(new AccelerometerSample(x, y, z));

        if (mSamples.size() >= 50) {
            // We have enough samples for a full activity
            mActivities.add(new UserActivity((ArrayList<AccelerometerSample>)mSamples.clone(), mActivityType));
            mSamples.clear();
            mActivitiesGathered += 1;
            updateGatheredSamplesView();
            if (mActivitiesGathered >= mActivitiesRequired) {
                stopSampling();
                beginActivityCollection();
            }
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
            mDb.addActivitiesToDatabase(mActivities);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            stop();
            mAccelerometerLiveData.setText("Database created!");
        }
    }
}
