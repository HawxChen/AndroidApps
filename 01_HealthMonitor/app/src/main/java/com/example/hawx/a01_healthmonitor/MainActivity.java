//
//
// Class: CSE535, Fall 2017
// Assignment 1
// Group 25
//
// This is the main activity in the assignment. It presents the required
// user interface and handles the necessary functionality.
//
//
package com.example.hawx.a01_healthmonitor;

import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RadioGroup;

import java.util.Random;;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private float[] mUptV;
    private GraphView mGview;
    private boolean mRunning = false;
    private Handler mHandler = new Handler();
    private HMonitorRunnable mJob;
    private final int NUM_RECORD_MAX = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Connect listeners
        findViewById(R.id.brun).setOnClickListener(this);
        findViewById(R.id.bstop).setOnClickListener(this);

        // Instantiate the Graph View
        FrameLayout fLayout = (FrameLayout)findViewById(R.id.framedraw);
        mUptV = new float[NUM_RECORD_MAX];
        String[] horizmark = new String[]{"2700", "2750", "2800", "2850", "2900","3000","3050", "3100"};
        String[] vertimark = new String[]{"2000", "1500", "1000", "500", "0"};
        mGview = new GraphView(this, mUptV, "Group 25's HealthMonitor", horizmark, vertimark, true);
        fLayout.addView(mGview);

        // Dummy handler for radio button changes
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.bgroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int rgID) {
                int btnidx = radioGroup.indexOfChild(radioGroup.findViewById(rgID));
                switch (btnidx) {
                    case 0:
                        break;
                    default:
                        break;
                }
            }
        });
    }

    // Handle on-click events for Run/Stop buttons
    @Override
    public void onClick(View vinfo) {
        switch (vinfo.getId()){
            case R.id.brun:
                onRunBtn();
                break;
            case R.id.bstop:
                onStopBtn();
                break;
            default:
        }
    }

    // Run button handler
    private void onRunBtn() {
        if (mRunning) {
            mHandler.removeCallbacks(mJob);
            mUptV = new float[0];
        }
        mJob = new HMonitorRunnable();
        mRunning = true;
        mHandler.post(mJob);
    }

    // Stop button handler
    private void onStopBtn() {
        mGview.setValues(new float[0]);
        mGview.invalidate();
        mRunning = false;

    }

    private class HMonitorRunnable implements Runnable{
        @Override
        public void run() {
            if(mRunning){
                updateData();
                mGview.setValues(mUptV);
                mGview.invalidate();
                mHandler.postDelayed(this, 200);
            }
        }
    }

    // Update graph data
    private void updateData() {
        Random myRandom = new Random();
        final int BOUNDARY_INT = 9;
        float[] nextUptV = new float[NUM_RECORD_MAX];
        if(mUptV == null || mUptV.length == 0){
            for(int i = 0; i < NUM_RECORD_MAX - 1; i++){
                nextUptV[i] = myRandom.nextInt(BOUNDARY_INT);
            }
        }else{
            final int BATCH_UPDATE = 1; //Optional value!
            for(int i = 0; i < NUM_RECORD_MAX - BATCH_UPDATE; i++){
                nextUptV[i] = mUptV[i + BATCH_UPDATE];
            }
            for(int i = NUM_RECORD_MAX - BATCH_UPDATE; i< NUM_RECORD_MAX; i++){
                nextUptV[i] = myRandom.nextInt(BOUNDARY_INT);
            }

        }
        mUptV = nextUptV;
    }
}
