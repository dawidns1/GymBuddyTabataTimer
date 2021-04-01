package com.example.gymbuddy_tabatatimer

import android.app.Activity
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.core.content.ContextCompat

object Helpers {

    var modifiedTabata: Tabata? = null

    fun textViewToSecs(v: TextView): Int {
        return 60 * Integer.parseInt(v.text[0].toString()) +
                10 * Integer.parseInt(v.text[2].toString()) +
                Integer.parseInt(v.text[3].toString())
    }

    fun secsToString(i: Int): String {
        return "${i / 60}:${String.format("%02d", i % 60)}"
    }

    fun hideKeyboard(context: Context, view: View) {
        val inputMethodManager = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
    fun setupActionBar(text1: String?, text2: String?, actionBar: ActionBar, context: Context) {
        actionBar.setBackgroundDrawable(
            ColorDrawable(
                ContextCompat.getColor(context,R.color.purple_500)
            )
        )
        actionBar.setDisplayShowTitleEnabled(false)
        actionBar.setDisplayUseLogoEnabled(false)
        actionBar.setDisplayHomeAsUpEnabled(false)
        actionBar.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowHomeEnabled(false)
        val params = ActionBar.LayoutParams(
            ActionBar.LayoutParams.MATCH_PARENT,
            ActionBar.LayoutParams.MATCH_PARENT
        )
        val customActionBar: View = LayoutInflater.from(context).inflate(R.layout.action_bar, null)
        actionBar.setCustomView(customActionBar, params)
        val abText1 = customActionBar.findViewById<TextView>(R.id.abText1)
        val abText2 = customActionBar.findViewById<TextView>(R.id.abText2)
        abText1.text = text1
        abText2.text = text2
    }
}