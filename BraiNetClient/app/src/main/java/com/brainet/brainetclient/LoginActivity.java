package com.brainet.brainetclient;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
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
import android.widget.EditText;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mUsernameView;
    private Button mSignalAcquisitionButton;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    private String TAG = "LoginActivity";

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_UPDATE_SETTINGS:
                /* Settings were updated */
                Log.d(TAG, "Settings updated:");

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

                String remote_server_addr = prefs.getString("remote_server_addr", "");
                String fog_server_addr = prefs.getString("fog_server_addr", "");
                String server_pref = prefs.getString("server_preference", "");

                Log.d(TAG, String.format("Remote Server Addr: %s", remote_server_addr));
                Log.d(TAG, String.format("Fog Server Addr: %s", fog_server_addr));
                Log.d(TAG, String.format("Server Preference: %s", server_pref));
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
        // Set up the login form.
        mUsernameView = (AutoCompleteTextView) findViewById(R.id.username);
//        mPasswordView = (EditText) findViewById(R.id.password);
//        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
//                if (id == R.id.login || id == EditorInfo.IME_NULL) {
//                    attemptLogin();
//                    return true;
//                }
//                return false;
//            }
//        });

        // Set up Sign-In Button Handler
        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        // Set up Signal Acquisition Handler
        mSignalAcquisitionButton = (Button) findViewById(R.id.signal_acquisition_button);
        mSignalAcquisitionButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                acquireSignal();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
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
//        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();
//        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
//pass

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

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);

            String serverUrl = "http://10.252.136.174:8080/login";
            String signalFilePath = Environment.getExternalStorageDirectory() + File.separator + "S001R03_22.txt";

            mAuthTask = new UserLoginTask(username, serverUrl, signalFilePath);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isUsernameValid(String username) {
        //TODO: Replace this with your own logic
        return true;
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

        /*
         * Constructor
         */
        UserLoginTask(String username, String serverUrl, String signalFilePath) {
            mUsername = username;
            mServerUrl = serverUrl;
            mSignalFilePath = signalFilePath;
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

        private boolean doLogin() throws Exception {
            Log.d(TAG, "doLogin()");

            final String mark_boundary = "XXXXXXXXXXXXX";
            HttpURLConnection conn = returnHttpSSLConn();

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
            int cnt = 0;
            while((cnt = db_file.read(output_buf)) > 0){
                httpPacket.write(output_buf, 0, cnt);
            }

            // Write Endings
            httpPacket.writeBytes("\r\n--" + mark_boundary + "--\r\n");;

            // Flush and close the connection
            httpPacket.flush();
            httpPacket.close();
            final int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Failed with http status: " + status);
                return false;
            }

            Log.e(TAG, "Finish Upload!!!");
            return true;
        }

    }
}

