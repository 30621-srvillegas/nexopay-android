package com.uc1.nexopay.ui.dashboard

import android.view.WindowManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.uc1.nexopay.R

/** Estilo común y formularios desplazables cuando aparece el teclado. */
abstract class WalletSheet : BottomSheetDialogFragment() {
    override fun getTheme(): Int = R.style.ThemeOverlay_NexoPay_BottomSheet

    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }
}
