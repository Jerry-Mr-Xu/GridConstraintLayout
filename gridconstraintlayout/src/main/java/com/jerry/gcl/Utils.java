package com.jerry.gcl;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.View;

import java.util.concurrent.atomic.AtomicInteger;

class Utils {
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

    /**
     * 通过行列数的拼接来得到位置
     *
     * @param rowIndex 第几行
     * @param colIndex 第几列
     * @return 位置
     */
    static int getPosByRowAndColIndex(final short rowIndex, final short colIndex) {
        return (rowIndex << 16) + colIndex;
    }

    /**
     * 通过位置拆分得到行数
     *
     * @param pos 位置
     * @return 第几行
     */
    static short getRowIndexByPos(final int pos) {
        return (short) (pos >> 16);
    }

    /**
     * 通过位置拆分得到列数
     *
     * @param pos 位置
     * @return 第几列
     */
    static short getColIndexByPos(final int pos) {
        return (short) (pos & 0xFFFF);
    }
}
