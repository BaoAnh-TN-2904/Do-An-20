package com.example.doan20.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.doan20.R
import com.example.doan20.databinding.FragmentMainBinding
import com.example.doan20.ui.home.HomeFragment
import com.example.doan20.ui.items.ItemsFragment
import com.example.doan20.ui.profile.ProfileFragment
import com.example.doan20.ui.statistics.StatisticsFragment
import com.example.doan20.ui.viewmodel.ItemViewModel
import kotlinx.coroutines.launch

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var isSelectingFromNav = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWindowInsets()
        setupViewPager()
        setupBottomNavigation()
        observeSelectedTab()
        setupBackHandler()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.viewPagerMain.setPadding(0, systemBars.top, 0, 0)
            binding.bottomNavigation.setPadding(
                0,
                0,
                0,
                systemBars.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun setupViewPager() {
        val adapter = MainPagerAdapter(this)
        binding.viewPagerMain.adapter = adapter
        // Keep all 4 tabs in memory so swiping is instantaneous and scroll positions are retained
        binding.viewPagerMain.offscreenPageLimit = 3

        binding.viewPagerMain.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (isSelectingFromNav) return
                val targetMenuId = when (position) {
                    0 -> R.id.nav_home
                    1 -> R.id.nav_items
                    2 -> R.id.nav_statistics
                    3 -> R.id.nav_profile
                    else -> R.id.nav_home
                }
                if (binding.bottomNavigation.selectedItemId != targetMenuId) {
                    binding.bottomNavigation.menu.findItem(targetMenuId)?.isChecked = true
                }
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val targetPosition = when (item.itemId) {
                R.id.nav_home -> 0
                R.id.nav_items -> 1
                R.id.nav_statistics -> 2
                R.id.nav_profile -> 3
                else -> 0
            }
            if (binding.viewPagerMain.currentItem != targetPosition) {
                isSelectingFromNav = true
                binding.viewPagerMain.setCurrentItem(targetPosition, false)
                isSelectingFromNav = false
            }
            true
        }

        binding.bottomNavigation.setOnItemReselectedListener {
            // No reload on reselecting the active tab
        }
    }

    private fun observeSelectedTab() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedTab.collect { tabIndex ->
                    if (tabIndex in 0..3 && binding.viewPagerMain.currentItem != tabIndex) {
                        isSelectingFromNav = true
                        binding.viewPagerMain.setCurrentItem(tabIndex, false)
                        isSelectingFromNav = false
                        val targetMenuId = when (tabIndex) {
                            0 -> R.id.nav_home
                            1 -> R.id.nav_items
                            2 -> R.id.nav_statistics
                            3 -> R.id.nav_profile
                            else -> R.id.nav_home
                        }
                        binding.bottomNavigation.menu.findItem(targetMenuId)?.isChecked = true
                    }
                }
            }
        }
    }

    private fun setupBackHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.viewPagerMain.currentItem != 0) {
                    binding.viewPagerMain.setCurrentItem(0, false)
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class MainPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragment()
                1 -> ItemsFragment()
                2 -> StatisticsFragment()
                3 -> ProfileFragment()
                else -> HomeFragment()
            }
        }
    }
}
