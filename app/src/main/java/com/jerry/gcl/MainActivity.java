package com.jerry.gcl;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GridConstraintLayout gcl = new GridConstraintLayout(this);
        gcl.setCell((short) 1000, (short) 1000, new View(this));
    }
}
