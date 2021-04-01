package com.example.gymbuddy_tabatatimer

import java.io.Serializable
import kotlin.collections.ArrayList

data class Tabata(
    var id: Int,
    var name: String = "Tabata",
    var durationTotal: Int = 0,
    var parts: ArrayList<Part> = ArrayList(),
    var defRounds: Int = 1,
    var defPrep: Int = 5,
    var state: IntArray = intArrayOf(0,0,0,0,0,0)
) : Serializable

