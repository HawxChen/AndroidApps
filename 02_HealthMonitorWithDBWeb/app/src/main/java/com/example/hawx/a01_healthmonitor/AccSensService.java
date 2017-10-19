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
        public long mTStamp;
        public double x;
        public double y;
        public double z;
    }

    public static final String KEY_TBNAME = "_tbname";
//    public static final String ACC_ACTION = "com.example.hawx.a01_healthmonitor/.AccSensService";
    private SensorManager mAccSensMgr;
    private static final int NUM_MICROSEC = 1000000; //1 seconds
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

        final long now = System.currentTimeMillis();
        AddDBRecord adrcd = new AddDBRecord();
        adrcd.accData.mTStamp = now;
        adrcd.accData.x = sensorEvent.values[0];
        adrcd.accData.y = sensorEvent.values[1];
        adrcd.accData.z = sensorEvent.values[2];

        Log.d(TAG, String.format("New sensor event: %d %f %f %f",
                now,
                sensorEvent.values[0],
                sensorEvent.values[1],
                sensorEvent.values[2]));

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
        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, String.format("Adding record into database: %d %f %f %f", accData.mTStamp, accData.x, accData.y, accData.z));
            ContentValues cv = new ContentValues();
            cv.put(SDSQLiteHelper.SDSQLiteSchema.TS_FIELD, accData.mTStamp);
            cv.put(SDSQLiteHelper.SDSQLiteSchema.X_FIELD, accData.x);
            cv.put(SDSQLiteHelper.SDSQLiteSchema.Y_FIELD, accData.y);
            cv.put(SDSQLiteHelper.SDSQLiteSchema.Z_FIELD, accData.z);
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
