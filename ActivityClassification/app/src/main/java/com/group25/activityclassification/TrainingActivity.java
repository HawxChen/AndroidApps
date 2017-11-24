package com.group25.activityclassification;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

    // Sample database
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
        collectionInterval = 10;

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
        stopSampling();
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
        if (mActivityTimer != null) {
            mActivityTimer.cancel();
            mActivityTimer = null;
        }
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
        case 0:
            mActivity = ActivityType.ACTIVITY_WALKING;
            mActivityTextView.setText("Walking");
            break;
        case 1:
            mActivity = ActivityType.ACTIVITY_RUNNING;
            mActivityTextView.setText("Running");
            break;
        case 2:
            mActivity = ActivityType.ACTIVITY_JUMPING;
            mActivityTextView.setText("Jumping");
            break;
        case 3:
            stop();
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
}
