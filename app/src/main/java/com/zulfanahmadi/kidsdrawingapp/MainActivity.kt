package com.zulfanahmadi.kidsdrawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vadiole.colorpicker.ColorModel
import vadiole.colorpicker.ColorPickerDialog
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception


//in this apps you will find another way to handle click image
//in this app we use LinearLayout to act as an array/list to access the imagebutton,
// and not using ID, these image inside linearlayout act as if they have indexes
class MainActivity : AppCompatActivity() {

    private var drawingView : DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    private var colorDef = Color.BLACK
    private var mDefaultColor: Int = 0
    private var colorTag: String? = null
    var customProgressDialog : Dialog? = null

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
            if (result.resultCode == RESULT_OK && result.data!=null) {
                val imageBackGround: ImageView = findViewById(R.id.iv_background)

                imageBackGround.setImageURI(result.data?.data)
            }

        }

    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permission ->
            permission.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value

                if (isGranted) {
                    Toast.makeText(this@MainActivity,
                    "Permission granted now you can read the storage files.",
                    Toast.LENGTH_LONG)
                        .show()

                    val pickIntent = Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                }else{
                    if (permissionName==Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(this@MainActivity,
                            "Oops you just denied the permission.",
                            Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())


        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)

        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton //berarti kita mau menggunakan item di linearLayoutPaintColors index 1 and treated it like ImageButton


        val ib_brush : ImageButton = findViewById(R.id.ib_brush)
        ib_brush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        val ibUndo : ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener {
            drawingView?.onClickUndo()
        }

        val ibSave : ImageButton = findViewById(R.id.ib_save)
        ibSave.setOnClickListener {

            if (isReadStorageAllowed()) {
                showProgressDialog()
                lifecycleScope.launch {
                    val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
//                    val myBitmap : Bitmap = getBitmapFromView(flDrawingView)
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }

        }

        val ibGallery : ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener {
            requestStoragePermission()
        }


    }
    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")
        val smallBtn : ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn : ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn : ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()


    }

    fun paintClicked(view: View){
        //make the the CurrentPaint/color you are clicked is not the same, you wont cant choose the same color twice
        if (view != mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton
            colorTag = imageButton.tag.toString() //get the tag attribute from imageButton view which is clicked
            drawingView?.setColor(colorTag!!)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )

            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )

            mImageButtonCurrentPaint = view

        }

    }

    private fun isReadStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(this,
        Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
        ){
            showRationalDialog("Kids Drawing App","Kids Drawing App " +
            "needs to Acces Your External Storage")
        }else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }

    //getBitmapFromView, the view is FrameLayout, put it inside the parameter
    private fun getBitmapFromView(view: View): Bitmap {
        //Define a bitmap with the same size as the view.
        // CreateBitmap : Returns a mutable bitmap with the specified width and height
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

        //bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        //Get the view's background
        val bgDrawable = view.background
        if (bgDrawable != null) {
            // has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas)
        }else{
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE)
        }
        //draw the view on the canvas
        view.draw(canvas)
        //return the bitmap
        return returnedBitmap
    }


    //get the bitmap from the method getBitmapFromView
    private suspend fun saveBitmapFile(mBitmap: Bitmap?) : String {
        var result = ""
        withContext(Dispatchers.IO){
            if (mBitmap != null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    //this is where we store the image inside our phone
                    val f = File(externalCacheDir?.absoluteFile.toString()
                    + File.separator + "KidsDrawingApp_" + System.currentTimeMillis() / 1000 + ".png"
                    )

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()
                        if (result.isNotEmpty()) {
                            Toast.makeText(this@MainActivity,
                            "File saved successfully :$result",
                            Toast.LENGTH_SHORT)
                                .show()
                            shareImage(result)
                        }else{
                            Toast.makeText(this@MainActivity,
                            "Something went wrong while saving the file.",
                            Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }catch (e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun showRationalDialog(
        title: String,
        message: String
    ){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    /**
     * Method is used to show the Custom Progress Dialog.
     */
    private fun showProgressDialog() {
        customProgressDialog = Dialog(this@MainActivity)

        /* Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen. */
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        //Start the dialog and display it on screen.
        customProgressDialog?.show()

    }

    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
            }
    }

    private fun shareImage(result: String) {
        MediaScannerConnection.scanFile(this, arrayOf(result), null) {
            path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

    fun openColorPickerDialogue(view: View) {

        // the AmbilWarnaDialog callback needs 3 parameters
        // one is the context, second is default color,

        val colorPicker: ColorPickerDialog = ColorPickerDialog.Builder()

            //  set initial (default) color
            .setInitialColor(colorDef)

            //  set Color Model. ARGB, RGB or HSV
            .setColorModel(ColorModel.RGB)

            //  set is user be able to switch color model
            .setColorModelSwitchEnabled(true)

            //  set your localized string resource for OK button
            .setButtonOkText(android.R.string.ok)

            //  set your localized string resource for Cancel button
            .setButtonCancelText(android.R.string.cancel)

            //  callback for picked color (required)
            .onColorSelected { color: Int ->
                //  use color
                mDefaultColor = color

                // now change the picked color
                // preview box to mDefaultColor
                var mDefaultColorString: String = java.lang.String.format(
                    "#%06X",
                    0xFFFFFF and mDefaultColor
                )
                colorTag = mDefaultColorString

                drawingView?.setColor(mDefaultColorString)

                colorDef = mDefaultColor
            }

            //  create dialog
            .create()

        colorPicker.show(supportFragmentManager, "color_picker")

//        val colorPickerDialogue = AmbilWarnaDialog(this, mDefaultColor,
//            object : OnAmbilWarnaListener {
//                override fun onCancel(dialog: AmbilWarnaDialog) {
//                    // leave this function body as
//                    // blank, as the dialog
//                    // automatically closes when
//                    // clicked on cancel button
//                }
//
//                override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
//                    // change the mDefaultColor to
//                    // change the GFG text color as
//                    // it is returned when the OK
//                    // button is clicked from the
//                    // color picker dialog
//                    mDefaultColor = color
//
//                    // now change the picked color
//                    // preview box to mDefaultColor
//                    var mDefaultColorString: String = java.lang.String.format(
//                        "#%06X",
//                        0xFFFFFF and mDefaultColor
//                    )
//                    colorTag = mDefaultColorString
//
//                    drawingView?.setColor(mDefaultColorString)
//
//                }
//            })
//        colorPickerDialogue.show()
    }
}