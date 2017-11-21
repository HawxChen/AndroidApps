package com.brainet.brainetclient;

import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class SignalAcquisitionActivity extends AppCompatActivity {
    TextView timer_display;
    int seconds_remaining;
    Handler timer_handler = new Handler();
    Runnable timer_runnable = new Runnable() {
        @Override
        public void run() {
            timer_display.setText(String.format("%d", seconds_remaining));
            if (seconds_remaining > 0) {
                seconds_remaining -= 1;
                timer_handler.postDelayed(this, 1000);
            } else {
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signal_acquisition);
        timer_display = (TextView) findViewById(R.id.timer_display);
        timer_handler.postDelayed(timer_runnable, 0);
        seconds_remaining = 10;
    }
}
