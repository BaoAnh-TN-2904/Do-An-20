package com.example.doan20.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.doan20.data.model.ItemStatus
import com.example.doan20.databinding.FragmentStatisticsBinding
import com.example.doan20.ui.viewmodel.ItemViewModel
import kotlinx.coroutines.launch

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeData()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allItems.collect { items ->
                    val total = items.size
                    val active = items.count { it.status == ItemStatus.ACTIVE }
                    val broken = items.count { it.status == ItemStatus.BROKEN }

                    val activeRate = if (total > 0) (active * 100) / total else 100
                    val brokenRate = if (total > 0) (broken * 100) / total else 0

                    // 1. Total Banner
                    binding.tvTotalCountLarge.text = total.toString()

                    // 2. Donut Chart
                    binding.donutProgress.setProgressCompat(activeRate, true)
                    binding.tvDonutCenterCount.text = total.toString()

                    binding.tvActiveCountStat.text = active.toString()
                    binding.tvActivePercentStat.text = "$activeRate%"

                    binding.tvBrokenCountStat.text = broken.toString()
                    binding.tvBrokenPercentStat.text = "$brokenRate%"

                    // 3. Category Breakdown matching Figma
                    val furnitureCount = items.count { it.category.equals("Nội thất", ignoreCase = true) }
                    val electronicsCount = items.count { it.category.equals("Điện tử", ignoreCase = true) }
                    val studyCount = items.count { it.category.contains("học tập", ignoreCase = true) || it.category.contains("cá nhân", ignoreCase = true) }
                    val otherCount = (total - furnitureCount - electronicsCount - studyCount).coerceAtLeast(0)

                    val furniturePercent = if (total > 0) (furnitureCount * 100) / total else 0
                    val electronicsPercent = if (total > 0) (electronicsCount * 100) / total else 0
                    val studyPercent = if (total > 0) (studyCount * 100) / total else 0
                    val otherPercent = if (total > 0) (otherCount * 100) / total else 0

                    binding.tvFurnitureLabel.text = "$furnitureCount · $furniturePercent%"
                    binding.progressFurniture.setProgressCompat(furniturePercent, true)

                    binding.tvElectronicsLabel.text = "$electronicsCount · $electronicsPercent%"
                    binding.progressElectronics.setProgressCompat(electronicsPercent, true)

                    binding.tvStudyLabel.text = "$studyCount · $studyPercent%"
                    binding.progressStudy.setProgressCompat(studyPercent, true)

                    binding.tvOtherLabel.text = "$otherCount · $otherPercent%"
                    binding.progressOther.setProgressCompat(otherPercent, true)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
