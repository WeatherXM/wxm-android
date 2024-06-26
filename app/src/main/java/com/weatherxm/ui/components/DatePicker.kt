package com.weatherxm.ui.components

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CalendarConstraints.DateValidator
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds
import com.weatherxm.R
import com.weatherxm.util.DateTimeHelper.timestampToLocalDate
import com.weatherxm.util.toUTCEpochMillis
import java.time.LocalDate

object DatePicker {

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
        val validatorPoints = mutableListOf<DateValidator>()
        (dateStart?.toUTCEpochMillis() ?: LocalDate.MIN.toUTCEpochMillis())?.let {
            validatorPoints.add(DateValidatorPointForward.from(it))
        }
        validatorPoints.add(
            DateValidatorPointBackward.before(
                dateEnd?.toUTCEpochMillis() ?: todayInUtcMilliseconds()
            )
        )
        val constraints = CalendarConstraints.Builder()
            .setValidator(CompositeDateValidator.allOf(validatorPoints))
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

        require(context is AppCompatActivity)
        picker.show(context.supportFragmentManager, TAG)

        return picker
    }
}
