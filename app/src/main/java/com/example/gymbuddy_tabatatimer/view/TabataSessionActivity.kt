package com.example.gymbuddy_tabatatimer.view

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.gymbuddy_tabatatimer.R
import com.example.gymbuddy_tabatatimer.databinding.ActivityTabataSessionBinding
import com.example.gymbuddy_tabatatimer.helpers.Constants
import com.example.gymbuddy_tabatatimer.helpers.Helpers
import com.example.gymbuddy_tabatatimer.helpers.OnSwipeTouchListener
import com.example.gymbuddy_tabatatimer.helpers.Utils
import com.example.gymbuddy_tabatatimer.model.Tabata
import com.example.gymbuddy_tabatatimer.viewModel.TabataSessionViewModel
import com.google.android.gms.ads.AdLoader
import kotlinx.android.synthetic.main.activity_tabata_session.*

@SuppressLint("SetTextI18n")
class TabataSessionActivity : AppCompatActivity() {
    private lateinit var soundPool: SoundPool
    private var soundFinishCycle = 0
    private var soundFinishSet = 0
    private var soundFinishRound = 0
    private var soundFinishPart = 0
    private var soundTick = 0
    private var soundFinishWorkout = 0
    private lateinit var tabata: Tabata
    private var partTypeImgs = ArrayList<ImageView>()
    private var partTxts = ArrayList<TextView>()
    private var doubleBackToExitPressedOnce = false
    private var volume = 0f
    private var resumed = false
    private var adLoader: AdLoader? = null
    private lateinit var viewModel: TabataSessionViewModel
    private lateinit var binding: ActivityTabataSessionBinding
    private var positionX = 0f

    override fun onResume() {
        viewModel.isInBackground = false
        super.onResume()
    }

    override fun onPause() {
        if (tabata.state[0] != 0 && viewModel.currentPart.value != 0) {
            tabata.state[4] = viewModel.rounds
            tabata.state[5] = viewModel.prep
        }
        Utils.getInstance(this).updateTabata(tabata)
        viewModel.isInBackground = true
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_GymBuddyTabataTimer)
        super.onCreate(savedInstanceState)
        binding = ActivityTabataSessionBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        positionX = binding.txtTime.x

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window?.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        viewModel = ViewModelProvider(this).get(TabataSessionViewModel::class.java)

        adLoader = Helpers.handleNativeAds(tabataSessionAdTemplate, this, Constants.AD_ID_TABATA_SESSION_NATIVE, null)

        supportActionBar?.hide()

        val intent = intent
        tabata = intent.getSerializableExtra(Constants.TABATA_KEY) as Tabata
        viewModel.apply {
            prep = intent.getIntExtra(Constants.PREP_KEY, 0)
            rounds = intent.getIntExtra(Constants.ROUNDS_KEY, 0)
        }
        resumed = intent.getBooleanExtra(Constants.RESUMED_KEY, false)


        if (viewModel.workout.isEmpty()) {
            viewModel.workoutInit(tabata.parts)
        }

        soundPoolInit()

        viewInit()

        if (savedInstanceState == null) {
            if (resumed) {
                viewModel.apply {
                    setCurrentPart(tabata.state[0])
                    setCurrentSet(tabata.state[1])
                    setCurrentRound(tabata.state[2])
                    setCurrentCycle(tabata.state[3])
                    setTimeLeftInMs((viewModel.workout[viewModel.currentPart.value!!].duration * 1000).toLong())
                }
            }
        }

        handleColoring()

        parentSession.setOnTouchListener(object : OnSwipeTouchListener() {
            override fun onSwipeRight() {
                viewModel.apply {
                    if (locked.value == false) {
                        if (currentPart.value != 0) {
                            previousPart()
                            changeTimerOnSwipe()
                        }
                        super.onSwipeRight()
                    }
                }
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onSwipeLeft() {

                viewModel.apply {
                    if (locked.value == false) {
                        if (currentPart.value != workout.lastIndex) {
                            nextPart()
                            changeTimerOnSwipe()
                        }
                        super.onSwipeLeft()
                    }
                }
            }
        })

        binding.txtTime.setOnClickListener {
            if (viewModel.locked.value == false) {
                when {
                    viewModel.timerStarted.value == false -> {
                        binding.txtHint1.visibility = View.GONE
                        binding.txtHint2.visibility = View.GONE
                        binding.txtRound.visibility = View.VISIBLE
                        tabata.state[0] = 1
                        viewModel.restartTimer((viewModel.workout[viewModel.currentPart.value!!].duration * 1000).toLong())
                        viewModel.setTimerPaused(false)
                        viewModel.setTimerStarted(true)
                    }
                    viewModel.timerPaused.value == true -> {
                        viewModel.resumeTimer(viewModel.timeLeftInMs.value)
                    }
                    else -> {
                        viewModel.pauseTimer()
                    }
                }
            }
        }

        viewModel.timeLeftInMs.observe(this, {
            updateTimer(it)
            playSound((it / 1000).toInt())
        })

        viewModel.workoutFinished.observe(this, {
            if (it) {
                txtTime.text = resources.getString(R.string.end)
                partTxts[0].text = resources.getString(R.string.finish)
                partTypeImgs[0].setImageResource(R.drawable.ic_done)
                Handler(Looper.getMainLooper()).postDelayed({
                    tabata.state[0] = 0
                    finish()
                }, 1000)
            }
        })

        viewModel.currentPart.observe(this, {
            tabata.state[0] = it
            handleColoring()
            val textViewAnimation = when (viewModel.next) {
                1 -> {
                    ObjectAnimator.ofFloat(binding.txtTime, "X", windowManager.defaultDisplay.width.toFloat(), positionX)
                }
                else -> {
                    for (i in 0..2) {
                        if (viewModel.currentPart.value!! + i <= viewModel.workout.lastIndex) {
                            partTxts[i].text = viewModel.workout[viewModel.currentPart.value!! + i].name
                            partTypeImgs[i].setImageResource(viewModel.workout[viewModel.currentPart.value!! + i].imgID)
                        }
                    }
                    ObjectAnimator.ofFloat(binding.txtTime, "X", 0f - binding.txtTime.width, positionX)
                }
            }
            if (viewModel.next != 0) {
                textViewAnimation.duration = 200
                textViewAnimation.start()
            }
        })

        viewModel.timerStarted.observe(this, {
            if (it) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                binding.txtHint1.visibility = View.GONE
                binding.txtHint2.visibility = View.GONE
                binding.txtRound.visibility = View.VISIBLE
            }
        })

        viewModel.timerPaused.observe(this, {
            if (it) window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        })

        viewModel.locked.observe(this, {
            if (it) {
                binding.imgLocked.setImageResource(R.drawable.ic_locked)
            } else {
                binding.imgLocked.setImageResource(R.drawable.ic_unlocked)
            }
        })

        viewModel.muted.observe(this, {
            volume = if (!it) {
                binding.imgMuted.setImageResource(R.drawable.ic_unmuted)
                1f
            } else {
                binding.imgMuted.setImageResource(R.drawable.ic_muted)
                0f
            }
        })

        viewModel.currentRound.observe(this, {
            binding.txtRound.text = "${resources.getString(R.string.round)} ${it}/${viewModel.rounds}"
            tabata.state[2] = it
        })

        viewModel.currentSet.observe(this, {

            binding.txtSet.text = viewModel.currentSet.value.toString()
            binding.txtCycleTotal.text = "/${viewModel.cycles[it - 1]}"
            tabata.state[1] = it
        })

        viewModel.currentCycle.observe(this, {
            binding.txtCycle.text = it.toString()
            tabata.state[3] = it
        })

        binding.imgMuted.setOnClickListener {
            if (viewModel.muted.value == true) {
                viewModel.setMuted(false)
            } else {
                viewModel.setMuted(true)
            }
        }

        binding.imgLocked.setOnTouchListener(object : View.OnTouchListener {
            val gestureDetector = GestureDetector(object :
                GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent?): Boolean {
                    if (viewModel.locked.value == true) {
                        viewModel.setLocked(false)
                    } else {
                        viewModel.setLocked(true)
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
        soundPool.release()
        super.onDestroy()
    }

    private fun handleColoring() {
        for (i in 0..2) {
            if (viewModel.currentPart.value?.plus(i)!! <= viewModel.workout.lastIndex) {
                partTxts[i].text = viewModel.workout[viewModel.currentPart.value!! + i].name
                partTypeImgs[i].setImageResource(viewModel.workout[viewModel.currentPart.value!! + i].imgID)
                partTxts[i].setTextColor(
                    ContextCompat.getColor(
                        this, when (viewModel.increments[viewModel.currentPart.value!! + i]) {
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
                partTxts[i].setTextColor(ContextCompat.getColor(this, R.color.purple_700))
            }
        }
    }

    private fun updateTimer(timeLeftInMs: Long?) {
        val minutes = timeLeftInMs?.toInt()?.div(60000)
        val seconds = timeLeftInMs?.toInt()?.rem(60000)?.div(1000)
        var timeLeftTxt: String = "" + minutes
        timeLeftTxt += ":"
        if (seconds != null && seconds < 10) {
            timeLeftTxt += "0"
        }
        timeLeftTxt += seconds
        val color = when (seconds) {
            0 -> R.color.purple_500
            1, 2, 3 -> R.color.purple_200
            else -> R.color.white
        }
        binding.txtTime.setTextColor(ContextCompat.getColor(applicationContext, color))
        binding.txtTime.text = timeLeftTxt
    }

    private fun viewInit() {
        partTxts.apply {
            add(binding.txtPart1)
            add(binding.txtPart2)
            add(binding.txtPart3)
        }
        partTypeImgs.apply {
            add(binding.imgPart1Type)
            add(binding.imgPart2Type)
            add(binding.imgPart3Type)
        }
        binding.txtCycleTotal.text = "/${viewModel.cycles[viewModel.currentSet.value!! - 1]}"
        binding.txtSetTotal.text = "/${viewModel.cycles.size}"
        binding.txtCycle.text = "${viewModel.currentCycle}"
    }

    private fun playSound(time: Int?) {
        when (time) {
            3, 2, 1 -> soundPool.play(soundTick, volume, volume, 0, 0, 1F)
            0 -> {
                if (!viewModel.finalSoundPlayed) {
                    when (viewModel.currentPart.value) {
                        viewModel.workout.lastIndex -> soundPool.play(soundFinishWorkout, volume, volume, 0, 0, 1F)
                        else -> when (viewModel.workout[viewModel.currentPart.value!!].increment) {
                            1 -> soundPool.play(soundFinishCycle, volume, volume, 0, 0, 1F)
                            2 -> soundPool.play(soundFinishSet, volume, volume, 0, 0, 1F)
                            3 -> soundPool.play(soundFinishRound, volume, volume, 0, 0, 1F)
                            else -> soundPool.play(soundFinishPart, volume, volume, 0, 0, 1F)
                        }
                    }
                    viewModel.finalSoundPlayed = true
                }
            }
        }
    }

    private fun soundPoolInit() {
        val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        soundPool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(audioAttributes).build()
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
//        outState.putInt(Constants.CURRENT_PART, currentPart)
//        outState.putBoolean(Constants.TIMER_PAUSED, timerPaused)
//        outState.putBoolean(Constants.TIMER_STARTED, timerStarted)
//        outState.putInt(Constants.CURRENT_SET, currentSet)
//        outState.putInt(Constants.CURRENT_ROUND, currentRound)
//        outState.putLong(Constants.TIME_LEFT, timeLeftInMs)
//        outState.putIntegerArrayList(Constants.CYCLES, cycles)
//        outState.putSerializable(Constants.PARTS, parts)
//        outState.putString(Constants.CYCLE_TXT, txtCycle.text.toString())
//        outState.putBoolean(Constants.LOCKED, locked)
        outState.putLong(Constants.SYSTEM_TIME, System.currentTimeMillis())
//        outState.putBoolean(Constants.IN_BACKGROUND, isInBackGround)
        super.onSaveInstanceState(outState)
    }


}