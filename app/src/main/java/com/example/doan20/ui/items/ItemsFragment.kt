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

                // Toggle between default title bar and active search header
                if (query.isNotBlank()) {
                    binding.tvItemsTitle.visibility = View.GONE
                    binding.btnAddItem.visibility = View.GONE
                    binding.layoutSearchTitle.visibility = View.VISIBLE
                } else {
                    binding.tvItemsTitle.visibility = View.VISIBLE
                    binding.btnAddItem.visibility = View.VISIBLE
                    binding.layoutSearchTitle.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
        }

        binding.btnSearchBack.setOnClickListener {
            binding.etSearch.text?.clear()
        }
    }

    private fun setupListeners() {
        binding.btnAddItem.setOnClickListener {
            findNavController().navigate(R.id.nav_add_item)
        }

        // Open item filter screen
        binding.btnFilterSliders.setOnClickListener {
            findNavController().navigate(R.id.nav_filter)
        }

        binding.btnResetFilter.setOnClickListener {
            binding.etSearch.text?.clear()
            viewModel.resetFilters()
        }
    }

    private fun observeData() {
        // Observe filtered list and total items
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allItems.collect { all ->
                        if (viewModel.searchQuery.value.isBlank()) {
                            binding.tvTotalItemHeader.text = "${all.size} vật dụng"
                        }
                    }
                }
                launch {
                    viewModel.filteredItems.collect { items ->
                        itemAdapter.submitList(items)
                        val total = viewModel.allItems.value.size
                        val query = viewModel.searchQuery.value

                        if (query.isNotBlank()) {
                            // Search results header
                            binding.tvTotalItemHeader.text = "Kết quả tìm kiếm"
                            binding.tvShowingCount.text = "${items.size} kết quả phù hợp cho \"$query\""
                        } else {
                            // Default item list header
                            binding.tvTotalItemHeader.text = "$total vật dụng"
                            binding.tvShowingCount.text = "Hiển thị ${items.size} / $total vật dụng"
                        }

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
