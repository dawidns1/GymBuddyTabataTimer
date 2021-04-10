package com.example.gymbuddy_tabatatimer

import android.animation.ObjectAnimator
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.*
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_tabata_session.*


class TabataSessionActivity : AppCompatActivity() {
    private var timeLeftInMs: Long = 0
    private lateinit var soundPool: SoundPool
    private var soundFinishCycle = 0
    private var soundFinishSet = 0
    private var soundFinishRound = 0
    private var soundFinishPart = 0
    private var soundTick = 0
    private var soundFinishWorkout = 0
    private lateinit var tabata: Tabata
    private var rounds = 0
    private var currentRound = 1
    private var currentSet = 1
    private var prep = 0
    private var cycles = ArrayList<Int>()
    private var starts = ArrayList<Int>()
    private var ends = ArrayList<Int>()
    private var timerPaused = false
    private var timerStarted = false
    private var parts = ArrayList<Part>()
    private var workout = ArrayList<Part>()
    private var partTypeImgs = ArrayList<ImageView>()
    private var partTxts = ArrayList<TextView>()
    private var currentPart = 0
    private var increments = ArrayList<Int>()
    private var locked = false
    private var doubleBackToExitPressedOnce = false
    private var volume = 0f
    private var countDownTimer: CountDownTimer? = null
    private var resumed = false
    var isInBackGround = false
    private var systemTimeOnSaveState: Long = 0

    override fun onResume() {
        isInBackGround = false
        super.onResume()
    }

    override fun onPause() {
        if (tabata.state[0] != 0 && currentPart != 0) {
            tabata.state[0] = currentPart
            tabata.state[1] = currentSet
            tabata.state[2] = currentRound
            tabata.state[3] = txtCycle.text.toString().toInt()
            tabata.state[4] = rounds
            tabata.state[5] = prep
//            Toast.makeText(this, "${tabata.state[0]}", Toast.LENGTH_SHORT).show()
        }
        Utils.getInstance(this).updateTabata(tabata)
//        Toast.makeText(this, "${Utils.getInstance(this).getAllTabatas()!![0].state[0]}", Toast.LENGTH_SHORT).show()
        isInBackGround = true
        super.onPause()
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            finish()
            super.onBackPressed()
            return
        }
        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, R.string.pressBackToExit, Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_GymBuddyTabataTimer)
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        setContentView(R.layout.activity_tabata_session)

        Helpers.handleAds(tabataSessionAdContainer,this)

        supportActionBar?.hide()

        val intent = intent
        tabata = intent.getSerializableExtra(Constants.TABATA_KEY) as Tabata
        prep = intent.getIntExtra(Constants.PREP_KEY, 0)
        rounds = intent.getIntExtra(Constants.ROUNDS_KEY, 0)
        resumed = intent.getBooleanExtra(Constants.RESUMED_KEY, false)


        if (workout.isEmpty()) {
            parts = tabata.parts
            cleanupAndGetDetails()
            workoutBuilder()
            soundPoolInit()
        }

        viewInit()

        if (savedInstanceState != null) {
//            Toast.makeText(this, "here", Toast.LENGTH_SHORT).show()
            currentPart = savedInstanceState.getInt(Constants.CURRENT_PART)
            currentSet = savedInstanceState.getInt(Constants.CURRENT_SET)
            currentRound = savedInstanceState.getInt(Constants.CURRENT_ROUND)
            timerPaused = savedInstanceState.getBoolean(Constants.TIMER_PAUSED, true)
            timerStarted = savedInstanceState.getBoolean(Constants.TIMER_STARTED, false)
            timeLeftInMs = savedInstanceState.getLong(Constants.TIME_LEFT)
            cycles = savedInstanceState.getIntegerArrayList(Constants.CYCLES)!!
            parts = savedInstanceState.getSerializable(Constants.PARTS) as ArrayList<Part>
            systemTimeOnSaveState = savedInstanceState.getLong(Constants.SYSTEM_TIME)
            isInBackGround = savedInstanceState.getBoolean(Constants.IN_BACKGROUND)
            txtSet.text = currentSet.toString()
            txtCycle.text = savedInstanceState.getString(Constants.CYCLE_TXT)
            txtRound.text = "${resources.getString(R.string.round)} $currentRound/$rounds"
            if (timerStarted) {
                txtHint1.visibility = View.GONE
                txtHint2.visibility = View.GONE
                txtRound.visibility = View.VISIBLE
                var timeDifference = System.currentTimeMillis() - systemTimeOnSaveState
                if (!timerPaused) {
                    if (timeLeftInMs - 1000 - timeDifference > 0) {
                        restartTimer(timeLeftInMs - 1000 - timeDifference)
                    } else {
                        if (currentPart != workout.lastIndex) {
                            nextPart()
                            updateTimer(((workout[currentPart].durartion) * 1000).toLong())
                            restartTimer(((workout[currentPart].durartion) * 1000).toLong())
                            if (isInBackGround) {
                                pauseTimer()
                                timeLeftInMs = (workout[currentPart].durartion) * 1000.toLong()
                            }
                        } else {
                            tabata.state[0] = 0
                            finish()
                        }
                    }
                } else {
                    updateTimer(timeLeftInMs)
                }
            } else {
                updateTimer((workout[currentPart].durartion * 1000).toLong())
            }
            locked = savedInstanceState.getBoolean(Constants.LOCKED)
            if (locked) {
                imgLocked.setImageResource(R.drawable.ic_locked)
            }
        } else {
            if (resumed) {
                currentPart = tabata.state[0]
                currentSet = tabata.state[1]
                currentRound = tabata.state[2]
                txtCycle.text = tabata.state[3].toString()
//                Toast.makeText(this, "$currentPart", Toast.LENGTH_SHORT).show()
            }
            updateTimer((workout[currentPart].durartion * 1000).toLong())
        }

        for (i in 0..2) {
            if (currentPart + i <= workout.lastIndex) {
                partTxts[i].text = workout[currentPart + i].name
                partTypeImgs[i].setImageResource(workout[currentPart + i].imgID)
                partTxts[i].setTextColor(
                    ContextCompat.getColor(
                        this, when (increments[currentPart + i]) {
                            1 -> R.color.purple_200
                            2 -> R.color.purple_500
                            3 -> R.color.purple_700
                            else -> R.color.white
                        }
                    )
                )
            } else {
                partTxts[i].text = resources.getString(R.string.finish)
                partTypeImgs[i].setImageResource(R.drawable.ic_done)
            }

        }

        parentSession.setOnTouchListener(object : OnSwipeTouchListener() {
            override fun onSwipeRight() {
                if (!locked) {
                    if (currentPart != 0) {
                        previousPart()
                        changeTimerOnSwipe()
                    }
                    super.onSwipeRight()
                }
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onSwipeLeft() {
                if (!locked) {
                    if (currentPart != workout.lastIndex) {
                        nextPart()
                        changeTimerOnSwipe()
                    }
                    super.onSwipeLeft()
                }
            }
        })

        txtTime.setOnClickListener {
            if (!locked) {
                if (!timerStarted) {
                    txtHint1.visibility = View.GONE
                    txtHint2.visibility = View.GONE
                    txtRound.visibility = View.VISIBLE
                    tabata.state[0] = 1
                    updateTimer((workout[currentPart].durartion * 1000).toLong())
                    restartTimer((workout[currentPart].durartion * 1000).toLong())
                    timerPaused = false
                    timerStarted = true
                } else if (timerPaused) {
                    resumeTimer(timeLeftInMs)
                } else {
                    pauseTimer()
                }
            }
        }

        imgLocked.setOnTouchListener(object : View.OnTouchListener {
            val gestureDetector = GestureDetector(object :
                GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent?): Boolean {
                    if (locked) {
                        locked = false
                        imgLocked.setImageResource(R.drawable.ic_unlocked)
                    } else {
                        locked = true
                        imgLocked.setImageResource(R.drawable.ic_locked)
                    }
                    return super.onDoubleTap(e)
                }
            })

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                gestureDetector.onTouchEvent(event)
                return true
            }
        })
    }


    override fun onDestroy() {
//        if (tabata.state[0] != 0 && currentPart != 0) {
//            tabata.state[0] = currentPart
//            tabata.state[1] = currentSet
//            tabata.state[2] = currentRound
//            tabata.state[3] = txtCycle.text.toString().toInt()
//            tabata.state[4] = rounds
//            tabata.state[5] = prep
//            Toast.makeText(this, "${tabata.state[0]}", Toast.LENGTH_SHORT).show()
//        }
//
//        Utils.getInstance(this).updateTabata(tabata)
//        Toast.makeText(this, "${Utils.getInstance(this).getAllTabatas()!![0].state[0]}", Toast.LENGTH_SHORT).show()
        soundPool.release()
        super.onDestroy()
    }

    private fun previousPart() {
        currentPart--
        when {
            increments[currentPart] == 1 -> {
                txtCycle.text = ((txtCycle.text.toString().toInt() - 1).toString())
            }
            increments[currentPart] == 2 -> {
                currentSet--
                txtSet.text = currentSet.toString()
                txtCycle.text = "${cycles[currentSet - 1]}"
                txtCycleTotal.text = "/${cycles[currentSet - 1]}"
            }
            increments[currentPart] == 3 -> {
                currentRound--
                currentSet = cycles.size
                txtRound.text = "${resources.getString(R.string.round)} $currentRound/$rounds"
                txtCycle.text = "${cycles[currentSet - 1]}"
                txtCycleTotal.text = "/${cycles[currentSet - 1]}"
                txtSet.text = currentSet.toString()
            }
        }
        for (i in 0..2) {
            if (currentPart + i <= workout.lastIndex) {
                partTxts[i].text = workout[currentPart + i].name
                partTypeImgs[i].setImageResource(workout[currentPart + i].imgID)
            }
        }
        handleColoring()
        val textViewAnimation =
            ObjectAnimator.ofFloat(txtTime, "X", 0f - txtTime.width, txtTime.x)
        textViewAnimation.duration = 200
        textViewAnimation.start()
    }

    private fun changeTimerOnSwipe() {
        if (timerStarted && !timerPaused) {
            countDownTimer?.cancel()
            restartTimer((workout[currentPart].durartion * 1000).toLong())
        } else {
            updateTimer((workout[currentPart].durartion * 1000).toLong())
            timerStarted = false
        }
    }

    private fun nextPart() {
        when {
            increments[currentPart] == 1 -> {
                txtCycle.text = ((txtCycle.text.toString().toInt() + 1).toString())
            }
            increments[currentPart] == 2 -> {
                currentSet += 1
                txtSet.text = currentSet.toString()
                txtCycle.text = 1.toString()
                txtCycleTotal.text = "/${cycles[currentSet - 1]}"
            }
            increments[currentPart] == 3 -> {
                currentRound++
                currentSet = 1
                txtRound.text = "${resources.getString(R.string.round)} $currentRound/$rounds"
                txtCycle.text = 1.toString()
                txtSet.text = currentSet.toString()
                txtCycleTotal.text = "/${cycles[currentSet - 1]}"
            }
        }
        currentPart++
        handleColoring()
        val textViewAnimation = ObjectAnimator.ofFloat(txtTime, "X", windowManager.defaultDisplay.width.toFloat(), txtTime.x)
        textViewAnimation.duration = 200
        textViewAnimation.start()
    }

    private fun handleColoring() {
        for (i in 0..2) {
            if (currentPart + i <= workout.lastIndex) {
                partTxts[i].text = workout[currentPart + i].name
                partTypeImgs[i].setImageResource(workout[currentPart + i].imgID)
                partTxts[i].setTextColor(
                    ContextCompat.getColor(
                        this, when (increments[currentPart + i]) {
                            1 -> R.color.purple_200
                            2 -> R.color.purple_500
                            3 -> R.color.purple_700
                            else -> R.color.white
                        }
                    )
                )
            } else {
                partTxts[i].text = resources.getString(R.string.finish)
                partTypeImgs[i].setImageResource(R.drawable.ic_done)
            }

        }
    }

    private fun cleanupAndGetDetails() {
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
                    cycles.add(parts[i].durartion)
                    if (i + 1 <= parts.lastIndex) starts.add(i + 1)
                }
            }
            i++
        }
        if (parts[parts.lastIndex].type != "set marker") {
            parts.add(Part(0, resources.getString(R.string.setMarker), "set marker", 1))
            ends.add(parts.lastIndex - 1)
            cycles.add(1)
        }
    }

    private fun workoutBuilder() {
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
                            workout.add(Part(0, resources.getString(R.string.setBreak), resources.getString(R.string.breakTxt), parts[k + 1].setBreak, R.drawable.ic_break))
                            increments.add(0)
                        }
                    }
                }
            }
        }
        if (prep != 0) {
            workout.add(0, Part(0, resources.getString(R.string.preparation), resources.getString(R.string.preparation), prep, R.drawable.ic_break))
            increments.add(0, 0)
        }
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        timerPaused = true
    }

    private fun resumeTimer(timeLeftInMs: Long) {
        restartTimer(timeLeftInMs - 1000)
        timerPaused = false
    }

    private fun restartTimer(time: Long) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        countDownTimer?.cancel()
        countDownTimer = null

        countDownTimer = object : CountDownTimer(time + 1000, 1000) {

            @RequiresApi(api = Build.VERSION_CODES.N)
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMs = millisUntilFinished
                updateTimer(timeLeftInMs)
                playSound((timeLeftInMs / 1000).toInt())
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            override fun onFinish() {
                if (currentPart != workout.lastIndex) {
                    nextPart()
                    updateTimer((workout[currentPart].durartion * 1000).toLong())
                    restartTimer((workout[currentPart].durartion * 1000).toLong())
                    if (isInBackGround) {
                        pauseTimer()
                        timeLeftInMs = (workout[currentPart].durartion) * 1000.toLong()
                    }
                } else {
                    txtTime.text = resources.getString(R.string.end)
                    partTxts[0].text = resources.getString(R.string.finish)
                    partTypeImgs[0].setImageResource(R.drawable.ic_done)
                    Handler(Looper.getMainLooper()).postDelayed({
                        tabata.state[0] = 0
                        finish()
                    }, 1000)
                }
            }
        }.start()
    }

    private fun updateTimer(timeLeftInMs: Long) {
        val minutes = timeLeftInMs.toInt() / 60000
        val seconds = timeLeftInMs.toInt() % 60000 / 1000
        var timeLeftTxt: String = "" + minutes
        timeLeftTxt += ":"
        if (seconds < 10) {
            timeLeftTxt += "0"
        }
        timeLeftTxt += seconds
        val color = when (seconds) {
            0 -> R.color.purple_500
            1, 2, 3 -> R.color.purple_200
            else -> R.color.white
        }
        txtTime.setTextColor(ContextCompat.getColor(applicationContext, color))
        txtTime.text = timeLeftTxt
    }

    private fun viewInit() {
        partTxts.add(txtPart1)
        partTxts.add(txtPart2)
        partTxts.add(txtPart3)
        partTypeImgs.add(imgPart1Type)
        partTypeImgs.add(imgPart2Type)
        partTypeImgs.add(imgPart3Type)
        viewPreparation()
    }

    private fun viewPreparation() {

        partTxts[0].text = workout[currentPart].name
        partTxts[1].text =
            if (currentPart + 1 > parts.lastIndex) resources.getString(R.string.finish) else workout[currentPart + 1].name
        partTxts[2].text =
            if (currentPart + 2 > parts.lastIndex) resources.getString(R.string.finish) else workout[currentPart + 2].name
        partTypeImgs[0].setImageResource(workout[currentPart].imgID)
        partTypeImgs[1].setImageResource(if (currentPart + 1 > parts.lastIndex) R.drawable.ic_done else workout[currentPart + 1].imgID)
        partTypeImgs[2].setImageResource(if (currentPart + 2 > parts.lastIndex) R.drawable.ic_done else workout[currentPart + 2].imgID)

        for (i in 0..2) {
            if (i <= workout.lastIndex) {
                partTxts[i].setTextColor(
                    ContextCompat.getColor(
                        this, when (increments[currentPart + i]) {
                            1 -> R.color.purple_200
                            2 -> R.color.purple_500
                            3 -> R.color.purple_700
                            else -> R.color.white
                        }
                    )
                )
            }
        }

        txtCycleTotal.text = "/${cycles[currentSet - 1]}"
        txtSetTotal.text = "/${cycles.size}"
        txtRound.text = "${resources.getString(R.string.round)} 1/$rounds"
    }

    private fun playSound(time: Int) {
        when (time) {
            3, 2, 1 -> soundPool.play(soundTick, volume, volume, 0, 0, 1F)
            0 -> when (currentPart) {
                workout.lastIndex -> soundPool.play(
                    soundFinishWorkout,
                    volume,
                    volume,
                    0,
                    0,
                    1F
                )
                else -> when (workout[currentPart].increment) {
                    1 -> soundPool.play(soundFinishCycle, volume, volume, 0, 0, 1F)
                    2 -> soundPool.play(soundFinishSet, volume, volume, 0, 0, 1F)
                    3 -> soundPool.play(soundFinishRound, volume, volume, 0, 0, 1F)
                    else -> soundPool.play(soundFinishPart, volume, volume, 0, 0, 1F)
                }
            }

        }
    }

    private fun soundPoolInit() {
        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(audioAttributes)
                .build()
        } else {
            SoundPool(2, AudioManager.STREAM_MUSIC, 0)
        }

        soundPoolLoad()
    }

    private fun soundPoolLoad() {
        volume = 1F
        soundFinishCycle = soundPool.load(this, R.raw.arcade_finish_cycle, 1)
        soundFinishSet = soundPool.load(this, R.raw.arcade_finish_set, 1)
        soundFinishRound = soundPool.load(this, R.raw.arcade_finish_round, 1)
        soundFinishWorkout = soundPool.load(this, R.raw.arcade_finish_workout, 1)
        soundFinishPart = soundPool.load(this, R.raw.arcade_finish_part, 1)
        soundTick = soundPool.load(this, R.raw.arcade_tick, 1)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(Constants.CURRENT_PART, currentPart)
        outState.putBoolean(Constants.TIMER_PAUSED, timerPaused)
        outState.putBoolean(Constants.TIMER_STARTED, timerStarted)
        outState.putInt(Constants.CURRENT_SET, currentSet)
        outState.putInt(Constants.CURRENT_ROUND, currentRound)
        outState.putLong(Constants.TIME_LEFT, timeLeftInMs)
        outState.putIntegerArrayList(Constants.CYCLES, cycles)
        outState.putSerializable(Constants.PARTS, parts)
        outState.putString(Constants.CYCLE_TXT, txtCycle.text.toString())
        outState.putBoolean(Constants.LOCKED, locked)
        outState.putLong(Constants.SYSTEM_TIME, System.currentTimeMillis())
        outState.putBoolean(Constants.IN_BACKGROUND, isInBackGround)

        super.onSaveInstanceState(outState)
    }

//    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
//        currentPart = savedInstanceState.getInt(Constants.CURRENT_PART)
//        currentSet = savedInstanceState.getInt(Constants.CURRENT_SET)
//        currentRound = savedInstanceState.getInt(Constants.CURRENT_ROUND)
//        timerPaused = savedInstanceState.getBoolean(Constants.TIMER_RUNNING)
//        timerStarted = savedInstanceState.getBoolean(Constants.TIMER_STARTED)
//        timeLeftInMs = savedInstanceState.getLong(Constants.TIME_LEFT)
//        cycles = savedInstanceState.getIntegerArrayList(Constants.CYCLES)!!
//        rotated = savedInstanceState.getBoolean(Constants.ROTATED, false)
//        parts = savedInstanceState.getSerializable(Constants.PARTS) as ArrayList<Part>
//        txtSet.text = currentSet.toString()
//        txtCycle.text = savedInstanceState.getString(Constants.CYCLE_TXT)
//        if (timerStarted) {
//            txtHint1.visibility = View.GONE
//            txtHint2.visibility = View.GONE
//            txtRound.visibility = View.VISIBLE
//            if (!timerPaused) startTimer(timeLeftInMs)
//        }
//        if (timerPaused) {
//            updateTimer(timeLeftInMs)
//        }
//        locked = savedInstanceState.getBoolean(Constants.LOCKED)
//        if (locked) {
//            imgLocked.setImageResource(R.drawable.ic_locked)
//        }
//        super.onRestoreInstanceState(savedInstanceState)
//    }

}