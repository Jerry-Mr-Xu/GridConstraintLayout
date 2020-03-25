package com.jerry.gcl;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.LayoutRes;
import android.support.annotation.MainThread;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
     * 原子数组
     * <key>原子在网格中的位置</key>
     * <value>原子对象</value>
     */
    private SparseArray<Cell> cellArray = null;

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
        cellArray = new SparseArray<>();

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
    public Cell setCell(final short rowIndex, final short colIndex, @LayoutRes final int layoutId) throws Exception {
        return setCell(rowIndex, colIndex, LayoutInflater.from(getContext()).inflate(layoutId, this, false));
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
    public Cell setCell(final short rowIndex, final short colIndex, final View view) throws Exception {
        if (view == null) {
            throw new NullPointerException("view is null");
        }

        if (view.getLayoutParams() != null) {
            return setCell(rowIndex, colIndex, view.getLayoutParams().width, view.getLayoutParams().height, view);
        } else {
            return setCell(rowIndex, colIndex, ConstraintSet.MATCH_CONSTRAINT, ConstraintSet.MATCH_CONSTRAINT, view);
        }
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
    public Cell setCell(final short rowIndex, final short colIndex, final int viewWidthInPx, final int viewHeightInPx, @LayoutRes final int layoutId) throws Exception {
        return setCell(rowIndex, colIndex, viewWidthInPx, viewHeightInPx, LayoutInflater.from(getContext()).inflate(layoutId, this, false));
    }

    /**
     * 在网格指定位置设置原子
     *
     * @param rowIndex       行数
     * @param colIndex       列数
     * @param viewWidthInPx  原子View像素宽度
     * @param viewHeightInPx 原子View像素高度
     * @param view           原子View
     * @return 生成成功的原子
     */
    @MainThread
    public Cell setCell(final short rowIndex, final short colIndex, final int viewWidthInPx, final int viewHeightInPx, final View view) throws Exception {
        if (view == null) {
            throw new NullPointerException("view is null");
        }
        if (getLayoutParams() == null) {
            throw new NullPointerException("grid is not set layout params");
        }

        // 检测原子的宽高和网格是否匹配
        final int containerWidth = getLayoutParams().width;
        final int containerHeight = getLayoutParams().height;
        // 如果原子宽高是wrap_content但网格是确定数值则认为不匹配
        if (viewWidthInPx == ConstraintSet.WRAP_CONTENT && containerWidth != ViewGroup.LayoutParams.WRAP_CONTENT) {
            throw new LayoutParamNotMatchException("cell width is wrap_content but parent width is exact value");
        }
        if (viewHeightInPx == ConstraintSet.WRAP_CONTENT && containerHeight != ViewGroup.LayoutParams.WRAP_CONTENT) {
            throw new LayoutParamNotMatchException("cell height is wrap_content but parent height is exact value");
        }

        final int pos = Utils.getPosByRowAndColIndex(rowIndex, colIndex);
        final Cell oldCell = cellArray.get(pos);
        if (oldCell != null) {
            // 如果指定位置已有原子，则移除
            removeView(cellArray.get(pos).view);
        }

        // 生成原子对象添加到数组中
        final Cell cell = new Cell(view, view.getId(), rowIndex, colIndex, viewWidthInPx, viewHeightInPx);
        cellArray.put(pos, cell);

        view.setId(Utils.generateViewId());
        if (view.getParent() == null) {
            // 如果要添加的View没有父容器则直接添加到网格
            addView(view);
        } else if (view.getParent() != this) {
            // 如果要添加的View有父容器且父容器不是当前网格，则先从当前父容器中移除然后添加到网格
            ((ViewGroup) view.getParent()).removeView(view);
            addView(view);
        }

        return cell;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
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

        public Cell(View view, int viewId, short rowIndex, short colIndex, int viewWidth, int viewHeight) {
            this.view = view;
            this.viewId = viewId;
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
