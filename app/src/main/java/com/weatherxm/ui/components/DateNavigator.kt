package com.weatherxm.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.weatherxm.R
import com.weatherxm.databinding.ListItemDateBinding
import com.weatherxm.databinding.ViewDateNavigatorBinding
import com.weatherxm.util.DateTimeHelper.getFormattedRelativeDay
import com.weatherxm.util.LocalDateRange
import java.time.LocalDate

class DateNavigator : ConstraintLayout {

    companion object {
        private const val PAGER_OFFSCREEN_LIMIT = 4
    }

    fun interface OnDateSelectedListener {
        fun onDateSelected(date: LocalDate)
    }

    private lateinit var binding: ViewDateNavigatorBinding
    private lateinit var adapter: DatesViewPagerAdapter
    private lateinit var dateStart: LocalDate
    private lateinit var dateEnd: LocalDate
    private lateinit var onPageChanged: OnPageChangeCallback

    constructor(context: Context) : super(context) {
        onCreate(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        onCreate(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        onCreate(context)
    }

    private fun onCreate(context: Context) {
        binding = ViewDateNavigatorBinding.inflate(LayoutInflater.from(context), this)
    }

    private fun getCurrentDate(): LocalDate {
        return adapter.getItem(binding.pager.currentItem)
    }

    fun init(
        dateStart: LocalDate,
        dateEnd: LocalDate,
        selectedDate: LocalDate = LocalDate.now(),
        listener: OnDateSelectedListener
    ) {
        this.dateStart = dateStart
        this.dateEnd = dateEnd

        @SuppressLint("WrongConstant")
        binding.pager.offscreenPageLimit = PAGER_OFFSCREEN_LIMIT
        binding.pager.apply {
            val recyclerView = getChildAt(0) as RecyclerView
            recyclerView.apply {
                val padding = resources.getDimensionPixelOffset(R.dimen.date_navigator_page_margin)
                // TODO: expose in later versions not to rely on getChildAt(0) which might break
                setPadding(padding, 0, padding, 0)
                clipToPadding = false
            }
        }


        adapter = DatesViewPagerAdapter(
            context,
            LocalDateRange(dateStart, dateEnd)
        ) { position ->
            // If user clicked a different page than the current one
            if (position != binding.pager.currentItem) {
                binding.pager.setCurrentItem(position, true)
            }
        }
        binding.pager.adapter = adapter

        onPageChanged = object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                listener.onDateSelected(getCurrentDate())
            }
        }

        setCurrentDate(selectedDate, false)
    }

    fun setCurrentDate(date: LocalDate, scroll: Boolean = true) {
        adapter.getPosition(date)?.let { position ->
            binding.pager.unregisterOnPageChangeCallback(onPageChanged)
            binding.pager.setCurrentItem(position, scroll)
            binding.pager.registerOnPageChangeCallback(onPageChanged)
        }
    }

    class DatesViewPagerAdapter(
        val context: Context,
        private val dates: LocalDateRange,
        val listener: OnDateClickListener
    ) : RecyclerView.Adapter<DatesViewPagerAdapter.DateViewHolder>() {

        fun interface OnDateClickListener {
            fun onDateClick(position: Int)
        }

        inner class DateViewHolder(
            private val binding: ListItemDateBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(date: LocalDate) {
                binding.date.text =
                    date.getFormattedRelativeDay(context, useCustomFormatter = false)
                itemView.setOnClickListener {
                    listener.onDateClick(absoluteAdapterPosition)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
            val binding = ListItemDateBinding.inflate(LayoutInflater.from(context), parent, false)
            return DateViewHolder(binding)
        }

        override fun getItemCount(): Int {
            return dates.count()
        }

        override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        fun getItem(position: Int): LocalDate {
            return dates.elementAt(position)
        }

        fun getPosition(date: LocalDate): Int? {
            return with(dates.indexOf(date)) {
                if (this == -1) null else this
            }
        }
    }
}
