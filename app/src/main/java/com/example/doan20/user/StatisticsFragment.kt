package com.example.doan20.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.doan20.R
import com.example.doan20.databinding.FragmentStatisticsBinding

class StatisticsFragment : Fragment(), MainActivity.DataRefreshable {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

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
        refreshUI()
    }

    override fun refreshUI() {
        if (_binding == null) return
        val ctx = context ?: return
        val items = (activity as? MainActivity)?.deviceList ?: emptyList()

        val total = items.size
        val broken = items.count { it.isBroken() }
        val active = total - broken

        val activeRate = if (total > 0) (active * 100) / total else 0
        val brokenRate = if (total > 0) (broken * 100) / total else 0

        binding.tvTotalCountLarge.text = total.toString()

        val trackColor = when {
            total == 0 -> ContextCompat.getColor(ctx, R.color.surface_variant)
            broken > 0 -> ContextCompat.getColor(ctx, R.color.status_broken)
            else -> ContextCompat.getColor(ctx, R.color.surface_variant)
        }
        binding.donutProgress.trackColor = trackColor
        binding.donutProgress.setProgressCompat(if (total > 0) activeRate else 0, true)
        binding.tvDonutCenterCount.text = total.toString()

        binding.tvActiveCountStat.text = active.toString()
        binding.tvActivePercentStat.text = "$activeRate%"

        binding.tvBrokenCountStat.text = broken.toString()
        binding.tvBrokenPercentStat.text = "$brokenRate%"

        val furnitureCount = items.count { it.getCategoryDisplayName().equals("Nội thất", ignoreCase = true) }
        val electronicsCount = items.count { it.isElectronic() }
        val personalCount = items.count {
            val cat = it.getCategoryDisplayName()
            cat.contains("cá nhân", ignoreCase = true) || cat.contains("học tập", ignoreCase = true)
        }
        val otherCount = (total - furnitureCount - electronicsCount - personalCount).coerceAtLeast(0)

        val furniturePercent = if (total > 0) (furnitureCount * 100) / total else 0
        val electronicsPercent = if (total > 0) (electronicsCount * 100) / total else 0
        val personalPercent = if (total > 0) (personalCount * 100) / total else 0
        val otherPercent = if (total > 0) (otherCount * 100) / total else 0

        binding.tvFurnitureLabel.text = "$furnitureCount · $furniturePercent%"
        binding.progressFurniture.setProgressCompat(furniturePercent, true)

        binding.tvElectronicsLabel.text = "$electronicsCount · $electronicsPercent%"
        binding.progressElectronics.setProgressCompat(electronicsPercent, true)

        binding.tvStudyLabel.text = "$personalCount · $personalPercent%"
        binding.progressStudy.setProgressCompat(personalPercent, true)

        binding.tvOtherLabel.text = "$otherCount · $otherPercent%"
        binding.progressOther.setProgressCompat(otherPercent, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
