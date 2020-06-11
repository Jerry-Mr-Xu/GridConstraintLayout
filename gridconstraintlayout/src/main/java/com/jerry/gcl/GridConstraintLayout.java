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
     * 使用中的最大行列数
     */
    private int inUseMaxCol = 0, inUseMaxRow = 0;
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
        calInUseMaxSize();

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

        calInUseMaxSize();
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

        calInUseMaxSize();
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
     * 计算使用中的最大行/列坐标
     */
    private void calInUseMaxSize() {
        inUseMaxCol = inUseMaxRow = 0;
        for (int i = 0, size = cellArray.size(); i < size; i++) {
            final int cellPos = cellArray.keyAt(i);
            final int cellCol = Utils.getRealCol(cellPos);
            final int cellRow = Utils.getRealRow(cellPos);
            inUseMaxCol = Math.max(inUseMaxCol, cellCol);
            inUseMaxRow = Math.max(inUseMaxRow, cellRow);
        }
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
            if (isWidthWrapContent) {
                // 如果宽度自适应采用Barrier
                setupVerBarrier(i);
            } else {
                // 否则采用Guideline
                setupVerGuideline(i);
            }
        }
    }

    /**
     * 建立垂直Guideline
     *
     * @param realCol 第几列
     */
    private void setupVerGuideline(int realCol) {
        // 左侧和右侧Guideline的位置
        final int startGuidelinePos = Utils.getColPosByRealCol(realCol), endGuidelinePos = Utils.changeCol(startGuidelinePos, 1);
        // 左侧Guideline和id
        final View startGuideline = gridLineArray.get(startGuidelinePos);
        final int startGuidelineId = startGuideline == null ? ConstraintSet.PARENT_ID : startGuideline.getId();
        // 右侧Guideline
        View endGuideline = null;

        if (realCol < colCount - 1) {
            // 如果不是最后一列
            endGuideline = gridLineArray.get(endGuidelinePos);
            if (endGuideline == null) {
                // 如果没有右侧Guideline，则创建
                endGuideline = createAndAddGuideline(endGuidelinePos, ConstraintSet.VERTICAL_GUIDELINE, (realCol + 1) * 1.0f / colCount);
            } else {
                // 如果有右侧Guideline，则刷新约束
                refreshGuideline(endGuideline.getId(), ConstraintSet.VERTICAL_GUIDELINE, (realCol + 1) * 1.0f / colCount);
            }
        }

        for (int i = 0; i < rowCount; i++) {
            // 遍历这一列所有原子建立约束
            final Cell cell = cellArray.get(Utils.setPosRow(startGuidelinePos, i));
            if (cell == null) {
                continue;
            }

            constraintSet.clone(this);
            if (cell.innerCol == 0) {
                // 如果是原子左边缘，则建立左侧约束
                constraintSet.connect(cell.view.getId(), ConstraintSet.START, startGuidelineId, ConstraintSet.START, realCol * horSpacing / colCount);
            }
            if (cell.innerCol == cell.colSpan - 1) {
                // 如果是原子右边缘，则建立右侧约束
                constraintSet.connect(cell.view.getId(), ConstraintSet.END, endGuideline == null ? ConstraintSet.PARENT_ID : endGuideline.getId(), ConstraintSet.END, (colCount - realCol - 1) * horSpacing / colCount);
            }
            constraintSet.applyTo(this);
        }
    }

    /**
     * 建立垂直Barrier
     *
     * @param realCol 第几列
     */
    private void setupVerBarrier(int realCol) {
        // 左侧和右侧Barrier的位置
        final int startBarrierPos = Utils.getColPosByRealCol(realCol), endBarrierPos = Utils.changeCol(startBarrierPos, 1);
        if (realCol > inUseMaxCol) {
            // 如果超出使用中范围则移除左右Barrier
            removeGridLine(startBarrierPos);
            removeGridLine(endBarrierPos);
            // 直接返回因为这一列没有原子了
            return;
        }

        // 左侧Barrier和id
        final View startBarrier = gridLineArray.get(startBarrierPos);
        final int startBarrierId = startBarrier == null ? ConstraintSet.PARENT_ID : startBarrier.getId();
        // 右边缘原子id数组
        final List<Integer> endCellIdList = new ArrayList<>(rowCount);

        for (int i = 0; i < rowCount; i++) {
            // 遍历这一列所有原子
            final Cell cell = cellArray.get(Utils.setPosRow(startBarrierPos, i));
            if (cell == null) {
                continue;
            }

            constraintSet.clone(this);
            if (cell.innerCol == 0) {
                // 如果是原子左边缘，则建立左侧约束
                constraintSet.connect(cell.view.getId(), ConstraintSet.START, startBarrierId, ConstraintSet.START, startBarrierId == ConstraintSet.PARENT_ID ? 0 : horSpacing);
            }
            if (cell.innerCol == cell.colSpan - 1) {
                if (realCol == inUseMaxCol) {
                    // 如果是最后的有原子一列则添加与父容器的约束
                    constraintSet.connect(cell.view.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
                } else {
                    // 如果是原子右边缘，则添加到右边缘数组中,为后面添加Barrier
                    constraintSet.clear(cell.view.getId(), ConstraintSet.END);
                    endCellIdList.add(cell.view.getId());
                }
            }
            constraintSet.applyTo(this);
        }
        if (realCol == inUseMaxCol) {
            return;
        }

        if (endCellIdList.isEmpty()) {
            // 如果这列无需建立右侧Barrier，则右侧Barrier即为左侧Barrier
            replaceGridLine(endBarrierPos, startBarrierPos);
        } else {
            View endBarrier = gridLineArray.get(endBarrierPos);
            if (endBarrier == null) {
                // 如果没有右侧Barrier，则创建
                endBarrier = createAndAddBarrier(endBarrierPos, Barrier.END, Utils.convertIntListToArray(endCellIdList));
            } else {
                // 如果有右侧Barrier，则刷新约束
                refreshBarrier(endBarrier.getId(), Barrier.END, Utils.convertIntListToArray(endCellIdList));
            }

            // 建立右侧约束，为了Gravity能生效
//            constraintSet.clone(this);
//            for (Integer cellId : endCellIdList) {
//                constraintSet.connect(cellId, ConstraintSet.END, endBarrier.getId(), ConstraintSet.START);
//            }
//            constraintSet.applyTo(this);
        }
    }

    /**
     * 建立水平网格线
     */
    private void setupHorGridLinesAndConstraint() {
        final boolean isHeightWrapContent = getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT;
        for (int i = 0; i < rowCount; i++) {
            if (isHeightWrapContent) {
                // 如果高度自适应采用Barrier
                setupHorBarrier(i);
            } else {
                // 否则采用Guideline
                setupHorGuideline(i);
            }
        }
    }

    /**
     * 建立水平Guideline
     *
     * @param realRow 第几行
     */
    private void setupHorGuideline(int realRow) {
        // 上侧和下侧Guideline的位置
        final int startGuidelinePos = Utils.getRowPosByRealRow(realRow), endGuidelinePos = Utils.changeRow(startGuidelinePos, 1);
        // 上侧Guideline和id
        final View startGuideline = gridLineArray.get(startGuidelinePos);
        final int startGuidelineId = startGuideline == null ? ConstraintSet.PARENT_ID : startGuideline.getId();
        // 下侧Guideline
        View endGuideline = null;

        if (realRow < rowCount - 1) {
            // 如果不是最后一行
            endGuideline = gridLineArray.get(endGuidelinePos);
            if (endGuideline == null) {
                // 如果没有下侧Guideline，则创建
                endGuideline = createAndAddGuideline(endGuidelinePos, ConstraintSet.HORIZONTAL_GUIDELINE, (realRow + 1) * 1.0f / rowCount);
            } else {
                // 如果有下侧Guideline，则刷新约束
                refreshGuideline(endGuideline.getId(), ConstraintSet.HORIZONTAL_GUIDELINE, (realRow + 1) * 1.0f / rowCount);
            }
        }

        for (int i = 0; i < colCount; i++) {
            // 遍历这一行所有原子建立约束
            final Cell cell = cellArray.get(Utils.setPosCol(startGuidelinePos, i));
            if (cell == null) {
                continue;
            }

            constraintSet.clone(this);
            if (cell.innerRow == 0) {
                // 如果是原子上边缘，则建立上侧约束
                constraintSet.connect(cell.view.getId(), ConstraintSet.TOP, startGuidelineId, ConstraintSet.TOP, realRow * verSpacing / rowCount);
            }
            if (cell.innerRow == cell.rowSpan - 1) {
                // 如果是原子下边缘，则建立下侧约束
                constraintSet.connect(cell.view.getId(), ConstraintSet.BOTTOM, endGuideline == null ? ConstraintSet.PARENT_ID : endGuideline.getId(), ConstraintSet.BOTTOM, (rowCount - realRow - 1) * verSpacing / rowCount);
            }
            constraintSet.applyTo(this);
        }
    }

    /**
     * 建立水平Barrier
     *
     * @param realRow 第几行
     */
    private void setupHorBarrier(int realRow) {
        // 上侧和下侧Barrier的位置
        final int startBarrierPos = Utils.getRowPosByRealRow(realRow), endBarrierPos = Utils.changeRow(startBarrierPos, 1);
        if (realRow > inUseMaxRow) {
            // 如果超出使用中范围则移除上下Barrier
            removeGridLine(startBarrierPos);
            removeGridLine(endBarrierPos);
            // 直接返回因为这一行没有原子了
            return;
        }

        // 上侧Barrier和id
        final View startBarrier = gridLineArray.get(startBarrierPos);
        final int startBarrierId = startBarrier == null ? ConstraintSet.PARENT_ID : startBarrier.getId();
        // 下边缘原子id数组
        final List<Integer> endCellIdList = new ArrayList<>(colCount);

        for (int i = 0; i < colCount; i++) {
            // 遍历这一行所有原子
            final Cell cell = cellArray.get(Utils.setPosCol(startBarrierPos, i));
            if (cell == null) {
                continue;
            }

            constraintSet.clone(this);
            if (cell.innerRow == 0) {
                // 如果是原子上边缘，则建立上侧约束
                constraintSet.connect(cell.view.getId(), ConstraintSet.TOP, startBarrierId, ConstraintSet.TOP, startBarrierId == ConstraintSet.PARENT_ID ? 0 : verSpacing);
            }
            if (cell.innerRow == cell.rowSpan - 1) {
                if (realRow == inUseMaxRow) {
                    // 如果是最后的有原子一行则添加与父容器的约束
                    constraintSet.connect(cell.view.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
                } else {
                    // 如果是原子下边缘，则添加到下边缘数组中,为后面添加Barrier
                    constraintSet.clear(cell.view.getId(), ConstraintSet.BOTTOM);
                    endCellIdList.add(cell.view.getId());
                }
            }
            constraintSet.applyTo(this);
        }
        if (realRow == inUseMaxRow) {
            return;
        }

        if (endCellIdList.isEmpty()) {
            // 如果这行无需建立下侧Barrier，则下侧Barrier即为下侧Barrier
            replaceGridLine(endBarrierPos, startBarrierPos);
        } else {
            View endBarrier = gridLineArray.get(endBarrierPos);
            if (endBarrier == null) {
                // 如果没有下侧Barrier，则创建
                endBarrier = createAndAddBarrier(endBarrierPos, Barrier.BOTTOM, Utils.convertIntListToArray(endCellIdList));
            } else {
                // 如果有下侧Barrier，则刷新约束
                refreshBarrier(endBarrier.getId(), Barrier.BOTTOM, Utils.convertIntListToArray(endCellIdList));
            }

            // 建立下侧约束，为了Gravity能生效
//            constraintSet.clone(this);
//            for (Integer cellId : endCellIdList) {
//                constraintSet.connect(cellId, ConstraintSet.BOTTOM, endBarrier.getId(), ConstraintSet.BOTTOM);
//            }
//            constraintSet.applyTo(this);
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
     * @return 创建好的Guideline
     */
    private Guideline createAndAddGuideline(final int guidelinePos, final int guidelineOri, final float guidelinePer) {
        final Guideline guideline = new Guideline(getContext());
        guideline.setId(Utils.generateViewId());
        addGridLine(guidelinePos, guideline);

        refreshGuideline(guideline.getId(), guidelineOri, guidelinePer);
        return guideline;
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
        if (indexInArray >= 0) {
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
        if (newIndex >= 0) {
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
        if (replaceLine == newLine) {
            // 如果要替换的和新的是同一个对象则忽略
            return;
        }
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

//        if (viewWidth == ConstraintSet.WRAP_CONTENT) {
//            LayoutParams cellLp = (LayoutParams) cellView.getLayoutParams();
//            cellLp.constrainedWidth = true;
//        }
//        if (viewHeight == ConstraintSet.WRAP_CONTENT) {
//            LayoutParams cellLp = (LayoutParams) cellView.getLayoutParams();
//            cellLp.constrainedHeight = true;
//        }

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
