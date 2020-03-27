package com.jerry.gcl;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.View;

import java.util.concurrent.atomic.AtomicInteger;

class Utils {
    private static final String TAG = "Utils";

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    /**
     * 生成ViewId
     *
     * @return viewId
     */
    @SuppressLint("ObsoleteSdkInt")
    static int generateViewId() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return View.generateViewId();
        } else {
            for (; ; ) {
                final int result = sNextGeneratedId.get();
                // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
                int newValue = result + 1;
                if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
                if (sNextGeneratedId.compareAndSet(result, newValue)) {
                    return result;
                }
            }
        }
    }

    private static final int MASK_POS_ROW = 0xFFFF0000;
    private static final int MASK_POS_COL = 0x0000FFFF;

    /**
     * 通过行列数的拼接来得到位置
     *
     * @param rowIndex 第几行
     * @param colIndex 第几列
     * @return 位置
     */
    static int getPosByRowAndColIndex(final int rowIndex, final int colIndex) {
        return (rowIndex << 16) + colIndex;
    }

    /**
     * 通过实际位置得到这行所在的位置
     *
     * @param pos 位置
     * @return 行所在的位置
     */
    static int getRowByPos(final int pos) {
        return pos & MASK_POS_ROW | MASK_POS_COL;
    }

    /**
     * 通过实际位置得到这列所在的位置
     *
     * @param pos 位置
     * @return 列所在的位置
     */
    static int getColByPos(final int pos) {
        return pos & MASK_POS_COL | MASK_POS_ROW;
    }

    /**
     * 给当前位置改变指定行
     *
     * @param pos       位置
     * @param changeNum 改变几行
     * @return 改变后位置
     */
    static int changeRow(final int pos, final int changeNum) {
        int row = pos >>> 16;
        final int col = pos & MASK_POS_COL;
        row += changeNum;
        return getPosByRowAndColIndex(row, col);
    }

    /**
     * 给当前位置改变指定列
     *
     * @param pos       位置
     * @param changeNum 改变几列
     * @return 改变后位置
     */
    static int changeCol(final int pos, final int changeNum) {
        final int row = pos >>> 16;
        int col = pos & MASK_POS_COL;
        col += changeNum;
        return getPosByRowAndColIndex(row, col);
    }

    /**
     * 给当前位置改变指定行列数
     *
     * @param pos       位置
     * @param rowChange 改变几行
     * @param colChange 改变几列
     * @return 改变后位置
     */
    static int changeRowAndCol(final int pos, final int rowChange, final int colChange) {
        int row = pos >>> 16;
        int col = pos & MASK_POS_COL;
        row += rowChange;
        col += colChange;
        return getPosByRowAndColIndex(row, col);
    }
}
