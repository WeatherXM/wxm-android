package com.weatherxm.ui.widget

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.weatherxm.ui.common.Animation
import com.weatherxm.ui.common.hide
import com.weatherxm.ui.common.show

class HidingBottomNavigationView : BottomNavigationView {

    constructor(context: Context) : super(context)

    constructor(
        context: Context,
        attrs: AttributeSet
    ) : super(context, attrs)

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    @Suppress("unused")
    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    companion object {
        const val ANIM_STATE_NONE = 0
        const val ANIM_STATE_HIDING = 1
        const val ANIM_STATE_SHOWING = 2
    }

    private var animState = ANIM_STATE_NONE

    private fun isOrWillBeShown(): Boolean {
        return if (visibility != VISIBLE) {
            // If we not currently visible, return true if we're animating to be shown
            animState == ANIM_STATE_SHOWING
        } else {
            // Otherwise if we're visible, return true if we're not animating to be hidden
            animState != ANIM_STATE_HIDING
        }
    }

    private fun isOrWillBeHidden(): Boolean {
        return if (visibility == VISIBLE) {
            // If we currently visible, return true if we're animating to be hidden
            animState == ANIM_STATE_HIDING
        } else {
            // Otherwise if we're not visible, return true if we're not animating to be shown
            animState != ANIM_STATE_SHOWING
        }
    }

    fun show() {
        if (!isOrWillBeShown()) {
            val listener: AnimatorListener = object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    animState = ANIM_STATE_SHOWING
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    animState = ANIM_STATE_NONE
                }
            }
            this.show(Animation.ShowAnimation.SlideInFromBottom, listener)
        }
    }

    fun hide() {
        if (!isOrWillBeHidden()) {
            val listener: AnimatorListener = object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    animState = ANIM_STATE_HIDING
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    animState = ANIM_STATE_NONE
                }
            }
            this.hide(Animation.HideAnimation.SlideOutToBottom, listener)
        }
    }
}
