package com.zulfanahmadi.kidsdrawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

// This class is said will be visible inside of MainActivity and act as the view for it
// ini adalah sebuah class yang digunakan untuk membuat view, supaya bisa di gunakan di XML sebagai View Object
// karena aplikasi ini digunakan untuk menggambar, android tidak menyediakan object view untuk menggambar
// sehingga kita perlu membuat sendiri view object tersebut.
class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var mDrawPath : CustomPath? = null
    private var mCanvasBitmap: Bitmap? = null
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null
    private var mBrushSize: Float = 0.toFloat()
    private var color = Color.BLACK

    /**
     * A Variable for canvas which will be initialized later and used
     * The Canvas class holds the "draw" calls. To draw something, you need 4 basic components which are
     * the draw calls (writing into the bitmap), a drawing primitive (e.g. Rect, Path, text, Bitmap), and a paint
     * (to describe the colors and styles for the drawing)
     */
    private var canvas: Canvas? = null
    private val mPaths = ArrayList<CustomPath>() //where we store the path/ path yang menyebabkan apa yang kita gambar tergambar di canvas,
    // atau tepatnya ini adalah arraylist yang berisikan apa yang kita gambar, lalu valuenya di simpan dan di oper ke lainnya
    private val mUndoPaths = ArrayList<CustomPath>() //menyimpan path untuk di undo/hapus , karena kita mau delete entry dari mpath dan store ke undopath

    init {
        setUpDrawing()

    }

    fun onClickUndo(){
        if (mPaths.size > 0) {
            mUndoPaths.add(mPaths.removeAt(mPaths.size -1)) //remove the last value inside an array with index,
        // the size of list, or the last position -1, then adding it to the undopath, but just this wont take care the undraw method
        invalidate()
        }
    }

    private fun setUpDrawing(){
        // this is where we setup all the variable that we write up there, after the setup the variable become
        // for example mDrawPaint variable become the object of Paint class, and so are mDrawPath become the object
        // of CustomPath inner class that we have created
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color, mBrushSize)
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
//        mBrushSize = 20.toFloat()

    }

    // how we display the bitmap is using the function onSizeChanged from the View class.
    // because we want to display the view as Canvas.
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)


    }

    /**
     * This method is called when a stroke is drawn on the canvas as a part of the painting
     */
    //what should happen when we want to draw on our canvas use this function
    // Change Canvas to Canvas? if fails
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        /**
         * Draw the specified bitmap, with its top/left corner at (x,y), using the specified parameter transformed by the current matrix
         *
         * if the bitmap and canvas have different densities, this function will take care of it by
         * automatically scaling the bitmap to draw at the same densiriy as the canvas
         *
         * @param bitmap The bitmap to be drawn
         * @param left the position of the left side of the bitmap being drawn
         * @param top The position of the top side of the bitmap being drawn
         * @param paint The paint used to draw the bitmap(may be null)
         */

        canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)

        for (path in mPaths) { //using for and mPaths array to make the drawing persist on the screen
            mDrawPaint!!.strokeWidth = path.brushThickness //this is set how thick the paint should be
            mDrawPaint!!.color = path.color
            canvas.drawPath(path, mDrawPaint!!)
        }

        if (!mDrawPath!!.isEmpty){
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness //this is set how thick the paint should be
            mDrawPaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }

    }

    //there are 3 most important motion event/ action for onTouchEvent, first Action_Down, when a finger touching the screen, then Action_Move when our finger drag across the screen
    //after that Action_Up when our finger touch release the screen
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize //this is set how thick the Path should be

                mDrawPath!!.reset()
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.moveTo(touchX, touchY)
                    }
                }
            }
            MotionEvent.ACTION_MOVE ->{
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.lineTo(touchX, touchY)
                    }
                }
            }
            MotionEvent.ACTION_UP ->{
                mPaths.add(mDrawPath!!) //using mPaths array to make the drawing persist on the screen
                mDrawPath = CustomPath(color, mBrushSize)
            }
            else -> return false
        }
        invalidate()

        return true
    }

    fun setSizeForBrush(newSize : Float) {
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                newSize, resources.displayMetrics//how to get metrics
            )
        mDrawPaint!!.strokeWidth = mBrushSize
    }

    fun setColor(newColor: String) {
        color = Color.parseColor(newColor)
        mDrawPaint!!.color = color
    }

    // An inner class for custom path with two parasm as color and stroke size
    // internal inner class so the class can access the variable outside of the class from the parent class
    internal inner class CustomPath(var color: Int,
                                    var brushThickness: Float) : Path(){

    }


}