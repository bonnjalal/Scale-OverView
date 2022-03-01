package com.username.packagename;

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import kotlin.jvm.JvmOverloads
import android.view.MotionEvent
import android.graphics.drawable.Drawable
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import com.bonnjalal.pdfcropper.R
import java.util.ArrayList
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class ScaleView (
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = -1
) : View(context, attrs, defStyle) {
    var points = arrayOfNulls<Point>(4)

    /**
     * point1 and point 3 are of same group and same as point 2 and point4
     */
    var groupId = -1
    //private val colorballs = ArrayList<ColorBall>()
    private val mStrokeColor = Color.parseColor("#CAEAF8")
    private val mFillColor = Color.parseColor("#55A3B8F8")
    private val mCropRect = RectF()
    private val mTempRect = RectF()

    // array that holds the balls
    private var balID = 0

    // variable to know what ball is being dragged
    var paint: Paint? = null
    //var linePaint : Paint? = null

    private var mGridPoints: FloatArray? = null
    //private val mCropViewRect = RectF()
    private var mCropGridRowCount = 4
    private  var mCropGridColumnCount = 3
    private val mCropGridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mCropFramePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mTouchPointThreshold = 0
    private var mCropRectMinSize = 0
    private lateinit var mCropGridCorners: FloatArray
    ///private var barX = -1
    //private var barY = -1
    private lateinit var mPrevious: PointF
    private lateinit var bitmapCorner :Bitmap

    private var mShowCropGrid = true
    private var mDimmedColor = mFillColor
    private var mEnableDrag = true
    private var mGridColor = mStrokeColor
    private var mStrokeSize = 3f
    private var mBitmapId = R.drawable.baseline_fiber_manual_record_grey_400_18dp
    private var mBitmapDrawable : Drawable? = null
    private var mCropGridSize = 2f

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
        Log.e("mBitmapId 1: ", " id $mBitmapId")
        //mBitmapId = R.drawable.baseline_fiber_manual_record_grey_400_18dp
        val ta = context.obtainStyledAttributes(attrs, R.styleable.ScaleView, 0, 0)
        try {
            mCropGridColumnCount = ta.getInteger(R.styleable.ScaleView_ScaleView_grid_column_count, 3)
            mCropGridRowCount = ta.getInteger(R.styleable.ScaleView_ScaleView_grid_row_count, 4)
            mDimmedColor = ta.getColor(R.styleable.ScaleView_ScaleView_dimmed_color, mFillColor)
            mGridColor = ta.getColor(R.styleable.ScaleView_ScaleView_grid_color, mStrokeColor)
            mStrokeSize = ta.getDimension(R.styleable.ScaleView_ScaleView_stroke_size, mStrokeSize)
            mShowCropGrid = ta.getBoolean(R.styleable.ScaleView_ScaleView_show_crop_grid, true)
            mEnableDrag = ta.getBoolean(R.styleable.ScaleView_ScaleView_enable_drag, true)
            mBitmapId = ta.getResourceId(R.styleable.ScaleView_ScaleView_corner_bitmap, R.drawable.baseline_fiber_manual_record_grey_400_18dp)
            Log.e("mBitmapId 2: ", " id $mBitmapId")
            //mBitmapId = R.drawable.baseline_fiber_manual_record_grey_400_18dp
            mCropGridSize = ta.getDimension(R.styleable.ScaleView_ScaleView_crop_grid_size, mCropGridSize)
        } finally {
            ta.recycle()
        }

        mTouchPointThreshold =
            resources.getDimensionPixelSize(R.dimen.ucrop_default_crop_rect_corner_touch_threshold)

        mCropRectMinSize = resources.getDimensionPixelSize(R.dimen.ucrop_default_crop_rect_min_size)

        paint = Paint()
        //linePaint = Paint()
        isFocusable = true // necessary for getting the touch events
        mPrevious = PointF()
        bitmapCorner = BitmapFactory.decodeResource(
            context.resources,
            mBitmapId
        )
    }

    /**
     * The possible grabpoint of the rectangle.
     */
    private enum class TouchAction {
        DRAG, LEFTTOP, RIGHTTOP, RIGHTBOTTOM, LEFTBOTTOM, LEFTSIDE, RIGHTSIDE, BOTTOMSIDE, TOPSIDE
    }
    private fun initRectangle(X: Int, Y: Int) {
        //initialize rectangle.
        points[0] = Point()
        points[0]!!.x = X
        points[0]!!.y = Y
        points[1] = Point()
        points[1]!!.x = X
        points[1]!!.y = Y + 200
        points[2] = Point()
        points[2]!!.x = X // + 200;
        points[2]!!.y = Y // + 200;
        points[3] = Point()
        points[3]!!.x = X + 200
        points[3]!!.y = Y
        balID = 2
        groupId = 1

        var left: Int
        var top: Int
        var right: Int
        var bottom: Int
        left = points[0]!!.x
        top = points[0]!!.y
        right = points[0]!!.x
        bottom = points[0]!!.y
        for (i in 1 until points.size) {
            left = Math.min(left, points[i]!!.x)
            top = Math.min(top, points[i]!!.y)
            right = Math.max(right, points[i]!!.x) // +50);
            bottom = Math.max(bottom, points[i]!!.y) // +50);
        }

        mCropRect.left = left.toFloat()
        mCropRect.top = top.toFloat()
        mCropRect.right = right.toFloat()
        mCropRect.bottom = bottom.toFloat()
        
    }

    // the method that draws the balls
    override fun onDraw(canvas: Canvas) {
        if (points[3] == null) {
            //point4 null when view first create
            initRectangle(width / 2, height / 2)
        }


        paint!!.isAntiAlias = true
        paint!!.isDither = true
        paint!!.strokeJoin = Paint.Join.ROUND
       
        //draw stroke
        paint!!.style = Paint.Style.STROKE
        paint!!.color = mGridColor
        paint!!.strokeWidth = mStrokeSize
        //mCropRect.left = left.toFloat()//(left + colorballs[0].widthOfBall / 2).toFloat()
        //mCropRect.top = top.toFloat()//(top + colorballs[1].widthOfBall / 2).toFloat()
        //mCropRect.right = right.toFloat()//(right + colorballs[2].widthOfBall / 2).toFloat()
        //mCropRect.bottom = bottom.toFloat()//(bottom + colorballs[3].widthOfBall / 2).toFloat()
        canvas.drawRect(mCropRect, paint!!)

        //fill the rectangle
        paint!!.style = Paint.Style.FILL
        paint!!.color = mDimmedColor
        paint!!.strokeWidth = 0f
        canvas.drawRect(mCropRect, paint!!)

        // draw the balls on the canvas
        paint!!.color = Color.WHITE
        paint!!.textSize = 18f
        paint!!.strokeWidth = 0f

        mCropGridCorners = getCornersFromRect(mCropRect)
        var i = 0
        while (i < 8) {
            canvas.drawBitmap(
                bitmapCorner, mCropGridCorners[i] - (bitmapCorner.width/2), mCropGridCorners[i+1] - (bitmapCorner.width/2),
                paint
            )
            i += 2
        }
        /*
        for (i in colorballs.indices) {
            val ball = colorballs[i]
            canvas.drawBitmap(
                ball.bitmap, ball.x.toFloat(), ball.y.toFloat(),
                paint
            )
            //canvas.drawText("" + (i + 1), ball.x.toFloat(), ball.y.toFloat(), paint!!)

        }

         */

        if (mShowCropGrid){
            drawCropGrid(canvas)
        }

    }

    /**
     * Setter for crop grid rows count.
     * Resets [.mGridPoints] variable because it is not valid anymore.
     */
    fun setCropGridRowCount(@IntRange(from = 0) cropGridRowCount: Int) {
        mCropGridRowCount = cropGridRowCount
        mGridPoints = null
    }

    /**
     * Setter for crop grid columns count.
     * Resets [.mGridPoints] variable because it is not valid anymore.
     */
    fun setCropGridColumnCount(@IntRange(from = 0) cropGridColumnCount: Int) {
        mCropGridColumnCount = cropGridColumnCount
        mGridPoints = null
    }

    /**
     * Setter for [.mShowCropGrid] variable.
     *
     * @param showCropGrid - set to true if you want to see a crop grid on top of an image
     */
    fun setShowCropGrid(showCropGrid: Boolean) {
        mShowCropGrid = showCropGrid
    }

    /**
     * Setter for [.mShowCropGrid] variable.
     *
     * @param enableDrag - set to true if you want to see a crop grid on top of an image
     */
    fun setEnableDrag(enableDrag: Boolean) {
        mEnableDrag = enableDrag
    }

    /**
     * Setter for [.mDimmedColor] variable.
     *
     * @param dimmedColor - desired color of dimmed area around the crop bounds
     */
    fun setDimmedColor(@ColorInt dimmedColor: Int) {
        mDimmedColor = dimmedColor
    }

    /**
     * Setter for crop grid stroke width
     */
    fun setCropGridStrokeWidth(@IntRange(from = 0) width: Int) {
        mCropGridPaint.strokeWidth = width.toFloat()
    }

    /**
     * Setter for crop grid color
     */
    fun setCropGridColor(@ColorInt color: Int) {
        mCropGridPaint.color = color
    }

    private fun drawCropGrid(canvas: Canvas) {

        mCropGridPaint.strokeWidth = mCropGridSize
        mCropGridPaint.color = mGridColor

        mGridPoints = FloatArray(mCropGridRowCount * 4 + mCropGridColumnCount * 4)
        var index = 0
        for (i in 0 until mCropGridRowCount) {
            mGridPoints!![index++] = mCropRect.left.toFloat()
            mGridPoints!![index++] =
                mCropRect.height() * ((i.toFloat() + 1.0f) / (mCropGridRowCount + 1).toFloat()) + mCropRect.top
            mGridPoints!![index++] = mCropRect.right.toFloat()
            mGridPoints!![index++] =
                mCropRect.height() * ((i.toFloat() + 1.0f) / (mCropGridRowCount + 1).toFloat()) + mCropRect.top
        }
        for (i in 0 until mCropGridColumnCount) {
            mGridPoints!![index++] =
                mCropRect.width() * ((i.toFloat() + 1.0f) / (mCropGridColumnCount + 1).toFloat()) + mCropRect.left
            mGridPoints!![index++] = mCropRect.top.toFloat()
            mGridPoints!![index++] =
                mCropRect.width() * ((i.toFloat() + 1.0f) / (mCropGridColumnCount + 1).toFloat()) + mCropRect.left
            mGridPoints!![index++] = mCropRect.bottom.toFloat()
        }

        canvas.drawLines(mGridPoints!!, mCropGridPaint)

    }

    // events when touching the screen
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val eventAction = event.action
        val X = event.x
        val Y = event.y
        when (eventAction) {
            MotionEvent.ACTION_DOWN ->                 // a ball
                if (points[0] == null) {
                    initRectangle(X.toInt(), Y.toInt())
                } else {
                    //resize rectangle
                    balID = -1
                    groupId = -1

                    paint!!.color = Color.CYAN
                    mPrevious.set(event.x, event.y)
                    groupId = getCurrentTouchIndex(event.x, event.y)
                    invalidate()

                }
            MotionEvent.ACTION_MOVE -> if(event.pointerCount == 1) {
                paint!!.color = Color.CYAN

                updateCropViewRect(event.x,event.y)
                invalidate()
                mPrevious.set(event.x, event.y)
            }
            MotionEvent.ACTION_UP -> {
                mPrevious = PointF()
            }
        }
        // redraw the canvas
        invalidate()
        return true
    }

    private fun updateCropViewRect(touchX: Float, touchY: Float) {
        mTempRect.set(mCropRect)
        when (groupId) {
            0 -> mTempRect.set(touchX, touchY, mCropRect.right, mCropRect.bottom)
            1 -> mTempRect.set(mCropRect.left, touchY, touchX, mCropRect.bottom)
            2 -> mTempRect.set(mCropRect.left, mCropRect.top, touchX, touchY)
            3 -> mTempRect.set(touchX, mCropRect.top, mCropRect.right, touchY)
            4 -> mTempRect.set(mCropRect.left, touchY, mCropRect.right, mCropRect.bottom)
            5 -> mTempRect.set(mCropRect.left, mCropRect.top, touchX, mCropRect.bottom)
            6 -> mTempRect.set(mCropRect.left, mCropRect.top, mCropRect.right, touchY)
            7 -> mTempRect.set(touchX, mCropRect.top, mCropRect.right, mCropRect.bottom)
            8 -> {
                mTempRect.offset(touchX - mPrevious.x, touchY - mPrevious.y)
                if (mTempRect.left > left && mTempRect.top > top && mTempRect.right < right && mTempRect.bottom < bottom) {
                    mCropRect.set(mTempRect)
                    //updateGridPoints()
                    invalidate()
                }
                return
            }
        }
        val changeHeight: Boolean = mTempRect.height() >= mCropRectMinSize
        val changeWidth: Boolean = mTempRect.width() >= mCropRectMinSize
        mCropRect.set(
            if (changeWidth) mTempRect.left else mCropRect.left,
            if (changeHeight) mTempRect.top else mCropRect.top,
            if (changeWidth) mTempRect.right else mCropRect.right,
            if (changeHeight) mTempRect.bottom else mCropRect.bottom
        )
        if (changeHeight || changeWidth) {
            //updateGridPoints()
            invalidate()
        }
    }


    private fun getCurrentTouchIndex(touchX: Float, touchY: Float): Int {
        mCropGridCorners = getCornersFromRect(mCropRect)
        var closestPointIndex = -1
        var closestPointDistance: Double = mTouchPointThreshold.toDouble()
        var i = 0

        while (i < 8) {

            val distanceToCorner = sqrt(
                (touchX - mCropGridCorners[i]).toDouble().pow(2.0)
                        + (touchY - mCropGridCorners[i + 1]).toDouble().pow(2.0)
            )
            if (distanceToCorner < closestPointDistance) {
                closestPointDistance = distanceToCorner
                closestPointIndex = i / 2
            }
            i += 2
        }

        

        return if (closestPointIndex >= 0){
            closestPointIndex
        } else if (mEnableDrag &&
            touchY > (mCropRect.top + closestPointDistance) &&
            touchY < (mCropRect.bottom - closestPointDistance) &&
            touchX > (mCropRect.left + closestPointDistance) &&
            touchX < (mCropRect.right - closestPointDistance)) {
            8
        }else if (abs(touchY - mCropRect.top) < closestPointDistance &&
            touchX  >  (mCropRect.left + closestPointDistance) &&
            touchX  <  (mCropRect.right - closestPointDistance)) {
            4
        }else if (abs(touchX - mCropRect.right) < closestPointDistance &&
            touchY  >  (mCropRect.top + closestPointDistance) &&
            touchY  <  (mCropRect.bottom - closestPointDistance)) {
            5
        }else if (abs(touchY - mCropRect.bottom) < closestPointDistance &&
            touchX  >  (mCropRect.left + closestPointDistance) &&
            touchX  <  (mCropRect.right - closestPointDistance)) {
            6
        }else if (abs(touchX - mCropRect.left) < closestPointDistance &&
            touchY  >  (mCropRect.top + closestPointDistance) &&
            touchY  <  (mCropRect.bottom - closestPointDistance)) {
            7
        }else{
            -1
        }

    }

    private fun getCornersFromRect(r: RectF): FloatArray {
        return floatArrayOf(
            r.left, r.top,
            r.right, r.top,
            r.right, r.bottom,
            r.left, r.bottom
        )
    }

    val cropHeight: Int
        get() {
            return mCropRect.bottom.toInt() - mCropRect.top.toInt()
        }
    val cropWidth: Int
        get() {
            return mCropRect.right.toInt() - mCropRect.left.toInt()
        }
    val cropLeft: Int
        get() = mCropRect.left.toInt()
    val cropTop: Int
        get() = mCropRect.top.toInt()

    val cropBottom: Int
        get() = mCropRect.bottom.toInt()

    val cropRight: Int
        get() = mCropRect.right.toInt()


    val cropRect: Rect
        get() = Rect(mCropRect.left.toInt(),mCropRect.top.toInt(), mCropRect.right.toInt(), mCropRect.bottom.toInt() )

}
