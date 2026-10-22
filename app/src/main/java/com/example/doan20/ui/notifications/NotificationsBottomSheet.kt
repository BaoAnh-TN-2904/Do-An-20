package com.example.doan20.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.doan20.databinding.BottomSheetNotificationsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NotificationsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetNotificationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnCloseNotifications.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "NotificationsBottomSheet"
        fun newInstance() = NotificationsBottomSheet()
    }
}
