package com.jerry.gcl;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.View;

import java.util.List;
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
     * @param row 第几行
     * @param col 第几列
     * @return 位置
     */
    static int getPosByRowAndCol(final int row, final int col) {
        return (row << 16) + col;
    }

    /**
     * 通过真实行数得到这行所在位置
     *
     * @param realRow 真实第几行
     * @return 行所在位置
     */
    static int getRowPosByRealRow(final int realRow) {
        return (realRow << 16) + MASK_POS_COL;
    }

    /**
     * 通过真实列数得到这列所在位置
     *
     * @param realCol 真实第几列
     * @return 列所在位置
     */
    static int getColPosByRealCol(final int realCol) {
        return MASK_POS_ROW + realCol;
    }

    /**
     * 通过位置得到这行所在位置
     *
     * @param pos 位置
     * @return 行所在位置
     */
    static int getRowPosByPos(final int pos) {
        return pos & MASK_POS_ROW | MASK_POS_COL;
    }

    /**
     * 通过位置得到这列所在位置
     *
     * @param pos 位置
     * @return 列所在位置
     */
    static int getColPosByPos(final int pos) {
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
        int row = getRealRow(pos);
        final int col = getRealCol(pos);
        row += changeNum;
        return getPosByRowAndCol(row, col);
    }

    /**
     * 给当前位置改变指定列
     *
     * @param pos       位置
     * @param changeNum 改变几列
     * @return 改变后位置
     */
    static int changeCol(final int pos, final int changeNum) {
        final int row = getRealRow(pos);
        int col = getRealCol(pos);
        col += changeNum;
        return getPosByRowAndCol(row, col);
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
        int row = getRealRow(pos);
        int col = getRealCol(pos);
        row += rowChange;
        col += colChange;
        return getPosByRowAndCol(row, col);
    }

    /**
     * 获取真实的第几行
     *
     * @param pos 位置
     * @return 第几行
     */
    static int getRealRow(final int pos) {
        return pos >>> 16;
    }

    /**
     * 获取真实的第几列
     *
     * @param pos 位置
     * @return 第几列
     */
    static int getRealCol(final int pos) {
        return pos & MASK_POS_COL;
    }

    /**
     * 判断所指定位置是否是行
     *
     * @param pos 位置
     * @return 是否是行
     */
    static boolean isRowPos(final int pos) {
        return (pos & MASK_POS_COL) == MASK_POS_COL;
    }

    /**
     * 判断所指定位置是否是列
     *
     * @param pos 位置
     * @return 是否是列
     */
    static boolean isColPos(final int pos) {
        return (pos & MASK_POS_ROW) == MASK_POS_ROW;
    }

    /**
     * 将整型List转为整型数组
     *
     * @param intList 整型List
     * @return 整型数组
     */
    static int[] convertIntListToArray(final List<Integer> intList) {
        if (intList == null) {
            return null;
        }

        final int[] resultIntArray = new int[intList.size()];
        int i = 0;
        for (Integer integer : intList) {
            resultIntArray[i] = integer;
            i++;
        }
        return resultIntArray;
    }
}
