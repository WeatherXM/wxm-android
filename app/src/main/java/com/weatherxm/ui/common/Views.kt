package com.weatherxm.ui.common

import android.app.Activity
import android.content.Context
import android.text.Html
import android.widget.Toast
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.parseAsHtml
import androidx.core.text.toHtml
import androidx.core.text.toSpanned
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.tabs.TabLayout

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
