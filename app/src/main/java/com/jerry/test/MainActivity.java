package com.jerry.test;

import android.support.constraint.ConstraintSet;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

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

                        final TextView view = new TextView(MainActivity.this);
                        view.setText(String.valueOf(rowSpan * colSpan));
                        view.setGravity(Gravity.CENTER);
                        view.setBackgroundColor(colorArray[colorIndex]);
                        try {
                            gclContainer.setCell(new GridConstraintLayout.CellBuilder(view, startRow, startCol).size(ConstraintSet.MATCH_CONSTRAINT, ConstraintSet.MATCH_CONSTRAINT).span(rowSpan, colSpan));
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
        TextView view;
        try {
            view = new TextView(this);
            view.setText("4");
            view.setBackgroundColor(colorArray[3]);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gclWrapContainer.removeCell(1, 0);
                }
            });
            gclWrapContainer.setCell(new GridConstraintLayout.CellBuilder(view, 1, 0).size(210, ConstraintSet.WRAP_CONTENT).span(1, 2).gravity(Gravity.START | Gravity.TOP));
            view = new TextView(this);
            view.setText("1");
            view.setBackgroundColor(colorArray[0]);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gclWrapContainer.removeCell(0, 0);
                }
            });
            gclWrapContainer.setCell(new GridConstraintLayout.CellBuilder(view, 0, 0).size(100, ConstraintSet.WRAP_CONTENT).gravity(Gravity.TOP));
            view = new TextView(this);
            view.setText("2");
            view.setBackgroundColor(colorArray[1]);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gclWrapContainer.removeCell(0, 1);
                }
            });
            gclWrapContainer.setCell(new GridConstraintLayout.CellBuilder(view, 0, 1).size(100, ConstraintSet.WRAP_CONTENT));
            view = new TextView(this);
            view.setText("3");
            view.setBackgroundColor(colorArray[2]);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gclWrapContainer.removeCell(0, 3);
                }
            });
            gclWrapContainer.setCell(new GridConstraintLayout.CellBuilder(view, 0, 3).size(ConstraintSet.WRAP_CONTENT, ConstraintSet.WRAP_CONTENT).gravity(Gravity.END | Gravity.BOTTOM));
            view = new TextView(this);
            view.setText("5");
            view.setBackgroundColor(colorArray[4]);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gclWrapContainer.removeCell(1, 2);
                }
            });
            gclWrapContainer.setCell(new GridConstraintLayout.CellBuilder(view, 1, 2).size(210, ConstraintSet.WRAP_CONTENT).span(1, 2));
            gclWrapContainer.setSize(2, 5);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
