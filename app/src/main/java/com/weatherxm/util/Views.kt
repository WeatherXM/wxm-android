package com.weatherxm.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import dev.chrisbanes.insetter.applyInsetter

@Suppress("EmptyFunctionBlock")
fun EditText.onTextChanged(callback: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            callback(s.toString())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

@Suppress("EmptyFunctionBlock")
fun TabLayout.onTabSelected(callback: (TabLayout.Tab) -> Unit) {
    this.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            tab?.let { callback(it) }
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {}
        override fun onTabReselected(tab: TabLayout.Tab?) {}
    })
}

fun Chip.setTextAndColor(@StringRes text: Int, color: Int) {
    this.setChipBackgroundColorResource(color)
    this.text = this.resources.getString(text)
}

fun CoordinatorLayout.applyTopInset() {
    this.applyInsetter {
        type(statusBars = true) {
            padding(left = false, top = true, right = false, bottom = false)
        }
    }
}

fun CoordinatorLayout.applyTopBottomInsets() {
    this.applyInsetter {
        type(statusBars = true) {
            padding(left = false, top = true, right = false, bottom = false)
        }
        type(navigationBars = true) {
            padding(left = false, top = false, right = false, bottom = true)
        }
    }
}

fun FloatingActionButton.showIfNot() {
    if (this.isOrWillBeHidden) {
        this.show()
    }
}

fun FloatingActionButton.hideIfNot() {
    if (this.isOrWillBeShown) {
        this.hide()
    }
}
