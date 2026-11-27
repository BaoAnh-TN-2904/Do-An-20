package com.example.doan20.user

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.doan20.databinding.BottomSheetEditProfileBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class EditProfileBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEditProfileBinding? = null
    private val binding get() = _binding!!

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

        val user = (activity as? MainActivity)?.currentUser
        binding.tvSheetAvatarInitials.text = user?.getInitials() ?: "ND"
        binding.etEditUsername.setText(user?.email.orEmpty())
        binding.etEditName.setText(user?.fullName.orEmpty())
        binding.etEditRoom.setText(user?.roomName.orEmpty())

        binding.etEditName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val inputName = s?.toString().orEmpty()
                binding.tvSheetAvatarInitials.text = getInitials(inputName)
                if (inputName.isNotBlank()) binding.tilEditName.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnCloseEditProfile.setOnClickListener { dismiss() }

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etEditName.text?.toString().orEmpty().trim()
            val room = binding.etEditRoom.text?.toString().orEmpty().trim().ifBlank { "Phòng cá nhân" }

            if (name.isBlank()) {
                binding.tilEditName.error = "Vui lòng nhập họ và tên!"
                return@setOnClickListener
            }

            val uid = user?.uid?.ifBlank { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
                ?: FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            if (uid.isBlank()) {
                Toast.makeText(requireContext(), "Chưa đăng nhập!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBarEditProfile.visibility = View.VISIBLE
            binding.btnSaveProfile.isEnabled = false

            FirebaseAuth.getInstance().currentUser?.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(name).build()
            )

            val updateMap = hashMapOf<String, Any>(
                "uid" to uid,
                "fullName" to name,
                "name" to name,
                "roomName" to room,
                "email" to (user?.email ?: FirebaseAuth.getInstance().currentUser?.email.orEmpty())
            )

            FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(updateMap, SetOptions.merge())
                .addOnSuccessListener {
                    if (!isAdded) return@addOnSuccessListener
                    binding.progressBarEditProfile.visibility = View.GONE
                    binding.btnSaveProfile.isEnabled = true
                    Toast.makeText(requireContext(), "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                .addOnFailureListener { e ->
                    if (!isAdded) return@addOnFailureListener
                    binding.progressBarEditProfile.visibility = View.GONE
                    binding.btnSaveProfile.isEnabled = true
                    Toast.makeText(requireContext(), e.message ?: "Cập nhật thất bại!", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun getInitials(name: String): String {
        val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "ND"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts.first().first().uppercase()}${parts.last().first().uppercase()}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "EditProfileBottomSheet"
    }
}
