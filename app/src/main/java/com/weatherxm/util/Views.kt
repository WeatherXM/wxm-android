@file:Suppress("TooManyFunctions")

package com.weatherxm.util

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import android.text.Editable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.TextPaint
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.view.GestureDetector
import android.view.MotionEvent
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
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.MarkerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.weatherxm.R
import dev.chrisbanes.insetter.applyInsetter
import kotlin.math.abs

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
    val drawable = getDrawable(resources, R.drawable.ic_warn, context.theme)
    drawable?.setTint(getColor(resources, R.color.warning, context.theme))
    this.setImageDrawable(drawable)
}

fun ImageView.setBluetoothDrawable(context: Context) {
    val drawable = getDrawable(resources, R.drawable.ic_bluetooth, context.theme)
    drawable?.setTint(getColor(resources, R.color.midGrey, context.theme))
    this.setImageDrawable(drawable)
}

fun ImageView.setNoDevicesFoundDrawable(context: Context) {
    val drawable = getDrawable(resources, R.drawable.ic_no_devices_found, context.theme)
    drawable?.setTint(getColor(resources, R.color.darkGrey, context.theme))
    this.setImageDrawable(drawable)
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
    this.setChipBackgroundColorResource(R.color.successTint)
    this.chipIcon = getDrawable(resources, R.drawable.ic_checkmark, context.theme)
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

fun TextView.highlightText(
    originalText: String,
    queryStartIndex: Int,
    queryEndIndex: Int,
    @ColorRes defaultColorResId: Int,
    @ColorRes highlightColorResId: Int
) {
    val newText = SpannableStringBuilder(originalText)

    val defaultColor = ForegroundColorSpan(context.getColor(defaultColorResId))
    val highlightColor = ForegroundColorSpan(context.getColor(highlightColorResId))

    newText.setSpan(defaultColor, 0, originalText.length, SPAN_EXCLUSIVE_EXCLUSIVE)
    newText.setSpan(
        highlightColor,
        queryStartIndex,
        queryEndIndex,
        SPAN_EXCLUSIVE_EXCLUSIVE
    )
    newText.setSpan(
        StyleSpan(Typeface.BOLD),
        queryStartIndex,
        queryEndIndex,
        SPAN_EXCLUSIVE_EXCLUSIVE
    )
    text = newText
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

fun TextView.removeLinksUnderline() {
    val spannable = SpannableString(text)
    spannable.getSpans(0, spannable.length, URLSpan::class.java).forEach {
        spannable.setSpan(object : URLSpan(it.url) {
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }, spannable.getSpanStart(it), spannable.getSpanEnd(it), 0)
    }
    text = spannable
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

fun View?.hideKeyboard() {
    this?.let { this.context?.hideKeyboard(it) }
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

// https://stackoverflow.com/a/46165723/5403137
class HorizontalScrollGestureListener(
    private val recyclerView: RecyclerView
) : GestureDetector.SimpleOnGestureListener() {
    companion object {
        const val SCROLL_Y = 10
    }

    override fun onDown(e: MotionEvent): Boolean {
        // Prevent ViewPager from intercepting touch events as soon as a DOWN is detected.
        // If we don't do this the next MOVE event may trigger the ViewPager to switch
        // tabs before this view can intercept the event.
        recyclerView.requestDisallowInterceptTouchEvent(true)
        return super.onDown(e)
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (abs(distanceX) > abs(distanceY)) {
            // Detected a horizontal scroll, prevent the viewpager from switching tabs
            recyclerView.requestDisallowInterceptTouchEvent(true)
        } else if (abs(distanceY) > SCROLL_Y) {
            // Detected a vertical scroll of large enough magnitude so allow the the event
            // to propagate to ancestor views to allow vertical scrolling.  Without the buffer
            // a tab swipe would be triggered while holding finger still while glow effect was
            // visible.
            recyclerView.requestDisallowInterceptTouchEvent(false)
        }
        return super.onScroll(e1, e2, distanceX, distanceY)
    }
}
