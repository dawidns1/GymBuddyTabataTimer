package com.example.gymbuddy_tabatatimer.helpers

import android.content.Context
import com.example.gymbuddy_tabatatimer.helpers.Constants.REWARD_GRANTED_KEY
import com.example.gymbuddy_tabatatimer.model.Part
import com.example.gymbuddy_tabatatimer.model.Tabata
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Utils private constructor(context: Context) {

    private val sharedPreferences =
        context.getSharedPreferences(Constants.DB_NAME, Context.MODE_PRIVATE)!!
    var tabatasID: Int
    var partsID: Int
    var lastAppRating: Long
    var lastAdShown: Long
    var rewardGranted: Long

    init {
        if (null == getAllTabatas()) {
            initData()
        }

        tabatasID = getTabatasID()
        partsID = getPartsID()
        lastAppRating=getLastAppRating()
        lastAdShown=getLastAdShown()
        rewardGranted=getRewardGranted()

    }

    private fun initData() {
        val tabatas: ArrayList<Tabata> = ArrayList()
        val editor = sharedPreferences.edit()
        val gson = Gson()
        editor.putString(Constants.ALL_TABATAS, gson.toJson(tabatas))
        editor.apply()
    }

    @JvmName("rewardGranted")
    fun getRewardGranted(): Long {
        return sharedPreferences.getLong(REWARD_GRANTED_KEY, 0)
    }

    fun setRewardGranted(rewardGranted: Long): Boolean {
        val editor = sharedPreferences.edit()
        editor.putLong(REWARD_GRANTED_KEY, rewardGranted)
        editor.apply()
        return true
    }

    @JvmName("getLastAdShown1")
    fun getLastAdShown(): Long {
        return sharedPreferences.getLong(Constants.LAST_AD_SHOWN_KEY, 0)
    }

    fun setLastAdShown(lastAdShown: Long): Boolean {
        val editor = sharedPreferences.edit()
        editor.putLong(Constants.LAST_AD_SHOWN_KEY, lastAdShown)
        editor.apply()
        return true
    }

    @JvmName("getLastAppRating1")
    fun getLastAppRating(): Long {
        return sharedPreferences.getLong(Constants.APP_RATING_KEY, 0)
    }

    fun setLastAppRating(lastAppRating: Long): Boolean {
        val editor = sharedPreferences.edit()
        editor.putLong(Constants.APP_RATING_KEY, lastAppRating)
        editor.apply()
        return true
    }

    @JvmName("getTabatasID1")
    fun getTabatasID(): Int {
        val id = sharedPreferences.getInt(Constants.TABATA_ID, 0)
        setTabatasID(id + 1)
        return id
    }

    @JvmName("getPartsID1")
    fun getPartsID(): Int {
        val id = sharedPreferences.getInt(Constants.PART_ID, 0)
        setTabataPartsID(id + 1)
        return id
    }

    private fun setTabatasID(tabatasID: Int): Boolean {
        val editor = sharedPreferences.edit()
        editor.putInt(Constants.TABATA_ID, tabatasID)
        editor.apply()
        return true
    }

    private fun setTabataPartsID(tabataPartsID: Int): Boolean {
        val editor = sharedPreferences.edit()
        editor.putInt(Constants.PART_ID, tabataPartsID)
        editor.apply()
        return true
    }

    fun getAllTabatas(): ArrayList<Tabata>? {
        val gson = Gson()
        val type = object : TypeToken<ArrayList<Tabata?>?>() {}.type
        return gson.fromJson<ArrayList<Tabata>>(
            sharedPreferences.getString(
                Constants.ALL_TABATAS,
                null
            ), type
        )
    }

    fun addTabata(tabata: Tabata): Boolean {
        val tabatas = getAllTabatas()
        if (null != tabatas) {
            if (tabatas.add(tabata)) {
                val gson = Gson()
                val editor = sharedPreferences.edit()
                editor.remove(Constants.ALL_TABATAS)
                editor.putString(Constants.ALL_TABATAS, gson.toJson(tabatas))
                editor.apply()
                return true
            }
        }
        return false
    }

    fun deleteTabata(tabata: Tabata): Boolean {
        val tabatas = getAllTabatas()
        if (null != tabatas) {
            for (t in tabatas) {
                if (t.id == tabata.id) {
                    if (tabatas.remove(t)) {
                        val gson = Gson()
                        val editor = sharedPreferences.edit()
                        editor.remove(Constants.ALL_TABATAS)
                        editor.putString(
                            Constants.ALL_TABATAS,
                            gson.toJson(tabatas)
                        )
                        editor.apply()
                        return true
                    }
                }
            }
        }
        return false
    }

    fun updateTabata(tabata: Tabata): Boolean {
        val tabatas = getAllTabatas()
        if (null != tabatas) {
            for (t in tabatas) {
                if (t.id == tabata.id) {
                    t.state=tabata.state
                    t.durationTotal=tabata.durationTotal
                    t.defRounds = tabata.defRounds
                    t.defPrep = tabata.defPrep
                    t.parts=tabata.parts
                    val gson = Gson()
                    val editor = sharedPreferences.edit()
                    editor.remove(Constants.ALL_TABATAS)
                    editor.putString(Constants.ALL_TABATAS, gson.toJson(tabatas))
                    editor.apply()
                    return true

                }
            }
        }
        return false
    }

    fun addPartToTabata(tabata: Tabata, part: Part): Boolean {
        val tabatas = getAllTabatas()
        if (null != tabatas) {
            for (t in tabatas) {
                if (t.id == tabata.id) {
                    val parts = t.parts
                    if (parts.add(part)) {
                        t.parts = parts
                        t.durationTotal = tabata.durationTotal
                        val gson = Gson()
                        val editor = sharedPreferences.edit()
                        editor.remove(Constants.ALL_TABATAS)
                        editor.putString(
                            Constants.ALL_TABATAS,
                            gson.toJson(tabatas)
                        )
                        editor.apply()
                        return true
                    }
                }
            }
        }
        return false
    }

    fun deletePartFromTabata(part: Part): Boolean {
        val tabatas = getAllTabatas()
        if (null != tabatas) {
            for (t in tabatas) {
                val parts = t.parts
                for (p in parts) if (p.id == part.id) {
                    if (parts.remove(p)) {
                        t.parts = parts
                        val gson = Gson()
                        val editor = sharedPreferences.edit()
                        editor.remove(Constants.ALL_TABATAS)
                        editor.putString(
                            Constants.ALL_TABATAS,
                            gson.toJson(tabatas)
                        )
                        editor.apply()
                        return true
                    }
                }
            }
        }
        return false
    }

    fun updatePart(part: Part): Boolean {
        val tabatas = getAllTabatas()
        if (null != tabatas) {
            for (t in tabatas) {
                for (i in t.parts.indices) if (t.parts[i].id == part.id) {
                    t.parts.removeAt(i)
                    t.parts.add(i, part)
                    val gson = Gson()
                    val editor = sharedPreferences.edit()
                    editor.remove(Constants.ALL_TABATAS)
                    editor.putString(
                        Constants.ALL_TABATAS,
                        gson.toJson(tabatas)
                    )
                    editor.apply()
                    return true

                }
            }
        }
        return false
    }

    fun updateTabatasParts(tabata: Tabata, parts: ArrayList<Part>): Boolean {
        val tabatas = getAllTabatas()
        if (null != tabatas) {
            for (t in tabatas) {
                if (t.id == tabata.id) {
                    t.parts = parts
                    t.durationTotal = tabata.durationTotal
                    val gson = Gson()
                    val editor = sharedPreferences.edit()
                    editor.remove(Constants.ALL_TABATAS)
                    editor.putString(
                        Constants.ALL_TABATAS,
                        gson.toJson(tabatas)
                    )
                    editor.apply()
                    return true
                }
            }
        }
        return false
    }

    fun updateTabatas(tabatas: ArrayList<Tabata>): Boolean {
        val gson = Gson()
        val editor = sharedPreferences.edit()
        editor.remove(Constants.ALL_TABATAS)
        editor.putString(Constants.ALL_TABATAS, gson.toJson(tabatas))
        editor.apply()
        return true
    }


    companion object : SingletonHolder<Utils, Context>(::Utils)
}
