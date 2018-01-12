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
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

    private static final String TAG = "MainActivity";

    public final static int REQUEST_TRAINING_FINISHED = 2;

    // UI Elements
    private TextView       mAccelerometerLiveData;
    private SensorManager  mSensorMgr;
    private TextView       mActivityTextView;
    private TextView       mTimerTextView;
    private TextView       mModelInfoTextView;
    private EditText       mCostEditText;
    private EditText       mGammaEditText;
    private EditText       mFoldsEditText;
    private Button         mStartButton;

    // State tracking
    private Boolean        mIsRunning;
    private Boolean        mIsAccelerometerRegistered;
    private ActivityType   mActivity;
    private CountDownTimer mTimer;
    private int            mPredictionInterval;
    private float          mCost;
    private float          mGamma;
    private int            mK;
    private long           mLastTime;

    private Classifier                     mClassifier;
    private ArrayList<AccelerometerSample> mActivitySamples;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        if (!isExternalStorageWritable()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("External storage is not writable! Please check permissions.");
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            builder.show();
            return;
        }

        setContentView(R.layout.activity_main);

        // Setup buttons
        ((Button)findViewById(R.id.trainingDataButton)).setOnClickListener(this);
        ((Button)findViewById(R.id.trainSvmButton)).setOnClickListener(this);

        // Get handles for views
        mModelInfoTextView     = (TextView) findViewById(R.id.modelInfoTextView);
        mCostEditText          = (EditText) findViewById(R.id.costEditText);
        mGammaEditText         = (EditText) findViewById(R.id.gammaEditText);
        mFoldsEditText         = (EditText) findViewById(R.id.foldsEditText);
        mStartButton           = (Button)   findViewById(R.id.startButton);
        mActivityTextView      = (TextView) findViewById(R.id.activityTextView);
        mTimerTextView         = (TextView) findViewById(R.id.timerTextView);
        mAccelerometerLiveData = (TextView) findViewById(R.id.accelerometerLiveData);

        mStartButton.setOnClickListener(this);

        mClassifier                = new Classifier();
        mSensorMgr                 = (SensorManager) getSystemService(SENSOR_SERVICE);
        mIsAccelerometerRegistered = false;
        mActivitySamples           = new ArrayList<AccelerometerSample>();
        mPredictionInterval        = 5;

        // Destroy any existing models to force re-train
        if (mClassifier.isModelAvailable()) {
            mClassifier.destroyModel();
        }

        // Load default training data if necessary
        loadDefaultTrainingData();

        stop();
    }

    //
    // Make sure we have the necessary storage permissions
    // https://developer.android.com/guide/topics/data/data-storage.html#filesExternal
    //
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    //
    // Load pre-recorded training data
    //
    private void loadDefaultTrainingData() {
        DatabaseHelper dbHelper = new DatabaseHelper();

        if (dbHelper.exists()) {
            // Already have a database file
            return;
        }

        String dst = dbHelper.getDbPath();

        // Make sure directories exist
        new File(dbHelper.getDbDir()).mkdirs();

        // https://stackoverflow.com/questions/4081763/access-resource-files-in-android
        InputStream in = getResources().openRawResource(R.raw.default_training_data);

        // https://stackoverflow.com/questions/9292954/how-to-make-a-copy-of-a-file-in-android
        try {
            try {
                FileOutputStream out = new FileOutputStream(dst);
                try {
                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                } finally {
                    out.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.trainingDataButton:
                stop();
                Intent intent = new Intent(getBaseContext(), TrainingActivity.class);
                startActivityForResult(intent, REQUEST_TRAINING_FINISHED);
                break;
            case R.id.trainSvmButton:
                train();
                break;
            case R.id.startButton:
                start();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_TRAINING_FINISHED:
                // Reached here when we return from the Training Activity
                break;
        }
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("CSE535,G25,A3: Activity Classifier");
        }
    }

    //
    // Start sample collection
    //
    private void start() {
        mIsRunning = true;
        mStartButton.setVisibility(View.GONE);
        mActivityTextView.setVisibility(View.VISIBLE);
        startActivityRecording();
    }

    //
    // Stop activity state machine
    //
    private void stop() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        stopSampling();
        mIsRunning = false;
        mModelInfoTextView.setText("");
        mTimerTextView.setText("");
        mStartButton.setEnabled(false);
        mStartButton.setVisibility(View.VISIBLE);
        mActivityTextView.setText("");
        mActivityTextView.setVisibility(View.GONE);
        mAccelerometerLiveData.setText("");
    }

    //
    // Predict activity
    //
    private void predictActivity() {
        String activityString;

        if (mActivitySamples.size() < 50) {
            // Need at least 50 samples to make a prediction
            mActivity = ActivityType.ACTIVITY_UNKNOWN;
        } else {
            // Trim all but the latest 50 samples
            for (int i = mActivitySamples.size()-50; i > 0; i--) {
                mActivitySamples.remove(0);
            }

            mActivity = mClassifier.classifyActivity(new UserActivity(mActivitySamples));
        }

        switch (mActivity) {
            case ACTIVITY_WALKING: activityString = "Walking"; break;
            case ACTIVITY_RUNNING: activityString = "Running"; break;
            case ACTIVITY_JUMPING: activityString = "Jumping"; break;
            default:               activityString = "Unknown";
        }

        mActivityTextView.setText(activityString);
    }

    //
    // Start sampling
    //
    private void startActivityRecording() {
        mTimerTextView.setText("");
        mAccelerometerLiveData.setText("");

        predictActivity();
        startSampling();

        // Create a countdown timer to periodically re-calculate activity
        mTimer = new CountDownTimer(mPredictionInterval * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                mTimerTextView.setText(String.format("%d seconds until next analysis...", millisUntilFinished / 1000 + 1));
            }

            public void onFinish() {
                stopSampling();
                predictActivity();
                startSampling();

                // Restart the timer
                start();
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
            mLastTime = System.currentTimeMillis();
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

        // Update accelerometer live text view
        mAccelerometerLiveData.setText(String.format("X: %f, Y: %f, Z: %f", x, y, z));

        // Try to smooth out accelerometer readings (they seem to come in much faster than requested)
        long now = System.currentTimeMillis();
        if (now - mLastTime < 100) return;
        mLastTime = now;

        mActivitySamples.add(new AccelerometerSample(x, y, z));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void train() {
        stop();
        mCost = Float.valueOf(mCostEditText.getText().toString()).floatValue();
        mGamma = Float.valueOf(mGammaEditText.getText().toString()).floatValue();
        mK = Integer.valueOf(mFoldsEditText.getText().toString()).intValue();
        SvmTrainerTask trainer = new SvmTrainerTask();
        trainer.execute();
    }

    //
    // Simple async task to train the model in the background
    //
    private class SvmTrainerTask extends AsyncTask<Void, Void, Boolean> {

        String mErrorMessage;

        @Override
        protected Boolean doInBackground(Void... voids) {
            mErrorMessage = "";

            // Get samples from database
            DatabaseHelper db = new DatabaseHelper();
            if (!db.exists()) {
                mErrorMessage = "Cannot start training because database does not exist! Please collect training data before training.";
                return false;
            }
            db.initDatabase();

            ArrayList<UserActivity> activities = db.getActivitiesFromDatabase();

            if (activities == null || activities.size() == 0) {
                mErrorMessage = "No data in database! Please retry collecting training data.";
                return false;
            }

            // Begin training
            int result = mClassifier.train(activities, mK, mCost, mGamma);

            if (result != 0) {
                mErrorMessage = mClassifier.mErrorMessage;
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!result) {
                Toast.makeText(getApplicationContext(), String.format("Error: %s", mErrorMessage), Toast.LENGTH_LONG).show();
                return;
            } else {
                mModelInfoTextView.setText(String.format("Cross Validation Accuracy: %f%%", mClassifier.mCrossValidationAccuracy));
                mStartButton.setEnabled(true);
            }
        }
    }
}
