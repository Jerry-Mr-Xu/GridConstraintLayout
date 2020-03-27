package com.jerry.test;

import android.support.constraint.ConstraintSet;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.jerry.gcl.GridConstraintLayout;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GridConstraintLayout gclContainer = findViewById(R.id.gcl_container);
        try {
            final TextView view1 = new TextView(this);
            view1.setBackgroundColor(0x88FF0000);
            view1.setText("SSSSSSSSS");
            gclContainer.setCell(view1, 0, 1, ConstraintSet.WRAP_CONTENT, ConstraintSet.WRAP_CONTENT);
            final View view2 = new View(this);
            view2.setBackgroundColor(0x8800FF00);
            gclContainer.setCell(view2, 0, 2, 200, 200);
        } catch (Exception e) {
            Log.e(TAG, "onCreate: errMsg = " + e.getMessage());
        }
    }
}
