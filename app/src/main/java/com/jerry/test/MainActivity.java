package com.jerry.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.jerry.gcl.GridConstraintLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GridConstraintLayout gcl = new GridConstraintLayout(this);
        try {
            gcl.setCell((short) 1232, (short) 1123, new TextView(this));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
