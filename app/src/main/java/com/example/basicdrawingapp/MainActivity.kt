package com.example.basicdrawingapp

import android.app.Activity
import android.app.Dialog
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.example.backgroundexecution.CoroutineAsyncTask
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.jar.Manifest
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity() {

    private var mImageButtonCurrentPaint: ImageButton? = null

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawing_view.setSizeForBrush(10.toFloat())

        mImageButtonCurrentPaint = ll_paint_colors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_selected)
        )


        ib_brush.setOnClickListener {
            showBrushSizeDialogBox()
        }

        ib_gallery.setOnClickListener {
            if (isReadStorageAllowed()) {
                //run our code to get the image from the gallery
                val pickPhotoIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                    startActivityForResult(pickPhotoIntent, GALLERY)
            } else {
                requestPermissionStorage()
            }
        }

        ib_undo.setOnClickListener {
//            Toast.makeText(this,"undo btn clicked",Toast.LENGTH_LONG).show()
            drawing_view.onClickUndo()
        }

        ib_redo.setOnClickListener {
//            Toast.makeText(this,"redo btn clicked",Toast.LENGTH_LONG).show()
            drawing_view.onClickRedo()
        }

        ib_save.setOnClickListener {
            if (isReadStorageAllowed()) {
                BitmapAsyncTask(getBitmapFromView(fl_drawing_view_container)).execute()
            } else {
                requestPermissionStorage()
            }
        }


        ib_delete.setOnClickListener {
//            Toast.makeText(this,"redo btn clicked",Toast.LENGTH_LONG).show()
            drawing_view.onClickDelete()
            deleteBackgroundImage()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY) {
                try {
                    if (data!!.data != null) {
                        Toast.makeText(this, "Image chosen", Toast.LENGTH_LONG).show()
                        iv_bg.visibility = View.VISIBLE
                        iv_bg.setImageURI(data.data)
                    } else {
                        Toast.makeText(
                            this,
                            "Error in parsing the image or its corrupted",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun deleteBackgroundImage(){
        iv_bg.visibility = View.INVISIBLE
        iv_bg.setImageURI(null)
    }

    private fun requestPermissionStorage() {

        //return a boolean
        //needs to convert to string
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).toString()
            )
        ) {
            Toast.makeText(this, "Need permission to add a background", Toast.LENGTH_SHORT).show()
        }

        ActivityCompat.requestPermissions(
            this, arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), STORAGE_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Permission granted now you can read the storage file",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Oops you just denied the permission \nPermission is needed to add background",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    //helper function
    private fun isReadStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun showBrushSizeDialogBox() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size : ")

        val thinBtn = brushDialog.ib_thin_brush
        thinBtn.setOnClickListener {
            drawing_view.setSizeForBrush(5.toFloat())
            brushDialog.dismiss()
        }


        val smallBtn = brushDialog.ib_small_brush
        smallBtn.setOnClickListener {
            drawing_view.setSizeForBrush(12.toFloat())
            brushDialog.dismiss()
        }

        val mediumBtn = brushDialog.ib_medium_brush
        mediumBtn.setOnClickListener {
            drawing_view.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val largeBtn = brushDialog.ib_large_brush
        largeBtn.setOnClickListener {
            drawing_view.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    //view is button that is clicked
    fun paintClicked(view: View) {
        if (view != mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton

            val colorTag = imageButton.tag.toString()
            drawing_view.setColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_selected)
            )
            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )
            mImageButtonCurrentPaint = view
        }
    }


    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)

        return returnedBitmap
    }

    private inner class BitmapAsyncTask(val mBitmap: Bitmap) :
        CoroutineAsyncTask<Int, Int, String>() {

        private lateinit var mProgressDialog: Dialog

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        override fun doInBackground(vararg params: Int?): String {

            var result = ""

            if (mBitmap != null)
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f =
                        File(externalCacheDir!!.absoluteFile.toString() + File.separator + "BasicDrawingApp_" + System.currentTimeMillis() / 1000 + ".png")

                    val fos = FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    result = f.absolutePath
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }

            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelProgressDialog()

            if (result != null) {
                if (result.isNotEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "File Saved Successfully :$result",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Something went wrong while saving the file.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }


            //share
            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null) { path, uri ->
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type = "image/png"  //mime type of the data being handled
                startActivity(
                    //allow to pick
                    //by the system
                    Intent.createChooser(
                        shareIntent, "Share"
                    )
                )
            }
        }

        private fun showProgressDialog() {
            mProgressDialog = Dialog(this@MainActivity)
            mProgressDialog.setContentView(R.layout.dialog_custom_progress)
            mProgressDialog.show()
        }

        private fun cancelProgressDialog() {
            mProgressDialog.dismiss()
        }


    }
}