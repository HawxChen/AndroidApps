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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.util.Log;

import java.util.HashMap;
import java.util.ArrayList;

public class MainActivity extends Activity implements View.OnClickListener {
    private float[] mUptV;
    private GraphView mGview;
    private boolean mRunning = false;
    private Handler mHandler = new Handler();
    private HMonitorRunnable mJob;
    private final int NUM_RECORD_MAX = 100;
    private String mConcatName = "a";
    private String mTableNameCurrentOpen = "";
    private EditText mNameInput;
    private EditText mIDInput;
    private EditText mAgeInput;
    private RadioButton mSexBtn;
    private SDSQLiteHelper sddbhelper;
    private SQLiteDatabase sddb;
    private HashMap<String, Boolean> createdTableName = new HashMap();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Connect listeners
        findViewById(R.id.brun).setOnClickListener(this);
        findViewById(R.id.bstop).setOnClickListener(this);
        findViewById(R.id.bupload).setOnClickListener(this);
        findViewById(R.id.bdownload).setOnClickListener(this);
        mNameInput = findViewById(R.id.nameText);
        mAgeInput = findViewById(R.id.ageText);
        mIDInput = findViewById(R.id.idText);


        // Instantiate the Graph View
        FrameLayout fLayout = findViewById(R.id.framedraw);
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
        mSexBtn = findViewById(radioGroup.getCheckedRadioButtonId());
        SDSQLiteHelper.deleteDB();
        sddbhelper = new SDSQLiteHelper();
        //sddbhelper.createTables(buildConcatTableName());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stoptAccSensService();
        mRunning = false;
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
            case R.id.bupload:
                onUploadBtn();
                break;
            case R.id.bdownload:
                onDownloadBtn();
                break;
            default:
        }
    }

    private String buildConcatTableName() {
         mConcatName = mNameInput.getText().toString() + "_"
                + mIDInput.getText().toString() + "_"
                + mAgeInput.getText().toString() + "_"
                + mSexBtn.getText().toString();
        return mConcatName;
    }

    // Run button handler
    private void onRunBtn() {
        if (mRunning) {
            mHandler.removeCallbacks(mJob);
            mUptV = new float[0];
        }

        buildConcatTableName();

        if(!createdTableName.containsKey(mConcatName)) {
            sddbhelper.createTables(mConcatName);
            createdTableName.put(mConcatName, true);
        }


        if(!mTableNameCurrentOpen.equals(mConcatName)) {
            sddb = sddbhelper.getWritableDatabase(mConcatName);
            mTableNameCurrentOpen = mConcatName;
            startAccSensService();
        }

        mJob = new HMonitorRunnable();
        mRunning = true;
        mHandler.post(mJob);
    }

    private void startAccSensService(){
        stoptAccSensService();
        Intent intent = new Intent(AccSensService.ACC_ACTION);
        intent.setPackage(getPackageName());
        intent.putExtra(AccSensService.KEY_TBNAME, mTableNameCurrentOpen);
        startService(intent);
    }

    private void stoptAccSensService(){
        Intent intent = new Intent(this, AccSensService.class);
        stopService(intent);
    }

    // Stop button handler
    private void onStopBtn() {
        mGview.setValues(new float[0]);
        mGview.invalidate();
        mRunning = false;

    }

    private void onUploadBtn() {
    }

    private void onDownloadBtn() {
    }

    private void redrawView() {
        mGview.setValues(mUptV);
        mGview.invalidate();
    }
    private class HMonitorRunnable implements Runnable{
        @Override
        public void run() {
            if(mRunning){
                new RedrawJob().execute();
            }
        }
    }

    // Update graph data
    private void updateAccValues(ArrayList<AccSensService.AccSensData> accData) {
        int len = accData.size();
        float[] values = new float[len * 3];
        for(int i = 0; i < len; i++){
            AccSensService.AccSensData tmp = accData.get(i);
            values[i * 3 ] = (float) tmp.x;
            values[i * 3 + 1] = (float) tmp.y;
            values[i * 3 + 2] = (float) tmp.z;
        }
        mUptV = values;
    }

    private static AccSensService.AccSensData translateRecord(Cursor cursor){
        AccSensService.AccSensData ret_data = new AccSensService.AccSensData();
        ret_data.x = cursor.getDouble(cursor.getColumnIndex(SDSQLiteHelper.SDSQLiteSchema.X_FIELD));
        ret_data.y = cursor.getDouble(cursor.getColumnIndex(SDSQLiteHelper.SDSQLiteSchema.Y_FIELD));
        ret_data.z = cursor.getDouble(cursor.getColumnIndex(SDSQLiteHelper.SDSQLiteSchema.Z_FIELD));

        return  ret_data;
    }

    private class RedrawJob extends AsyncTask<Void, Void, ArrayList<AccSensService.AccSensData>> {
        @Override
        protected ArrayList<AccSensService.AccSensData> doInBackground(Void... params) {
            return readfromTable();
        }

        @Override
        protected void onPostExecute(ArrayList<AccSensService.AccSensData> sensorDatas) {
            updateAccValues(sensorDatas);
            redrawView();
            mHandler.postDelayed(mJob, 1000);
        }

        private  ArrayList<AccSensService.AccSensData>  readfromTable() {
            // Read the prior ten second data
            final int PREV_10_SECS = 10;
            long now = System.currentTimeMillis();
            final long startingTS = now - PREV_10_SECS * 1000;
            Cursor cursor =  sddb.query(
                    mTableNameCurrentOpen,
                    new String[]{SDSQLiteHelper.SDSQLiteSchema.X_FIELD,
                            SDSQLiteHelper.SDSQLiteSchema.Y_FIELD,
                            SDSQLiteHelper.SDSQLiteSchema.Z_FIELD,
                            SDSQLiteHelper.SDSQLiteSchema.TS_FIELD},
                    SDSQLiteHelper.SDSQLiteSchema.TS_FIELD + " >= ?",
                    new String[]{String.valueOf(startingTS)},
                    null,
                    null,
                    SDSQLiteHelper.SDSQLiteSchema.TS_FIELD + " ASC"
            );

            if(cursor == null) return new ArrayList<>(0);

            final int INIT_CAPACITY = PREV_10_SECS + 5;
            ArrayList<AccSensService.AccSensData> dataRecord = new ArrayList<>(INIT_CAPACITY);
            if(cursor.moveToFirst()){
                do{
                    AccSensService.AccSensData data = translateRecord(cursor);
                    dataRecord.add(data);
                }while (cursor.moveToNext());
            }
            cursor.close();
            Log.e("DEBUG", "Record Size:" + dataRecord.size());
            return dataRecord;
        }
    }
}
