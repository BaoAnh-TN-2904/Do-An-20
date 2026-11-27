package com.example.doan20.user

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.doan20.databinding.ActivityFilterBinding
import com.example.doan20.utils.UiHelper

class FilterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFilterBinding
    private var tempCategory: String? = null
    private var tempStatus: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UiHelper.applySystemBarPadding(binding.root)

        tempCategory = intent.getStringExtra("FILTER_CATEGORY")
        tempStatus = intent.getStringExtra("FILTER_STATUS")

        updateCategoryUI()
        updateStatusUI()
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnFilterBack.setOnClickListener { finish() }

        binding.rowCategoryAll.setOnClickListener { tempCategory = null; updateCategoryUI() }
        binding.rowCategoryFurniture.setOnClickListener { tempCategory = "Nội thất"; updateCategoryUI() }
        binding.rowCategoryElectronics.setOnClickListener { tempCategory = "Điện tử"; updateCategoryUI() }
        binding.rowCategoryPersonal.setOnClickListener { tempCategory = "Đồ dùng cá nhân"; updateCategoryUI() }
        binding.rowCategoryOther.setOnClickListener { tempCategory = "Khác"; updateCategoryUI() }

        binding.btnStatusAll.setOnClickListener { tempStatus = null; updateStatusUI() }
        binding.btnStatusActive.setOnClickListener { tempStatus = "active"; updateStatusUI() }
        binding.btnStatusBroken.setOnClickListener { tempStatus = "broken"; updateStatusUI() }

        binding.btnApplyFilter.setOnClickListener {
            val data = Intent().apply {
                putExtra("FILTER_CATEGORY", tempCategory)
                putExtra("FILTER_STATUS", tempStatus)
            }
            setResult(RESULT_OK, data)
            finish()
        }
    }

    private fun updateCategoryUI() {
        binding.rbCatAll.isChecked = tempCategory == null
        binding.rbCatFurniture.isChecked = tempCategory.equals("Nội thất", ignoreCase = true)
        binding.rbCatElectronics.isChecked = tempCategory.equals("Điện tử", ignoreCase = true)
        binding.rbCatPersonal.isChecked = tempCategory.equals("Đồ dùng cá nhân", ignoreCase = true)
        binding.rbCatOther.isChecked = tempCategory.equals("Khác", ignoreCase = true)
    }

    private fun updateStatusUI() {
        binding.btnStatusAll.isSelected = tempStatus == null
        binding.btnStatusActive.isSelected = tempStatus == "active"
        binding.btnStatusBroken.isSelected = tempStatus == "broken"
    }
}
