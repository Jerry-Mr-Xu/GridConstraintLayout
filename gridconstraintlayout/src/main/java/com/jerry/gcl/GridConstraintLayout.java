package com.jerry.gcl;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.IntRange;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.constraint.Guideline;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Gravity;
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
     * <value>原子对象</value>
     */
    private SparseArray<Cell> cellArray = new SparseArray<Cell>(10);
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
     * @param cellBuilder 原子构造器
     * @return 生成成功的原子View
     */
    public View setCell(@NonNull final CellBuilder cellBuilder) throws Exception {
        if (getLayoutParams() == null) {
            throw new NullPointerException("grid is not set layout params");
        }
        // 检查是否能放下
        checkCanSetCell(cellBuilder.cellRow, cellBuilder.cellCol, cellBuilder.viewWidth, cellBuilder.viewHeight, cellBuilder.rowSpan, cellBuilder.colSpan);

        final int cellPos = Utils.getPosByRowAndCol(cellBuilder.cellRow, cellBuilder.cellCol);
        final SparseIntArray needCalculatePos = new SparseIntArray();
        // 先移除指定位置（包括跨度内）原有的原子View
        removeExistingCell(cellPos, cellBuilder.rowSpan, cellBuilder.colSpan, needCalculatePos);
        // 再将新原子View添加到网格中
        addCellView(cellBuilder.cellView, cellBuilder.viewWidth, cellBuilder.viewHeight, cellPos, cellBuilder.rowSpan, cellBuilder.colSpan, cellBuilder.viewGravity, needCalculatePos);

        calculateMaxSize(needCalculatePos);

        // 建立基准线
        setupGuidelines();
        // 设置原子View的约束
        refreshCellConstraint(cellPos, cellBuilder.rowSpan, cellBuilder.colSpan);

        return cellBuilder.cellView;
    }

    /**
     * 获取指定位置的原子View
     *
     * @param cellRow 原子在第几行
     * @param cellCol 原子在第几列
     * @return 原子View
     */
    public View getCellView(final int cellRow, final int cellCol) {
        final Cell cell = cellArray.get(Utils.getPosByRowAndCol(cellRow, cellCol));
        if (cell == null) {
            return null;
        } else {
            return cell.view;
        }
    }

    /**
     * 移除指定位置的原子
     *
     * @param cellRow 原子在第几行
     * @param cellCol 原子在第几列
     */
    public void removeCell(final int cellRow, final int cellCol) {
        // TODO: 2020/4/20 这里需要重新计算行列最大宽高并重新布局
        removeCell(Utils.getPosByRowAndCol(cellRow, cellCol), null);
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getColCount() {
        return colCount;
    }

    /**
     * 刷新原子View的约束
     *
     * @param cellPos     原子在网格中的位置
     * @param cellRowSpan 原子跨几行
     * @param cellColSpan 原子跨几列
     */
    private void refreshCellConstraint(final int cellPos, @IntRange(from = 1) final int cellRowSpan, @IntRange(from = 1) final int cellColSpan) {
        constraintSet.clone(this);
        final int cellViewId = cellArray.get(cellPos).view.getId();

        final int cellLeftTopPos = cellPos;
        final int cellRightBottomPos = Utils.changeRowAndCol(cellPos, cellRowSpan - 1, cellColSpan - 1);

        final int cellRealLeftCol = Utils.getRealCol(cellLeftTopPos);
        final int cellRealTopRow = Utils.getRealRow(cellLeftTopPos);
        final int cellRealRightCol = Utils.getRealCol(cellRightBottomPos);
        final int cellRealBottomRow = Utils.getRealRow(cellRightBottomPos);

        final int cellLeftColPos = Utils.getColPosByPos(cellLeftTopPos);
        final int cellTopRowPos = Utils.getRowPosByPos(cellLeftTopPos);
        final int cellRightColPos = Utils.getColPosByPos(cellRightBottomPos);
        final int cellBottomRowPos = Utils.getRowPosByPos(cellRightBottomPos);

        final boolean isWidthWrapContent = getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT;
        final boolean isHeightWrapContent = getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT;

        final int leftMargin, topMargin, rightMargin, bottomMargin;
        if (isWidthWrapContent) {
            leftMargin = 0;
            rightMargin = cellRealRightCol == colCount - 1 ? 0 : horSpacing;
        } else {
            leftMargin = cellRealLeftCol * horSpacing / colCount;
            rightMargin = (colCount - cellRealRightCol - 1) * horSpacing / colCount;
        }
        if (isHeightWrapContent) {
            topMargin = 0;
            bottomMargin = cellRealBottomRow == rowCount - 1 ? 0 : verSpacing;
        } else {
            topMargin = cellRealTopRow * verSpacing / rowCount;
            bottomMargin = (rowCount - cellRealBottomRow - 1) * verSpacing / rowCount;
        }

        constraintSet.connect(cellViewId, ConstraintSet.START, guidelineIdArray.get(cellLeftColPos), ConstraintSet.START, leftMargin);
        constraintSet.connect(cellViewId, ConstraintSet.TOP, guidelineIdArray.get(cellTopRowPos), ConstraintSet.TOP, topMargin);
        constraintSet.connect(cellViewId, ConstraintSet.END, guidelineIdArray.get(Utils.changeCol(cellRightColPos, 1)), ConstraintSet.END, rightMargin);
        constraintSet.connect(cellViewId, ConstraintSet.BOTTOM, guidelineIdArray.get(Utils.changeRow(cellBottomRowPos, 1)), ConstraintSet.BOTTOM, bottomMargin);
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
                        curOffset += maxSize;
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
                        curOffset += maxSize;
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
            // 如果有比例优先比例
            constraintSet.setGuidelinePercent(glId, glPer);
        } else if (glOff >= 0) {
            // 其次偏移
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
     * @param cellView         原子View
     * @param viewWidth        原子View宽度
     * @param viewHeight       原子View高度
     * @param cellPos          原子在网格中的位置
     * @param cellRowSpan      原子跨几行
     * @param cellColSpan      原子跨几列
     * @param viewGravity      原子View的Gravity
     * @param needCalculatePos 需要计算最大宽高的行列位置
     */
    private void addCellView(@NonNull final View cellView, final int viewWidth, final int viewHeight, final int cellPos, @IntRange(from = 1) final int cellRowSpan, @IntRange(from = 1) final int cellColSpan, final int viewGravity, @Nullable final SparseIntArray needCalculatePos) {
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

        constraintSet.clone(this);
        // 给原子View设置宽高
        constraintSet.constrainWidth(cellView.getId(), viewWidth);
        constraintSet.constrainHeight(cellView.getId(), viewHeight);
        setCellViewGravity(cellView.getId(), viewGravity);
        // 给原子View设置Gravity
        constraintSet.applyTo(this);

        // 尝试获取测量后大小
        final int[] viewSize = testMeasure(cellView, viewWidth, viewHeight);
        for (int row = 0; row < cellRowSpan; row++) {
            for (int col = 0; col < cellColSpan; col++) {
                // 这里需要计算跨度偏移量
                final int relativeCellPos = Utils.changeRowAndCol(cellPos, row, col);
                cellArray.put(relativeCellPos, new Cell(cellView, viewSize[0], viewSize[1], cellRowSpan, cellColSpan, row, col));

                if (needCalculatePos != null) {
                    // 被移除原子所在的行和列
                    if (getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                        final int addedCol = Utils.getColPosByPos(relativeCellPos);
                        needCalculatePos.put(addedCol, addedCol);
                    }
                    if (getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                        final int addedRow = Utils.getRowPosByPos(relativeCellPos);
                        needCalculatePos.put(addedRow, addedRow);
                    }
                }
            }
        }
    }

    /**
     * 给原子View设置gravity
     *
     * @param viewId      原子ViewId
     * @param viewGravity 原子ViewGravity
     */
    private void setCellViewGravity(int viewId, int viewGravity) {
        final int horGravity = viewGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        final int verGravity = viewGravity & Gravity.VERTICAL_GRAVITY_MASK;

        if (horGravity == Gravity.LEFT) {
            constraintSet.setHorizontalBias(viewId, 0);
        }
        if (verGravity == Gravity.TOP) {
            constraintSet.setVerticalBias(viewId, 0);
        }
        if (horGravity == Gravity.RIGHT) {
            constraintSet.setHorizontalBias(viewId, 1);
        }
        if (verGravity == Gravity.BOTTOM) {
            constraintSet.setVerticalBias(viewId, 1);
        }
        if (horGravity == Gravity.CENTER_HORIZONTAL) {
            constraintSet.setHorizontalBias(viewId, 0.5f);
        }
        if (verGravity == Gravity.CENTER_VERTICAL) {
            constraintSet.setVerticalBias(viewId, 0.5f);
        }
    }

    /**
     * 从网格中移除已有原子为新设置的原子腾出位置
     * 要将跨度内所有原子移除
     *
     * @param cellPos          原子位置
     * @param cellRowSpan      原子跨几行
     * @param cellColSpan      原子跨几列
     * @param needCalculatePos 需要计算最大宽高的行列位置
     */
    private void removeExistingCell(final int cellPos, @IntRange(from = 1) final int cellRowSpan, @IntRange(from = 1) final int cellColSpan, @Nullable final SparseIntArray needCalculatePos) {
        for (int i = 0; i < cellRowSpan; i++) {
            for (int j = 0; j < cellColSpan; j++) {
                // 要将跨度内所有原子移除
                final int relativeCellPos = Utils.changeRowAndCol(cellPos, i, j);
                removeCell(relativeCellPos, needCalculatePos);
            }
        }
    }

    /**
     * 移除指定位置的原子
     *
     * @param cellPos          指定位置
     * @param needCalculatePos 需要计算最大宽高的行列位置
     */
    private void removeCell(final int cellPos, @Nullable final SparseIntArray needCalculatePos) {
        // 获取指定位置的原子
        final Cell cell = cellArray.get(cellPos);
        if (cell == null) {
            return;
        }

        // 获取原子左上角位置
        final int cellLeftTopPos = Utils.changeRowAndCol(cellPos, -cell.innerRow, -cell.innerCol);
        for (int i = 0; i < cell.rowSpan; i++) {
            for (int j = 0; j < cell.colSpan; j++) {
                // 把跨度内每个原子从数组中移除
                final int relativeCellPos = Utils.changeRowAndCol(cellLeftTopPos, i, j);
                cellArray.remove(relativeCellPos);

                if (needCalculatePos != null) {
                    // 被移除原子所在的行和列
                    if (getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                        final int removedCol = Utils.getColPosByPos(relativeCellPos);
                        needCalculatePos.put(removedCol, removedCol);
                    }
                    if (getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                        final int removedRow = Utils.getRowPosByPos(relativeCellPos);
                        needCalculatePos.put(removedRow, removedRow);
                    }
                }
            }
        }

        if (cell.view == null) {
            return;
        }
        // 把原子View从容器中移除
        removeView(cell.view);
    }

    /**
     * 计算行最大高度或列最大宽度
     *
     * @param needCalculatePos 需要计算最大宽高的行列位置
     */
    private void calculateMaxSize(@Nullable final SparseIntArray needCalculatePos) {
        if (needCalculatePos == null) {
            return;
        }

        for (int i = 0; i < needCalculatePos.size(); i++) {
            int rowOrColPos = needCalculatePos.keyAt(i);
            int maxSize = 0;
            if (Utils.isRowPos(rowOrColPos)) {
                // 如果是行的位置
                // 计算这一行的最大高度
                maxSize = calculateRowMaxHeight(Utils.getRealRow(rowOrColPos));
            } else if (Utils.isColPos(rowOrColPos)) {
                // 如果是列的位置
                // 计算这一列的最大宽度
                maxSize = calculateColMaxWidth(Utils.getRealCol(rowOrColPos));
            }
            maxSizeArray.put(rowOrColPos, maxSize);
        }
    }

    /**
     * 计算行最大高度
     *
     * @param realRow 真实行序号
     * @return 行最大高度
     */
    private int calculateRowMaxHeight(final int realRow) {
        int maxHeight = 0;
        // 遍历这一行找出最大高度
        for (int i = 0; i < colCount; i++) {
            final int pos = Utils.getPosByRowAndCol(realRow, i);
            final Cell cell = cellArray.get(pos);
            if (cell == null) {
                continue;
            }

            if (cell.innerRow == cell.rowSpan - 1) {
                // 如果一个原子占多行则只在最后一行参与比较
                int lastRowHeight = cell.viewHeight;
                // 将View高度减去其他行的最大高度
                for (int j = -cell.rowSpan + 1; j < 0; j++) {
                    lastRowHeight -= maxSizeArray.get(Utils.getRowPosByRealRow(realRow + j));
                }

                // 设置最大高度
                if (maxHeight < lastRowHeight) {
                    maxHeight = lastRowHeight;
                }
            }
        }
        return maxHeight;
    }

    /**
     * 计算列最大宽度
     *
     * @param realCol 真实列序号
     * @return 列最大宽度
     */
    private int calculateColMaxWidth(final int realCol) {
        int maxWidth = 0;
        // 遍历这一列找出最大宽度
        for (int i = 0; i < rowCount; i++) {
            final int pos = Utils.getPosByRowAndCol(i, realCol);
            final Cell cell = cellArray.get(pos);
            if (cell == null) {
                continue;
            }

            if (cell.innerCol == cell.colSpan - 1) {
                // 如果一个原子占多列则只在最后一列参与比较
                int lastColWidth = cell.viewWidth;
                // 将View宽度减去其他列的最大宽度
                for (int j = -cell.colSpan + 1; j < 0; j++) {
                    lastColWidth -= maxSizeArray.get(Utils.getColPosByRealCol(realCol + j));
                }

                // 设置最大宽度
                if (maxWidth < lastColWidth) {
                    maxWidth = lastColWidth;
                }
            }
        }
        return maxWidth;
    }

    /**
     * 检测该原子是否能够加入网格
     *
     * @param cellRow     原子在第几行
     * @param cellCol     原子在第几列
     * @param cellWidth   原子View宽度
     * @param cellHeight  原子View高度
     * @param cellRowSpan 原子跨几行
     * @param cellColSpan 原子跨几列
     * @throws LayoutParamNotMatchException 宽高不匹配异常
     */
    private void checkCanSetCell(final int cellRow, final int cellCol, final int cellWidth, final int cellHeight, @IntRange(from = 1) final int cellRowSpan, @IntRange(from = 1) final int cellColSpan) throws Exception {
        if (cellRow + cellRowSpan - 1 >= rowCount) {
            throw new IndexOutOfBoundsException("cell is out of gird");
        }
        if (cellCol + cellColSpan - 1 >= colCount) {
            throw new IndexOutOfBoundsException("cell is out of gird");
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

    /**
     * 测试测量
     *
     * @param needMeasureView 需要测量的View
     * @param originWidth     原始宽度
     * @param originHeight    原始高度
     * @return 测量后大小
     */
    @Size(value = 2)
    private int[] testMeasure(@NonNull final View needMeasureView, final int originWidth, final int originHeight) {
        final int[] afterMeasureSize = new int[]{originWidth, originHeight};
        if (originWidth == ConstraintSet.WRAP_CONTENT || originHeight == ConstraintSet.WRAP_CONTENT) {
            // 如果宽度或高度是自适应则手动测量一下
            needMeasureView.measure(0, 0);
            afterMeasureSize[0] = originWidth == ConstraintSet.WRAP_CONTENT ? needMeasureView.getMeasuredWidth() : originWidth;
            afterMeasureSize[1] = originHeight == ConstraintSet.WRAP_CONTENT ? needMeasureView.getMeasuredHeight() : originHeight;
        }
        return afterMeasureSize;
    }

    /**
     * 原子
     */
    private static final class Cell {
        /**
         * 原子View
         */
        private final View view;
        /**
         * 原子View宽高
         */
        private final int viewWidth, viewHeight;
        /**
         * 原子行列跨度
         */
        private final int rowSpan, colSpan;
        /**
         * 原子在跨度内部位置
         */
        private final int innerRow, innerCol;

        Cell(View view, int viewWidth, int viewHeight, int rowSpan, int colSpan, int innerRow, int innerCol) {
            this.view = view;
            this.viewWidth = viewWidth;
            this.viewHeight = viewHeight;
            this.rowSpan = rowSpan;
            this.colSpan = colSpan;
            this.innerRow = innerRow;
            this.innerCol = innerCol;
        }
    }

    /**
     * 原子构造器
     */
    public static final class CellBuilder {
        private final View cellView;
        private final int cellRow, cellCol;
        private int viewWidth, viewHeight;
        private int rowSpan, colSpan;
        private int viewGravity;

        public CellBuilder(@NonNull final GridConstraintLayout parent, @LayoutRes final int layoutId, final int cellRow, final int cellCol) throws NullPointerException {
            this(LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false), cellRow, cellCol);
        }

        public CellBuilder(final View view, final int cellRow, final int cellCol) throws NullPointerException {
            if (view == null) {
                throw new NullPointerException("cellView is null");
            }
            this.cellView = view;
            this.cellRow = cellRow;
            this.cellCol = cellCol;
            this.viewWidth = view.getLayoutParams() == null ? 0 : view.getLayoutParams().width;
            this.viewHeight = view.getLayoutParams() == null ? 0 : view.getLayoutParams().height;
            this.rowSpan = 1;
            this.colSpan = 1;
            this.viewGravity = Gravity.NO_GRAVITY;
        }

        public CellBuilder size(final int viewWidth, final int viewHeight) {
            this.viewWidth = viewWidth;
            this.viewHeight = viewHeight;
            return this;
        }

        public CellBuilder span(final int rowSpan, final int colSpan) {
            this.rowSpan = rowSpan;
            this.colSpan = colSpan;
            return this;
        }

        public CellBuilder gravity(final int gravity) {
            this.viewGravity = gravity;
            return this;
        }
    }
}
