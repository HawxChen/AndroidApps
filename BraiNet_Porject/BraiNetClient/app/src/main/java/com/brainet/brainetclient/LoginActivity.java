package com.brainet.brainetclient;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.BatteryManager;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.support.annotation.IntegerRes;
import android.support.v7.app.AppCompatActivity;

import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    class ServerStatus {
        public String       addr;
        public String       name;
        public Boolean      active;
        public Boolean      request_successful;
        public long         network_delay;
        public long         computation_time;
        public long         alltime;
        public long         response_time;
        public TextView     network_delay_view;
        public TextView     comptuation_time_view;
        public TextView     heading_view;
    }

    private String mServerPref = "";
    private String signalFilePath;

    private final static String TAG = "LoginActivity";

    private UserLoginTask  mAuthTask = null;
    private ServerTestTask mServerTestTask = null;
    private ServerStatus   mRemoteServerStatus;
    private ServerStatus   mFogServerStatus;
    private ServerStatus   mRunningServerStatus;


    // UI references.
    private AutoCompleteTextView mUsernameView;
    private Button               mSignalAcquisitionButton;
    private View                 mProgressView;
    private View                 mLoginFormView;
    private TextView             mServerStatusView;
    private TextView             mBatteryView;


    // Activity response requests IDs
    public final static int REQUEST_UPDATE_SETTINGS = 1;
    public final static int REQUEST_SIGNAL_ACQUIRED = 2;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_login_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
                startActivityForResult(intent, REQUEST_UPDATE_SETTINGS);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    protected void updatePreferences() {
        Log.d(TAG, "Settings updated:");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String remote_server_addr = prefs.getString("remote_server_addr", "");
        String fog_server_addr = prefs.getString("fog_server_addr", "");
        String server_pref = prefs.getString("server_preference", "");

        Log.d(TAG, String.format("Remote Server Addr: %s", remote_server_addr));
        Log.d(TAG, String.format("Fog Server Addr: %s", fog_server_addr));
        Log.d(TAG, String.format("Server Preference: %s", server_pref));

        mServerPref = server_pref;

        mRemoteServerStatus.addr = remote_server_addr;
        mRemoteServerStatus.active = server_pref.equals("remote") || server_pref.equals("auto");
        mRemoteServerStatus.request_successful = false;
        mRemoteServerStatus.response_time = 0;

        mFogServerStatus.addr = fog_server_addr;
        mFogServerStatus.active = server_pref.equals("fog") || server_pref.equals("auto");
        mFogServerStatus.request_successful = false;
        mFogServerStatus.response_time = 0;



        /* Test the servers */
        mServerStatusView.setText("Testing Servers...");
        mServerTestTask = new ServerTestTask();
        mServerTestTask.execute((Void) null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_UPDATE_SETTINGS:
                /* Settings were updated */
                updatePreferences();
                break;

            case REQUEST_SIGNAL_ACQUIRED:
                Log.d(TAG, "Signal acquisition finished!");
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mUsernameView = (AutoCompleteTextView) findViewById(R.id.username);
        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
        mSignalAcquisitionButton = (Button) findViewById(R.id.signal_acquisition_button);
        mSignalAcquisitionButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                acquireSignal();
            }
        });

        mRemoteServerStatus = new ServerStatus();
        mFogServerStatus = new ServerStatus();

        mLoginFormView = findViewById(R.id.login_form);


        mProgressView = findViewById(R.id.login_progress);
        mServerStatusView = (TextView) findViewById(R.id.server_status);
        mBatteryView = (TextView) findViewById(R.id.BatteryLevel_ID);

        mRemoteServerStatus.name = "Remote Server - all processing time: ";
        mRemoteServerStatus.comptuation_time_view = (TextView) findViewById(R.id.remoteCompt_ID);
        mRemoteServerStatus.network_delay_view = (TextView) findViewById(R.id.remoteDelay_id);
        mRemoteServerStatus.heading_view = (TextView) findViewById(R.id.RemoteServer_ID);

        mFogServerStatus.name = "Fog Server - all processing time: ";
        mFogServerStatus.comptuation_time_view = (TextView) findViewById(R.id.fogCompt_ID);
        mFogServerStatus.network_delay_view = (TextView) findViewById(R.id.fogDelay_id);
        mFogServerStatus.heading_view = (TextView) findViewById(R.id.FogServer_ID);

        signalFilePath = Environment.getExternalStorageDirectory() + File.separator + "signal.txt";

        updatePreferences();
    }

    /*
     * Called when the "Start Signal Acquisition" button is pressed. Starts the Signal Acquistion
     * activity to capture EEG signal.
     */
    private void acquireSignal() {
        Intent intent = new Intent(getBaseContext(), SignalAcquisitionActivity.class);
        startActivityForResult(intent, REQUEST_SIGNAL_ACQUIRED);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mSignalAcquisitionButton.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();

        boolean cancel = false;
        View focusView = mUsernameView;

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        } else if (!isUsernameValid(username)) {
            mUsernameView.setError(getString(R.string.error_invalid_username));
            focusView = mUsernameView;
            cancel = true;
        }

        //
        // Get signal data
        //
        if (!(new File(signalFilePath).exists())) {
            // Error! Could not load signal file from SD Card!
            focusView = mSignalAcquisitionButton;

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Could not find signal file in SD Card! Expected it to be at " + signalFilePath);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
//                    finish();
                }
            });
            builder.show();
            return;
        }

        //
        // Check server
        //
        Boolean useRemote = false;
        Boolean useFog = false;

        if (mServerPref.equals("auto")) {
            // Pick server which is available and has lowest latency
            useRemote = mRemoteServerStatus.request_successful;
            useFog = mFogServerStatus.request_successful;
        } else if (mServerPref.equals("remote")) {
            useRemote = mRemoteServerStatus.request_successful;
        } else if (mServerPref.equals("fog")) {
            useFog = mFogServerStatus.request_successful;
        }

        //
        // Break the tie based on server latency
        //
        if (useRemote && useFog) {
            useFog = mFogServerStatus.response_time < mRemoteServerStatus.response_time;
            useRemote = !useFog;

            Toast.makeText(getApplicationContext(),
                    String.format("Selecting %s server based on response time...", useFog ? "Fog" : "Remote"),
                    Toast.LENGTH_SHORT).show();
        }

        if (useRemote) {
            mRunningServerStatus = mRemoteServerStatus;
        } else if (useFog) {
            mRunningServerStatus = mFogServerStatus;
        } else {
            // No available server!
            Toast.makeText(getApplicationContext(), "Error! No available server.", Toast.LENGTH_SHORT).show();
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);

            String serverUrl = String.format("http://%s/login", mRunningServerStatus.addr);
            mAuthTask = new UserLoginTask(username, serverUrl, signalFilePath);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isUsernameValid(String username) {
        //TODO: Replace this with your own logic
        return true;
    }

    //https://stackoverflow.com/questions/15746709/get-battery-level-only-once-using-android-sdk
    public float getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if(level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float)level / (float)scale) * 100.0f;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUsername;
        private final String mServerUrl;
        private final String mSignalFilePath;
        private float start_battery_level = Float.MIN_VALUE;
        private float end_battery_level = Float.MIN_VALUE;

        /*
         * Constructor
         */
        UserLoginTask(String username, String serverUrl, String signalFilePath) {
            mUsername = username;
            mServerUrl = serverUrl;
            mSignalFilePath = signalFilePath;
        }

        @Override
        protected void onPreExecute() {
            start_battery_level = getBatteryLevel();
        }
        /*
         * Main background task
         */
        @Override
        protected Boolean doInBackground(Void... params) {

            boolean result = false;
            try {
                result = doLogin();
            } catch (Exception e) {
                e.printStackTrace();
            }
            end_battery_level = getBatteryLevel();
            return result;
        }

        /*
         * Login has finished (may not be successful)
         */
        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                Toast.makeText(getApplicationContext(), "Login successful!", Toast.LENGTH_SHORT).show();
//                finish();
            } else {
                Toast.makeText(getApplicationContext(), "Login failed!", Toast.LENGTH_SHORT).show();
                mSignalAcquisitionButton.setError(getString(R.string.error_incorrect_password));
                mSignalAcquisitionButton.requestFocus();
            }
            mBatteryView.setText("BatteryLevel: " + "("+ Float.toString(start_battery_level) +","+ Float.toString(end_battery_level) +")");
            mRunningServerStatus.network_delay_view.setText("\t\tNetwork Delay: " + Long.toString(mRunningServerStatus.network_delay));
            mRunningServerStatus.comptuation_time_view.setText("\t\tComputation Time: " + Long.toString(mRunningServerStatus.computation_time));
            mRunningServerStatus.heading_view.setText(mRunningServerStatus.name + Long.toString(mRunningServerStatus.alltime));

        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
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
            return (HttpURLConnection) new URL(mServerUrl).openConnection();
        }

        private HttpURLConnection writeMsg() throws Exception {

            HttpURLConnection conn = returnHttpSSLConn();
            final String mark_boundary = "XXXXXXXXXXXXX";
            final int connectTimeout = 5000;
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
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + mark_boundary);

            DataOutputStream httpPacket = new DataOutputStream(conn.getOutputStream());

            /* Request looks like this:

                ------WebKitFormBoundaryQAN2O0TwVBTFDVBc
                Content-Disposition: form-data; name="username"

                Matt
                ------WebKitFormBoundaryQAN2O0TwVBTFDVBc
                Content-Disposition: form-data; name="signal"; filename="S001R03_22.txt"
                Content-Type: text/plain


                ------WebKitFormBoundaryQAN2O0TwVBTFDVBc
                Content-Disposition: form-data; name="Login"

                Submit
                ------WebKitFormBoundaryQAN2O0TwVBTFDVBc--

             */

            //
            // Write request payload
            //

            // Write Username
            httpPacket.writeBytes("--" + mark_boundary + "\r\n");
            httpPacket.writeBytes("Content-Disposition: form-data; name=\"username\"\r\n\r\n");
            httpPacket.writeBytes(String.format("%s\r\n", mUsername));

            // Write Submit
            httpPacket.writeBytes("--" + mark_boundary + "\r\n");
            httpPacket.writeBytes("Content-Disposition: form-data; name=\"Login\"\r\n\r\n");
            httpPacket.writeBytes("Submit\r\n");

            httpPacket.writeBytes("--" + mark_boundary + "\r\n");
            httpPacket.writeBytes("Content-Disposition: form-data; name=\"signal\"; filename=\"signal.txt\"\r\n\r\n");

            // Write signal data
            FileInputStream db_file = new FileInputStream(mSignalFilePath);
            byte[] output_buf = new byte[4096];
            int cnt;
            while((cnt = db_file.read(output_buf)) > 0){
                httpPacket.write(output_buf, 0, cnt);
            }

            // Write Endings
            httpPacket.writeBytes("\r\n--" + mark_boundary + "--\r\n");

            // Flush and close the connection
            httpPacket.flush();
            httpPacket.close();
            return conn;
        }

        private boolean doLogin() throws Exception {
            Log.d(TAG, "doLogin()");


            long time_start = System.currentTimeMillis();
            HttpURLConnection conn = writeMsg();

            //On the Emulator, the value will be ZERO becuase of the emulator's slow virtual timer.
            mRunningServerStatus.network_delay =  System.currentTimeMillis() - time_start;

            final int status = conn.getResponseCode();
            Log.e(TAG, "STATUS: " + Integer.toString(status));


            mRunningServerStatus.computation_time = getComputationTime(conn);
            mRunningServerStatus.alltime = mRemoteServerStatus.network_delay + mRunningServerStatus.computation_time;

            if (status != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Failed with http status: " + status);
                return false;
            }

            Log.e(TAG, "Finish Upload!!!");

            return true;
        }

    }
    private long getComputationTime(HttpURLConnection conn) {
        InputStream ins;
        long computation_time = 0;
        try {
            if(conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                ins = conn.getInputStream();
            } else {
                ins = conn.getErrorStream();
            }

            // https://stackoverflow.com/questions/9856195/how-to-read-an-http-input-stream
            BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
            StringBuilder result = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null) {
                result.append(line);
            }
            Log.e(TAG,"BODY MSG2:" + result.toString());
            String []msg = result.toString().split(" ");
            computation_time = new Long(msg[msg.length-1]);

        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }

        return computation_time;
    }

    /**
     * Represents an asynchronous task to test a server's availabiltiy.
     */
    public class ServerTestTask extends AsyncTask<Void, Void, Boolean> {

        /*
         * Constructor
         */
        ServerTestTask() {
        }

        /*
         * Main background task
         */
        @Override
        protected Boolean doInBackground(Void... params) {

            long time_start, time_end;

            // Test Remote Server
            if (mRemoteServerStatus.active) {
                try {
                    time_start = System.currentTimeMillis();
                    if (testServer(String.format("http://%s", mRemoteServerStatus.addr))) {
                        time_end = System.currentTimeMillis();
                        mRemoteServerStatus.request_successful = true;
                        mRemoteServerStatus.response_time = time_end - time_start;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Test Fog Server
            if (mFogServerStatus.active) {
                try {
                    time_start = System.currentTimeMillis();
                    if (testServer(String.format("http://%s", mFogServerStatus.addr))) {
                        time_end = System.currentTimeMillis();
                        mFogServerStatus.request_successful = true;
                        mFogServerStatus.response_time = time_end - time_start;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return true;
        }

        /*
         * Request has finished (may not be successful)
         */
        @Override
        protected void onPostExecute(final Boolean success) {
            StringBuilder serverStatusViewText = new StringBuilder();

            serverStatusViewText.append("Remote Server: ");
            if (mRemoteServerStatus.active) {
                if (mRemoteServerStatus.request_successful) {
                    serverStatusViewText.append(String.format(" OK (Response Time: %d ms)\n", mRemoteServerStatus.response_time));
                } else {
                    serverStatusViewText.append("Failed to connect!\n");
                }
            } else {
                serverStatusViewText.append("[Deactivated]\n");
            }

            serverStatusViewText.append("Fog Server: ");
            if (mFogServerStatus.active) {
                if (mFogServerStatus.request_successful) {
                    serverStatusViewText.append(String.format(" OK (Response Time: %d ms)", mFogServerStatus.response_time));
                } else {
                    serverStatusViewText.append("Failed to connect!");
                }
            } else {
                serverStatusViewText.append("[Deactivated]");
            }

            mServerStatusView.setText(serverStatusViewText.toString());
        }

        @Override
        protected void onCancelled() {
        }

        //Handle SSL and HTTP connection
        private HttpURLConnection returnHttpSSLConn(String Url) throws Exception {
            // How to use SSL and X509TrustManager
            //Reference: https://www.programcreek.com/java-api-examples/javax.net.ssl.X509TrustManager
            //Reference: http://pankajmalhotra.com/Skip-SSL-HostName-Verification-Java-HttpsURLConnection
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) { return true; }
            });

            SSLContext sslctxt = null;
            sslctxt = SSLContext.getInstance("TLS");
            //Reference: http://www.javased.com/index.php?api=java.security.cert.X509Certificate
            //Reference: Follow the naming of parameters
            sslctxt.init(null, new X509TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, new SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sslctxt.getSocketFactory());
            return (HttpURLConnection) new URL(Url).openConnection();
        }

        private boolean testServer(String serverUrl) throws Exception {
            Log.d(TAG, "doTest()");
            HttpURLConnection conn = returnHttpSSLConn(serverUrl);
            final int connectTimeout = 5000;
            final int readTimeout = 50000;
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Cache-Control", "no-cache");
            final int status = conn.getResponseCode();
            Log.e(TAG, "BODY MSG: " + conn.getResponseMessage());
            if (status != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Failed with http status: " + status);
                return false;
            }
            Log.e(TAG, "Request successful!!!");

            return true;
        }
    }
}

