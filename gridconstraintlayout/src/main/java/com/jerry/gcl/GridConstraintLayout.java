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
     * @param cellLayout 原子View布局id
     * @param cellRow    原子在第几行
     * @param cellCol    原子在第几列
     * @return 生成成功的原子View
     */
    @MainThread
    public View setCell(@LayoutRes final int cellLayout, final int cellRow, final int cellCol) throws Exception {
        return setCell(LayoutInflater.from(getContext()).inflate(cellLayout, this, false), cellRow, cellCol);
    }

    /**
     * 在网格指定位置设置原子
     *
     * @param cellView 原子View
     * @param cellRow  原子在第几行
     * @param cellCol  原子在第几列
     * @return 生成成功的原子View
     */
    @MainThread
    public View setCell(final View cellView, final int cellRow, final int cellCol) throws Exception {
        if (cellView == null) {
            throw new NullPointerException("cellView is null");
        }

        if (cellView.getLayoutParams() != null) {
            return setCell(cellView, cellRow, cellCol, cellView.getLayoutParams().width, cellView.getLayoutParams().height);
        } else {
            return setCell(cellView, cellRow, cellCol, ConstraintSet.MATCH_CONSTRAINT, ConstraintSet.MATCH_CONSTRAINT);
        }
    }

    /**
     * 在网格指定位置设置原子
     *
     * @param cellLayout 原子View布局id
     * @param cellRow    原子在第几行
     * @param cellCol    原子在第几列
     * @param cellWidth  原子View宽度
     * @param cellHeight 原子View高度
     * @return 生成成功的原子View
     */
    @MainThread
    public View setCell(@LayoutRes final int cellLayout, final int cellRow, final int cellCol, final int cellWidth, final int cellHeight) throws Exception {
        return setCell(LayoutInflater.from(getContext()).inflate(cellLayout, this, false), cellRow, cellCol, cellWidth, cellHeight);
    }

    /**
     * 在网格指定位置设置原子
     *
     * @param cellView   原子View
     * @param cellRow    原子在第几行
     * @param cellCol    原子在第几列
     * @param cellWidth  原子View宽度
     * @param cellHeight 原子View高度
     * @return 生成成功的原子View
     */
    @MainThread
    public View setCell(final View cellView, final int cellRow, final int cellCol, final int cellWidth, final int cellHeight) throws Exception {
        if (cellView == null) {
            throw new NullPointerException("cellView is null");
        }
        if (getLayoutParams() == null) {
            throw new NullPointerException("grid is not set layout params");
        }
        checkCanSetCell(cellRow, cellCol, cellWidth, cellHeight);

        final int cellPos = Utils.getPosByRowAndColIndex(cellRow, cellCol);
        // 先移除指定位置原有的原子View
        removeCellView(cellPos);
        // 再将新原子View添加到网格中
        addCellView(cellView, cellWidth, cellHeight, cellPos);

        // 建立基准线
        setupGuidelines();
        // 设置原子View的约束
        refreshCellConstraint(cellPos);

        return cellView;
    }

    /**
     * 刷新原子View的约束
     *
     * @param cellPos 原子在网格中的位置
     */
    private void refreshCellConstraint(final int cellPos) {
        constraintSet.clone(this);
        final int cellViewId = cellViewArray.get(cellPos).getId();
        final int cellRealRow = Utils.getRealRow(cellPos);
        final int cellRealCol = Utils.getRealCol(cellPos);
        final int cellRowPos = Utils.getRowPosByPos(cellPos);
        final int cellColPos = Utils.getColPosByPos(cellPos);
        constraintSet.connect(cellViewId, ConstraintSet.START, guidelineIdArray.get(cellColPos), ConstraintSet.START, cellRealCol == 0 ? 0 : horSpacing / 2);
        constraintSet.connect(cellViewId, ConstraintSet.TOP, guidelineIdArray.get(cellRowPos), ConstraintSet.TOP, cellRealRow == 0 ? 0 : verSpacing / 2);
        constraintSet.connect(cellViewId, ConstraintSet.END, guidelineIdArray.get(Utils.changeCol(cellColPos, 1)), ConstraintSet.END, cellRealCol == colCount - 1 ? 0 : horSpacing / 2);
        constraintSet.connect(cellViewId, ConstraintSet.BOTTOM, guidelineIdArray.get(Utils.changeRow(cellRowPos, 1)), ConstraintSet.BOTTOM, cellRealRow == rowCount - 1 ? 0 : verSpacing / 2);
        constraintSet.applyTo(this);
    }

    /**
     * 给网格建立基准线
     * 用于原子的约束
     */
    private void setupGuidelines() {
        // 水平基准线
        setupHorGuidelines();
        // 垂直基准线
        setupVerGuidelines();
    }

    /**
     * 建立垂直基准线
     */
    private void setupVerGuidelines() {
        int curOffset = 0;
        float percent = 0;
        final boolean isWidthWrapContent = getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT;
        for (int i = 0; i <= colCount; i++) {
            final int glPos = Utils.getColPosByRealCol(i);
            if (i == 0 || i == colCount) {
                guidelineIdArray.put(glPos, ConstraintSet.PARENT_ID);
            } else {
                if (isWidthWrapContent) {
                    // 如果网格为自适应宽度，则根据原子宽度来确定基准线
                    final int maxSize = maxSizeArray.get(Utils.getColPosByRealCol(i - 1));
                    if (maxSize > 0) {
                        curOffset += (maxSize + (i - 1 == 0 ? horSpacing / 2 : horSpacing));
                    }
                } else {
                    // 否则按照比例来确定基准线
                    percent = i * 1.0f / colCount;
                }

                final int glId = guidelineIdArray.get(glPos);
                if (glId > 0) {
                    // 如果已经有基准线则刷新其位置
                    refreshGuideline(glId, ConstraintSet.VERTICAL_GUIDELINE, curOffset, percent);
                } else {
                    // 否则创建基准线
                    createGuideline(glPos, ConstraintSet.VERTICAL_GUIDELINE, curOffset, percent);
                }
            }
        }
    }

    /**
     * 建立水平基准线
     */
    private void setupHorGuidelines() {
        int curOffset = 0;
        float percent = 0;
        final boolean isHeightWrapContent = getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT;
        for (int i = 0; i <= rowCount; i++) {
            final int glPos = Utils.getRowPosByRealRow(i);
            if (i == 0 || i == rowCount) {
                guidelineIdArray.put(glPos, ConstraintSet.PARENT_ID);
            } else {
                if (isHeightWrapContent) {
                    // 如果网格为自适应高度，则根据原子高度来确定基准线
                    final int maxSize = maxSizeArray.get(Utils.getRowPosByRealRow(i - 1));
                    if (maxSize > 0) {
                        curOffset += (maxSize + (i - 1 == 0 ? verSpacing / 2 : verSpacing));
                    }
                } else {
                    // 否则按照比例来确定基准线
                    percent = i * 1.0f / rowCount;
                }

                final int glId = guidelineIdArray.get(glPos);
                if (glId > 0) {
                    // 如果已经有基准线则刷新其位置
                    refreshGuideline(glId, ConstraintSet.HORIZONTAL_GUIDELINE, curOffset, percent);
                } else {
                    // 否则创建基准线
                    createGuideline(glPos, ConstraintSet.HORIZONTAL_GUIDELINE, curOffset, percent);
                }
            }
        }
    }

    /**
     * 刷新基准线的约束
     *
     * @param glId  基准线id
     * @param glOri 基准线方向
     * @param glOff 基准线偏移
     * @param glPer 基准线相对于父容器的比例
     */
    private void refreshGuideline(final int glId, final int glOri, final int glOff, final float glPer) {
        constraintSet.clone(this);
        constraintSet.create(glId, glOri);
        constraintSet.constrainWidth(glId, ConstraintSet.MATCH_CONSTRAINT);
        constraintSet.constrainHeight(glId, ConstraintSet.MATCH_CONSTRAINT);
        if (glPer > 0) {
            constraintSet.setGuidelinePercent(glId, glPer);
        } else if (glOff >= 0) {
            constraintSet.setGuidelineBegin(glId, glOff);
        }
        constraintSet.applyTo(this);
    }

    /**
     * 创建基准线
     *
     * @param glPos 基准线在网格中位置
     * @param glOri 基准线方向
     * @param glOff 基准线偏移
     * @param glPer 基准线相对于父容器的比例
     */
    private void createGuideline(final int glPos, final int glOri, final int glOff, final float glPer) {
        final Guideline guideline = new Guideline(getContext());
        guideline.setId(Utils.generateViewId());
        addView(guideline);
        guidelineIdArray.put(glPos, guideline.getId());

        refreshGuideline(guideline.getId(), glOri, glOff, glPer);
    }

    /**
     * 把原子View添加到网格中
     *
     * @param cellView   原子View
     * @param cellWidth  原子View宽度
     * @param cellHeight 原子View高度
     * @param cellPos    原子在网格中的位置
     */
    private void addCellView(@NonNull final View cellView, int cellWidth, int cellHeight, final int cellPos) {
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
        constraintSet.constrainWidth(cellView.getId(), cellWidth);
        constraintSet.constrainHeight(cellView.getId(), cellHeight);
        constraintSet.applyTo(this);

        cellViewArray.put(cellPos, cellView);

        if (cellWidth == ConstraintSet.WRAP_CONTENT || cellHeight == ConstraintSet.WRAP_CONTENT) {
            // 如果宽度或高度是自适应则手动测量一下
            cellView.measure(0, 0);
            cellWidth = cellWidth == ConstraintSet.WRAP_CONTENT ? cellView.getMeasuredWidth() : cellWidth;
            cellHeight = cellHeight == ConstraintSet.WRAP_CONTENT ? cellView.getMeasuredHeight() : cellHeight;
        }

        if (cellWidth != ConstraintSet.MATCH_CONSTRAINT) {
            // 如果宽度不是充满父容器
            // 和当前列的最大宽度比较，如果大于最大宽度则替换
            final int colPos = Utils.getColPosByPos(cellPos);
            if (maxSizeArray.get(colPos) < cellWidth) {
                maxSizeArray.put(colPos, cellWidth);
            }
        }
        if (cellHeight != ConstraintSet.MATCH_CONSTRAINT) {
            // 如果高度不是充满父容器
            // 和当前行的最大高度比较，如果大于最大高度则替换
            final int rowPos = Utils.getRowPosByPos(cellPos);
            if (maxSizeArray.get(rowPos) < cellHeight) {
                maxSizeArray.put(rowPos, cellHeight);
            }
        }
    }

    /**
     * 从网格中移除指定位置的原子View
     *
     * @param cellPos 原子位置
     */
    private void removeCellView(final int cellPos) {
        final View cellView = cellViewArray.get(cellPos);
        if (cellView != null) {
            // 如果指定位置已有原子View，则移除
            removeView(cellView);
        }
    }

    /**
     * 检测该原子是否能够加入网格
     *
     * @param cellRow    原子在第几行
     * @param cellCol    原子在第几列
     * @param cellWidth  原子View宽度
     * @param cellHeight 原子View高度
     * @throws LayoutParamNotMatchException 宽高不匹配异常
     */
    private void checkCanSetCell(final int cellRow, final int cellCol, final int cellWidth, final int cellHeight) throws LayoutParamNotMatchException {
        if (cellRow >= rowCount) {
            throw new IndexOutOfBoundsException("cell cellRow is bigger than rowCount, out of gird");
        }
        if (cellCol >= colCount) {
            throw new IndexOutOfBoundsException("cell cellCol is bigger than colCount, out of gird");
        }

        // 检测原子的宽高和网格是否匹配
        final int containerWidth = getLayoutParams().width;
        final int containerHeight = getLayoutParams().height;
        // 如果原子宽高是match_constraint但网格是wrap_content则认为不匹配
        if (cellWidth == ConstraintSet.MATCH_CONSTRAINT && containerWidth == ViewGroup.LayoutParams.WRAP_CONTENT) {
            throw new LayoutParamNotMatchException("cell width is match_constraint but parent width is wrap_content");
        }
        if (cellHeight == ConstraintSet.MATCH_CONSTRAINT && containerHeight == ViewGroup.LayoutParams.WRAP_CONTENT) {
            throw new LayoutParamNotMatchException("cell height is match_constraint but parent height is wrap_content");
        }
    }
}
