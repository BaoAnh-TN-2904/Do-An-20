package com.example.doan20.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.doan20.databinding.ItemAdminUserBinding
import com.example.doan20.model.UserModel

class AdminUserAdapter(
    private var userList: List<UserModel>,
    private val onItemClick: ((UserModel) -> Unit)? = null
) : RecyclerView.Adapter<AdminUserAdapter.UserViewHolder>() {

    inner class UserViewHolder(val binding: ItemAdminUserBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding =
            ItemAdminUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        with(holder.binding) {
            tvUserInitials.text = user.getInitials()
            tvUserName.text = user.fullName.ifBlank { "Người dùng #${position + 1}" }
            tvUserEmail.text = user.email.ifBlank { "Chưa có email" }
            tvDeviceCount.text = user.deviceCount.toString()

            root.setOnClickListener {
                onItemClick?.invoke(user)
            }
        }
    }

    override fun getItemCount(): Int = userList.size

    fun updateData(newList: List<UserModel>) {
        userList = newList
        notifyDataSetChanged()
    }
}
