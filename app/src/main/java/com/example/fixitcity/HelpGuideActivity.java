package com.example.fixitcity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class HelpGuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_guide);
        // All static content – just set up listeners if needed
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
    }
}