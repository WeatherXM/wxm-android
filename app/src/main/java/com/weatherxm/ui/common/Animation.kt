package com.weatherxm.ui.common

import android.animation.Animator.AnimatorListener
import android.view.View
import androidx.annotation.Keep

@Keep
sealed class Animation {
    @Keep
    sealed class ShowAnimation : Animation() {
        object FadeIn : ShowAnimation()
        object SlideInFromTop : ShowAnimation()
        object SlideInFromBottom : ShowAnimation()
    }

    @Keep
    sealed class HideAnimation : Animation() {
        object FadeOut : HideAnimation()
        object SlideOutToTop : HideAnimation()
        object SlideOutToBottom : HideAnimation()
    }
}

fun animate(view: View, animation: Animation, listener: AnimatorListener? = null) {
    when (animation) {
        is Animation.ShowAnimation -> {
            val animator = view.animate()
                // For the Show animation to run properly, the view visibility must be changed first
                .withStartAction { view.visible(true) }
                .alpha(1F)
            when (animation) {
                Animation.ShowAnimation.SlideInFromTop -> {
                    // This doesn't literally translate from -100% to 0%,
                    // but assumes the original translationY was -100% and sets the target to 0%
                    animator.translationY(0F)
                }
                Animation.ShowAnimation.SlideInFromBottom -> {
                    // This doesn't literally translate from 100% to 0%,
                    // but assumes the original translationY was 100% and sets the target to 0%
                    animator.translationY(0F)
                }
                else -> Unit
            }
            listener?.let {
                animator.setListener(it)
            }
            animator.start()
        }
        is Animation.HideAnimation -> {
            val animator = view.animate()
                // For the Hide animation to run properly, the view visibility must be changed last
                .withEndAction { view.visible(false) }
                .alpha(0F)
            when (animation) {
                Animation.HideAnimation.SlideOutToBottom -> {
                    // This doesn't literally translate from 0% to 100%,
                    // but assumes the original translationY was 0% and sets the target to 100%
                    animator.translationY(view.height.toFloat())
                }
                Animation.HideAnimation.SlideOutToTop -> {
                    // This doesn't literally translate from 0% to -100%,
                    // but assumes the original translationY was 0% and sets the target to -100%
                    animator.translationY(-view.height.toFloat())
                }
                else -> Unit
            }
            listener?.let {
                animator.setListener(it)
            }
            animator.start()
        }
    }
}
