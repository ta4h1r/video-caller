package com.jb.androidwebrtc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // The screen is always on
        getSupportActionBar().hide();                                           // Hide the action bar at the top


        startActivity(new Intent(MainActivity.this, TelepresenceSelectorActivity.class));

        // Telepresence button
        findViewById(R.id.btnTelepresence).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the activity in here if you want it to occur on a button press
            }
        });
    }
}
