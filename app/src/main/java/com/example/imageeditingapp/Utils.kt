package com.example.imageeditingapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.scale
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class Utils {

    companion object {
        //Convert Uri To Bitmap
        @Throws(FileNotFoundException::class, IOException::class)
        fun getBitmap(uri: Uri?, context: Context): Bitmap? {
            val input: InputStream = context.contentResolver.openInputStream(uri!!)!!
            val bitmap = BitmapFactory.decodeStream(input, null, null)
            input.close()
            return bitmap
        }

        //Make Toast
        fun toast(context: Context, message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        //Set View Margins
        fun View.setMargins(
            leftMarginDp: Int? = null,
            topMarginDp: Int? = null,
            rightMarginDp: Int? = null,
            bottomMarginDp: Int? = null
        ) {
            if (layoutParams is ViewGroup.MarginLayoutParams) {
                val params = layoutParams as ViewGroup.MarginLayoutParams
                leftMarginDp?.run { params.leftMargin = this.dpToPx(context) }
                topMarginDp?.run { params.topMargin = this.dpToPx(context) }
                rightMarginDp?.run { params.rightMargin = this.dpToPx(context) }
                bottomMarginDp?.run { params.bottomMargin = this.dpToPx(context) }
                requestLayout()
            }
        }

        private fun Int.dpToPx(context: Context): Int {
            val metrics = context.resources.displayMetrics
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), metrics)
                .toInt()
        }
    }
}