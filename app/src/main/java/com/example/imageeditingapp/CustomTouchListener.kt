package com.example.imageeditingapp

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View

class CustomTouchListener(private val screenWidth: Int, private val screenHeight: Int) :
    View.OnTouchListener {

    private var dX: Float = 0.toFloat()
    private var dY: Float = 0.toFloat()


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {

        val newX: Float
        val newY: Float

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dX = view.x - event.rawX
                dY = view.y - event.rawY
            }
            MotionEvent.ACTION_MOVE -> {

                newX = event.rawX + dX
                newY = event.rawY + dY

                if ((newX <= 0 || newX >= screenWidth - view.width) || (newY <= 0 || newY >= screenHeight - view.height)) {
                    return true
                }

                view.animate()
                    .x(newX)
                    .y(newY)
                    .setDuration(0)
                    .start()
            }
            else -> return true
        }
        return true
    }
}