@file:Suppress("TooManyFunctions")

package com.weatherxm.ui.common

import android.animation.Animator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.BundleCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.parseAsHtml
import androidx.core.text.toHtml
import androidx.core.text.toSpanned
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.request.ImageRequest
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.shape.CornerFamily
import com.google.android.material.tabs.TabLayout
import com.weatherxm.R
import com.weatherxm.util.AndroidBuildInfo
import com.weatherxm.util.DateTimeHelper.getRelativeFormattedTime
import com.weatherxm.util.Weather.getWeatherAnimation
import dev.chrisbanes.insetter.applyInsetter
import java.util.Locale
import kotlin.math.abs

@IntDef(value = [Toast.LENGTH_SHORT, Toast.LENGTH_LONG])
@Retention(AnnotationRetention.SOURCE)
private annotation class Duration

fun Context?.toast(
    @StringRes message: Int,
    vararg args: Any = emptyArray(),
    @Duration duration: Int = Toast.LENGTH_SHORT
) {
    this?.toast(this.getString(message, *args), duration)
}

fun Context?.toast(
    message: String,
    @Duration duration: Int = Toast.LENGTH_SHORT
) {
    this?.let {
        Toast.makeText(it, message, duration).show()
    }
}

fun Activity.getRichText(
    @StringRes resId: Int,
    vararg args: Any = emptyArray()
) = getText(resId).toSpanned().toHtml().format(*args).parseAsHtml().trim().toString()

fun Activity.classSimpleName(): String {
    return this::class.simpleName ?: String.empty()
}

fun Fragment.classSimpleName(): String {
    return this::class.simpleName ?: String.empty()
}

// https://stackoverflow.com/questions/76614322/boolean-java-lang-class-isinterface-on-a-null-object-reference
inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? {
    return this.extras?.let {
        if (AndroidBuildInfo.sdkInt >= TIRAMISU) {
            BundleCompat.getParcelable(it, key, T::class.java)
        } else {
            @Suppress("DEPRECATION") getParcelableExtra(key) as? T
        }
    }
}

// https://stackoverflow.com/questions/76614322/boolean-java-lang-class-isinterface-on-a-null-object-reference
inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? =
    if (AndroidBuildInfo.sdkInt >= TIRAMISU) {
        BundleCompat.getParcelable(this, key, T::class.java)
    } else {
        @Suppress("DEPRECATION") getParcelable(key) as? T
    }

/**
 * Remove colon ":" character from Serial Number text
 */
fun Editable?.unmask(): String = this.toString().unmask()

/**
 * Remove colon ":" character from Serial Number text
 */
fun String.unmask(): String = this.replace(":", String.empty())

/**
 * hello world ---> Hello World
 */
fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { it.capitalized() }
}

fun String.capitalized(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase()) {
            it.titlecase(Locale.getDefault())
        } else {
            it.toString()
        }
    }
}

fun String.boldText(boldText: String): SpannableStringBuilder {
    val formattedText = SpannableStringBuilder(this)
    val boldToStart = this.indexOf(boldText, ignoreCase = true)
    formattedText.setSpan(
        StyleSpan(Typeface.BOLD),
        boldToStart,
        boldToStart.plus(boldText.length),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    return formattedText
}

@Suppress("FunctionOnlyReturningConstant")
fun String.Companion.empty() = ""

fun View.show(
    animation: Animation.ShowAnimation? = Animation.ShowAnimation.FadeIn,
    listener: Animator.AnimatorListener? = null
) {
    if (animation == null) this.visible(true) else animate(this, animation, listener)
}

fun View.hide(
    animation: Animation.HideAnimation? = Animation.HideAnimation.FadeOut,
    listener: Animator.AnimatorListener? = null
) {
    if (animation == null) this.visible(false) else animate(this, animation, listener)
}

/**
 * Makes the view VISIBLE or GONE depending on the boolean value as described:
 * https://developer.android.com/reference/android/view/View#attr_android:visibility
 * VISIBLE: Visible on screen
 * GONE: Completely hidden, as if the view had not been added. No space taken for layout purposes.
 */
fun View.visible(visible: Boolean) {
    if (visible) this.visibility = View.VISIBLE else this.visibility = View.GONE
}

/**
 * Makes the view INVISIBLE.
 * https://developer.android.com/reference/android/view/View#INVISIBLE
 * INVISIBLE: Not displayed, but taken into account during layout (space is left for it).
 */
fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.isVisibleOnScreen(): Boolean {
    if (!isShown) {
        return false
    }
    val actualPosition = Rect()
    val isGlobalVisible = getGlobalVisibleRect(actualPosition)
    val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    val screenHeight = Resources.getSystem().displayMetrics.heightPixels
    val screen = Rect(0, 0, screenWidth, screenHeight)
    return isGlobalVisible && Rect.intersects(actualPosition, screen)
}

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

fun EditText.clear() = this.setText(String.empty())

fun ImageView.setWarningDrawable(context: Context) {
    val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_warn, context.theme)
    drawable?.setTint(ResourcesCompat.getColor(resources, R.color.warning, context.theme))
    this.setImageDrawable(drawable)
}

fun ImageView.setBluetoothDrawable(context: Context) {
    val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_bluetooth, context.theme)
    drawable?.setTint(ResourcesCompat.getColor(resources, R.color.midGrey, context.theme))
    this.setImageDrawable(drawable)
}

fun ImageView.setNoDevicesFoundDrawable(context: Context) {
    val drawable =
        ResourcesCompat.getDrawable(resources, R.drawable.ic_no_devices_found, context.theme)
    drawable?.setTint(ResourcesCompat.getColor(resources, R.color.midGrey, context.theme))
    this.setImageDrawable(drawable)
}

fun ImageView.loadImage(imageLoader: ImageLoader, data: Any?) {
    imageLoader.enqueue(
        ImageRequest.Builder(this.context)
            .data(data)
            .target(this)
            .build()
    )
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
    this.chipIcon = AppCompatResources.getDrawable(context, drawable)
}

fun Chip.setErrorChip() {
    setChipBackgroundColorResource(R.color.errorTint)
    setIcon(R.drawable.ic_error_hex_filled)
    setChipIconTintResource(R.color.error)
}

fun Chip.setBundleChip(device: UIDevice) {
    if (device.isHelium()) {
        setIcon(R.drawable.ic_helium)
    } else if (device.isWifi()) {
        setIcon(R.drawable.ic_wifi)
    } else if (device.isCellular()) {
        setIcon(R.drawable.ic_cellular)
    }
    text = device.bundleTitle
}

fun Chip.setStatusChip(device: UIDevice) {
    text = device.lastWeatherStationActivity?.getRelativeFormattedTime(
        fallbackIfTooSoon = context.getString(R.string.just_now)
    )
    when (device.isActive) {
        true -> {
            setChipBackgroundColorResource(R.color.successTint)
            setChipIconTintResource(R.color.success)
        }
        false -> {
            setChipBackgroundColorResource(R.color.errorTint)
            setChipIconTintResource(R.color.error)
        }
        else -> {
            setChipBackgroundColorResource(R.color.midGrey)
            setChipIconTintResource(R.color.darkGrey)
        }
    }
}

fun ImageView.setColor(@ColorRes color: Int) {
    this.setColorFilter(ResourcesCompat.getColor(resources, color, null))
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

    newText.setSpan(defaultColor, 0, originalText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    newText.setSpan(
        highlightColor,
        queryStartIndex,
        queryEndIndex,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    newText.setSpan(
        StyleSpan(Typeface.BOLD),
        queryStartIndex,
        queryEndIndex,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
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

fun TextView.setDisplayTimezone(timezone: String?) {
    timezone?.let {
        text = context.getString(R.string.displayed_times, it)
        visible(true)
    } ?: visible(false)
}

fun TextView.clearMargins() {
    val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
    params.setMargins(0, 0, 0, 0)
    setLayoutParams(params)
}

fun LottieAnimationView.setWeatherAnimation(animation: String?) {
    setAnimation(getWeatherAnimation(animation))
    playAnimation()
}

fun View.applyOnGlobalLayout(listener: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
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

fun View.screenLocation(): IntArray {
    val point = IntArray(2)
    getLocationOnScreen(point)
    return point
}

fun Fragment.hideKeyboard() {
    view?.let { this.activity?.hideKeyboard(it) }
}

fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus ?: View(this))
}

fun MaterialCardView.setCardRadius(
    topLeftCorner: Float,
    topRightCorner: Float,
    bottomLeftCorner: Float,
    bottomRightCorner: Float
) {
    setShapeAppearanceModel(
        shapeAppearanceModel
            .toBuilder()
            .setTopLeftCorner(CornerFamily.ROUNDED, topLeftCorner)
            .setTopRightCorner(CornerFamily.ROUNDED, topRightCorner)
            .setBottomRightCorner(CornerFamily.ROUNDED, bottomLeftCorner)
            .setBottomLeftCorner(CornerFamily.ROUNDED, bottomRightCorner)
            .build()
    )
}

fun MaterialCardView.setCardStroke(@ColorRes colorResId: Int, width: Int) {
    strokeColor = context.getColor(colorResId)
    strokeWidth = width
}

/**
 * Fallback in case img url or drawable is missing so the texts can be visible
 */
fun MaterialCardView.setBoostFallbackBackground() {
    setCardBackgroundColor(context.getColor(R.color.blue))
}

private fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun RecyclerView.moveItemToCenter(position: Int, parentWidth: Int, itemWidth: Int) {
    val centerOfScreen: Int = parentWidth / 2 - (itemWidth / 2)
    (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, centerOfScreen)
}

@Suppress("EmptyFunctionBlock")
fun RecyclerView.blockParentViewPagerOnScroll() {
    addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
        override fun onTouchEvent(view: RecyclerView, event: MotionEvent) {}

        override fun onInterceptTouchEvent(view: RecyclerView, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            return false
        }

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
    })
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
