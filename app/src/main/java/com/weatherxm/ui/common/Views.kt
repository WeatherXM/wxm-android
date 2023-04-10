package com.weatherxm.ui.common

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.text.Editable
import android.view.View
import android.widget.Toast
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.parseAsHtml
import androidx.core.text.toHtml
import androidx.core.text.toSpanned
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.tabs.TabLayout
import java.util.Locale

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

fun AppCompatActivity.addFragment(
    fragment: Fragment,
    tag: String,
    frameId: Int,
    addToBackStack: Boolean = false
) {
    val fragmentExists = supportFragmentManager.findFragmentByTag(tag)
    if (fragmentExists != null) {
        replaceFragment(fragmentExists, tag, frameId, addToBackStack)
    } else {
        if (addToBackStack) {
            supportFragmentManager.inTransaction { add(frameId, fragment, tag).addToBackStack(tag) }
        } else {
            supportFragmentManager.inTransaction { add(frameId, fragment, tag) }
        }
    }
}

fun AppCompatActivity.replaceFragment(
    fragment: Fragment,
    tag: String,
    frameId: Int,
    addToBackStack: Boolean = true
) {
    val fragmentExists = supportFragmentManager.findFragmentByTag(tag)
    if (fragmentExists != null) {
        supportFragmentManager.inTransaction {
            replace(frameId, fragmentExists, tag)
                .also { if (addToBackStack) addToBackStack(tag) }
        }
    } else {
        supportFragmentManager.inTransaction {
            replace(frameId, fragment, tag)
                .also { if (addToBackStack) addToBackStack(tag) }
        }
    }
}

inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) =
    beginTransaction().func().commit()

fun TabLayout.getLastTab(): TabLayout.Tab? {
    return getTabAt(tabCount - 1)
}

fun TabLayout.getSelectedTab(): TabLayout.Tab? {
    return getTabAt(selectedTabPosition)
}

fun Activity.getRichText(
    @StringRes resId: Int,
    vararg args: Any = emptyArray()
) = getText(resId).toSpanned().toHtml().format(*args).parseAsHtml().trim()

/**
 * Remove colon ":" character from Serial Number text
 */
fun Editable?.unmask(): String = this.toString().unmask()

/**
 * Remove colon ":" character from Serial Number text
 */
fun String.unmask(): String = this.toString().replace(":", "")

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

fun View.toggleVisibility() {
    if (this.isVisible) {
        hide()
    } else {
        show()
    }
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
