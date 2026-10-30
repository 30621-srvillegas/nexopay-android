package com.uc1.nexopay.ui.common

import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.uc1.nexopay.R

/** Mensajes temporales que permiten seguir usando la pantalla sin cerrar un diálogo. */
object UiNotice {
    fun show(view: View, message: String, success: Boolean = false) {
        val background = if (success) R.color.nexo_success_container else R.color.nexo_danger_container
        val foreground = if (success) R.color.nexo_on_success_container else R.color.nexo_on_danger_container
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(view.context, background))
            .setTextColor(ContextCompat.getColor(view.context, foreground))
            .setTextMaxLines(5)
            .show()
    }
}
