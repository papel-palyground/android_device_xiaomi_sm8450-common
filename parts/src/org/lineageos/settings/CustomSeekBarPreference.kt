/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings

import android.content.Context
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import kotlin.math.floorDiv

open class CustomSeekBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = TypedArrayUtils.getAttr(
        context,
        androidx.preference.R.attr.preferenceStyle,
        android.R.attr.preferenceStyle
    ),
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes), SeekBar.OnSeekBarChangeListener {

    protected val TAG: String = javaClass.name

    companion object {
        private const val SETTINGS_NS = "http://schemas.android.com/apk/res/com.android.settings"
        private const val ANDROIDNS = "http://schemas.android.com/apk/res/android"
    }

    protected var interval = 1
    protected var showSign = false
    protected var units = ""
    protected var continuousUpdates = false

    protected var minValue = 0
    protected var maxValue = 100
    protected var defaultValueExists = false
    protected var defaultVal = 0
    protected var defaultValueTextExists = false
    protected var defaultValueText: String? = null

    protected var currentValue = 0

    protected var valueTextView: TextView? = null
    protected var resetImageView: ImageView? = null
    protected var minusImageView: ImageView? = null
    protected var plusImageView: ImageView? = null
    protected var seekBar: SeekBar

    protected var trackingTouch = false
    protected var trackingValue = 0

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CustomSeekBarPreference)
        try {
            showSign = a.getBoolean(R.styleable.CustomSeekBarPreference_showSign, showSign)
            val unitsStr = a.getString(R.styleable.CustomSeekBarPreference_units)
            if (unitsStr != null) units = " $unitsStr"
            continuousUpdates = a.getBoolean(R.styleable.CustomSeekBarPreference_continuousUpdates, continuousUpdates)
            val dvt = a.getString(R.styleable.CustomSeekBarPreference_defaultValueText)
            defaultValueTextExists = !dvt.isNullOrEmpty()
            if (defaultValueTextExists) defaultValueText = dvt
        } finally {
            a.recycle()
        }

        try {
            val newInterval = attrs?.getAttributeValue(SETTINGS_NS, "interval")
            if (newInterval != null) interval = newInterval.toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Invalid interval value", e)
        }

        minValue = attrs?.getAttributeIntValue(SETTINGS_NS, "min", minValue) ?: minValue
        maxValue = attrs?.getAttributeIntValue(ANDROIDNS, "max", maxValue) ?: maxValue
        if (maxValue < minValue) maxValue = minValue

        val defaultValue = attrs?.getAttributeValue(ANDROIDNS, "defaultValue")
        defaultValueExists = !defaultValue.isNullOrEmpty()
        if (defaultValueExists) {
            defaultVal = getLimitedValue(defaultValue!!.toInt())
            currentValue = defaultVal
        } else {
            currentValue = minValue
        }

        seekBar = SeekBar(context, attrs)
        layoutResource = R.layout.preference_custom_seekbar
    }

    override fun onDependencyChanged(dependency: Preference, disableDependent: Boolean) {
        super.onDependencyChanged(dependency, disableDependent)
        setShouldDisableView(true)
        seekBar.isEnabled = !disableDependent
        resetImageView?.isEnabled = !disableDependent
        plusImageView?.isEnabled = !disableDependent
        minusImageView?.isEnabled = !disableDependent
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        try {
            val oldContainer = seekBar.parent
            val newContainer = holder.findViewById(R.id.seekbar) as ViewGroup
            if (oldContainer != newContainer) {
                (oldContainer as? ViewGroup)?.removeView(seekBar)
                newContainer.removeAllViews()
                newContainer.addView(seekBar, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error binding view: $ex")
        }

        seekBar.max = getSeekValue(maxValue)
        seekBar.progress = getSeekValue(currentValue)
        seekBar.isEnabled = isEnabled

        valueTextView = holder.findViewById(R.id.value) as? TextView
        resetImageView = holder.findViewById(R.id.reset) as? ImageView
        minusImageView = holder.findViewById(R.id.minus) as? ImageView
        plusImageView = holder.findViewById(R.id.plus) as? ImageView

        updateValueViews()

        seekBar.setOnSeekBarChangeListener(this)
        
        resetImageView?.setOnClickListener {
            Toast.makeText(context, context.getString(R.string.custom_seekbar_default_value_to_set, getTextValue(defaultVal)), Toast.LENGTH_LONG).show()
        }
        resetImageView?.setOnLongClickListener {
            setValue(defaultVal, true)
            true
        }
        minusImageView?.setOnClickListener {
            setValue(currentValue - interval, true)
        }
        minusImageView?.setOnLongClickListener {
            val newVal = if (maxValue - minValue > interval * 2 && maxValue + minValue < currentValue * 2) {
                floorDiv(maxValue + minValue, 2)
            } else minValue
            setValue(newVal, true)
            true
        }
        plusImageView?.setOnClickListener {
            setValue(currentValue + interval, true)
        }
        plusImageView?.setOnLongClickListener {
            val newVal = if (maxValue - minValue > interval * 2 && maxValue + minValue > currentValue * 2) {
                -1 * floorDiv(-1 * (maxValue + minValue), 2)
            } else maxValue
            setValue(newVal, true)
            true
        }
    }

    protected fun getLimitedValue(v: Int): Int = v.coerceIn(minValue, maxValue)

    protected fun getSeekValue(v: Int): Int = 0 - floorDiv(minValue - v, interval)

    protected fun getTextValue(v: Int): String {
        if (defaultValueTextExists && defaultValueExists && v == defaultVal) {
            return defaultValueText ?: ""
        }
        return (if (showSign && v > 0) "+" else "") + v.toString() + units
    }

    protected fun updateValueViews() {
        valueTextView?.let { tv ->
            if (!trackingTouch || continuousUpdates) {
                if (defaultValueTextExists && defaultValueExists && currentValue == defaultVal) {
                    tv.text = "$defaultValueText (${context.getString(R.string.custom_seekbar_default_value)})"
                } else {
                    val suffix = if (defaultValueExists && currentValue == defaultVal) {
                        " (${context.getString(R.string.custom_seekbar_default_value)})"
                    } else ""
                    tv.text = context.getString(R.string.custom_seekbar_value, getTextValue(currentValue)) + suffix
                }
            } else {
                if (defaultValueTextExists && defaultValueExists && trackingValue == defaultVal) {
                    tv.text = "[$defaultValueText]"
                } else {
                    tv.text = context.getString(R.string.custom_seekbar_value, "[${getTextValue(trackingValue)}]")
                }
            }
        }

        resetImageView?.visibility = if (!defaultValueExists || currentValue == defaultVal || trackingTouch) View.INVISIBLE else View.VISIBLE

        minusImageView?.let {
            if (currentValue == minValue || trackingTouch) {
                it.isClickable = false
                it.setColorFilter(context.getColor(R.color.disabled_text_color), PorterDuff.Mode.MULTIPLY)
            } else {
                it.isClickable = true
                it.clearColorFilter()
            }
        }

        plusImageView?.let {
            if (currentValue == maxValue || trackingTouch) {
                it.isClickable = false
                it.setColorFilter(context.getColor(R.color.disabled_text_color), PorterDuff.Mode.MULTIPLY)
            } else {
                it.isClickable = true
                it.clearColorFilter()
            }
        }
    }

    protected open fun changeValue(newValue: Int) {
        // for subclasses
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        val newValue = getLimitedValue(minValue + (progress * interval))
        if (trackingTouch && !continuousUpdates) {
            trackingValue = newValue
            updateValueViews()
        } else if (currentValue != newValue) {
            if (!callChangeListener(newValue)) {
                this.seekBar.progress = getSeekValue(currentValue)
                return
            }
            changeValue(newValue)
            persistInt(newValue)
            currentValue = newValue
            updateValueViews()
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        trackingValue = currentValue
        trackingTouch = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        trackingTouch = false
        if (!continuousUpdates) onProgressChanged(this.seekBar, getSeekValue(trackingValue), false)
        notifyChanged()
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        if (restoreValue) currentValue = getPersistedInt(currentValue)
    }

    override fun setDefaultValue(defaultValue: Any?) {
        when (defaultValue) {
            is Int -> setDefaultValue(defaultValue, seekBar != null)
            else -> setDefaultValue(defaultValue?.toString(), seekBar != null)
        }
    }

    fun setDefaultValue(newValue: Int, update: Boolean) {
        val limited = getLimitedValue(newValue)
        if (!defaultValueExists || defaultVal != limited) {
            defaultValueExists = true
            defaultVal = limited
            if (update) updateValueViews()
        }
    }

    fun setDefaultValue(newValue: String?, update: Boolean) {
        if (defaultValueExists && newValue.isNullOrEmpty()) {
            defaultValueExists = false
            if (update) updateValueViews()
        } else if (!newValue.isNullOrEmpty()) {
            setDefaultValue(newValue.toInt(), update)
        }
    }

    fun setValue(newValue: Int) {
        currentValue = getLimitedValue(newValue)
        seekBar.progress = getSeekValue(currentValue)
    }

    fun setValue(newValue: Int, update: Boolean) {
        val limited = getLimitedValue(newValue)
        if (currentValue != limited) {
            if (update) seekBar.progress = getSeekValue(limited)
            else currentValue = limited
        }
    }

    fun getValue(): Int = currentValue

    fun refresh(newValue: Int) {
        setValue(newValue, true)
    }
}
