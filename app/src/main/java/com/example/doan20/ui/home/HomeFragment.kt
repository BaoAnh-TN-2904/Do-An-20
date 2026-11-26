package com.example.doan20.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan20.R
import com.example.doan20.data.model.ItemStatus
import com.example.doan20.databinding.FragmentHomeBinding
import com.example.doan20.ui.items.ItemAdapter
import com.example.doan20.ui.notifications.NotificationsBottomSheet
import com.example.doan20.ui.viewmodel.ItemViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by activityViewModels()
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
        observeData()
    }

    private fun setupRecyclerView() {
        recentAdapter = ItemAdapter(
            onItemClick = { item ->
                findNavController().navigate(
                    R.id.nav_detail,
                    bundleOf("itemId" to item.id)
                )
            },
            onStatusClick = { item ->
                viewModel.toggleStatus(item.id)
            },
            onSwitchToggle = { item, isChecked ->
                viewModel.setSwitchState(item.id, isChecked)
            }
        )

        binding.rvRecentItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        // User avatar clicks to smoothly switch to profile tab
        binding.btnUserAvatar.setOnClickListener {
            viewModel.selectTab(3)
        }

        // Notification bell opens NotificationsBottomSheet
        binding.btnNotification.setOnClickListener {
            NotificationsBottomSheet.newInstance().show(
                childFragmentManager,
                NotificationsBottomSheet.TAG
            )
        }

        // Stats card clicks: Card total switches to Statistics tab
        binding.cardStatTotal.setOnClickListener {
            viewModel.selectTab(2)
        }

        binding.cardStatActive.setOnClickListener {
            viewModel.setStatusFilter(ItemStatus.ACTIVE)
            viewModel.selectTab(1)
        }

        binding.cardStatBroken.setOnClickListener {
            viewModel.setStatusFilter(ItemStatus.BROKEN)
            viewModel.selectTab(1)
        }

        // See all items switches to Items tab
        binding.tvSeeAll.setOnClickListener {
            viewModel.setStatusFilter(null)
            viewModel.selectTab(1)
        }

        // Add item button opens AddItemFragment
        binding.btnAddItemHome.setOnClickListener {
            findNavController().navigate(R.id.nav_add_item)
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentUser.collect { user ->
                        val name = user?.name.orEmpty()
                        binding.tvGreetingTitle.text = if (name.isNotBlank()) "Chào $name!" else "Xin chào!"
                        binding.tvHomeAvatarInitials.text = user?.initials ?: "ND"
                    }
                }
                launch {
                    viewModel.allItems.collect { items ->
                        // Update stats counters
                        val total = items.size
                        val active = items.count { it.status == ItemStatus.ACTIVE }
                        val broken = items.count { it.status == ItemStatus.BROKEN }

                        binding.tvStatTotalCount.text = total.toString()
                        binding.tvStatActiveCount.text = active.toString()
                        binding.tvStatBrokenCount.text = broken.toString()

                        // Update recent items preview (first 4)
                        recentAdapter.submitList(items.take(4))

                        // Notification alert badge: ONLY show when there are items AND there are broken devices
                        val hasAlerts = items.isNotEmpty() && broken > 0
                        binding.viewNotifBadge.visibility = if (hasAlerts) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
