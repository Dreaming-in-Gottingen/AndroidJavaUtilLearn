package com.example.service_server;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.widget.Button;
import android.view.View;

import android.util.Log;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "ServiceServerDemo";

    @Override
    protected void onCreate(Bundle savedState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedState);   
        setContentView(R.layout.main_activity);

        Button btn_quit = (Button) findViewById(R.id.quit);
        btn_quit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "quit activity!");
                finish();
            }
        });
    }
}
