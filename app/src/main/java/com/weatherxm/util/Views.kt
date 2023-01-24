@file:Suppress("TooManyFunctions")

package com.weatherxm.util

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.core.content.res.ResourcesCompat.getDrawable
import androidx.core.text.HtmlCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.components.MarkerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.weatherxm.R
import com.weatherxm.ui.common.hide
import com.weatherxm.ui.common.show
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

fun EditText.clear() {
    this.setText("")
}

fun ImageView.setWarningDrawable(context: Context) {
    val drawable = getDrawable(resources, R.drawable.ic_warning, context.theme)
    drawable?.setTint(getColor(resources, R.color.warning, context.theme))
    this.setImageDrawable(drawable)
}

fun ImageView.setBluetoothDrawable(context: Context) {
    val drawable = getDrawable(resources, R.drawable.ic_bluetooth, context.theme)
    drawable?.setTint(getColor(resources, R.color.light_mid_grey, context.theme))
    this.setImageDrawable(drawable)
}

fun ImageView.setNoDevicesFoundDrawable(context: Context) {
    val drawable = getDrawable(resources, R.drawable.ic_no_devices_found, context.theme)
    drawable?.setTint(getColor(resources, R.color.light_mid_grey, context.theme))
    this.setImageDrawable(drawable)
}

fun MaterialButton.disable(context: Context) {
    this.isEnabled = false
    this.setBackgroundColor(resources.getColor(R.color.light_mid_grey, context.theme))
    this.setTextColor(resources.getColor(R.color.light_dark_grey, context.theme))
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

fun Chip.setIcon(@DrawableRes drawable: Int) {
    this.chipIcon = getDrawable(resources, drawable, context.theme)
}

fun Chip.setSuccessChip() {
    this.setChipBackgroundColorResource(R.color.success_tint)
    this.chipIcon = getDrawable(resources, R.drawable.ic_checkmark, context.theme)
    this.setChipIconTintResource(R.color.dark_background)
    this.setTextColor(getColor(resources, R.color.dark_background, context.theme))
}

fun ImageView.setColor(@ColorRes color: Int) {
    this.setColorFilter(getColor(resources, color, null))
}

fun ViewGroup.applyInsets(top: Boolean = true, bottom: Boolean = true) {
    this.applyInsetter {
        type(statusBars = top) {
            padding(left = false, top = true, right = false, bottom = false)
        }
        type(navigationBars = bottom) {
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

fun BottomNavigationView.showIfNot() {
    if (!this.isShown) {
        this.show()
    }
}

fun BottomNavigationView.hideIfNot() {
    if (this.isShown) {
        this.hide()
    }
}

fun TextView.setHtml(
    @StringRes resId: Int,
    vararg args: Any = emptyArray(),
    flags: Int = HtmlCompat.FROM_HTML_MODE_LEGACY
) {
    val html = resources.getText(resId).toString().format(*args)
    setText(HtmlCompat.fromHtml(html, flags), TextView.BufferType.SPANNABLE)
}

fun TextView.setHtml(
    message: String,
    vararg args: Any = emptyArray(),
    flags: Int = HtmlCompat.FROM_HTML_MODE_LEGACY
) {
    val html = message.format(*args)
    setText(HtmlCompat.fromHtml(html, flags), TextView.BufferType.SPANNABLE)
}

fun View.applyOnGlobalLayout(listener: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            // Remove listener so that it doesn't run again
            viewTreeObserver.removeOnGlobalLayoutListener(this)
            // Invoke listener
            listener()
        }
    })
}

fun Fragment.hideKeyboard() {
    view?.let { this.activity?.hideKeyboard(it) }
}

fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus ?: View(this))
}

fun ChipGroup.setChildrenEnabled(enable: Boolean) {
    children.forEach { it.isEnabled = enable }
}

fun MaterialCardView.setStroke(width: Int, @ColorRes color: Int) {
    strokeWidth = width
    strokeColor = context.getColor(color)
}

@Suppress("MagicNumber")
fun MarkerView.customDraw(canvas: Canvas, posx: Float, posy: Float) {
    // translate to the correct position and draw
    var newPosX = posx
    var newPosY = posy
    // Prevent overflow to the right
    if (posx > canvas.width / 2) {
        newPosX = ((canvas.width / 3).toFloat())
    }

    // We do this as for continuous 0 values on the y Axis the marker view hides those values
    if (posy > canvas.height / 2) {
        newPosY = 0F
    }

    // Add 10 to posy so that the marker view isn't over the point selected but a bit lower
    canvas.translate(newPosX, newPosY)
    this.draw(canvas)
}

private fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}
