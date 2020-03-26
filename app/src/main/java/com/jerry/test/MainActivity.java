package com.jerry.test;

import android.support.constraint.ConstraintSet;
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

        GridConstraintLayout gclContainer = findViewById(R.id.gcl_container);
        try {
            final TextView view1 = new TextView(this);
            view1.setBackgroundColor(0x88FF0000);
            view1.setText("SSSSSSSSS");
            gclContainer.setCell(view1, 0, 0, ConstraintSet.WRAP_CONTENT, ConstraintSet.WRAP_CONTENT);
            final View view2 = new View(this);
            view2.setBackgroundColor(0x8800FF00);
            gclContainer.setCell(view2, 1, 2, ConstraintSet.MATCH_CONSTRAINT, ConstraintSet.MATCH_CONSTRAINT);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
