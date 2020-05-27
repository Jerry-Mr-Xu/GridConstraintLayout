package com.jerry.gcl;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.IntRange;
import android.support.annotation.LayoutRes;
import android.support.annotation.MainThread;
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
     * 实际使用到的最大行列数（有可能未填充满）
     */
    private int inUseMaxRow = 0, inUseMaxCol = 0;

    /**
     * 原子View数组
     * <key>原子在网格中的位置</key>
     * <value>原子对象</value>
     */
    private SparseArray<Cell> cellArray = new SparseArray<>(10);
    /**
     * 基准线数组
     * <key>基准线在网格中的位置</key>
     * <value>基准线</value>
     */
    private SparseArray<Guideline> guidelineArray = new SparseArray<>(10);

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
     * 设置网格大小
     *
     * @param rowCount 多少行
     * @param colCount 多少列
     */
    @MainThread
    public void setSize(final int rowCount, final int colCount) {
        if (rowCount < this.rowCount) {
            // 如果行数变少了，则移除超出范围的原子和基准线
            for (int i = rowCount; i <= this.rowCount; i++) {
                removeGuideline(Utils.getRowPosByRealRow(i));
            }
            for (int i = rowCount; i < this.rowCount; i++) {
                for (int j = 0; j < this.colCount; j++) {
                    removeCell(Utils.getPosByRowAndCol(i, j));
                }
            }
        }
        if (colCount < this.colCount) {
            // 如果列数变少了，则移除超出范围的原子和基准线
            for (int i = colCount; i <= this.colCount; i++) {
                removeGuideline(Utils.getColPosByRealCol(i));
            }
            for (int i = colCount; i < this.colCount; i++) {
                for (int j = 0; j < rowCount; j++) {
                    removeCell(Utils.getPosByRowAndCol(j, i));
                }
            }
        }

        this.rowCount = rowCount;
        this.colCount = colCount;

        calculateMaxSize();

        // 建立基准线
        setupGuidelines();
        // 设置原子View的约束
        setCellConstraint();
    }

    /**
     * 在网格指定位置设置原子
     *
     * @param cellBuilder 原子构造器
     * @return 生成成功的原子View
     */
    @MainThread
    public View setCell(@NonNull final CellBuilder cellBuilder) throws Exception {
        if (getLayoutParams() == null) {
            throw new NullPointerException("grid is not set layout params");
        }
        // 检查是否能放下
        checkCanSetCell(cellBuilder.cellRow, cellBuilder.cellCol, cellBuilder.viewWidth, cellBuilder.viewHeight, cellBuilder.rowSpan, cellBuilder.colSpan);

        final int cellPos = Utils.getPosByRowAndCol(cellBuilder.cellRow, cellBuilder.cellCol);
        // 先移除指定位置（包括跨度内）原有的原子View
        removeExistingCell(cellPos, cellBuilder.rowSpan, cellBuilder.colSpan);
        // 再将新原子View添加到网格中
        addCellView(cellBuilder.cellView, cellBuilder.viewWidth, cellBuilder.viewHeight, cellPos, cellBuilder.rowSpan, cellBuilder.colSpan, cellBuilder.viewGravity);

        calculateMaxSize();

        // 建立基准线
        setupGuidelines();
        // 设置原子View的约束
        setCellConstraint();

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
    @MainThread
    public void removeCell(final int cellRow, final int cellCol) {
        removeCell(Utils.getPosByRowAndCol(cellRow, cellCol));

        // 移除原子后需要重新计算一遍
        calculateMaxSize();

        setupGuidelines();
        // 设置原子View的约束
        setCellConstraint();
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getColCount() {
        return colCount;
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
     * 刷新原子View的约束
     */
    private void setCellConstraint() {
        for (int i = 0; i < cellArray.size(); i++) {
            final int cellPos = cellArray.keyAt(i);
            final Cell cell = cellArray.valueAt(i);
            if (cell.innerRow != 0 || cell.innerCol != 0) {
                // 每个原子只刷新一次
                continue;
            }

            final int cellViewId = cell.view.getId();

            final int cellLeftTopPos = cellPos;
            final int cellRightBottomPos = Utils.changeRowAndCol(cellPos, cell.rowSpan - 1, cell.colSpan - 1);

            final int cellRealLeftCol = Utils.getRealCol(cellLeftTopPos);
            final int cellRealTopRow = Utils.getRealRow(cellLeftTopPos);
            final int cellRealRightCol = Utils.getRealCol(cellRightBottomPos);
            final int cellRealBottomRow = Utils.getRealRow(cellRightBottomPos);

            final Guideline cellLeftGl = guidelineArray.get(Utils.getColPosByPos(cellLeftTopPos));
            final Guideline cellTopGl = guidelineArray.get(Utils.getRowPosByPos(cellLeftTopPos));
            final Guideline cellRightGl = guidelineArray.get(Utils.changeCol(Utils.getColPosByPos(cellRightBottomPos), 1));
            final Guideline cellBottomGl = guidelineArray.get(Utils.changeRow(Utils.getRowPosByPos(cellRightBottomPos), 1));

            final boolean isWidthWrapContent = getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT;
            final boolean isHeightWrapContent = getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT;

            final int leftMargin, topMargin, rightMargin, bottomMargin;
            if (isWidthWrapContent) {
                leftMargin = 0;
                rightMargin = cellRealRightCol == inUseMaxCol ? 0 : horSpacing;
            } else {
                leftMargin = cellRealLeftCol * horSpacing / colCount;
                rightMargin = (colCount - cellRealRightCol - 1) * horSpacing / colCount;
            }
            if (isHeightWrapContent) {
                topMargin = 0;
                bottomMargin = cellRealBottomRow == inUseMaxRow ? 0 : verSpacing;
            } else {
                topMargin = cellRealTopRow * verSpacing / rowCount;
                bottomMargin = (rowCount - cellRealBottomRow - 1) * verSpacing / rowCount;
            }

            constraintSet.clone(this);
            constraintSet.connect(cellViewId, ConstraintSet.START, cellLeftGl == null ? ConstraintSet.PARENT_ID : cellLeftGl.getId(), ConstraintSet.START, leftMargin);
            constraintSet.connect(cellViewId, ConstraintSet.TOP, cellTopGl == null ? ConstraintSet.PARENT_ID : cellTopGl.getId(), ConstraintSet.TOP, topMargin);
            constraintSet.connect(cellViewId, ConstraintSet.END, cellRightGl == null ? ConstraintSet.PARENT_ID : cellRightGl.getId(), ConstraintSet.END, rightMargin);
            constraintSet.connect(cellViewId, ConstraintSet.BOTTOM, cellBottomGl == null ? ConstraintSet.PARENT_ID : cellBottomGl.getId(), ConstraintSet.BOTTOM, bottomMargin);
            constraintSet.applyTo(this);
        }
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
            if (i == 0 || i == inUseMaxCol + 1) {
                // 如果是边界则以父容器为基准
                replaceGuideline(glPos, null);
            } else if (i > 0 && i < inUseMaxCol + 1) {
                // 如果非边界则建立基准线
                if (isWidthWrapContent) {
                    // 如果网格为自适应宽度，则根据原子宽度来确定基准线
                    final int maxSize = maxSizeArray.get(Utils.getColPosByRealCol(i - 1));
                    if (maxSize > 0) {
                        curOffset += maxSize;
                    } else {
                        // 如果这一列没有宽度则基准线和左一列相同
                        replaceGuideline(glPos, Utils.changeCol(glPos, -1));
                        continue;
                    }
                } else {
                    // 否则按照比例来确定基准线
                    percent = i * 1.0f / colCount;
                }

                final Guideline gl = guidelineArray.get(glPos), leftGl = guidelineArray.get(Utils.changeCol(glPos, -1));
                if (gl != null && gl != leftGl) {
                    // 如果已经有基准线且和左一列基准线不同则刷新其位置
                    refreshGuideline(gl.getId(), ConstraintSet.VERTICAL_GUIDELINE, curOffset, percent);
                } else {
                    // 否则创建基准线
                    createAndAddGl(glPos, ConstraintSet.VERTICAL_GUIDELINE, curOffset, percent);
                }
            } else {
                // 如果超出边界则移除基准线
                removeGuideline(glPos);
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
            if (i == 0 || i == inUseMaxRow + 1) {
                // 如果是边界则以父容器为基准
                replaceGuideline(glPos, null);
            } else if (i > 0 && i < inUseMaxRow + 1) {
                // 如果非边界则建立基准线
                if (isHeightWrapContent) {
                    // 如果网格为自适应高度，则根据原子高度来确定基准线
                    final int maxSize = maxSizeArray.get(Utils.getRowPosByRealRow(i - 1));
                    if (maxSize > 0) {
                        curOffset += maxSize;
                    } else {
                        // 如果这一行没有高度则基准线和上一行相同
                        replaceGuideline(glPos, Utils.changeRow(glPos, -1));
                        continue;
                    }
                } else {
                    // 否则按照比例来确定基准线
                    percent = i * 1.0f / rowCount;
                }

                final Guideline gl = guidelineArray.get(glPos), topGl = guidelineArray.get(Utils.changeRow(glPos, -1));
                if (gl != null && gl != topGl) {
                    // 如果已经有基准线且和上一行基准线不同则刷新其位置
                    refreshGuideline(gl.getId(), ConstraintSet.HORIZONTAL_GUIDELINE, curOffset, percent);
                } else {
                    // 否则创建基准线
                    createAndAddGl(glPos, ConstraintSet.HORIZONTAL_GUIDELINE, curOffset, percent);
                }
            } else {
                // 如果超出边界则移除基准线
                removeGuideline(glPos);
            }
        }
    }

    /**
     * 创建基准线添加到网格中
     *
     * @param glPos 基准线在网格中位置
     * @param glOri 基准线方向
     * @param glOff 基准线偏移
     * @param glPer 基准线相对于父容器的比例
     */
    private void createAndAddGl(final int glPos, final int glOri, final int glOff, final float glPer) {
        final Guideline guideline = new Guideline(getContext());
        guideline.setId(Utils.generateViewId());
        addGuideline(glPos, guideline);

        refreshGuideline(guideline.getId(), glOri, glOff, glPer);
    }

    /**
     * 添加基准线到网格中
     *
     * @param glPos        基准线要添加到的位置
     * @param newGuideline 要添加的基准线
     */
    private void addGuideline(final int glPos, @Nullable final Guideline newGuideline) {
        guidelineArray.put(glPos, newGuideline);

        if (newGuideline == null) {
            return;
        }
        final ViewGroup oldParent = (ViewGroup) newGuideline.getParent();
        if (oldParent == null) {
            // 如果没有父容器则直接添加
            addView(newGuideline);
        } else if (oldParent != this) {
            // 如果有父容器且不是本网格，则从容器中移除再添加到网格
            oldParent.removeView(newGuideline);
            addView(newGuideline);
        }
    }

    /**
     * 从网格中移除基准线
     *
     * @param glPos 基准线在网格中的位置
     */
    private void removeGuideline(final int glPos) {
        final int indexInArray = guidelineArray.indexOfKey(glPos);
        if (indexInArray > 0) {
            // 如果有则移除
            final Guideline needRemoveGl = guidelineArray.valueAt(indexInArray);
            if (needRemoveGl != null) {
                removeView(needRemoveGl);
            }
            guidelineArray.removeAt(indexInArray);
        }
    }

    /**
     * 替换基准线
     *
     * @param replaceGlPos 被替换的基准线位置
     * @param newGlPos     新基准线位置
     */
    private void replaceGuideline(final int replaceGlPos, final int newGlPos) {
        final int newIndex = guidelineArray.indexOfKey(newGlPos);
        if (newIndex > 0) {
            // 如果新基准线存在则替换
            replaceGuideline(replaceGlPos, guidelineArray.valueAt(newIndex));
        } else {
            // 如果新基准线不存在则移除基准线
            removeGuideline(replaceGlPos);
        }
    }

    /**
     * 替换基准线
     *
     * @param replaceGlPos 被替换的基准线位置
     * @param newGuideline 新基准线
     */
    private void replaceGuideline(final int replaceGlPos, @Nullable final Guideline newGuideline) {
        final Guideline replaceGl = guidelineArray.get(replaceGlPos);
        if (replaceGl != null) {
            // 移除被替换的基准线
            removeView(replaceGl);
        }

        // 添加要替换的基准线
        addGuideline(replaceGlPos, newGuideline);
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
     * 把原子View添加到网格中
     *
     * @param cellView    原子View
     * @param viewWidth   原子View宽度
     * @param viewHeight  原子View高度
     * @param cellPos     原子在网格中的位置
     * @param cellRowSpan 原子跨几行
     * @param cellColSpan 原子跨几列
     * @param viewGravity 原子View的Gravity
     */
    private void addCellView(@NonNull final View cellView, final int viewWidth, final int viewHeight, final int cellPos, @IntRange(from = 1) final int cellRowSpan, @IntRange(from = 1) final int cellColSpan, final int viewGravity) {
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
     * @param cellPos     原子位置
     * @param cellRowSpan 原子跨几行
     * @param cellColSpan 原子跨几列
     */
    private void removeExistingCell(final int cellPos, @IntRange(from = 1) final int cellRowSpan, @IntRange(from = 1) final int cellColSpan) {
        for (int i = 0; i < cellRowSpan; i++) {
            for (int j = 0; j < cellColSpan; j++) {
                // 要将跨度内所有原子移除
                final int relativeCellPos = Utils.changeRowAndCol(cellPos, i, j);
                removeCell(relativeCellPos);
            }
        }
    }

    /**
     * 移除指定位置的原子
     *
     * @param cellPos 指定位置
     */
    private void removeCell(final int cellPos) {
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
     */
    private void calculateMaxSize() {
        if (getLayoutParams().height == ConstraintSet.WRAP_CONTENT) {
            // 计算每一行的最大高度
            for (int i = 0; i < rowCount; i++) {
                final int maxHeight = calculateRowMaxHeight(i);
                maxSizeArray.put(Utils.getRowPosByRealRow(i), maxHeight);

                // 记录在使用的最后一行
                if (maxHeight > 0) {
                    inUseMaxRow = i;
                }
            }
        } else {
            inUseMaxRow = rowCount - 1;
        }

        if (getLayoutParams().width == ConstraintSet.WRAP_CONTENT) {
            // 计算每一列的最大宽度
            for (int i = 0; i < colCount; i++) {
                final int maxWidth = calculateColMaxWidth(i);
                maxSizeArray.put(Utils.getColPosByRealCol(i), maxWidth);

                // 记录在使用的最后一列
                if (maxWidth > 0) {
                    inUseMaxCol = i;
                }
            }
        } else {
            inUseMaxCol = colCount - 1;
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
                int lastRowHeight = cell.viewHeight + verSpacing;
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
                int lastColWidth = cell.viewWidth + horSpacing;
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
