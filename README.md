

# GridConstraintLayout

一个基于`ConstraintLayout`实现的网格布局

## 接入方式

在`build.gradle`文件中添加如下代码

```groovy
compile 'com.jerry.gcl:gridconstraintlayout:1.3@aar'
```

## API

### 属性

```xml
<attr name="gcl_row_count" format="integer" />
```

网格布局有多少行

```xml
<attr name="gcl_col_count" format="integer" />
```

网格布局有多少列

```xml
<attr name="gcl_hor_padding" format="dimension" />
```

网格内横向间距

```xml
<attr name="gcl_ver_padding" format="dimension" />
```

网格内纵向间距

### 方法

```java
public View setCell(@NonNull final CellBuilder cellBuilder) throws Exception
```

设置网格原子，提供原子构造器，返回已添加到网格的原子`View`

| 参数        | 含义 |
| ---------- | ---------- |
| cellBuilder | 原子构造器 |

------

```java
public void removeCell(final int cellRow, final int cellCol)
```

移除指定位置的原子

| 参数    | 含义         |
| ------- | ------------ |
| cellRow | 原子在第几行 |
| cellCol | 原子在第几列 |

------

```java
public void setSize(final int rowCount, final int colCount)
```

设置网格行列数，已支持在有内容的情况下修改网格行列数（超出范围的原子会被清除）

| 参数 | 含义         |
| -------- | ------------ |
| rowCount | 网格有多少行 |
| colCount | 网格有多少列 |

------

```java
public View getCellView(final int cellRow, final int cellCol)
```

获取指定位置的原子View，提供行列数，返回网格中指定位置原子`View`

| 参数 | 含义                 |
| -------------------- | -------------------- |
| cellRow | 要获取的原子在第几行 |
| cellCol | 要获取的原子在第几列 |

------

```java
public int getRowCount()
```

获取网格有多少行

------

```java
public int getColCount()
```

获取网格有多少列

### 原子构造器（CellBuilder）

```java
public CellBuilder(@NonNull final GridConstraintLayout parent, @LayoutRes final int layoutId, final int cellRow, final int cellCol) throws NullPointerException
```

提供布局文件来构造原子

| 参数     | 含义         |
| -------- | ------------ |
| parent   | 网格布局     |
| layoutId | 布局文件     |
| cellRow  | 原子所在行数 |
| cellCol  | 原子所在列数 |

------

```java
public CellBuilder(final View view, final int cellRow, final int cellCol) throws NullPointerException
```

提供`View`来构造原子

| 参数    | 含义         |
| ------- | ------------ |
| view    | 原子View     |
| cellRow | 原子所在行数 |
| cellCol | 原子所在列数 |

------

```java
public CellBuilder size(final int viewWidth, final int viewHeight)
```

设置原子`View`的宽高，如果`LayoutParams`指定了宽高可以不设置

| 参数       | 含义         |
| ---------- | ------------ |
| viewWidth  | 原子View宽度 |
| viewHeight | 原子View高度 |

------

```java
public CellBuilder span(final int rowSpan, final int colSpan)
```

设置原子所跨的行列数

| 参数    | 含义     |
| ------- | -------- |
| rowSpan | 所跨行数 |
| colSpan | 所跨列数 |

------

```java
public CellBuilder gravity(final int gravity)
```

设置原子Gravity

| 参数    | 含义                       |
| ------- | -------------------------- |
| gravity | 参考`android.view.Gravity` |

## 示例

```java
final GridConstraintLayout gclWrapContainer = findViewById(R.id.gcl_wrap_container);
try {
    TextView view = new TextView(this);
    view.setText("1");
    view.setBackgroundColor(colorArray[0]);
    gclWrapContainer.setCell(new GridConstraintLayout.CellBuilder(view, 0, 0).size(100, ConstraintSet.WRAP_CONTENT));
    view = new TextView(this);
    view.setText("2");
    view.setBackgroundColor(colorArray[1]);
    gclWrapContainer.setCell(new GridConstraintLayout.CellBuilder(view, 0, 2).size(100, ConstraintSet.WRAP_CONTENT));
    view = new TextView(this);
    view.setText("3");
    view.setBackgroundColor(colorArray[2]);
    gclWrapContainer.setCell(new GridConstraintLayout.CellBuilder(view, 0, 3).size(ConstraintSet.WRAP_CONTENT, ConstraintSet.WRAP_CONTENT).gravity(Gravity.RIGHT));
    view = new TextView(this);
    view.setText("4");
    view.setBackgroundColor(colorArray[3]);
    gclWrapContainer.setCell(new GridConstraintLayout.CellBuilder(view, 1, 0).size(210, ConstraintSet.WRAP_CONTENT).span(1, 2).gravity(Gravity.LEFT | Gravity.TOP));
    view = new TextView(this);
    view.setText("5");
    view.setBackgroundColor(colorArray[4]);
    gclWrapContainer.setCell(new GridConstraintLayout.CellBuilder(view, 1, 2).size(210, ConstraintSet.WRAP_CONTENT).span(1, 2));
} catch (Exception e) {
    e.printStackTrace();
}
```

`AdapterView`中使用方式，因为复用原因需要先检查是否已有原子

```java
public View getView(int position, View convertView, ViewGroup parent){
    final GridConstraintLayout gclContainer;
    ItemView itemView = (ItemView) gclContainer.getCellView(0, i);
    if (itemView == null) {
        try {
            itemView = (ItemView) gclContainer.setCell(new GridConstraintLayout.CellBuilder(new ItemView(context, 0, i).size(ConstraintSet.MATCH_CONSTRAINT, ConstraintSet.WRAP_CONTENT));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

![demo_img](https://github.com/Jerry-Mr-Xu/GridConstraintLayout/blob/master/screenshot/demo01.png?raw=true)

