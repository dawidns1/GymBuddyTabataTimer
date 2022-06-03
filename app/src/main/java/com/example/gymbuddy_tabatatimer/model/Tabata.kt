package com.example.gymbuddy_tabatatimer.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

@IgnoreExtraProperties
data class Tabata(
    var id: Int=0,
    var name: String = "Tabata",
    var durationTotal: Int = 0,
    var parts: ArrayList<Part> = ArrayList(),
    var defRounds: Int = 1,
    var defPrep: Int = 5,
    var state: MutableList<Int> = mutableListOf(0,0,0,0,0,0),
    var cloudId:String="",
    @ServerTimestamp var timestamp: Date? = null

) : Serializable

//state
//0 - currentPart
//1 - currentSet
//2 - currentRound
//3 - currentCycle
//4 - rounds
//5 - prep