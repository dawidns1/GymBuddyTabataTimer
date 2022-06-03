package com.example.gymbuddy_tabatatimer.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gymbuddy_tabatatimer.model.Part
import com.example.gymbuddy_tabatatimer.R

@SuppressLint("StaticFieldLeak")
class TabataSessionViewModel(application: Application) : AndroidViewModel(application) {

//    var currentPart = 0
    var swiped=false
    var workout = ArrayList<Part>()
    private var starts = ArrayList<Int>()
    private var ends = ArrayList<Int>()
    var increments = ArrayList<Int>()
    var cycles = ArrayList<Int>()
    var context = getApplication<Application>()
    var prep = 0
    var rounds = 0
    var next = 0
    var isInBackground = false
    var countDownTimer: CountDownTimer? = null
    var finalSoundPlayed = false

    private val TAG = "TabataSessionViewModel"

    fun restartTimer(time: Long?) {

        countDownTimer?.cancel()
        countDownTimer = null

        _timerStarted.value=true
        _timerPaused.value=false

        finalSoundPlayed = false
        countDownTimer = object : CountDownTimer(time?.plus(1000)!!, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                setTimeLeftInMs(millisUntilFinished)
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            override fun onFinish() {
                if (currentPart.value != workout.lastIndex) {
                    nextPart()
                    _timeLeftInMs.value=(workout[currentPart.value!!].duration * 1000).toLong()
                    restartTimer((workout[currentPart.value!!].duration * 1000).toLong())
                    if (isInBackground) {
                        pauseTimer()
                        setTimeLeftInMs((workout[currentPart.value!!].duration) * 1000.toLong())
                    }
                } else {
                    _workoutFinished.value = true
                }
            }
        }.start()
    }

    fun pauseTimer() {
        countDownTimer?.cancel()
        _timerPaused.value = true
    }

    fun resumeTimer(timeLeftInMs: Long?) {
        restartTimer(timeLeftInMs?.minus(1000))
        _timerPaused.value = false
    }

    fun nextPart() {
        next = 1
        when {
            increments[currentPart.value!!] == 1 -> {
                setCurrentCycle(currentCycle.value?.plus(1))
            }
            increments[currentPart.value!!] == 2 -> {
                setCurrentSet(currentSet.value?.plus(1))
                setCurrentCycle(1)
            }
            increments[currentPart.value!!] == 3 -> {
                setCurrentRound(currentRound.value?.plus(1))
                setCurrentSet(1)
                setCurrentCycle(1)
            }
        }
        setCurrentPart(currentPart.value?.plus(1))
        Log.d(TAG, "nextPart: ${currentPart.value}")
    }

    fun previousPart() {
        next = -1
        setCurrentPart(currentPart.value?.minus(1))
        when {
            increments[currentPart.value!!] == 1 -> {
                setCurrentCycle(currentCycle.value?.minus(1))
            }
            increments[currentPart.value!!] == 2 -> {
                setCurrentSet(currentSet.value?.minus(1))
                setCurrentCycle(cycles[currentSet.value?.minus(1)!!])
            }
            increments[currentPart.value!!] == 3 -> {
                setCurrentRound(currentRound.value?.minus(1))
                setCurrentSet(cycles.size)
                setCurrentCycle(cycles[currentSet.value?.minus(1)!!])
            }
        }
    }

    fun changeTimerOnSwipe() {
        if (timerStarted.value == true && timerPaused.value == false) {
            countDownTimer?.cancel()
            restartTimer((workout[currentPart.value!!].duration * 1000).toLong())
        }else{
            setTimeLeftInMs((workout[currentPart.value!!].duration * 1000).toLong())
        }
    }

    fun workoutInit(parts: ArrayList<Part>) {
        cleanupAndGetDetails(parts)
        workoutBuilder(parts)
    }

    private val _workoutFinished = MutableLiveData(false)
    val workoutFinished: LiveData<Boolean> = _workoutFinished

    private val _timeLeftInMs = MutableLiveData<Long>(0)
    val timeLeftInMs: LiveData<Long> = _timeLeftInMs
    fun setTimeLeftInMs(timeLeftInMs: Long) {
        _timeLeftInMs.value = timeLeftInMs
    }

    private val _locked = MutableLiveData(false)
    val locked: LiveData<Boolean> = _locked
    fun setLocked(locked: Boolean) {
        _locked.value = locked
    }

    private val _muted = MutableLiveData(false)
    val muted: LiveData<Boolean> = _muted
    fun setMuted(muted: Boolean) {
        _muted.value = muted
    }

    private val _currentPart=MutableLiveData(0)
    val currentPart:LiveData<Int> = _currentPart
    fun setCurrentPart(currentPart:Int?){
        _currentPart.value=currentPart
    }

    private val _currentCycle = MutableLiveData(1)
    val currentCycle: LiveData<Int> = _currentCycle
    fun setCurrentCycle(currentCycle: Int?) {
        _currentCycle.value = currentCycle
    }

    private val _currentRound = MutableLiveData(1)
    val currentRound: LiveData<Int> = _currentRound
    fun setCurrentRound(currentRound: Int?) {
        _currentRound.value = currentRound
    }

    private val _currentSet = MutableLiveData(1)
    val currentSet: LiveData<Int> = _currentSet
    fun setCurrentSet(currentSet: Int?) {
        _currentSet.value = currentSet
    }

    private val _timerStarted = MutableLiveData(false)
    val timerStarted: LiveData<Boolean> = _timerStarted
    fun setTimerStarted(timerStarted: Boolean?) {
        _timerStarted.value = timerStarted
    }

    private val _timerPaused = MutableLiveData(false)
    val timerPaused: LiveData<Boolean> = _timerPaused
    fun setTimerPaused(timerPaused: Boolean?) {
        _timerPaused.value = timerPaused
    }

    private fun cleanupAndGetDetails(parts: ArrayList<Part>) {
        starts.add(0)
        var i = 0
        while (i <= parts.lastIndex) {
            if (i == 0 && parts[0].type == "set marker") parts.removeAt(0)
            else if (parts[i].type == "set marker") {
                if (i + 1 <= parts.lastIndex && parts[i + 1].type == "set marker") {
                    parts.removeAt(i)
                    continue
                } else {
                    ends.add(i - 1)
                    cycles.add(parts[i].duration)
                    if (i + 1 <= parts.lastIndex) starts.add(i + 1)
                }
            }
            i++
        }
        if (parts[parts.lastIndex].type != "set marker") {
            parts.add(Part(0, context.resources.getString(R.string.setMarker), "set marker", 1, R.drawable.ic_set_marker))
            ends.add(parts.lastIndex - 1)
            cycles.add(1)
        }
    }

    private fun workoutBuilder(parts: ArrayList<Part>) {
        for (r in 1..rounds) {
            for (i in cycles.indices) {
                for (j in 0 until cycles[i]) {
                    for (k in starts[i]..ends[i]) {
                        workout.add(parts[k])
//                        Toast.makeText(this, "${parts[k].type} ${j} / ${cycles[i]-1}", Toast.LENGTH_SHORT).show()
                        if (k == ends[i]) {
                            workout[workout.lastIndex].increment = 1
                            if (j == cycles[i] - 1) {
                                workout[workout.lastIndex].increment = 2
                                if (i == cycles.lastIndex) {
                                    workout[workout.lastIndex].increment = 3
                                }
                            }
                        }
                        increments.add(workout[workout.lastIndex].increment)
                        if (parts[k + 1].type == "set marker" && k + 1 < parts.lastIndex && parts[k + 1].setBreak != 0 && increments[increments.lastIndex] == 2) {
                            workout.add(Part(0, context.resources.getString(R.string.setBreak), context.resources.getString(R.string.breakTxt), parts[k + 1].setBreak, R.drawable.ic_break))
                            increments.add(0)
                        }
                    }
                }
            }
        }
        if (prep != 0) {
            workout.add(0, Part(0, context.resources.getString(R.string.preparation), context.resources.getString(R.string.preparation), prep, R.drawable.ic_break))
            increments.add(0, 0)
        }
        setTimeLeftInMs((workout[currentPart.value!!].duration * 1000).toLong())
    }
}