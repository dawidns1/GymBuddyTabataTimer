package com.example.gymbuddy_tabatatimer

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.tasks.Task

object Helpers {

    var modifiedTabata: Tabata? = null

    fun showRatingUserInterface(activity: Activity) {
        val lastAppRating: Long = Utils.getInstance(activity.applicationContext).getLastAppRating()
        val days: Int = millisToDays(System.currentTimeMillis() - lastAppRating)
        if (days > 30) {
            Utils.getInstance(activity.applicationContext).setLastAppRating(System.currentTimeMillis())
            val manager: ReviewManager = ReviewManagerFactory.create(activity)
            val request: Task<ReviewInfo> = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                try {
                    if (task.isSuccessful) {
                        val reviewInfo: ReviewInfo = task.result
                        val flow: Task<Void> = manager.launchReviewFlow(activity, reviewInfo)
                        flow.addOnCompleteListener { }
                    }
                } catch (ex: Exception) {
                }
            }
        }
    }

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
                        ContextCompat.getColor(context, R.color.purple_500)
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

    fun handleAds(adContainer: FrameLayout, activity: Activity) {
        val ad = AdView(activity.applicationContext)
        ad.adUnitId = "ca-app-pub-3940256099942544/6300978111"
        adContainer.addView(ad)
        loadBanner(ad, activity)
        adContainer.layoutParams.height = ad.adSize.getHeightInPixels(activity.applicationContext)
    }

    private fun loadBanner(ad: AdView, activity: Activity) {
        val adRequest = AdRequest.Builder().build()
//        Toast.makeText(activity.applicationContext, "${adContainer.width}", Toast.LENGTH_SHORT).show()

        val adSize = getAdSize(activity)
        ad.adSize = adSize

        ad.loadAd(adRequest)
    }

    private fun getAdSize(activity: Activity): AdSize {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val widthPixels = outMetrics.widthPixels
        val density = outMetrics.density

        var divider = 1
        if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) divider = 2

        val adWidth = ((widthPixels / density) / divider).toInt()

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity.applicationContext, adWidth)
    }

    private fun millisToDays(millis: Long): Int {
        return (millis / 1000 / 60 / 60 / 24).toInt()
    }

}