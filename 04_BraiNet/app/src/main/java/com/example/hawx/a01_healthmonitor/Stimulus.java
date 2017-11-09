package com.example.hawx.a01_healthmonitor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.view.View;
import android.util.Log;

/**
 * Created by Hawx on 09/11/2017.
 */

public class Stimulus extends Activity implements View.OnClickListener {
    private static final String TAG = "Stimulus";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cust_toast_layout);
        findViewById(R.id.stimubtn).setOnClickListener(this);
        //ImageView image = (ImageView) findViewById(R.id.imageView1);
        //image.setImageResource(R.drawable.new_logo);
        TextView txtv = (TextView) findViewById(R.id.textView1);
        txtv.setText(R.string.tasks_str);
        Log.e(TAG, "cust_toast_layout started");

    }

    @Override
    public void onClick(View vinfo) {
        Log.e(TAG, "Clicking!");

        switch (vinfo.getId()){
            case R.id.stimubtn:
                onStimulusBtn();
                break;
            default:
        }
    }

    private void onStimulusBtn() {
        Log.e(TAG, "Closing the Window");
        finish();
    }
}
