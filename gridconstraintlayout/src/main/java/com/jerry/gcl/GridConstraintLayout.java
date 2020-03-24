package com.jerry.gcl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.LayoutRes;
import android.support.annotation.MainThread;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于约束布局的网格布局
 *
 * @author xujierui
 */
public class GridConstraintLayout extends ConstraintLayout {
    private static final String TAG = "GridConstraintLayout";

    /**
     * 网格行列数
     */
    private int rowCount = 0, colCount = 0;

    /**
     * 网格横纵向间距
     */
    private int horSpacing = 0, verSpacing = 0;

    /**
     * 原子Map
     * <key>原子在网格中的位置</key>
     * <value>原子对象</value>
     */
    private SparseArray<Cell> cellMap = null;

    /**
     * 该网格布局的约束关系
     */
    private ConstraintSet constraintSet = null;

    public GridConstraintLayout(Context context) {
        super(context);
        initAttr(context, null);
    }

    public GridConstraintLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttr(context, attrs);
    }

    public GridConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttr(context, attrs);
    }

    /**
     * 初始化一些属性
     *
     * @param context 上下文
     */
    private void initAttr(Context context, AttributeSet attrSet) {
        cellMap = new SparseArray<>();

        constraintSet = new ConstraintSet();
        constraintSet.clone(this);

        if (attrSet == null) {
            return;
        }

        TypedArray typedArray = context.obtainStyledAttributes(attrSet, R.styleable.GridConstraintLayout);
        rowCount = typedArray.getInt(R.styleable.GridConstraintLayout_gcl_row_count, rowCount);
        colCount = typedArray.getInt(R.styleable.GridConstraintLayout_gcl_col_count, colCount);
        horSpacing = typedArray.getDimensionPixelSize(R.styleable.GridConstraintLayout_gcl_hor_padding, horSpacing);
        verSpacing = typedArray.getDimensionPixelSize(R.styleable.GridConstraintLayout_gcl_ver_padding, verSpacing);
        typedArray.recycle();
    }

    /**
     * 在网格指定位置设置原子
     *
     * @param rowIndex 行数
     * @param colIndex 列数
     * @param layoutId 原子View布局id
     * @return 生成成功的原子，如果为空则设置失败
     */
    @MainThread
    public Cell setCell(final short rowIndex, final short colIndex, @LayoutRes final int layoutId) {
        try {
            return setCell(rowIndex, colIndex, LayoutInflater.from(getContext()).inflate(layoutId, this, false));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 在网格指定位置设置原子
     *
     * @param rowIndex 行数
     * @param colIndex 列数
     * @param view     原子View
     * @return 生成成功的原子，如果为空则设置失败
     */
    @MainThread
    public Cell setCell(final short rowIndex, final short colIndex, final View view) {
        if (view == null) {
            return null;
        }

        if (view.getLayoutParams() != null) {
            setCell(rowIndex, colIndex, view.getLayoutParams().width, view.getLayoutParams().height, view);
        } else {
            setCell(rowIndex, colIndex, ConstraintSet.MATCH_CONSTRAINT, ConstraintSet.MATCH_CONSTRAINT, view);
        }

        return null;
    }

    /**
     * 在网格指定位置设置原子
     *
     * @param rowIndex       行数
     * @param colIndex       列数
     * @param viewWidthInPx  原子View像素宽度
     * @param viewHeightInPx 原子View像素高度
     * @param layoutId       原子View布局id
     * @return 生成成功的原子，如果为空则设置失败
     */
    @MainThread
    public Cell setCell(final short rowIndex, final short colIndex, final int viewWidthInPx, final int viewHeightInPx, @LayoutRes final int layoutId) {
        try {
            setCell(rowIndex, colIndex, viewWidthInPx, viewHeightInPx, LayoutInflater.from(getContext()).inflate(layoutId, this, false));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 在网格指定位置设置原子
     *
     * @param rowIndex       行数
     * @param colIndex       列数
     * @param viewWidthInPx  原子View像素宽度
     * @param viewHeightInPx 原子View像素高度
     * @param view           原子View
     * @return 生成成功的原子，如果为空则设置失败
     */
    @MainThread
    public Cell setCell(final short rowIndex, final short colIndex, final int viewWidthInPx, final int viewHeightInPx, final View view) {
        if (view == null) {
            return null;
        }

        final Cell cell = new Cell(view, rowIndex, colIndex, viewWidthInPx, viewHeightInPx);
        final int pos = Utils.getPosByRowAndColIndex(rowIndex, colIndex);
        cellMap.put(pos, cell);

        refreshLayout();
        return null;
    }

    /**
     * 刷新布局
     */
    @MainThread
    private void refreshLayout() {
        final int gridWidth = getLayoutParams().width, girdHeight = getLayoutParams().height;
        // 先检测一下网格布局的宽高如果是wrap_content则需要根据原子手动计算
        if (gridWidth == ViewGroup.LayoutParams.WRAP_CONTENT) {
            
        }
    }

    /**
     * 网格原子
     */
    public static final class Cell {
        /**
         * 原子View
         */
        private View view;
        /**
         * 原子ViewId
         */
        private int viewId;

        /**
         * 原子在网格中的行列数
         */
        private short rowIndex;
        private short colIndex;

        /**
         * 原子View宽高
         */
        private int viewWidth;
        private int viewHeight;

        public Cell(View view, short rowIndex, short colIndex, int viewWidth, int viewHeight) {
            this.view = view;
            this.viewId = Utils.generateViewId();
            this.rowIndex = rowIndex;
            this.colIndex = colIndex;
            this.viewWidth = viewWidth;
            this.viewHeight = viewHeight;
        }

        public View getView() {
            return view;
        }

        public int getViewId() {
            return viewId;
        }

        public int getRowIndex() {
            return rowIndex;
        }

        public int getColIndex() {
            return colIndex;
        }

        public int getViewWidth() {
            return viewWidth;
        }

        public int getViewHeight() {
            return viewHeight;
        }
    }
}
