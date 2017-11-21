//
//
// Class: CSE535, Fall 2017
// Assignment 2
// Group 25
//
// This is the main activity in the assignment. It presents the required
// user interface and handles the necessary functionality.
//
//

package com.example.hawx.a01_healthmonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.util.Log;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.io.FileInputStream;
import java.io.DataOutputStream;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;
import static com.example.hawx.a01_healthmonitor.R.raw.fakegroup25;



public class MainActivity extends Activity implements View.OnClickListener {
    private boolean mRunning = false;
    private String mConcatName = "a";
    private String mTableNameCurrentOpen = "";
    private EditText mNameInput;
    private SDSQLiteHelper sddbhelper;
    private SQLiteDatabase sddb;
    private static final String TAG  = "MainActivity";
    private static final String UP_URL = "http://10.218.110.136/CSE535Fall17Folder/UploadToServer.php";
    //private static final String UP_URL = "https://192.168.0.17/CSE535Fall17Folder/UploadToServer.php";
    private static final String DOWN_URL = "http://10.218.110.136/CSE535Fall17Folder/Group25.db";
    //private static final String DOWN_URL = "https://192.168.0.17/CSE535Fall17Folder/Group25.db";
    private static final String DOWNLOAD_FILENAME = "CSE535_ASSIGNMENT2_Extra";
    private boolean isUploading = false;
    private Button bRecord;
    private Button bAnalyzing;
    private Button bOffload;
    private Button bDraw3D;
    private Button bcleanTable;
    private TextView nr_foldText;
    private TextView cross_validationText;
    private TextView resulText;
    private AlertDialog recordDialog, analyzingDialog;
    private static final boolean OFFICIAL_RELEASE = false;

    private static final String svm_dir_path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "CSE535_ASSIGNMENT2";
    private static final String svm_model_file_name = "svm_model.txt";
    private static final String svm_dup_db_name = "svm_fakedb.db";
    private static final String svm_raw_training_file_name = "svm_training_data.txt";
    private static final String svm_raw_testing_file_name = "svm_testing_data.txt";
    private static final String svm_dup_db_abs_path = svm_dir_path + File.separator + svm_dup_db_name;
    private static final String svm_raw_training_file_abs_path = svm_dir_path + File.separator + svm_raw_training_file_name;
    private static final String svm_raw_testing_file_abs_path = svm_dir_path + File.separator + svm_raw_testing_file_name;
    private static final String svm_model_file_abs_path = svm_dir_path + File.separator + svm_model_file_name;

    private static boolean bSvm_trained = false;
    private CountDownTimer analyzingTimer;
    private svm_train train_inst = new svm_train();
    private svm_scale scale_inst = new svm_scale();
    public static double mainAccuracy = 0;



    //
    // onCreate
    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Connect listeners
        bRecord = (Button)findViewById(R.id.brecord);
        bAnalyzing = (Button)findViewById(R.id.banalyzing);
        bOffload = (Button)findViewById(R.id.boffload);
        bDraw3D = (Button)findViewById(R.id.bdraw3d);
        bcleanTable = (Button)findViewById(R.id.bcleantable);

        bRecord.setOnClickListener(this);
        bAnalyzing.setOnClickListener(this);
        bOffload.setOnClickListener(this);
        bDraw3D.setOnClickListener(this);
        bcleanTable.setOnClickListener(this);

        resulText = findViewById(R.id.ResultText);
        nr_foldText = findViewById(R.id.nr_foldview);
        cross_validationText = findViewById(R.id.validationView);

        mNameInput = findViewById(R.id.nameText);

        SDSQLiteHelper.deleteDB();
        sddbhelper = new SDSQLiteHelper();

        /* Official Release  */
        if(OFFICIAL_RELEASE) {
            bcleanTable.setEnabled(false);
        }

    }

    //
    // onDestroy
    //
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAccSensService();
        mRunning = false;
    }

    //
    // Handle on-click events for Run/Stop buttons
    //
    @Override
    public void onClick(View vinfo) {
        switch (vinfo.getId()){
            case R.id.brecord:
                onRecordBtn();
                break;
            case R.id.banalyzing:
                onAnalyzingBtn();
                break;
            case R.id.boffload:
                onOffloaddBtn();
                break;
            case R.id.bdraw3d:
                onDraw3DBtn();
                break;
            case R.id.bcleantable:
                onCleanDBtn();
            default:
        }
    }

    private String buildConcatTableName() {
        mConcatName = mNameInput.getText().toString() + "_";

        mConcatName = mConcatName.replaceAll("\\s+", "");
        return mConcatName;
    }

    //
    // Record button handler
    //
    private void onRecordBtn() {
        Log.d(TAG, "onRunBtn");

        if (mRunning) {
            return;
        }

        buildConcatTableName();

        sddbhelper.createTables(mConcatName);
        sddb = sddbhelper.getWritableDatabase(mConcatName);
        mTableNameCurrentOpen = "\"" + mConcatName + "\"";
        startAccSensService();

        mRunning = true;
        showRecordWaiting();
    }

    //
    // Analyzing button handler
    //
    private void onAnalyzingBtn() {
        if(!mRunning){
            return;
        }
        mRunning = false;
        stopAccSensService();
        showAnalyziing();
    }

    private void onCleanDBtn() {

    }
    private void onOffloaddBtn() {

    }
    private void onDraw3DBtn() {

    }

    //
    // Start accelerometre services
    //
    private void startAccSensService(){
        Log.d(TAG, "Starting AccSensService");
        stopAccSensService();
        Intent intent = new Intent(this, AccSensService.class);
        intent.putExtra(AccSensService.KEY_TBNAME, mTableNameCurrentOpen);
        startService(intent);
    }

    //
    // Stop accelerometre services
    //
    private void stopAccSensService(){
        Intent intent = new Intent(this, AccSensService.class);
        stopService(intent);
    }

    //https://developer.android.com/reference/android/os/CountDownTimer.html
    private void showRecordWaiting() {
        AlertDialog.Builder recordshow = new AlertDialog.Builder(this);
        recordshow.setCancelable(false);
        recordshow.setMessage("Start Sensor & Collect data for at least 5 seconds!!!");

        recordDialog = recordshow.create();
        recordDialog.show();
        new CountDownTimer(6*1000, 1000) {
            public void onTick(long mills) {
                Log.e(TAG, "showRecordWaiting -- Seconds remaining: " + mills/1000);
            }
            public void onFinish(){
                Log.e(TAG, "showRecordWaiting -- Done! ");

                recordDialog.dismiss();
            }
        }.start();

    }

    private void showAnalyziing() {
        AlertDialog.Builder recordshow = new AlertDialog.Builder(this);
        recordshow.setCancelable(false);
        recordshow.setMessage("Stopped Sensor and Analyzing.....!!!");

        analyzingDialog = recordshow.create();
        analyzingDialog.show();
        analyzingTimer = new CountDownTimer(10000*1000, 1000) {
            boolean firstTime = true;
            public void onTick(long mills) {
                Log.e(TAG, "showAnalyziing -- Seconds remaining: " + mills/1000);
                if(firstTime) {
                    new AnalyzingTask().execute();
                    firstTime = false;
                }
            }
            public void onFinish(){
                Log.e(TAG, "showAnalyziing -- Stop Analyzing! ");
            }
        };
        analyzingTimer.start();

    }
    private boolean checkFileExist(String file_name) {
        String check_file_name = svm_dir_path + File.separator + file_name;
        Log.e(TAG, "Check " + check_file_name + " existing?!?!?");

        return new File(file_name).exists();
    }
    //

    // Load a sample object with data from an SQL query result
    //
    private static AccSensService.AccSensData translateRecord(Cursor cursor){
        AccSensService.AccSensData ret_data = new AccSensService.AccSensData();
        for(int i = 1; i <= AccSensService.NUM_SAMPLES_PER_ROUND; i++) {
            ret_data.x[i-1] = cursor.getDouble(cursor.getColumnIndex(SDSQLiteHelper.SDSQLiteSchema.X_FIELD+i));
            ret_data.y[i-1] = cursor.getDouble(cursor.getColumnIndex(SDSQLiteHelper.SDSQLiteSchema.Y_FIELD+i));
            ret_data.z[i-1] = cursor.getDouble(cursor.getColumnIndex(SDSQLiteHelper.SDSQLiteSchema.Z_FIELD+i));
        }

        ret_data.label = (int) cursor.getDouble(cursor.getColumnIndex(SDSQLiteHelper.SDSQLiteSchema.LABEL_FIELD));


        return ret_data;
    }
    private boolean allRecordtoString(Cursor cursor, StringBuilder strBuilder) {
        if(cursor.moveToFirst()){
            do{
                AccSensService.AccSensData data = translateRecord(cursor);
                strBuilder.append(Integer.toString(data.label));
                for(int i = 0; i < AccSensService.NUM_SAMPLES_PER_ROUND; i++) {
                    strBuilder.append(" " + (3*(i+1) -2) + ":" + data.x[i] + " "
                                     + (3*(i+1) -1) + ":" + data.y[i] + " "
                                     + 3*(i+1) + ":" + data.z[i]
                    );
                }
                strBuilder.append("\n");
            }while (cursor.moveToNext());
        }
        return true;
    }

    private boolean IN_OUT_stream(InputStream is, FileOutputStream fos) {
        byte[] buf = new byte[8192];
        int cnt;
        try{
            while ((cnt = is.read(buf)) > 0) {
                fos.write(buf, 0, cnt);
            }
            is.close();
            fos.flush();
            fos.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    private boolean duplicateRawDb(String output_db_name) {
        Log.e(TAG, "Duplicated path is: " + output_db_name);
        try{
            IN_OUT_stream(getResources().openRawResource(fakegroup25),new FileOutputStream(new File(output_db_name), false));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
    private boolean writeRecordToFile(StringBuilder strBuilder, String output_file_abs_path) {
        File outputFile = new File(output_file_abs_path);
        try{
            IN_OUT_stream(new ByteArrayInputStream(strBuilder.toString().getBytes(StandardCharsets.UTF_8)), new FileOutputStream(outputFile));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    private boolean translateDBtoFile (String db_abs_path, String output_file_abs_path) {
        String[] queryFields = SDSQLiteHelper.rowsymbolList.toArray(new String[SDSQLiteHelper.rowsymbolList.size()+1]);
        queryFields[SDSQLiteHelper.rowsymbolList.size()] = new String(SDSQLiteHelper.SDSQLiteSchema.LABEL_FIELD);

        SQLiteDatabase local_db = SQLiteDatabase.openDatabase(db_abs_path, null, SQLiteDatabase.OPEN_READWRITE);
        local_db.beginTransaction();
        Cursor cursor =  local_db.query(
                mTableNameCurrentOpen,
                queryFields,
                null,
                null,
                null,
                null,
                SDSQLiteHelper.SDSQLiteSchema.INCREASE_ID + " DESC", // Order by timestamps (take most recent first (descending))
                Integer.toString(AccSensService.NUM_TOTAL_TIMES_PER_TRAIN) // At least 60 activities.
        );
        local_db.setTransactionSuccessful();
        local_db.endTransaction();

        if(cursor == null) return false;

        StringBuilder strBuilder = new StringBuilder();
        allRecordtoString(cursor, strBuilder);
        writeRecordToFile(strBuilder, svm_raw_training_file_abs_path);

        cursor.close();
        local_db.close();
        return true;
    }

    private class AnalyzingTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();;

            return;
        }

        @Override
        protected void onPostExecute(Void voids) {

        }

        @Override
        protected Void doInBackground(Void... voids) {
            //Check if we have trained data first!
            Log.e(TAG, "AnalyzingTask: doInBackground!!!!! ");
            if (true || !checkFileExist(svm_raw_training_file_name)) { //testing version
                // if(!checkFileExist(svm_raw_training_file_name)) { //Official version
                //if not, just train it!
                Log.e(TAG, "AnalyzingTask: Training!!!!! ");
                duplicateRawDb(svm_dup_db_abs_path);
                translateDBtoFile(svm_dup_db_abs_path, svm_raw_training_file_abs_path);

                String[] trainParam = {svm_raw_training_file_abs_path, svm_model_file_abs_path};
                String[] scaleParam = {svm_raw_training_file_abs_path};
                try {
                    scale_inst.main(scaleParam);
                    train_inst.main(trainParam);
                    publishProgress();
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        protected void onProgressUpdate(Void... voids) {
            Log.e(TAG, "AnalyzingTask: onProgressUpdate");
            resulText.setText(" Accuracy is: !!! " + Double.toString(mainAccuracy));
            nr_foldText.setText("K-fold: " + train_inst.nr_fold);
            cross_validationText.setText("Cross Validation: " + train_inst.cross_validation);
            analyzingDialog.dismiss();
            analyzingTimer.cancel();
        }

    }



        // End of Assignment 3
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================
    //=============================================================================================

    private float[] mUptV;
    private Handler mHandler = new Handler();
    private HMonitorRunnable mJob;
    private final int NUM_RECORD_MAX = 100;
    //
    // Background task to upload the database to the remote server
    //

    //
    // Upload button handler
    //
    private void onUploadBtn() {
        Log.d(TAG, "onUploadBtn");

        if(isUploading) {
            // Already uploading, wait for timeout before attempting to upload again...
            Toast.makeText(getApplicationContext(), "Upload already in progress!", Toast.LENGTH_SHORT).show();
            return;
        }

        //
        // Check for basic network connectivity
        //
        try {
            ConnectivityManager connMgrCheck = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = connMgrCheck.getActiveNetworkInfo();
            if(!netInfo.isConnected() || null == netInfo) {
                Log.e(TAG, "!!!!!!!!!!! Network Error !!!!!!!!!!!!!!!!");
                Toast.makeText(getApplicationContext(), "Error: Network not connected!", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Network ok");

        //
        // Ensure database has been created
        //
        if(!(new File(sddbhelper.get_db_path())).exists()) {
            Log.e(TAG, "!!!!!!!!!!! DB doesn't EXIST !!!!!!!!!!!!!!!!");
            Toast.makeText(getApplicationContext(), "Error: Database does not yet exist!", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Database does exist...");

        //
        // Begin uploading database in the background
        //
        Toast.makeText(getApplicationContext(), "Starting Upload...", Toast.LENGTH_SHORT).show();
        isUploading = true;
        new UploadDBTask().execute();
    }
    private class UploadDBTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Boolean Result) {
            if (Result) {
                Toast.makeText(getApplicationContext(), "Upload successful!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Upload failed!", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {

            boolean result = false;
            try {
                result =doUploadDB();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        //Handle SSL and HTTP connection
        private HttpURLConnection returnHttpSSLConn () throws Exception {
            // How to use SSL and X509TrustManager
            //Reference: https://www.programcreek.com/java-api-examples/javax.net.ssl.X509TrustManager
            //Reference: http://pankajmalhotra.com/Skip-SSL-HostName-Verification-Java-HttpsURLConnection
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }});

            SSLContext sslctxt = null;
            sslctxt = SSLContext.getInstance("TLS");
            //Reference: http://www.javased.com/index.php?api=java.security.cert.X509Certificate
            //Reference: Follow the naming of parameters
            sslctxt.init(null,  new X509TrustManager[]{new X509TrustManager(){
                public void checkClientTrusted(X509Certificate[] chain, String authType)  {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0]; }}}, new SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sslctxt.getSocketFactory());
            return (HttpURLConnection) new URL(UP_URL).openConnection();
        }

        private boolean doUploadDB() throws Exception {
            final String mark_boundary = "XXXXXXXXXXXXX";
            HttpURLConnection httpSSLconn = returnHttpSSLConn();

            buildConnection(httpSSLconn, mark_boundary);
            DataOutputStream httpPacket = new DataOutputStream(httpSSLconn.getOutputStream());

            // Write Headers
            httpPacket.writeBytes("--" + mark_boundary + "\r\n");
            httpPacket.writeBytes("Content-Disposition: form-data; name=\"" +
                    "uploaded_file" + "\";filename=\"" +
                    sddbhelper.get_db_name() + "\"\r\n\r\n");

            // Write data
            FileInputStream db_file = new FileInputStream(sddbhelper.get_db_path());
            byte[] output_buf = new byte[4096];
            int cnt = 0;
            while((cnt = db_file.read(output_buf)) > 0){
                httpPacket.write(output_buf, 0, cnt);
            }

            // Write Endings
            httpPacket.writeBytes("\r\n--" + mark_boundary + "--\r\n");;

            // Flush and close the connection
            httpPacket.flush();
            httpPacket.close();
            final int status = httpSSLconn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                Log.e("uploadDb", "Failed with http status: " + status);
                isUploading = false;
                return false;
            }

            Log.e(TAG, "Finish Upload!!!");
            isUploading = false;
            return true;
        }

        // Function template
        // https://mttkay.github.io/blog/2013/03/02/herding-http-requests-or-why-your-keep-alive-connection-may-be-dead/
        private void buildConnection(HttpURLConnection conn, String mark_boundary) throws Exception {
            final int connectTimeout = 50000;
            final int readTimeout = 50000;
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Cache-Control", "no-cache");
            //boundary usage: https://stackoverflow.com/questions/11766878/sending-files-using-post-with-httpurlconnection
            conn.setRequestProperty(
                    "Content-Type", "multipart/form-data;boundary=" + mark_boundary);
        }
    }

    //
    // Download button handler
    //
    private void onDownloadBtn() {
        // Stop if we are already running (simply use the stop button handler)

        //
        // Check for basic network connectivity
        //
        try {
            ConnectivityManager connMgrCheck = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = connMgrCheck.getActiveNetworkInfo();
            if(!netInfo.isConnected() || null == netInfo) {
                Log.e(TAG, "!!!!!!!!!!! Network Error !!!!!!!!!!!!!!!!");
                Toast.makeText(getApplicationContext(), "Error: Network not connected!", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Network ok");

        //
        // Begin uploading database in the background
        //
        Toast.makeText(getApplicationContext(), "Starting Download...", Toast.LENGTH_SHORT).show();
        new DownloadDBTask().execute();
    }

    //
    // Background task to download the database from the remote server
    //
    private class DownloadDBTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
        }

        // Genreic file copy method referenced from https://stackoverflow.com/questions/9292954/how-to-make-a-copy-of-a-file-in-android
        public void copy(String src, String dst) throws IOException {
            InputStream in = new FileInputStream(src);
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
            } finally {
                in.close();
            }
        }

        @Override
        protected void onPostExecute(Boolean Result) {
            if (Result) {
                Toast.makeText(getApplicationContext(), "Download successful!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Download failed!", Toast.LENGTH_SHORT).show();
            }

            // Now copy downloaded database to expected location
            sddbhelper.closeDB();

            String src_path = Environment.getExternalStorageDirectory().getPath()+"/"+DOWNLOAD_FILENAME;
            String dest_path = sddbhelper.get_db_path();
            Log.d(TAG, "Copying " + src_path + " to " + dest_path);
            try {
                copy(src_path, dest_path);
            } catch (Exception e) {
                Log.d(TAG, "Error! Failed to copy!");
                e.printStackTrace();
            }

            Log.d(TAG, "Ok! Reloading database");

            // Re-load database to draw it
            sddbhelper = new SDSQLiteHelper();
            buildConcatTableName();
            sddbhelper.createTables(mConcatName);
            mTableNameCurrentOpen = "\"" + mConcatName + "\"";
            sddb = sddbhelper.getWritableDatabase(mConcatName);

            // Draw it once (per assignment spec)
            // new RedrawJob().execute();
        }

        @Override
        protected Boolean doInBackground(String... params) {

            boolean result = false;
            try {
                result = doDownloadDB();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        //Handle SSL and HTTP connection
        private HttpURLConnection returnHttpSSLConn () throws Exception {
            // How to use SSL and X509TrustManager
            //Reference: https://www.programcreek.com/java-api-examples/javax.net.ssl.X509TrustManager
            //Reference: http://pankajmalhotra.com/Skip-SSL-HostName-Verification-Java-HttpsURLConnection
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }});

            SSLContext sslctxt = null;
            sslctxt = SSLContext.getInstance("TLS");
            //Reference: http://www.javased.com/index.php?api=java.security.cert.X509Certificate
            //Reference: Follow the naming of parameters
            sslctxt.init(null,  new X509TrustManager[]{new X509TrustManager(){
                public void checkClientTrusted(X509Certificate[] chain, String authType)  {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0]; }}}, new SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sslctxt.getSocketFactory());
            return (HttpURLConnection) new URL(DOWN_URL).openConnection();
        }

        private Boolean doDownloadDB() throws Exception {
            InputStream input = null;
            FileOutputStream output = null;
            HttpURLConnection connection = null;
            Log.d(TAG, "doDownloadDB");

            try {
                // Open HTTPS connection to server
                Log.d(TAG, "Trying to open connection...");
                connection = returnHttpSSLConn();
                connection.connect();
                Log.d(TAG, "Connected!");

                // Check response code from server
                if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                    Log.d(TAG,  "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage());
                    return false;
                }

                int fileLength = connection.getContentLength();
                Log.d(TAG, String.format("File length is %d bytes", fileLength));

                // Download the file
                input = connection.getInputStream();
                output = new FileOutputStream(Environment.getExternalStorageDirectory().getPath()+"/"+DOWNLOAD_FILENAME);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }

            Log.e(TAG, "Finished Download!!!");
            return true;
        }
    }


    //
    // Redraw view
    //
    private void redrawView() {

    }

    //
    // Runnable to update the view
    //
    private class HMonitorRunnable implements Runnable{
        @Override
        public void run() {
            if(mRunning){
                // new RedrawJob().execute();
            }
        }
    }

    //
    // Update graph data
    //
    private void updateAccValues(ArrayList<AccSensService.AccSensData> accData) {
        int len = accData.size();
        float[] values = new float[len * 3];
        for(int i = 0; i < len; i++){
            AccSensService.AccSensData tmp = accData.get(i);
            values[i * 3 ] = (float) tmp.x[i];
            values[i * 3 + 1] = (float) tmp.y[i];
            values[i * 3 + 2] = (float) tmp.z[i];
        }
        mUptV = values;
    }



    //
    // Background task to fetch and draw the last ten seconds of samples
    //
    private class RedrawJob extends AsyncTask<Void, Void, ArrayList<AccSensService.AccSensData>> {
        @Override
        protected ArrayList<AccSensService.AccSensData> doInBackground(Void... params) {
            return readfromTable();
        }

        // Redraw view after the samples have been retrievde
        @Override
        protected void onPostExecute(ArrayList<AccSensService.AccSensData> sensorDatas) {
            updateAccValues(sensorDatas);
            redrawView();
            mHandler.postDelayed(mJob, 1000);
        }

        // Load last ten seconds of samples from database
        private ArrayList<AccSensService.AccSensData> readfromTable() {
            // Read the prior ten second data
            sddb.beginTransaction();
            Cursor cursor =  sddb.query(
                    mTableNameCurrentOpen,
                    SDSQLiteHelper.rowsymbolList.toArray(new String[0]),
                    null,
                    null,
                    null,
                    null,
                    SDSQLiteHelper.SDSQLiteSchema.INCREASE_ID + " DESC", // Order by timestamps (take most recent first (descending))
                    "1" // Limit last 10 seconds
            );
            sddb.setTransactionSuccessful();
            sddb.endTransaction();

            if(cursor == null) return new ArrayList<>(0);

            ArrayList<AccSensService.AccSensData> dataRecord = new ArrayList<>(0);
            if(cursor.moveToFirst()){
                do{
                    AccSensService.AccSensData data = translateRecord(cursor);
                    dataRecord.add(0, data);
                }while (cursor.moveToNext());
            }
            cursor.close();
            Log.e("DEBUG", "Record Size:" + dataRecord.size());
            return dataRecord;
        }
    }
}
