package com.example.doan20.ui.items

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.example.doan20.databinding.FragmentItemsBinding
import com.example.doan20.ui.viewmodel.ItemViewModel
import kotlinx.coroutines.launch

class ItemsFragment : Fragment() {

    private var _binding: FragmentItemsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by activityViewModels()
    private lateinit var itemAdapter: ItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        setupListeners()
        observeData()
    }

    private fun setupRecyclerView() {
        itemAdapter = ItemAdapter(
            onItemClick = { item ->
                findNavController().navigate(
                    R.id.action_items_to_detail,
                    bundleOf("itemId" to item.id)
                )
            },
            onStatusClick = { item ->
                viewModel.toggleStatus(item.id)
            },
            onSwitchToggle = { item, _ ->
                viewModel.toggleSwitch(item.id)
            }
        )

        binding.rvItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = itemAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                viewModel.setSearchQuery(query)
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
        }
    }

    private fun setupListeners() {
        binding.btnAddItem.setOnClickListener {
            findNavController().navigate(R.id.action_items_to_add)
        }

        binding.btnFilterSliders.setOnClickListener {
            // Cycle through filters: All -> Active -> Broken -> All
            val currentFilter = viewModel.statusFilter.value
            val nextFilter = when (currentFilter) {
                null -> ItemStatus.ACTIVE
                ItemStatus.ACTIVE -> ItemStatus.BROKEN
                ItemStatus.BROKEN -> null
            }
            viewModel.setStatusFilter(nextFilter)
            val msg = when (nextFilter) {
                null -> "Đang lọc: Tất cả"
                ItemStatus.ACTIVE -> "Đang lọc: Còn dùng"
                ItemStatus.BROKEN -> "Đang lọc: Hỏng"
            }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        binding.btnResetFilter.setOnClickListener {
            binding.etSearch.text?.clear()
            viewModel.setStatusFilter(null)
        }
    }

    private fun observeData() {
        // Observe filtered list and total items
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allItems.collect { all ->
                        binding.tvTotalItemHeader.text = "${all.size} vật dụng"
                    }
                }
                launch {
                    viewModel.filteredItems.collect { items ->
                        itemAdapter.submitList(items)
                        val total = viewModel.allItems.value.size
                        binding.tvShowingCount.text = "Hiển thị ${items.size} / $total vật dụng"

                        if (items.isEmpty()) {
                            binding.rvItems.visibility = View.GONE
                            binding.layoutEmptyState.visibility = View.VISIBLE
                        } else {
                            binding.rvItems.visibility = View.VISIBLE
                            binding.layoutEmptyState.visibility = View.GONE
                        }
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
