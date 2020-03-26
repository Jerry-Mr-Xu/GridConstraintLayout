package com.jerry.gcl;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.LayoutRes;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.constraint.Guideline;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.SparseIntArray;
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
     * 原子View数组
     * <key>原子在网格中的位置</key>
     * <value>原子View</value>
     */
    private SparseArray<View> cellViewArray = new SparseArray<>(10);
    /**
     * 基准线id数组
     * <key>基准线在网格中的位置</key>
     * <value>基准线id</value>
     */
    private SparseIntArray guidelineIdArray = new SparseIntArray(10);

    /**
     * 每行最高高度和每列最宽宽度数组
     * <key>每行或每列在网格中的位置</key>
     * <value>最高高度或最宽宽度</value>
     */
    private SparseIntArray maxSizeArray = new SparseIntArray(10);

    /**
     * 该网格布局的约束关系
     */
    private ConstraintSet constraintSet = new ConstraintSet();

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
     * @param layoutId 原子View布局id
     * @param rowIndex 行数
     * @param colIndex 列数
     * @return 生成成功的原子View
     */
    @MainThread
    public View setCell(@LayoutRes final int layoutId, final int rowIndex, final int colIndex) throws Exception {
        return setCell(LayoutInflater.from(getContext()).inflate(layoutId, this, false), rowIndex, colIndex);
    }

    /**
     * 在网格指定位置设置原子
     *
     * @param view     原子View
     * @param rowIndex 行数
     * @param colIndex 列数
     * @return 生成成功的原子View
     */
    @MainThread
    public View setCell(final View view, final int rowIndex, final int colIndex) throws Exception {
        if (view == null) {
            throw new NullPointerException("view is null");
        }

        if (view.getLayoutParams() != null) {
            return setCell(view, rowIndex, colIndex, view.getLayoutParams().width, view.getLayoutParams().height);
        } else {
            return setCell(view, rowIndex, colIndex, ConstraintSet.MATCH_CONSTRAINT, ConstraintSet.MATCH_CONSTRAINT);
        }
    }

    /**
     * 在网格指定位置设置原子
     *
     * @param layoutId   原子View布局id
     * @param rowIndex   行数
     * @param colIndex   列数
     * @param viewWidth  原子View宽度
     * @param viewHeight 原子View高度
     * @return 生成成功的原子View
     */
    @MainThread
    public View setCell(@LayoutRes final int layoutId, final int rowIndex, final int colIndex, final int viewWidth, final int viewHeight) throws Exception {
        return setCell(LayoutInflater.from(getContext()).inflate(layoutId, this, false), rowIndex, colIndex, viewWidth, viewHeight);
    }

    /**
     * 在网格指定位置设置原子
     *
     * @param view       原子View
     * @param rowIndex   行数
     * @param colIndex   列数
     * @param viewWidth  原子View宽度
     * @param viewHeight 原子View高度
     * @return 生成成功的原子View
     */
    @MainThread
    public View setCell(final View view, final int rowIndex, final int colIndex, final int viewWidth, final int viewHeight) throws Exception {
        if (view == null) {
            throw new NullPointerException("view is null");
        }
        if (getLayoutParams() == null) {
            throw new NullPointerException("grid is not set layout params");
        }
        checkCanSetCell(viewWidth, viewHeight);

        final int pos = Utils.getPosByRowAndColIndex(rowIndex, colIndex);
        // 先移除指定位置原有的原子View
        removeCellView(pos);
        // 再将新原子View添加到网格中
        addCellView(view, viewWidth, viewHeight, pos);

        // 建立基准线
        setupGuidelines();
        // 设置原子View的约束
        refreshCellViewConstraint(pos);

        return view;
    }

    /**
     * 刷新原子View的约束
     *
     * @param pos 原子在网格中的位置
     */
    private void refreshCellViewConstraint(final int pos) {
        constraintSet.clone(this);
        final int cellViewId = cellViewArray.get(pos).getId();
        constraintSet.connect(cellViewId, ConstraintSet.START, guidelineIdArray.get(Utils.getColByPos(pos)), ConstraintSet.START);
        constraintSet.connect(cellViewId, ConstraintSet.TOP, guidelineIdArray.get(Utils.getRowByPos(pos)), ConstraintSet.TOP);
        constraintSet.connect(cellViewId, ConstraintSet.END, guidelineIdArray.get(Utils.changeCol(Utils.getColByPos(pos), 1)), ConstraintSet.END);
        constraintSet.connect(cellViewId, ConstraintSet.BOTTOM, guidelineIdArray.get(Utils.changeRow(Utils.getRowByPos(pos), 1)), ConstraintSet.BOTTOM);
        constraintSet.applyTo(this);
    }

    /**
     * 给网格建立基准线
     * 用于原子的约束
     */
    private void setupGuidelines() {
        int curOffset = 0;
        float percent;
        // 水平基准线
        final boolean isHeightWrapContent = getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT;
        for (int i = 0; i <= rowCount; i++) {
            final int pos = Utils.getPosByRowAndColIndex(i,  0xFFFF);
            if (i == 0 || i == rowCount) {
                guidelineIdArray.put(pos, ConstraintSet.PARENT_ID);
            } else {
                if (isHeightWrapContent) {
                    // 如果网格为自适应高度，则根据原子高度来确定基准线
                    curOffset += maxSizeArray.get(Utils.getPosByRowAndColIndex( (i - 1),  0xFFFF));
                    percent = 0;
                } else {
                    // 否则按照比例来确定基准线
                    curOffset = 0;
                    percent = i * 1.0f / rowCount;
                }

                final int guidelineId = guidelineIdArray.get(pos);
                if (guidelineId > 0) {
                    refreshGuideline(guidelineId, ConstraintSet.HORIZONTAL_GUIDELINE, curOffset, percent);
                } else {
                    createGuideline(pos, ConstraintSet.HORIZONTAL_GUIDELINE, curOffset, percent);
                }
            }
        }

        curOffset = 0;
        // 垂直基准线
        final boolean isWidthWrapContent = getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT;
        for (int i = 0; i <= colCount; i++) {
            final int pos = Utils.getPosByRowAndColIndex(0xFFFF, i);
            if (i == 0 || i == colCount) {
                guidelineIdArray.put(pos, ConstraintSet.PARENT_ID);
            } else {
                if (isWidthWrapContent) {
                    // 如果网格为自适应宽度，则根据原子宽度来确定基准线
                    curOffset += maxSizeArray.get(Utils.getPosByRowAndColIndex(0xFFFF, i - 1));
                    percent = 0;
                } else {
                    // 否则按照比例来确定基准线
                    curOffset = 0;
                    percent = i * 1.0f / colCount;
                }

                final int guidelineId = guidelineIdArray.get(pos);
                if (guidelineId > 0) {
                    refreshGuideline(guidelineId, ConstraintSet.VERTICAL_GUIDELINE, curOffset, percent);
                } else {
                    createGuideline(pos, ConstraintSet.VERTICAL_GUIDELINE, curOffset, percent);
                }
            }
        }
    }

    /**
     * 刷新基准线的约束
     *
     * @param guidelineId 基准线id
     * @param orientation 基准线方向
     * @param offset      基准线偏移
     * @param percent     基准线相对于父容器的比例
     */
    private void refreshGuideline(final int guidelineId, final int orientation, final int offset, final float percent) {
        constraintSet.clone(this);
        constraintSet.create(guidelineId, orientation);
        constraintSet.constrainWidth(guidelineId, ConstraintSet.MATCH_CONSTRAINT);
        constraintSet.constrainHeight(guidelineId, ConstraintSet.MATCH_CONSTRAINT);
        if (offset > 0) {
            constraintSet.setGuidelineBegin(guidelineId, offset);
        }
        if (percent > 0) {
            constraintSet.setGuidelinePercent(guidelineId, percent);
        }
        constraintSet.applyTo(this);
    }

    /**
     * 创建基准线
     *
     * @param pos         基准线在网格中位置
     * @param orientation 基准线方向
     * @param offset      基准线偏移
     * @param percent     基准线相对于父容器的比例
     */
    private void createGuideline(final int pos, final int orientation, final int offset, final float percent) {
        final Guideline guideline = new Guideline(getContext());
        guideline.setId(Utils.generateViewId());
        addView(guideline);
        guidelineIdArray.put(pos, guideline.getId());

        constraintSet.clone(this);
        constraintSet.create(guideline.getId(), orientation);
        constraintSet.constrainWidth(guideline.getId(), ConstraintSet.MATCH_CONSTRAINT);
        constraintSet.constrainHeight(guideline.getId(), ConstraintSet.MATCH_CONSTRAINT);
        if (offset > 0) {
            constraintSet.setGuidelineBegin(guideline.getId(), offset);
        }
        if (percent > 0) {
            constraintSet.setGuidelinePercent(guideline.getId(), percent);
        }
        constraintSet.applyTo(this);
    }

    /**
     * 把原子View添加到网格中
     *
     * @param cellView   原子View
     * @param viewWidth  原子View宽度
     * @param viewHeight 原子View高度
     * @param pos        原子在网格中的位置
     */
    private void addCellView(@NonNull final View cellView, int viewWidth, int viewHeight, final int pos) {
        cellView.setId(Utils.generateViewId());
        final ViewGroup parent = (ViewGroup) cellView.getParent();
        if (parent == null) {
            // 如果要添加的View没有父容器则直接添加到网格
            addView(cellView);
        } else if (parent != this) {
            // 如果要添加的View有父容器且父容器不是当前网格，则先从当前父容器中移除然后添加到网格
            parent.removeView(cellView);
            addView(cellView);
        }

        // 给原子View设置宽高
        constraintSet.clone(this);
        constraintSet.constrainWidth(cellView.getId(), viewWidth);
        constraintSet.constrainHeight(cellView.getId(), viewHeight);
        constraintSet.applyTo(this);

        cellViewArray.put(pos, cellView);

        if (viewWidth == ConstraintSet.WRAP_CONTENT || viewHeight == ConstraintSet.WRAP_CONTENT) {
            // 如果宽度或高度是自适应则手动测量一下
            cellView.measure(0, 0);
            viewWidth = viewWidth == ConstraintSet.WRAP_CONTENT ? cellView.getMeasuredWidth() : viewWidth;
            viewHeight = viewHeight == ConstraintSet.WRAP_CONTENT ? cellView.getMeasuredHeight() : viewHeight;
        }

        if (viewWidth != ConstraintSet.MATCH_CONSTRAINT) {
            // 如果宽度不是充满父容器
            // 和当前列的最大宽度比较，如果大于最大宽度则替换
            final int colPos = Utils.getColByPos(pos);
            if (maxSizeArray.get(colPos) < viewWidth) {
                maxSizeArray.put(colPos, viewWidth);
            }
        }
        if (viewHeight != ConstraintSet.MATCH_CONSTRAINT) {
            // 如果高度不是充满父容器
            // 和当前行的最大高度比较，如果大于最大高度则替换
            final int rowPos = Utils.getRowByPos(pos);
            if (maxSizeArray.get(rowPos) < viewHeight) {
                maxSizeArray.put(rowPos, viewHeight);
            }
        }
    }

    /**
     * 从网格中移除指定位置的原子View
     *
     * @param pos 位置
     */
    private void removeCellView(final int pos) {
        final View cellView = cellViewArray.get(pos);
        if (cellView != null) {
            // 如果指定位置已有原子View，则移除
            removeView(cellView);
        }
    }

    /**
     * 检测该原子是否能够加入网格
     *
     * @param viewWidth  原子View宽度
     * @param viewHeight 原子View高度
     * @throws LayoutParamNotMatchException 宽高不匹配异常
     */
    private void checkCanSetCell(final int viewWidth, final int viewHeight) throws LayoutParamNotMatchException {
        // 检测原子的宽高和网格是否匹配
        final int containerWidth = getLayoutParams().width;
        final int containerHeight = getLayoutParams().height;
        // 如果原子宽高是match_constraint但网格是wrap_content则认为不匹配
        if (viewWidth == ConstraintSet.MATCH_CONSTRAINT && containerWidth == ViewGroup.LayoutParams.WRAP_CONTENT) {
            throw new LayoutParamNotMatchException("cell width is match_constraint but parent width is wrap_content");
        }
        if (viewHeight == ConstraintSet.MATCH_CONSTRAINT && containerHeight == ViewGroup.LayoutParams.WRAP_CONTENT) {
            throw new LayoutParamNotMatchException("cell height is match_constraint but parent height is wrap_content");
        }
    }
}
