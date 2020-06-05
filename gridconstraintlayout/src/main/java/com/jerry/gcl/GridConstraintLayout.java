package com.jerry.gcl;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.IntRange;
import android.support.annotation.LayoutRes;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.constraint.Barrier;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.constraint.Guideline;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

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
    private SparseArray<Cell> cellArray = new SparseArray<>(20);
    /**
     * 网格线数组
     * <key>网格线在网格中的位置</key>
     * <value>网格线</value>
     */
    private SparseArray<View> gridLineArray = new SparseArray<>(10);

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
            // 如果行数变少了，则移除超出范围的原子和Guideline
            for (int i = rowCount; i <= this.rowCount; i++) {
                removeGridLine(Utils.getRowPosByRealRow(i));
            }
            for (int i = rowCount; i < this.rowCount; i++) {
                for (int j = 0; j < this.colCount; j++) {
                    removeCell(Utils.getPosByRowAndCol(i, j));
                }
            }
        }
        if (colCount < this.colCount) {
            // 如果列数变少了，则移除超出范围的原子和Guideline
            for (int i = colCount; i <= this.colCount; i++) {
                removeGridLine(Utils.getColPosByRealCol(i));
            }
            for (int i = colCount; i < this.colCount; i++) {
                for (int j = 0; j < rowCount; j++) {
                    removeCell(Utils.getPosByRowAndCol(j, i));
                }
            }
        }

        this.rowCount = rowCount;
        this.colCount = colCount;

        // 建立Guideline
        setupGridLinesAndConstraint();
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

        // 建立网格线和约束
        setupGridLinesAndConstraint();

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

        setupGridLinesAndConstraint();
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
     * 建立网格线和原子约束
     */
    private void setupGridLinesAndConstraint() {
        // 水平网格线
        setupHorGridLinesAndConstraint();
        // 垂直网格线
        setupVerGridLinesAndConstraint();
    }

    /**
     * 建立垂直网格线
     */
    private void setupVerGridLinesAndConstraint() {
        final boolean isWidthWrapContent = getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT;
        for (int i = 0; i < colCount; i++) {
            // 当前列位置和下一列位置
            final int curColPos = Utils.getColPosByRealCol(i), nextColPos = Utils.changeCol(curColPos, 1);
            // 当前列的所有右边缘原子id，用于添加Barrier约束
            final List<Integer> rightCellIdArray = new ArrayList<>(colCount);

            if (!isWidthWrapContent && i + 1 < colCount) {
                // 如果非自适应宽度且不是最后一列
                final Guideline guideline = (Guideline) gridLineArray.get(nextColPos);
                if (guideline != null) {
                    // 如果原来有则刷新
                    refreshGuideline(guideline.getId(), ConstraintSet.VERTICAL_GUIDELINE, (i + 1) * 1.0f / colCount);
                } else {
                    // 否则创建
                    createAndAddGuideline(nextColPos, ConstraintSet.VERTICAL_GUIDELINE, (i + 1) * 1.0f / colCount);
                }
            }
            // 左右网格线id
            final int leftLineId = gridLineArray.indexOfKey(curColPos) < 0 ? ConstraintSet.PARENT_ID : gridLineArray.get(curColPos).getId();
            int rightLineId = gridLineArray.indexOfKey(nextColPos) < 0 ? ConstraintSet.PARENT_ID : gridLineArray.get(nextColPos).getId();

            constraintSet.clone(this);
            for (int j = 0; j < rowCount; j++) {
                final Cell cell = cellArray.get(Utils.getPosByRowAndCol(j, i));
                if (cell == null) {
                    continue;
                }
                if (cell.innerCol == 0) {
                    // 如果是原子首列则建立左侧约束
                    constraintSet.connect(cell.view.getId(), ConstraintSet.START, leftLineId, ConstraintSet.START, leftLineId == ConstraintSet.PARENT_ID ? 0 : horSpacing);
                }
                if (cell.innerCol == cell.colSpan - 1) {
                    // 如果是原子末列
                    if (isWidthWrapContent) {
                        // 如果自适应宽度则加入Barrier约束
                        rightCellIdArray.add(cell.view.getId());
                    } else {
                        // 否则添加原子右侧约束
                        constraintSet.connect(cell.view.getId(), ConstraintSet.END, rightLineId, ConstraintSet.END, (colCount - i - 1) * horSpacing / colCount);
                    }
                }
            }
            constraintSet.applyTo(this);

            if (isWidthWrapContent && rightCellIdArray.size() > 0) {
                // 如果宽度自适应且有需要约束的原子
                Barrier barrier = (Barrier) gridLineArray.get(nextColPos);
                if (barrier != null && barrier.getId() != leftLineId) {
                    // 如果原来有则刷新
                    refreshBarrier(barrier.getId(), Barrier.END, Utils.convertIntListToArray(rightCellIdArray));
                } else {
                    // 否则创建
                    barrier = createAndAddBarrier(nextColPos, Barrier.END, Utils.convertIntListToArray(rightCellIdArray));
                }

                constraintSet.clone(this);
                // 创建完后给当前列加上右侧约束
                for (int j = 0; j < rowCount; j++) {
                    final Cell cell = cellArray.get(Utils.getPosByRowAndCol(j, i));
                    if (cell == null) {
                        continue;
                    }
                    if (cell.innerCol == cell.colSpan - 1) {
                        // 如果是原子末列则建立右侧约束
                        constraintSet.connect(cell.view.getId(), ConstraintSet.END, barrier.getId(), ConstraintSet.END);
                    }
                }
                constraintSet.applyTo(this);
            } else if (isWidthWrapContent) {
                // 如果没有需要约束的原子则删除网格线
                replaceGridLine(nextColPos, curColPos);
            }
        }
    }

    /**
     * 建立水平网格线
     */
    private void setupHorGridLinesAndConstraint() {
        final boolean isHeightWrapContent = getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT;
        for (int i = 0; i < rowCount; i++) {
            // 当前行位置和下一行位置
            final int curRowPos = Utils.getRowPosByRealRow(i), nextRowPos = Utils.changeRow(curRowPos, 1);
            // 当前行的所有下边缘原子id，用于添加Barrier
            final List<Integer> bottomCellIdArray = new ArrayList<>(colCount);

            if (!isHeightWrapContent && i + 1 < rowCount) {
                // 如果非自适应高度且不是最后一行
                final Guideline guideline = (Guideline) gridLineArray.get(nextRowPos);
                if (guideline != null) {
                    // 如果原来有则刷新
                    refreshGuideline(guideline.getId(), ConstraintSet.HORIZONTAL_GUIDELINE, (i + 1) * 1.0f / rowCount);
                } else {
                    // 否则创建
                    createAndAddGuideline(nextRowPos, ConstraintSet.HORIZONTAL_GUIDELINE, (i + 1) * 1.0f / rowCount);
                }
            }
            // 上下网格线id
            final int topLineId = gridLineArray.indexOfKey(curRowPos) < 0 ? ConstraintSet.PARENT_ID : gridLineArray.get(curRowPos).getId();
            final int bottomLineId = gridLineArray.indexOfKey(nextRowPos) < 0 ? ConstraintSet.PARENT_ID : gridLineArray.get(nextRowPos).getId();

            constraintSet.clone(this);
            for (int j = 0; j < colCount; j++) {
                final Cell cell = cellArray.get(Utils.getPosByRowAndCol(i, j));
                if (cell == null) {
                    continue;
                }
                if (cell.innerRow == 0) {
                    // 如果是原子首行则建立上侧约束
                    constraintSet.connect(cell.view.getId(), ConstraintSet.TOP, topLineId, ConstraintSet.TOP, topLineId == ConstraintSet.PARENT_ID ? 0 : verSpacing);
                }
                if (cell.innerRow == cell.rowSpan - 1) {
                    // 如果是原子末行
                    if (isHeightWrapContent) {
                        // 如果自适应高度则加入Barrier约束
                        bottomCellIdArray.add(cell.view.getId());
                    } else {
                        // 否则添加原子下侧约束
                        constraintSet.connect(cell.view.getId(), ConstraintSet.BOTTOM, bottomLineId, ConstraintSet.BOTTOM, (rowCount - i - 1) * verSpacing / rowCount);
                    }
                }
            }
            constraintSet.applyTo(this);

            if (isHeightWrapContent && bottomCellIdArray.size() > 0) {
                // 如果高度自适应且有需要约束的原子
                Barrier barrier = (Barrier) gridLineArray.get(nextRowPos);
                if (barrier != null && barrier.getId() != topLineId) {
                    // 如果原来有则刷新
                    refreshBarrier(barrier.getId(), Barrier.BOTTOM, Utils.convertIntListToArray(bottomCellIdArray));
                } else {
                    // 否则创建
                    barrier = createAndAddBarrier(nextRowPos, Barrier.BOTTOM, Utils.convertIntListToArray(bottomCellIdArray));
                }

                constraintSet.clone(this);
                // 创建完后给当前行加上下侧约束
                for (int j = 0; j < colCount; j++) {
                    final Cell cell = cellArray.get(Utils.getPosByRowAndCol(i, j));
                    if (cell == null) {
                        continue;
                    }
                    if (cell.innerRow == cell.rowSpan - 1) {
                        // 如果是原子末行则建立下侧约束
                        constraintSet.connect(cell.view.getId(), ConstraintSet.BOTTOM, barrier.getId(), ConstraintSet.BOTTOM);
                    }
                }
                constraintSet.applyTo(this);
            } else if (isHeightWrapContent) {
                // 如果没有需要约束的原子则删除网格线
                replaceGridLine(nextRowPos, curRowPos);
            }
        }
    }

    /**
     * 创建并添加Barrier到网格中
     *
     * @param barrierPos Barrier在网格中位置
     * @param barrierOri Barrier方向
     * @param refIds     Barrier所约束的id
     */
    private Barrier createAndAddBarrier(final int barrierPos, final int barrierOri, final int[] refIds) {
        final Barrier barrier = new Barrier(getContext());
        barrier.setId(Utils.generateViewId());
        addGridLine(barrierPos, barrier);

        refreshBarrier(barrier.getId(), barrierOri, refIds);
        return barrier;
    }

    /**
     * 刷新Barrier的约束
     *
     * @param barrierId  Barrier.getId()
     * @param barrierOri Barrier方向
     * @param refIds     Barrier所约束的id
     */
    private void refreshBarrier(int barrierId, int barrierOri, int[] refIds) {
        constraintSet.clone(this);
        constraintSet.createBarrier(barrierId, barrierOri, refIds);
        constraintSet.applyTo(this);
    }

    /**
     * 创建Guideline添加到网格中
     *
     * @param guidelinePos Guideline在网格中位置
     * @param guidelineOri Guideline方向
     * @param guidelinePer Guideline相对于父容器的比例
     */
    private void createAndAddGuideline(final int guidelinePos, final int guidelineOri, final float guidelinePer) {
        final Guideline guideline = new Guideline(getContext());
        guideline.setId(Utils.generateViewId());
        addGridLine(guidelinePos, guideline);

        refreshGuideline(guideline.getId(), guidelineOri, guidelinePer);
    }

    /**
     * 刷新Guideline的约束
     *
     * @param guidelineId  Guideline.getId()
     * @param guidelineOri Guideline方向
     * @param guidelinePer Guideline相对于父容器的比例
     */
    private void refreshGuideline(final int guidelineId, final int guidelineOri, final float guidelinePer) {
        constraintSet.clone(this);
        constraintSet.create(guidelineId, guidelineOri);
        constraintSet.constrainWidth(guidelineId, ConstraintSet.MATCH_CONSTRAINT);
        constraintSet.constrainHeight(guidelineId, ConstraintSet.MATCH_CONSTRAINT);
        constraintSet.setGuidelinePercent(guidelineId, guidelinePer);
        constraintSet.applyTo(this);
    }

    /**
     * 添加网格线到网格中
     *
     * @param linePos 网格线要添加到的位置
     * @param newLine 要添加的网格线
     */
    private void addGridLine(final int linePos, @Nullable final View newLine) {
        gridLineArray.put(linePos, newLine);

        if (newLine == null) {
            return;
        }
        final ViewGroup oldParent = (ViewGroup) newLine.getParent();
        if (oldParent == null) {
            // 如果没有父容器则直接添加
            addView(newLine);
        } else if (oldParent != this) {
            // 如果有父容器且不是本网格，则从容器中移除再添加到网格
            oldParent.removeView(newLine);
            addView(newLine);
        }
    }

    /**
     * 从网格中移除网格线
     *
     * @param linePos 网格线在网格中的位置
     */
    private void removeGridLine(final int linePos) {
        final int indexInArray = gridLineArray.indexOfKey(linePos);
        if (indexInArray > 0) {
            // 如果有则移除
            final View needRemoveLine = gridLineArray.valueAt(indexInArray);
            if (needRemoveLine != null) {
                removeView(needRemoveLine);
            }
            gridLineArray.removeAt(indexInArray);
        }
    }

    /**
     * 替换网格线
     *
     * @param replaceLinePos 被替换的网格线位置
     * @param newLinePos     新网格线位置
     */
    private void replaceGridLine(final int replaceLinePos, final int newLinePos) {
        final int newIndex = gridLineArray.indexOfKey(newLinePos);
        if (newIndex > 0) {
            // 如果新网格线存在则替换旧网格线
            replaceGridLine(replaceLinePos, gridLineArray.valueAt(newIndex));
        } else {
            // 如果新网格线不存在则移除旧网格线
            removeGridLine(replaceLinePos);
        }
    }

    /**
     * 替换网格线
     *
     * @param replaceLinePos 被替换的网格线位置
     * @param newLine        新网格线
     */
    private void replaceGridLine(final int replaceLinePos, @Nullable final View newLine) {
        final View replaceLine = gridLineArray.get(replaceLinePos);
        if (replaceLine != null) {
            // 从容器中移除被替换的网格线
            removeView(replaceLine);
        }

        // 添加要替换的网格线
        addGridLine(replaceLinePos, newLine);
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

        for (int row = 0; row < cellRowSpan; row++) {
            for (int col = 0; col < cellColSpan; col++) {
                // 这里需要计算跨度偏移量
                final int relativeCellPos = Utils.changeRowAndCol(cellPos, row, col);
                cellArray.put(relativeCellPos, new Cell(cellView, viewWidth, viewHeight, cellRowSpan, cellColSpan, row, col));
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
