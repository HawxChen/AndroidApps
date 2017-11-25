package com.group25.activityclassification;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

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
        stringBuilder.append(String.format("%d ", 0));
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

class Classifier {

    private String modelDir;
    private String modelName;
    private String modelPath;

    public Classifier() {
        // Path to model in the filesystem
        modelDir = Environment.getExternalStorageDirectory().getPath() + File.separator + "Cse535_Group25";
        modelName = "model";
        modelPath = modelDir + File.separator + modelName;
    }

    public Boolean isModelAvailable() {
        return (new File(modelPath)).exists();
    }

    public ActivityType classifyActivity(UserActivity activity) {
        try {
            // Write activity in libsvm format to a temp file
            File inputFile = new File(modelDir, "input");
            FileOutputStream fout = new FileOutputStream(inputFile);
            PrintWriter p = new PrintWriter(fout);
            p.write(activity.getSvmFormat());
            Log.d("CLASSIFIER", activity.getSvmFormat());
            p.close();

            // Run svm_predict on the model + input file
            File outputFile = new File(modelDir, "output");
            String[] argv = new String[3];
            argv[0] = inputFile.getPath();
            argv[1] = modelPath;
            argv[2] = outputFile.getPath();

            Log.d("CLASSIFIER", String.format("%s %s %s", argv[0], argv[1], argv[2]));

            // Read output file to get activity type
            svm_predict.main(argv);
            BufferedReader br = new BufferedReader(new FileReader(outputFile));
            String line = br.readLine();

            Log.d("CLASSIFIER", line);

            if (line == null) {
                return ActivityType.ACTIVITY_UNKNOWN;
            }

            int activityId = (int)Float.parseFloat(line);

            br.close();

            switch (activityId) {
                case -1: return ActivityType.ACTIVITY_WALKING;
                case  0: return ActivityType.ACTIVITY_RUNNING;
                case  1: return ActivityType.ACTIVITY_JUMPING;
                default: return ActivityType.ACTIVITY_UNKNOWN;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return ActivityType.ACTIVITY_UNKNOWN;
        }
    }
}

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

    private static final String TAG = "MainActivity";

    // UI Elements
    private TextView       mAccelerometerLiveData;
    private SensorManager  mSensorMgr;
    private TextView       mActivityTextView;
    private TextView       mTimerTextView;

    // State tracking
    private Boolean        mIsRunning;
    private Boolean        mIsAccelerometerRegistered;
    private ActivityType   mActivity;
    private CountDownTimer mTimer;
    private int            mPredictionInterval;

    private Classifier                     mClassifier;
    private ArrayList<AccelerometerSample> mActivitySamples;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupActionBar();

        ((Button)findViewById(R.id.trainSvmButton)).setOnClickListener(this);

        // Get handles for views
        mAccelerometerLiveData = (TextView) findViewById(R.id.accelerometerLiveData);
        mActivityTextView      = (TextView) findViewById(R.id.activityTextView);
        mTimerTextView         = (TextView) findViewById(R.id.timerTextView);

        mClassifier = new Classifier();
        mSensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        mIsAccelerometerRegistered = false;
        mActivitySamples = new ArrayList<AccelerometerSample>();
        mPredictionInterval = 5;

        if (mClassifier.isModelAvailable()) {
            start();
        } else {
            mActivityTextView.setText("Unknown");
            mTimerTextView.setText("");
            mAccelerometerLiveData.setText("Model not available, please train!");
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.trainSvmButton:
                Intent intent = new Intent(getBaseContext(), TrainingActivity.class);
                startActivity(intent);
                break;
        }
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Activity Classifier");
        }
    }

    //
    // Start sample collection
    //
    private void start() {
        mIsRunning = true;
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
        mAccelerometerLiveData.setText("");
        mTimerTextView.setText("");
        mActivityTextView.setText("");
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

        mActivitySamples.add(new AccelerometerSample(x, y, z));

        // Update accelerometer live text view
        mAccelerometerLiveData.setText(String.format("X: %f, Y: %f, Z: %f", x, y, z));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
