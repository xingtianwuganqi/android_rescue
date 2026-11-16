package com.rescue.flutter_720yun.adoption.activity

import android.app.Activity
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.fragment.LocationSheetFragment
import com.rescue.flutter_720yun.adoption.models.AdoptionLabels
import com.rescue.flutter_720yun.adoption.models.ProfileInputPolicy
import com.rescue.flutter_720yun.adoption.ui.notice
import com.rescue.flutter_720yun.adoption.viewmodels.AdoptionProfileViewModel
import com.rescue.flutter_720yun.databinding.AdoptionProfileContentBinding
import com.rescue.flutter_720yun.databinding.AdoptionProfileOptionSheetBinding
import com.rescue.flutter_720yun.databinding.AdoptionProfileSheetOptionBinding

class AdoptionProfileActivity : AdoptionActivity() {
    private val vm by lazy { ViewModelProvider(this)[AdoptionProfileViewModel::class.java] }
    private var binding: AdoptionProfileContentBinding? = null
    private var optionSheet: BottomSheetDialog? = null

    private val errors = mutableMapOf<String, TextView>()
    private val errorRows = mutableMapOf<String, View>()
    private val fieldViews = mutableMapOf<String, View>()
    private val normalBackgrounds = mutableMapOf<View, Int>()

    override val contentLayoutRes: Int get() = R.layout.activity_adoption_profile_container

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setupToolbar(getString(R.string.adoption_profile_title))
        setupCenteredTitle()
        render()
        observe(vm, false)
        addObservers()
        requireLogin()
    }

    private fun setupCenteredTitle() {
        supportActionBar?.setDisplayShowTitleEnabled(false)
        baseBinding.toolbar.addView(TextView(this).apply {
            text = getString(R.string.adoption_profile_title)
            textSize = 17f
            setTextColor(ContextCompat.getColor(context, R.color.color_title))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }, Toolbar.LayoutParams(-2, -1, Gravity.CENTER))
    }

    private fun addObservers() {
        vm.data.observe(this) { if (it != null) fill() }
        vm.busy.observe(this) { updateSaveState() }
        vm.saved.observe(this) { saved ->
            if (saved == true) {
                notice(getString(R.string.save_success))
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
        vm.error.observe(this) { error ->
            if (error == null) return@observe
            error.fields.forEach { (key, value) -> showFieldError(key, value) }
            invalidateOptionsMenu()
            if (error.errorCode == "PROFILE_CHANGED") {
                vm.load(preserveDraft = true)
            }
        }
    }

    override fun onAuthenticated() {
        if (binding?.root?.parent != content) render()
        if (!vm.loaded) vm.load()
    }

    private fun render() {
        optionSheet?.dismiss()
        optionSheet = null
        content.removeAllViews()
        errors.clear()
        errorRows.clear()
        fieldViews.clear()
        normalBackgrounds.clear()

        val b = AdoptionProfileContentBinding.inflate(layoutInflater, content, true)
        binding = b

        // 滚动区域位于保存按钮上方；键盘压缩页面后将整个经验输入框滚入可见区域。
        fun revealExperienceInput() {
            b.profileScroll.post {
                if (binding !== b || !b.experienceInput.hasFocus()) return@post
                val input = b.experienceInputContainer
                input.requestRectangleOnScreen(Rect(0, 0, input.width, input.height), false)
            }
        }
        b.experienceInput.setOnFocusChangeListener { _, focused ->
            if (focused) revealExperienceInput()
        }
        b.profileScroll.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top < oldBottom - oldTop) revealExperienceInput()
        }

        b.cityRow.setOnClickListener { chooseRegion() }
        b.housingRow.setOnClickListener {
            chooseOption(
                getString(R.string.adoption_profile_housing_label),
                AdoptionLabels.housing,
                vm.housing
            ) { key ->
                vm.housing = key
                clearFieldError("housing_type")
                fill()
            }
        }
        b.employmentRow.setOnClickListener {
            chooseOption(
                getString(R.string.adoption_profile_employment_label),
                AdoptionLabels.employment,
                vm.employment
            ) { key ->
                vm.employment = key
                clearFieldError("employment_status")
                fill()
            }
        }
        b.save.setOnClickListener { saveProfile() }

        b.ageInput.doAfterTextChanged {
            vm.age = it?.toString().orEmpty()
            if (ProfileInputPolicy.age(vm.age) != null) clearFieldError("age")
            updateProgress()
        }
        b.experienceInput.doAfterTextChanged {
            vm.experience = it?.toString().orEmpty()
            if (vm.experience.length <= 200) clearFieldError("pet_experience")
            updateExperienceCounter()
            updateSaveState()
        }

        registerField("age", b.ageInputContainer, b.ageErrorRow, b.ageError, R.drawable.adoption_profile_field_background)
        registerField("residence_city", b.cityRow, b.cityErrorRow, b.cityError, R.drawable.adoption_profile_select_background)
        registerField("residence_district", b.cityRow, b.cityErrorRow, b.cityError, R.drawable.adoption_profile_select_background)
        registerField("housing_type", b.housingRow, b.housingErrorRow, b.housingError, R.drawable.adoption_profile_select_background)
        registerField("employment_status", b.employmentRow, b.employmentErrorRow, b.employmentError, R.drawable.adoption_profile_select_background)
        registerField("pet_experience", b.experienceInputContainer, b.experienceErrorRow, b.experienceError, R.drawable.adoption_profile_field_background)

        fill()
    }

    private fun registerField(
        key: String,
        target: View,
        errorRow: View,
        errorText: TextView,
        normalBackground: Int
    ) {
        fieldViews[key] = target
        errorRows[key] = errorRow
        errors[key] = errorText
        normalBackgrounds[target] = normalBackground
    }

    private fun fill() {
        val b = binding ?: return
        b.ageInput.setText(vm.age)
        b.ageInput.setSelection(b.ageInput.text.length)
        b.experienceInput.setText(vm.experience)
        b.experienceInput.setSelection(b.experienceInput.text.length)

        setSelection(
            b.cityValue,
            if (vm.city.isBlank() || vm.district.isBlank()) null else "${vm.city} · ${vm.district}",
            getString(R.string.adoption_profile_city_placeholder)
        )
        setSelection(
            b.housingValue,
            AdoptionLabels.housing[vm.housing],
            getString(R.string.adoption_profile_housing_placeholder)
        )
        setSelection(
            b.employmentValue,
            AdoptionLabels.employment[vm.employment],
            getString(R.string.adoption_profile_employment_placeholder)
        )

        updateExperienceCounter()
        updateProgress()
    }

    private fun setSelection(view: TextView, value: String?, placeholder: String) {
        val selected = !value.isNullOrBlank()
        view.text = if (selected) value else placeholder
        view.setTextColor(
            ContextCompat.getColor(
                this,
                if (selected) R.color.adoption_profile_ink else R.color.adoption_profile_faint
            )
        )
        view.typeface = if (selected) {
            Typeface.create("sans-serif-medium", Typeface.NORMAL)
        } else {
            Typeface.DEFAULT
        }
    }

    private fun updateExperienceCounter() {
        binding?.experienceCounter?.text =
            getString(R.string.adoption_profile_counter, vm.experience.length)
    }

    private fun completedCount(): Int = listOf(
        ProfileInputPolicy.age(vm.age) != null,
        vm.city.isNotBlank() && vm.district.isNotBlank(),
        vm.housing.isNotBlank(),
        vm.employment.isNotBlank()
    ).count { it }

    private fun updateProgress() {
        val b = binding ?: return
        val count = completedCount()
        b.profileProgress.progress = count
        b.progressText.text = getString(R.string.adoption_profile_progress_value, count)
        updateSaveState()
    }

    private fun updateSaveState() {
        val b = binding ?: return
        val hasInput = b.ageInput.text.toString().isNotBlank() ||
            vm.city.isNotBlank() || vm.district.isNotBlank() ||
            vm.housing.isNotBlank() || vm.employment.isNotBlank() ||
            b.experienceInput.text.toString().isNotBlank()
        b.save.isEnabled = vm.loaded && vm.busy.value != true && hasInput
    }

    private fun saveProfile() {
        val b = binding ?: return
        vm.age = b.ageInput.text.toString()
        vm.experience = b.experienceInput.text.toString()
        clearAllErrors()

        val failures = mutableMapOf<String, String>()
        if (ProfileInputPolicy.age(vm.age) == null) {
            failures["age"] = ProfileInputPolicy.AGE_ERROR
        }
        if (vm.city.isBlank() || vm.district.isBlank()) {
            failures["residence_city"] = getString(R.string.adoption_profile_city_error)
        }
        if (vm.housing.isBlank()) {
            failures["housing_type"] = getString(R.string.adoption_profile_housing_error)
        }
        if (vm.employment.isBlank()) {
            failures["employment_status"] = getString(R.string.adoption_profile_employment_error)
        }

        if (failures.isEmpty()) {
            vm.save()
        } else {
            failures.forEach { (key, value) -> showFieldError(key, value) }
        }
    }

    private fun showFieldError(key: String, value: String) {
        errors[key]?.text = value
        errorRows[key]?.isVisible = true
        fieldViews[key]?.setBackgroundResource(R.drawable.adoption_profile_field_error_background)
    }

    private fun clearFieldError(key: String) {
        errorRows[key]?.isVisible = false
        val target = fieldViews[key] ?: return
        val stillInvalid = errors.keys.any { other ->
            fieldViews[other] === target && errorRows[other]?.isVisible == true
        }
        if (!stillInvalid) {
            normalBackgrounds[target]?.let { target.setBackgroundResource(it) }
        }
    }

    private fun clearAllErrors() {
        errorRows.values.forEach { it.isVisible = false }
        fieldViews.values.toSet().forEach { view ->
            normalBackgrounds[view]?.let { view.setBackgroundResource(it) }
        }
    }

    private fun chooseOption(
        title: String,
        options: Map<String, String>,
        selected: String,
        changed: (String) -> Unit
    ) {
        val entries = options.entries.toList()
        showOptionSheet(title, entries.map { it.value }, options[selected]) { index ->
            changed(entries[index].key)
        }
    }

    private fun chooseRegion() {
        if (supportFragmentManager.isStateSaved ||
            supportFragmentManager.findFragmentByTag("profile_region") != null) return
        val sheet = LocationSheetFragment()
        sheet.selectCompletion = selected@{ items ->
            if (isFinishing || isDestroyed || binding == null) return@selected
            val city = items.getOrNull(1) ?: return@selected
            val district = if (city.children.isNullOrEmpty()) {
                "全市"
            } else {
                items.getOrNull(2)?.name ?: return@selected
            }
            vm.city = city.name
            vm.district = district
            clearFieldError("residence_city")
            clearFieldError("residence_district")
            fill()
        }
        sheet.show(supportFragmentManager, "profile_region")
    }

    private fun showOptionSheet(
        title: String,
        labels: List<String>,
        selectedLabel: String?,
        onSelected: (Int) -> Unit
    ) {
        if (labels.isEmpty()) return
        optionSheet?.dismiss()

        val sheet = AdoptionProfileOptionSheetBinding.inflate(layoutInflater)
        sheet.sheetTitle.text = title
        sheet.sheetOptions.removeAllViews()
        val maxHeight = (resources.displayMetrics.heightPixels * 0.55f).toInt()
        val desiredHeight = dp(47) * labels.size + dp(8)
        sheet.sheetOptionsScroll.layoutParams = sheet.sheetOptionsScroll.layoutParams.apply {
            height = minOf(maxHeight, desiredHeight)
        }

        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheet.root)
        dialog.setOnDismissListener { if (optionSheet === dialog) optionSheet = null }
        dialog.setOnShowListener {
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundColor(Color.TRANSPARENT)
        }

        labels.forEachIndexed { index, label ->
            val item = AdoptionProfileSheetOptionBinding.inflate(
                layoutInflater,
                sheet.sheetOptions,
                false
            )
            item.optionText.text = label
            val selected = label == selectedLabel
            if (selected) {
                item.optionRow.setBackgroundResource(R.drawable.adoption_profile_sheet_option_selected)
                item.optionText.setTextColor(
                    ContextCompat.getColor(this, R.color.adoption_profile_sheet_selected)
                )
                item.optionText.typeface =
                    Typeface.create("sans-serif-medium", Typeface.NORMAL)
                item.optionCheck.isVisible = true
            } else {
                item.optionRow.setBackgroundResource(R.drawable.adoption_profile_sheet_option_background)
                item.optionText.setTextColor(
                    ContextCompat.getColor(this, R.color.adoption_profile_sheet_text)
                )
                item.optionText.typeface = Typeface.DEFAULT
                item.optionCheck.isVisible = false
            }
            item.optionRow.setOnClickListener {
                dialog.dismiss()
                onSelected(index)
            }
            sheet.sheetOptions.addView(item.root)
        }

        optionSheet = dialog
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (vm.error.value != null && !vm.loaded) {
            menu.add(0, 901, 0, getString(R.string.common_retry)).apply {
                isEnabled = vm.busy.value != true
                setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 901) {
            vm.load()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        optionSheet?.dismiss()
        optionSheet = null
        super.onPause()
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
