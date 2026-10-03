package com.rescue.flutter_720yun.adoption.ui

import android.content.Context
import android.widget.*
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import com.rescue.flutter_720yun.adoption.models.AdoptionLabels

fun LinearLayout.label(text: String): TextView = TextView(context).apply {
    this.text = text; textSize = 16f; setPadding(0, 12, 0, 12); isSaveEnabled = false
}.also { addView(it) }
fun LinearLayout.button(text: String, action: () -> Unit): MaterialButton = MaterialButton(context).apply {
    this.text = text; setOnClickListener { action() }; isSaveEnabled = false
}.also { addView(it, ViewGroup.LayoutParams(-1, -2)) }
fun LinearLayout.field(hint: String, max: Int = 200): EditText = EditText(context).apply {
    this.hint = hint; isSaveEnabled = false; importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
    filters = arrayOf(android.text.InputFilter.LengthFilter(max))
}.also { addView(it, ViewGroup.LayoutParams(-1, -2)) }
fun LinearLayout.choice(title: String, labels: List<String>): Spinner {
    label(title)
    return Spinner(context).apply {
        adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, labels)
        isSaveEnabled = false
    }.also { addView(it, ViewGroup.LayoutParams(-1, -2)) }
}
fun Context.notice(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
