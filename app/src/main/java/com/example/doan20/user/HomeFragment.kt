package com.example.doan20.user

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan20.databinding.FragmentHomeBinding

class HomeFragment : Fragment(), MainActivity.DataRefreshable {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var recentAdapter: ItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        refreshUI()
    }

    private fun setupRecyclerView() {
        recentAdapter = ItemAdapter(
            deviceList = emptyList(),
            onItemClick = { device ->
                val main = activity as? MainActivity ?: return@ItemAdapter
                val intent = Intent(requireContext(), ItemDetailActivity::class.java).apply {
                    putExtra("DEVICE_ID", device.id)
                    putExtra("USER_ID", main.currentUser?.uid.orEmpty())
                    putExtra("ESP32_ID", main.currentUser?.esp32Id.orEmpty())
                    putExtra("IS_ADMIN", false)
                }
                startActivity(intent)
            },
            onStatusClick = { device ->
                (activity as? MainActivity)?.toggleDeviceStatus(device)
            },
            onSwitchToggle = { device, isChecked ->
                (activity as? MainActivity)?.toggleDeviceSwitch(device, isChecked)
            }
        )

        binding.rvRecentItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        binding.btnUserAvatar.setOnClickListener {
            (activity as? MainActivity)?.switchTab(3)
        }

        binding.btnNotification.setOnClickListener {
            NotificationsBottomSheet().show(childFragmentManager, NotificationsBottomSheet.TAG)
        }

        binding.cardStatTotal.setOnClickListener {
            (activity as? MainActivity)?.switchTab(2)
        }

        binding.cardStatActive.setOnClickListener {
            val main = activity as? MainActivity ?: return@setOnClickListener
            main.filterStatus = "active"
            main.switchTab(1)
        }

        binding.cardStatBroken.setOnClickListener {
            val main = activity as? MainActivity ?: return@setOnClickListener
            main.filterStatus = "broken"
            main.switchTab(1)
        }

        binding.tvSeeAll.setOnClickListener {
            val main = activity as? MainActivity ?: return@setOnClickListener
            main.filterStatus = null
            main.switchTab(1)
        }

        binding.btnAddItemHome.setOnClickListener {
            val main = activity as? MainActivity ?: return@setOnClickListener
            val intent = Intent(requireContext(), AddItemActivity::class.java).apply {
                putExtra("USER_ID", main.currentUser?.uid.orEmpty())
                putExtra("ESP32_ID", main.currentUser?.esp32Id.orEmpty())
            }
            startActivity(intent)
        }
    }

    override fun refreshUI() {
        if (_binding == null) return
        val main = activity as? MainActivity ?: return
        val user = main.currentUser
        val items = main.deviceList

        val name = user?.fullName.orEmpty()
        binding.tvGreetingTitle.text = if (name.isNotBlank()) "Chào $name!" else "Xin chào!"
        binding.tvHomeAvatarInitials.text = user?.getInitials() ?: "ND"

        val total = items.size
        val active = items.count { !it.isBroken() }
        val broken = items.count { it.isBroken() }
        val provisionFailed = items.count { it.isElectronic() && it.isProvisionFailed() }

        binding.tvStatTotalCount.text = total.toString()
        binding.tvStatActiveCount.text = active.toString()
        binding.tvStatBrokenCount.text = broken.toString()

        recentAdapter.updateData(items.take(4))
        binding.viewNotifBadge.visibility =
            if (items.isNotEmpty() && (broken > 0 || provisionFailed > 0)) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
