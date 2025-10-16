package com.foxynest.testappforonboarding.other.utils

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.inputmethod.InputMethodManager
import com.google.android.material.textfield.TextInputEditText

//class BaseTextInputEditText(context: Context, attrs: AttributeSet?) : TextInputEditText(context, attrs){
//    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
//        super.onFocusChanged(focused, direction, previouslyFocusedRect)
//        if (!focused) this.hideKeyboard()
//    }
//}

class BaseTextInputEditText(context: Context, attrs: AttributeSet?) : TextInputEditText(context, attrs){

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (!focused) {
            hideKeyboard()
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }
}