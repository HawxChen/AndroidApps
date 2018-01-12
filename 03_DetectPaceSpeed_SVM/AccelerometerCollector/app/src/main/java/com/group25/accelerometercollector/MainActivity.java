package com.group25.accelerometercollector;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

    private static final String TAG = "MainActivtiy";

    // UI Elements
    private TextView       mAccelerometerLiveData;
    private Button         mStartButton;
    private SensorManager  mSensorMgr;
    private RadioGroup     mActivityGroup;
    private TextView       mWalkingActivityCount;
    private TextView       mRunningActivityCount;
    private TextView       mJumpingActivityCount;

    // State tracking
    private Boolean        mIsRunning;
    private Boolean        mIsAccelerometerRegistered;
    private ActivityType   mActivity;
    private CountDownTimer mCountDownTimer;

    // Sample database
    private ArrayList<AccelerometerSample> mWalkingSamples;
    private ArrayList<AccelerometerSample> mRunningSamples;
    private ArrayList<AccelerometerSample> mJumpingSamples;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStartButton = (Button) findViewById(R.id.startButton);
        mStartButton.setOnClickListener(this);
        mActivityGroup = (RadioGroup) findViewById(R.id.activtiyGroup);
        mAccelerometerLiveData = (TextView) findViewById(R.id.accelerometerLiveData);
        mWalkingActivityCount = (TextView) findViewById(R.id.walkingActivityCount);
        mRunningActivityCount = (TextView) findViewById(R.id.runningActivityCount);
        mJumpingActivityCount = (TextView) findViewById(R.id.jumpingActivityCount);

        mSensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        mIsAccelerometerRegistered = false;

        mWalkingSamples = new ArrayList<AccelerometerSample>();
        mRunningSamples = new ArrayList<AccelerometerSample>();
        mJumpingSamples = new ArrayList<AccelerometerSample>();

        stop();
    }

    //
    // Helper method to enable or disable all of the activity options
    //
    private void setRadioGroupEnabled(Boolean enabled) {
        ((RadioButton)(findViewById(R.id.walkingActivity))).setEnabled(enabled);
        ((RadioButton)(findViewById(R.id.runningActivity))).setEnabled(enabled);
        ((RadioButton)(findViewById(R.id.jumpingActivity))).setEnabled(enabled);
    }

    //
    // Start sample collection (Start with countdown to allow user time to begin activity)
    //
    private void start() {
        mStartButton.setText("Stop");
        mIsRunning = true;
        setRadioGroupEnabled(false);
        switch (mActivityGroup.getCheckedRadioButtonId()) {
            case R.id.walkingActivity: mActivity = ActivityType.ACTIVITY_WALKING; break;
            case R.id.runningActivity: mActivity = ActivityType.ACTIVITY_RUNNING; break;
            case R.id.jumpingActivity: mActivity = ActivityType.ACTIVITY_JUMPING; break;
            default:                   mActivity = ActivityType.ACTIVITY_UNKNOWN; break;
        }
        mCountDownTimer = new CountDownTimer(5000, 1000) {
            public void onTick(long millisUntilFinished) {
                mAccelerometerLiveData.setText(String.format("Please begin activity!\n\nRecording starting in %d...", millisUntilFinished / 1000 + 1));
            }

            public void onFinish() {
                start_recording_activity();
            }
        }.start();
    }

    private void start_recording_activity() {
        if (!mIsAccelerometerRegistered) {
            Sensor accelSensor = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorMgr.registerListener(this, accelSensor, 100000); // Every 100ms (10Hz)
            mIsAccelerometerRegistered = true;
        }
    }

    //
    // Stop Sample Collection
    //
    private void stop() {
        if (mIsAccelerometerRegistered) {
            mSensorMgr.unregisterListener(this);
            mIsAccelerometerRegistered = false;
        }
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
        mIsRunning = false;
        mStartButton.setText("Start");
        mAccelerometerLiveData.setText("Press \"Start\" button to begin collecting samples.");
        setRadioGroupEnabled(true);
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
                mWalkingActivityCount.setText(String.format("%d", mWalkingSamples.size()));
                break;
            case ACTIVITY_RUNNING:
                mRunningSamples.add(new AccelerometerSample(x, y, z));
                mRunningActivityCount.setText(String.format("%d", mRunningSamples.size()));
                break;
            case ACTIVITY_JUMPING:
                mJumpingSamples.add(new AccelerometerSample(x, y, z));
                mJumpingActivityCount.setText(String.format("%d", mJumpingSamples.size()));
                break;
        }

        mAccelerometerLiveData.setText(String.format("X: %f, Y: %f; Z: %f", x, y, z));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
