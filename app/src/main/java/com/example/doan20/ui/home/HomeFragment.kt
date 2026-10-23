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
                    R.id.action_home_to_detail,
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
        // Notification sheet
        binding.btnNotification.setOnClickListener {
            NotificationsBottomSheet.newInstance().show(
                childFragmentManager,
                NotificationsBottomSheet.TAG
            )
        }

        // Stats card clicks - navigate to Items tab with active filter
        binding.cardStatTotal.setOnClickListener {
            viewModel.setStatusFilter(null)
            findNavController().navigate(R.id.action_home_to_items)
        }

        binding.cardStatActive.setOnClickListener {
            viewModel.setStatusFilter(ItemStatus.ACTIVE)
            findNavController().navigate(R.id.action_home_to_items)
        }

        binding.cardStatBroken.setOnClickListener {
            viewModel.setStatusFilter(ItemStatus.BROKEN)
            findNavController().navigate(R.id.action_home_to_items)
        }

        // See all items
        binding.tvSeeAll.setOnClickListener {
            viewModel.setStatusFilter(null)
            findNavController().navigate(R.id.action_home_to_items)
        }

        // Add item button
        binding.btnAddItemHome.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_add)
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentUser.collect { user ->
                        val name = user?.name ?: "Minh Anh"
                        binding.tvGreetingTitle.text = "Chào $name!"
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
