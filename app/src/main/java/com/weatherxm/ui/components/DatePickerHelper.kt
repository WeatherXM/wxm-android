package com.weatherxm.ui.components

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds
import com.weatherxm.R
import com.weatherxm.util.DateTimeHelper.timestampToLocalDate
import com.weatherxm.util.DateTimeHelper.toUTCEpochMillis
import java.time.LocalDate

object DatePickerHelper {

    fun interface OnDateSelectedListener {
        fun onDateSelected(date: LocalDate)
    }

    private const val TAG = "DatePicker"

    fun show(
        context: Context,
        selectedDate: LocalDate? = null,
        dateStart: LocalDate? = null,
        dateEnd: LocalDate? = null,
        listener: OnDateSelectedListener
    ): MaterialDatePicker<Long> {
        val constraints = CalendarConstraints.Builder()
            .setValidator(
                CompositeDateValidator.allOf(
                    listOf(
                        DateValidatorPointForward.from(
                            dateStart?.toUTCEpochMillis() ?: LocalDate.MIN.toUTCEpochMillis()
                        ),
                        DateValidatorPointBackward.before(
                            dateEnd?.toUTCEpochMillis() ?: todayInUtcMilliseconds()
                        )
                    )
                )
            )
            .build()

        val picker = MaterialDatePicker.Builder
            .datePicker()
            .setTitleText(R.string.select_date)
            .setSelection(selectedDate?.toUTCEpochMillis() ?: todayInUtcMilliseconds())
            .setCalendarConstraints(constraints)
            .build()

        picker.addOnPositiveButtonClickListener {
            listener.onDateSelected(timestampToLocalDate(it))
        }

        when (context) {
            is AppCompatActivity -> picker.show(context.supportFragmentManager, TAG)
            else -> {
                throw IllegalArgumentException("Can only display DatePicker from AppCompatActivity")
            }
        }

        return picker
    }
}
