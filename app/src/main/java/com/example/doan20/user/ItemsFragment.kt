package com.example.doan20.user

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan20.databinding.FragmentItemsBinding
import com.example.doan20.model.DeviceModel

class ItemsFragment : Fragment(), MainActivity.DataRefreshable {

    private var _binding: FragmentItemsBinding? = null
    private val binding get() = _binding!!
    private lateinit var itemAdapter: ItemAdapter

    private val filterLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val main = activity as? MainActivity ?: return@registerForActivityResult
            main.filterCategory = result.data?.getStringExtra("FILTER_CATEGORY")
            main.filterStatus = result.data?.getStringExtra("FILTER_STATUS")
            refreshUI()
        }
    }

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
        refreshUI()
    }

    private fun setupRecyclerView() {
        itemAdapter = ItemAdapter(
            deviceList = emptyList(),
            onItemClick = { device ->
                val main = activity as? MainActivity ?: return@ItemAdapter
                val intent = Intent(requireContext(), ItemDetailActivity::class.java).apply {
                    putExtra("DEVICE_ID", device.id)
                    putExtra("USER_ID", main.currentUser?.uid.orEmpty())
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
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                if (query.isNotBlank()) {
                    binding.tvItemsTitle.visibility = View.GONE
                    binding.btnAddItem.visibility = View.GONE
                    binding.layoutSearchTitle.visibility = View.VISIBLE
                } else {
                    binding.tvItemsTitle.visibility = View.VISIBLE
                    binding.btnAddItem.visibility = View.VISIBLE
                    binding.layoutSearchTitle.visibility = View.GONE
                }
                refreshUI()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener { binding.etSearch.text?.clear() }
        binding.btnSearchBack.setOnClickListener { binding.etSearch.text?.clear() }
    }

    private fun setupListeners() {
        binding.btnAddItem.setOnClickListener {
            val main = activity as? MainActivity ?: return@setOnClickListener
            val intent = Intent(requireContext(), AddItemActivity::class.java).apply {
                putExtra("USER_ID", main.currentUser?.uid.orEmpty())
            }
            startActivity(intent)
        }

        binding.btnFilterSliders.setOnClickListener {
            val main = activity as? MainActivity ?: return@setOnClickListener
            val intent = Intent(requireContext(), FilterActivity::class.java).apply {
                putExtra("FILTER_CATEGORY", main.filterCategory)
                putExtra("FILTER_STATUS", main.filterStatus)
            }
            filterLauncher.launch(intent)
        }

        binding.btnResetFilter.setOnClickListener {
            val main = activity as? MainActivity ?: return@setOnClickListener
            main.filterCategory = null
            main.filterStatus = null
            binding.etSearch.text?.clear()
            refreshUI()
        }
    }

    override fun refreshUI() {
        if (_binding == null) return
        val main = activity as? MainActivity ?: return
        val allItems = main.deviceList
        val query = binding.etSearch.text?.toString()?.trim().orEmpty()
        val statusFilter = main.filterStatus
        val categoryFilter = main.filterCategory

        val filtered: List<DeviceModel> = allItems.filter { device ->
            val matchesQuery = query.isBlank() ||
                device.name.contains(query, ignoreCase = true) ||
                device.getCategoryDisplayName().contains(query, ignoreCase = true) ||
                device.note.contains(query, ignoreCase = true)

            val matchesStatus = when (statusFilter) {
                "active" -> !device.isBroken()
                "broken" -> device.isBroken()
                else -> true
            }

            val matchesCategory = categoryFilter.isNullOrBlank() ||
                device.getCategoryDisplayName().equals(categoryFilter, ignoreCase = true)

            matchesQuery && matchesStatus && matchesCategory
        }

        itemAdapter.updateData(filtered)

        val total = allItems.size
        if (query.isNotBlank()) {
            binding.tvTotalItemHeader.text = "Kết quả tìm kiếm"
            binding.tvShowingCount.text = "${filtered.size} kết quả phù hợp cho \"$query\""
        } else {
            binding.tvTotalItemHeader.text = "$total vật dụng"
            binding.tvShowingCount.text = "Hiển thị ${filtered.size} / $total vật dụng"
        }

        if (filtered.isEmpty()) {
            binding.rvItems.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvItems.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
