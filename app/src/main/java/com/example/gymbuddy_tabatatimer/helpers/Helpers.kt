package com.example.gymbuddy_tabatatimer.helpers

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.core.content.ContextCompat
import com.example.gymbuddy_tabatatimer.R
import com.example.gymbuddy_tabatatimer.model.Tabata
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.tasks.Task

object Helpers {

    var modifiedTabata: Tabata? = null
    var savedAdRequest: AdRequest? = null
    var rewardGrantedThisSession = false

    fun showRatingUserInterface(activity: Activity) {
        val lastAppRating: Long = Utils.getInstance(activity.applicationContext).getLastAppRating()
        var days = 0
        if (lastAppRating != 0L) {
            days = millisToDays(System.currentTimeMillis() - lastAppRating)
        }
//        val days: Int = millisToDays(System.currentTimeMillis() - lastAppRating)
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

    fun textViewToSecs(v: TextView, zeroAllowed: Boolean = false): Int {
        var value = 60 * Integer.parseInt(v.text[0].toString()) + 10 * Integer.parseInt(v.text[2].toString()) + Integer.parseInt(v.text[3].toString())
        if (value < 5 && !zeroAllowed) value = 5
        return value
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
                ContextCompat.getColor(context, R.color.grey_900)
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
        abText1.setTextColor(ContextCompat.getColor(context, R.color.purple_500))
        val abText2 = customActionBar.findViewById<TextView>(R.id.abText2)
        abText1.setTextColor(ContextCompat.getColor(context, R.color.purple_500))
        abText1.text = text1
        abText2.text = text2
    }

    fun isRewardGranted(rewardGranted: Long): Boolean {
        rewardGrantedThisSession = !(rewardGranted == 0L || millisToDays(System.currentTimeMillis() - rewardGranted) > 7)
        return rewardGrantedThisSession

    }

    fun handleNativeAds(template: TemplateView, activity: Activity, adUnitId: String, adLoader: AdLoader?, rewardGranted: Boolean = rewardGrantedThisSession): AdLoader? {
//        val adUnitId = "ca-app-pub-3940256099942544/2247696110";
        if (!rewardGranted) {
            var currentAdLoader = adLoader
            if (currentAdLoader == null) {
                currentAdLoader = AdLoader.Builder(activity.applicationContext, adUnitId)
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            template.visibility = GONE
                            super.onAdFailedToLoad(loadAdError)
                        }

                        override fun onAdLoaded() {
                            template.mockLayout?.visibility = GONE
                            template.realLayout?.visibility = VISIBLE
                            super.onAdLoaded()
                        }
                    })
                    .forNativeAd { nativeAd: NativeAd ->
                        val styles = NativeTemplateStyle.Builder().withMainBackgroundColor(ColorDrawable(ContextCompat.getColor(activity.applicationContext, R.color.grey_900))).build()
                        template.setStyles(styles)
                        template.setNativeAd(nativeAd)
                        if (activity.isDestroyed) {
                            nativeAd.destroy()
                        }
                    }
                    .build()
            }
            val lastAdShown = Utils.getInstance(activity.applicationContext).lastAdShown
            val currentTime = System.currentTimeMillis()
            if (lastAdShown == 0L || (currentTime - lastAdShown) / 1000 > 60 || savedAdRequest == null) {
                savedAdRequest = AdRequest.Builder().build()
                Utils.getInstance(activity.applicationContext).lastAdShown = currentTime
                //            Toast.makeText(activity, "new Ad", Toast.LENGTH_SHORT).show();
            }
            currentAdLoader?.loadAd(savedAdRequest!!)
            return currentAdLoader
        } else {
            template.visibility = GONE
            return null
        }
    }

    private fun millisToDays(millis: Long): Int {
        return (millis / 1000 / 60 / 60 / 24).toInt()
    }

    fun View.toggleVisibility(boolean: Boolean) {
        if (boolean) {
            if (this.visibility == GONE) this.visibility = VISIBLE
        } else
            if (this.visibility == VISIBLE) this.visibility = GONE
    }

    fun ExtendedFloatingActionButton.toggleVisibilityEFAB(boolean: Boolean) {
        if (boolean) {
            if (this.visibility == GONE) this.show()
        } else
            if (this.visibility == VISIBLE) this.hide()
    }

}