package com.example.hawx.a01_healthmonitor;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by Hawx on 29/09/2017.
 */

public class AccSensService extends Service implements SensorEventListener {
    public static class AccSensData{
        public long mTStamp; //We should reference the record by primary key's order.
        private double []x = new double [NUM_SAMPLES_PER_ROUND];
        private double []y = new double [NUM_SAMPLES_PER_ROUND];
        private double []z = new double [NUM_SAMPLES_PER_ROUND];
        public int label;
    }

    //By the Spec: The data sampling frequency should be over 10 Hz for the proper accuracy
    public static final int NUM_MICROSEC = 100000; //OFFICIAL
    // public static final int NUM_MICROSEC = 1000000; //FOR TEST
    public static final int NUM_AXIS = 3;
    public static final int NUM_SAMPLE_SECONDS = 5;
    public static final int NUM_SAMPLES_PER_SECOND = (1000000/NUM_MICROSEC);
    public static final int NUM_SAMPLES_PER_ROUND = NUM_SAMPLE_SECONDS*NUM_SAMPLES_PER_SECOND;
    public static final String KEY_TBNAME = "_tbname";
//    public static final String ACC_ACTION = "com.example.hawx.a01_healthmonitor/.AccSensService";
    private SensorManager mAccSensMgr;
    private int sample_cnt = 0;
    private double []_x = new double [NUM_SAMPLES_PER_ROUND];
    private double []_y = new double [NUM_SAMPLES_PER_ROUND];
    private double []_z = new double [NUM_SAMPLES_PER_ROUND];

    private SQLiteDatabase mSDDB;
    private String mTBName = "";
    private static final String TAG = "AccSensService";

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        mAccSensMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        mTBName = intent.getStringExtra(KEY_TBNAME);
        SDSQLiteHelper sddbHelper = new SDSQLiteHelper();
        mSDDB = sddbHelper.getWritableDatabase(mTBName);
        registerSensorListener();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterSensorListener();
    }

    private void registerSensorListener(){
        mAccSensMgr.registerListener(this, mAccSensMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                NUM_MICROSEC);
    }

    private void unregisterSensorListener(){
        mAccSensMgr.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        /*Hawx: If we have time and fun in this assignement , here can be optimized by batch write.*/
        final long now = System.currentTimeMillis();
        _x[sample_cnt] =  sensorEvent.values[0];
        _y[sample_cnt] =  sensorEvent.values[1];
        _y[sample_cnt] =  sensorEvent.values[2];
        Log.d(TAG, String.format("New sensor event"+ (sample_cnt+1) +": %d %f %f %f",
                now,
                sensorEvent.values[0],
                sensorEvent.values[1],
                sensorEvent.values[2]));

        sample_cnt++;
        if(sample_cnt != NUM_SAMPLES_PER_ROUND) {
                return;
        }

        sample_cnt = 0;
        Log.d(TAG, String.format("Write records: ", now));

        AddDBRecord adrcd = new AddDBRecord();

        adrcd.accData.mTStamp = now;
        for(int i = 0; i < NUM_SAMPLES_PER_ROUND; i++) {
            adrcd.accData.x[i] = _x[i];
            adrcd.accData.y[i] = _y[i];
            adrcd.accData.z[i] = _z[i];
        }
        adrcd.execute();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    private class AddDBRecord extends AsyncTask<Void, Void, Void> {
        public AccSensData accData = new AccSensData();
        public int count;
        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, String.format("Adding record into database: %d %f %f %f", accData.mTStamp, accData.x[0], accData.y[0], accData.z[0]));
            ContentValues cv = new ContentValues();
            for(int i = 0; i < NUM_SAMPLES_PER_ROUND; i++) {
                cv.put(SDSQLiteHelper.SDSQLiteSchema.X_FIELD + (i+1), accData.x[i]);
                cv.put(SDSQLiteHelper.SDSQLiteSchema.Y_FIELD + (i+1), accData.y[i]);
                cv.put(SDSQLiteHelper.SDSQLiteSchema.Z_FIELD + (i+1), accData.z[i]);
            }

            mSDDB.beginTransaction();
            mSDDB.insert(mTBName, null, cv);
            mSDDB.setTransactionSuccessful();
            mSDDB.endTransaction();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

        }
    }
}
