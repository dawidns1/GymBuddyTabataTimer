package com.example.gymbuddy_tabatatimer.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.gymbuddy_tabatatimer.model.Part
import java.util.*
import kotlin.collections.ArrayList

class PartsViewModel : ViewModel() {

    private val _inputOngoing=MutableLiveData(false)
    val inputOngoing:LiveData<Boolean> = _inputOngoing
    fun setInputOngoing(inputOngoing: Boolean?){
        _inputOngoing.value=inputOngoing
    }

    private val _preparation=MutableLiveData(5)
    val preparation:LiveData<Int> = _preparation
    fun setPreparation(preparation: Int?){
        _preparation.value=preparation
    }

    private val _rounds=MutableLiveData(1)
    val rounds:LiveData<Int> = _rounds
    fun setRounds(rounds: Int?){
        _rounds.value=rounds
    }

    private val _duration=MutableLiveData(0)
    val duration:LiveData<Int> = _duration
    fun setDuration(duration:Int){
        _duration.value=duration
    }

    private val _parts = MutableLiveData<ArrayList<Part>>(ArrayList())
    val parts: LiveData<ArrayList<Part>> = _parts
    fun setParts(parts: ArrayList<Part>) {
        _parts.value = parts
    }
    fun addNewPart(part:Part){
        _parts.value?.add(part)
        _parts.notifyObserver()
    }

    fun addNewPart(part:Part, position:Int){
        _parts.value?.add(position,part)
        _parts.notifyObserver()
    }

    fun removePart(position:Int){
        _parts.value?.removeAt(position)
        _parts.notifyObserver()
    }

    fun swapParts(fromPosition:Int, toPosition:Int){
        Collections.swap(_parts.value!!, fromPosition, toPosition)
        _parts.notifyObserver()
    }

    private fun <T> MutableLiveData<T>.notifyObserver() {
        this.value = this.value
    }

    fun calculateDuration():Boolean{
        var exerciseOrBreakNumber=0
        val durations = ArrayList<Int>()
        var calculatedDuration=0
        durations.add(0)
        var setBreak = 0
        val cycles = ArrayList<Int>()
        for (i in _parts.value!!.indices) {
            if (_parts.value!![i].type == "set marker") {
                if (i + 1 <= _parts.value!!.lastIndex && _parts.value!![i + 1].type == "set marker") {
                    continue
                } else {
                    cycles.add(_parts.value!![i].duration)
                    setBreak += _parts.value!![i].setBreak
                    durations.add(0)
                }
            } else {
                exerciseOrBreakNumber++
                durations[durations.lastIndex] += _parts.value!![i].duration
            }
        }
        if (cycles.isEmpty()) calculatedDuration = durations[durations.lastIndex]
        else {
            for (i in cycles.indices) {
                calculatedDuration += cycles[i] * durations[i]
            }
            if (durations.size > cycles.size) {
                calculatedDuration += durations[durations.lastIndex]
            }
        }
        calculatedDuration += setBreak
        if(calculatedDuration!=_duration.value)_duration.value=calculatedDuration
        return exerciseOrBreakNumber>0
    }
}