package com.example.doan20.ui.profile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.example.doan20.data.model.getInitials
import com.example.doan20.databinding.BottomSheetEditProfileBinding
import com.example.doan20.ui.viewmodel.ItemViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EditProfileBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = viewModel.currentUser.value
        binding.tvSheetAvatarInitials.text = user?.initials ?: "ND"
        binding.etEditUsername.setText(user?.username.orEmpty())
        binding.etEditName.setText(user?.name.orEmpty())
        binding.etEditRoom.setText(user?.roomName.orEmpty())

        // Live update avatar initials as user types their name
        binding.etEditName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val inputName = s?.toString().orEmpty()
                binding.tvSheetAvatarInitials.text = getInitials(inputName)
                if (inputName.isNotBlank()) {
                    binding.tilEditName.error = null
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnCloseEditProfile.setOnClickListener {
            dismiss()
        }

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etEditName.text?.toString().orEmpty().trim()
            val room = binding.etEditRoom.text?.toString().orEmpty().trim()

            if (name.isBlank()) {
                binding.tilEditName.error = "Vui lòng nhập họ và tên!"
                return@setOnClickListener
            } else {
                binding.tilEditName.error = null
            }

            binding.progressBarEditProfile.visibility = View.VISIBLE
            binding.btnSaveProfile.isEnabled = false

            viewModel.updateProfile(name, room) { result ->
                if (!isAdded) return@updateProfile
                binding.progressBarEditProfile.visibility = View.GONE
                binding.btnSaveProfile.isEnabled = true

                result.onSuccess {
                    Toast.makeText(requireContext(), "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show()
                    dismiss()
                }.onFailure { error ->
                    Toast.makeText(requireContext(), error.message ?: "Cập nhật thất bại!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "EditProfileBottomSheet"
        fun newInstance() = EditProfileBottomSheet()
    }
}
