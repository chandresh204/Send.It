package chad.orionsoft.sendit

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.abs
import kotlin.math.min

class FullImageView : androidx.appcompat.widget.AppCompatImageView {

    private lateinit var scaleDetector:ScaleGestureDetector
    private var mode=NONE
    private lateinit var mat:Matrix
    private lateinit var m:FloatArray
    private var last=PointF()
    private var start=PointF()
    private var minScale=1f
    private var maxScale=3f
    private var saveScale=1f
    private var viewWidth =0f
    private var viewHeight = 0f
    private var origWidth =0f
    private var origHeight=0f
    private var oldMeasuredWidth=0f
    private var oldMeasuredHeight=0f

    constructor(ctx:Context) :super(ctx) { sharedConstructing(ctx) }
    constructor(ctx:Context,attrs:AttributeSet) : super(ctx,attrs) { sharedConstructing(ctx) }

    private fun sharedConstructing(ctx:Context) {

        scaleDetector= ScaleGestureDetector(ctx,ScaleListener())
        mat=Matrix()
        imageMatrix=mat
        m=FloatArray(9)
        scaleType=ScaleType.MATRIX

        setOnTouchListener { _, motionEvent ->
            scaleDetector.onTouchEvent(motionEvent)
            val curr=PointF(motionEvent.x,motionEvent.y)
            when(motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    last.set(curr)
                    start.set(last)
                    mode= DRAG
                }
                MotionEvent.ACTION_MOVE -> {
                    if(mode== DRAG) {
                        val deltaX=curr.x - last.x
                        val deltaY=curr.y - last.y
                        val fixTransX=getFixDragTrans(deltaX,viewWidth,origWidth*saveScale)
                        val fixTransY=getFixDragTrans(deltaY,viewHeight,origHeight*saveScale)
                        mat.postTranslate(fixTransX,fixTransY)
                        fixTrans()
                        last.set(curr.x,curr.y)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    mode= NONE
                    val xDiff= abs(curr.x-start.x).toInt()
                    val yDiff= abs(curr.y - start.y).toInt()
                    if(xDiff<CLICK && yDiff< CLICK)
                        performClick()
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    mode= NONE
                }
            }
            imageMatrix=mat
            invalidate()
            true
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth=MeasureSpec.getSize(widthMeasureSpec).toFloat()
        viewHeight=MeasureSpec.getSize(heightMeasureSpec).toFloat()

        if(oldMeasuredWidth==viewWidth && oldMeasuredHeight==viewHeight || viewWidth==0f || viewHeight==0f) {
            return
        }
        oldMeasuredHeight=viewHeight
        oldMeasuredWidth=viewWidth
        if(saveScale==1f) {
            val drawable= drawable
            if(drawable ==null || drawable.intrinsicWidth==0 || drawable.intrinsicHeight==0)
                return
            val bmWidth=drawable.intrinsicWidth
            val bmHeight=drawable.intrinsicHeight
            val scaleX=viewWidth/bmWidth
            val scaleY=viewHeight/bmHeight
            val scale= min(scaleX,scaleY)
            mat.setScale(scale,scale)

            var redundantYSpace=viewHeight-(scale*bmHeight)
            var redundantXSpace=viewWidth-(scale*bmWidth)
            redundantYSpace/=2f
            redundantXSpace/=2f
            mat.postTranslate(redundantXSpace,redundantYSpace)
            origWidth=viewWidth-2*redundantXSpace
            origHeight=viewHeight-2*redundantYSpace
            imageMatrix=mat
        }
        fixTrans()
    }

    private fun getFixDragTrans(delta:Float,viewSize:Float,contentSize:Float) : Float{
        if(contentSize<=viewSize) {
            return 0f
        }
        return delta
    }

    private fun fixTrans() {
        mat.getValues(m)
        val transX=m[Matrix.MTRANS_X]
        val transY=m[Matrix.MTRANS_Y]
        val fixTransX=getFixTrans(transX,viewWidth,origWidth*saveScale)
        val fixTransY=getFixTrans(transY,viewHeight,origHeight*saveScale)
        if(fixTransX!=0f || fixTransY!=0f)
            mat.postTranslate(fixTransX,fixTransY)
    }

    private fun getFixTrans(trans:Float,viewSize:Float,contentSize: Float) :Float {
        val minTrans:Float
        val maxTrans:Float
        if(contentSize<=viewSize) {
            minTrans=0f
            maxTrans=viewSize-contentSize
        } else {
            minTrans=viewSize-contentSize
            maxTrans=0f
        }
        if(trans<minTrans) {
            return -trans+minTrans
        }
        if(trans>maxTrans) {
            return -trans+maxTrans
        }
        return 0f
    }

    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            mode=ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            var scaleFactor=detector?.scaleFactor as Float
            val origScale=saveScale
            saveScale *= scaleFactor
            if(saveScale>maxScale) {
                saveScale=maxScale
                scaleFactor=maxScale/origScale
            } else if(saveScale<minScale) {
                saveScale=minScale
                scaleFactor= minScale/origScale
            }
            if(origWidth*saveScale <=viewWidth || origHeight*saveScale <= viewHeight)
                mat.postScale(scaleFactor,scaleFactor,viewWidth/2,viewHeight/2)
            else
                mat.postScale(scaleFactor,scaleFactor,detector.focusX,detector.focusY)
            fixTrans()
            return true
        }
    }

    companion object {
        const val NONE=0
        const val DRAG=1
        const val ZOOM=2
        const val CLICK=3
    }

}