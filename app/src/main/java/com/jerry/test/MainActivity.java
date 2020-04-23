package com.jerry.test;

import android.support.constraint.ConstraintSet;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import com.jerry.gcl.GridConstraintLayout;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private int[] colorArray = {0xFFFF8888, 0xFF88FF88, 0xFF8888FF, 0xFFFFFF88, 0xFFFF88FF, 0xFF88FFFF};
    private int colorIndex = 0;

    private int startRow, startCol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final GridConstraintLayout gclContainer = findViewById(R.id.gcl_container);
        gclContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int cellWidth = v.getWidth() / 5, cellHeight = v.getHeight() / 5;
                final int x = (int) event.getX(), y = (int) event.getY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        startRow = y / cellHeight;
                        startCol = x / cellWidth;
                        final View view = gclContainer.getCellView(startRow, startCol);
                        if (view != null) {
                            view.setBackgroundColor(0xFF000000);
                        }
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        final int upRow = y / cellHeight;
                        final int upCol = x / cellWidth;
                        final int rowSpan = Math.abs(upRow - startRow) + 1;
                        final int colSpan = Math.abs(upCol - startCol) + 1;
                        startRow = startRow > upRow ? upRow : startRow;
                        startCol = startCol > upCol ? upCol : startCol;

                        final View view = new View(MainActivity.this);
                        view.setBackgroundColor(colorArray[colorIndex]);
                        try {
                            gclContainer.setCellWithSpan(view, startRow, startCol, ConstraintSet.MATCH_CONSTRAINT, ConstraintSet.MATCH_CONSTRAINT, rowSpan, colSpan);
                            colorIndex = (colorIndex + 1) % colorArray.length;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    default: {
                        break;
                    }
                }
                return true;
            }
        });

        final GridConstraintLayout gclWrapContainer = findViewById(R.id.gcl_wrap_container);
        try {
            View view = new View(this);
            view.setBackgroundColor(colorArray[0]);
            gclWrapContainer.setCell(view, 0, 0, 200, 20);
            view = new View(this);
            view.setBackgroundColor(colorArray[1]);
            gclWrapContainer.setCell(view, 0, 1, 400, 30);
            view = new View(this);
            view.setBackgroundColor(colorArray[2]);
            gclWrapContainer.setCell(view, 0, 2, 100, 50);
            view = new View(this);
            view.setBackgroundColor(colorArray[3]);
            gclWrapContainer.setCellWithSpan(view, 1, 0, 500, 30, 1, 2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
