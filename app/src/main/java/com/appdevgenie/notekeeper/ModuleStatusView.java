package com.appdevgenie.notekeeper;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

/**
 * TODO: document your custom view class.
 */
public class ModuleStatusView extends View {

    private static final int EDIT_MODE_MODULE_COUNT = 7;
    private static final int INVALID_INDEX = -1;
    private static final int SHAPE_CIRCLE = 0;
//    private static final int SHAPE_SQUARE = 1;
    private static final float DEFAULT_OUTLINE_WIDTH_DP = 2f;
    public static final float DEFAULT_SIZE_DP = 48f;
    public static final float DEFAULT_SPACING_DP = 10f;

//    private String mExampleString; // TODO: use a default from R.string...
//    private int mExampleColor = Color.RED; // TODO: use a default from R.color...
//    private float mExampleDimension = 0; // TODO: use a default from R.dimen...
//    private Drawable mExampleDrawable;

    private float mSpacing;
    private float mShapeSize;
    private float mOutlineWidth;
    private Rect[] mModuleRectangles;
    private int mOutlineColor;
    private Paint mPaintOutline;
    private int mFillColor;
    private Paint mPaintFill;
    private float mRadius;
    private int mMaxHorizontalModules;
    private int mShape;


    public boolean[] getModuleStatus() {
        return mModuleStatus;
    }

    public void setModuleStatus(boolean[] moduleStatus) {
        mModuleStatus = moduleStatus;
    }

    private boolean[] mModuleStatus;

    public ModuleStatusView(Context context) {
        super(context);
        init(null, 0);
    }

    public ModuleStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ModuleStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        if (isInEditMode())
            setupEditModeValues();

        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        float displayDensity = dm.density;
        float defaultOutlineWidthPixels = displayDensity * DEFAULT_OUTLINE_WIDTH_DP;
        float defaultShapeSizePixels = displayDensity * DEFAULT_SIZE_DP;
        float defaultSpacingPixels = displayDensity * DEFAULT_SPACING_DP;

        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.ModuleStatusView, defStyle, 0);

        mOutlineColor = a.getColor(R.styleable.ModuleStatusView_outlineColor, Color.BLACK);
        mShape = a.getInt(R.styleable.ModuleStatusView_shape, SHAPE_CIRCLE);
        mOutlineWidth = a.getDimension(R.styleable.ModuleStatusView_outlineWidth, defaultOutlineWidthPixels);
        mShapeSize = a.getDimension(R.styleable.ModuleStatusView_shapeSize, defaultShapeSizePixels);
        mSpacing = a.getDimension(R.styleable.ModuleStatusView_spacing, defaultSpacingPixels);

        a.recycle();

        //mShapeSize = 144f;
        //mSpacing = 30f;
        mRadius = (mShapeSize - mOutlineWidth) / 2;

        mPaintOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintOutline.setStyle(Paint.Style.STROKE);
        mPaintOutline.setStrokeWidth(mOutlineWidth);
        mPaintOutline.setColor(mOutlineColor);

        mFillColor = getContext().getResources().getColor(R.color.pluralsight_orange);
        mPaintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintFill.setStyle(Paint.Style.FILL);
        mPaintFill.setColor(mFillColor);
    }

    private void setupEditModeValues() {
        boolean[] exampleModuleValues = new boolean[EDIT_MODE_MODULE_COUNT];
        int middle = EDIT_MODE_MODULE_COUNT / 2;
        for (int i = 0; i < middle; i++)
            exampleModuleValues[i] = true;

        setModuleStatus(exampleModuleValues);
    }

    private void setupModuleRectangles(int width) {
        int availableWidth = width - getPaddingLeft() - getPaddingRight();
        int horizontalModulesThatCanFit = (int) (availableWidth / (mShapeSize + mSpacing));
        int maxHorizontalModules = Math.min(horizontalModulesThatCanFit, mModuleStatus.length);
        mModuleRectangles = new Rect[mModuleStatus.length];
        for (int moduleIndex = 0; moduleIndex < mModuleRectangles.length; moduleIndex++) {
            int column = moduleIndex % maxHorizontalModules;
            int row = moduleIndex / maxHorizontalModules;
            int x = getPaddingLeft() + (int) (column * (mShapeSize + mSpacing));
            int y = getPaddingTop() + (int) (row * (mShapeSize + mSpacing));
            mModuleRectangles[moduleIndex] = new Rect(x, y, x + (int) mShapeSize, y + (int) mShapeSize);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = 0;
        int desiredHeight = 0;

        int specWidth = MeasureSpec.getSize(widthMeasureSpec);
        int availableWidth = specWidth - getPaddingLeft() - getPaddingRight();
        int horizontalModulesThatCanFit = (int) (availableWidth / (mShapeSize + mSpacing));
        mMaxHorizontalModules = Math.min(horizontalModulesThatCanFit, mModuleStatus.length);

        desiredWidth = (int) ((mMaxHorizontalModules * (mShapeSize + mSpacing)) - mSpacing);
        desiredWidth += getPaddingLeft() + getPaddingRight();

        int rows = ((mModuleStatus.length - 1) / mMaxHorizontalModules) + 1;
        desiredHeight = (int) ((rows * (mShapeSize + mSpacing)) - mSpacing);
        desiredHeight += getPaddingTop() + getPaddingBottom();

        int width = resolveSizeAndState(desiredWidth, widthMeasureSpec, 0);
        int height = resolveSizeAndState(desiredHeight, heightMeasureSpec, 0);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        setupModuleRectangles(w);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int moduleIndex = 0; moduleIndex < mModuleRectangles.length; moduleIndex++) {
            if (mShape == SHAPE_CIRCLE) {
                float x = mModuleRectangles[moduleIndex].centerX();
                float y = mModuleRectangles[moduleIndex].centerY();
                if (mModuleStatus[moduleIndex])
                    canvas.drawCircle(x, y, mRadius, mPaintFill);
                canvas.drawCircle(x, y, mRadius, mPaintOutline);
            } else {
                drawSquare(canvas, moduleIndex);
            }
        }
    }

    private void drawSquare(Canvas canvas, int moduleIndex) {
        Rect moduleRectangle = mModuleRectangles[moduleIndex];

        if (mModuleStatus[moduleIndex])
            canvas.drawRect(moduleRectangle, mPaintFill);

        canvas.drawRect(moduleRectangle.left + (mOutlineWidth / 2),
                moduleRectangle.top + (mOutlineWidth / 2),
                moduleRectangle.right - (mOutlineWidth / 2),
                moduleRectangle.bottom - (mOutlineWidth / 2),
                mPaintOutline);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_UP:
                int moduleIndex = findItemAtPoint(event.getX(), event.getY());
                onModuleSelected(moduleIndex);
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void onModuleSelected(int moduleIndex) {
        if (moduleIndex == INVALID_INDEX)
            return;

        mModuleStatus[moduleIndex] = !mModuleStatus[moduleIndex];
        invalidate();
    }

    private int findItemAtPoint(float x, float y) {
        int moduleIndex = INVALID_INDEX;
        for (int i = 0; i < mModuleRectangles.length; i++) {
            if (mModuleRectangles[i].contains((int) x, (int) y)) {
                moduleIndex = i;
                break;
            }
        }
        return moduleIndex;
    }
}