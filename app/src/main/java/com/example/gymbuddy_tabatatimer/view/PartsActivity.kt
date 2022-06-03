package com.example.gymbuddy_tabatatimer.view

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gymbuddy_tabatatimer.*
import com.example.gymbuddy_tabatatimer.databinding.ActivityPartsBinding
import com.example.gymbuddy_tabatatimer.helpers.Constants
import com.example.gymbuddy_tabatatimer.helpers.Helpers
import com.example.gymbuddy_tabatatimer.helpers.Helpers.toggleVisibility
import com.example.gymbuddy_tabatatimer.helpers.Helpers.toggleVisibilityEFAB
import com.example.gymbuddy_tabatatimer.helpers.Utils
import com.example.gymbuddy_tabatatimer.model.Part
import com.example.gymbuddy_tabatatimer.model.Tabata
import com.example.gymbuddy_tabatatimer.recyclerViewAdapters.PartsRVAdapter
import com.example.gymbuddy_tabatatimer.viewModel.PartsViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_parts.*
import kotlinx.android.synthetic.main.item_list_part.*
import kotlinx.android.synthetic.main.time_picker.view.*
import java.util.*

class PartsActivity : AppCompatActivity(), PartsRVAdapter.OnItemDoubleTapListener {
    lateinit var tabata: Tabata
    lateinit var adapter: PartsRVAdapter
    private var isEdited = false
    private var editedPosition = 0
    private var initialDuration = 0
    private var canStart = false
    private lateinit var tempPart: Part
    private lateinit var viewModel: PartsViewModel
    private lateinit var binding: ActivityPartsBinding
    private lateinit var partTypeArrayAdapter: ArrayAdapter<String>

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.parts_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val dialogView = layoutInflater.inflate(R.layout.img_dialog, null)
        AlertDialog.Builder(this@PartsActivity, R.style.DefaultAlertDialogTheme)
            .setView(dialogView)
            .show()
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (initialDuration != tabata.durationTotal) Helpers.modifiedTabata = tabata
        finish()
    }

    override fun onResume() {
        val tabatas = Utils.getInstance(this).getAllTabatas()
        if (tabatas != null) {
            for (t in tabatas) {
                if (t.id == tabata.id) {
                    tabata.state = t.state
                }
            }
        }
        toggleStartingButtonsVisibility()
        super.onResume()
    }

    override fun onPause() {
        tabata.parts = viewModel.parts.value!!
        Utils.getInstance(this).updateTabata(tabata)
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_GymBuddyTabataTimer)
        super.onCreate(savedInstanceState)
        binding = ActivityPartsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        viewModel = ViewModelProvider(this).get(PartsViewModel::class.java)
        partTypeArrayAdapter = ArrayAdapter(this, R.layout.dropdown_item, resources.getStringArray(R.array.partType))

        val intent = intent
        tabata = intent.getSerializableExtra(Constants.TABATA_KEY) as Tabata
        initialDuration = tabata.durationTotal
        Helpers.setupActionBar(tabata.name, "", supportActionBar!!, this)

        viewModel.setParts(tabata.parts)
        viewModel.setPreparation(tabata.defPrep)
        viewModel.setRounds(tabata.defRounds)
        viewModel.parts.observe(this, {
            canStart = viewModel.calculateDuration()
            toggleStartingButtonsVisibility()
        })
        viewModel.rounds.observe(this, {
            binding.txtRounds.text = it.toString()
            tabata.defRounds = it
            binding.txtTotalDuration.text = Helpers.secsToString(it * viewModel.duration.value!! + viewModel.preparation.value!!)
        })
        viewModel.duration.observe(this, {
            tabata.durationTotal = it
            binding.txtRoundDuration.text = Helpers.secsToString(it)
            binding.txtTotalDuration.text = Helpers.secsToString(viewModel.rounds.value!! * it + viewModel.preparation.value!!)
        })
        viewModel.preparation.observe(this, {
            tabata.defPrep = it
            binding.txtPreparation.text = Helpers.secsToString(it)
            binding.txtTotalDuration.text = Helpers.secsToString(viewModel.rounds.value!! * viewModel.duration.value!! + it)
        })

        adapter = PartsRVAdapter(viewModel.parts.value!!, this)
        binding.partsRV.adapter = adapter
        binding.partsRV.layoutManager = LinearLayoutManager(this)

        binding.partsRV.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!binding.addTabataView.isVisible) {
                    binding.apply {
                        toggleStartingButtonsVisibility(dy <= 0)
                        btnAddPart.toggleVisibilityEFAB(dy <= 0)
                    }
                }
            }
        })

        Helpers.handleNativeAds(partsAdTemplate, this, Constants.AD_ID_PARTS_NATIVE, null)

        val itemTouchHelper = ItemTouchHelper(simpleCallback)
        itemTouchHelper.attachToRecyclerView(binding.partsRV)

        viewModel.inputOngoing.observe(this, { inputOngoing ->
            binding.apply {
                partsRV.alpha = when (inputOngoing) {
                    true -> 0.5F
                    false -> 1F
                }
                addTabataView.toggleVisibility(inputOngoing)
                toggleStartingButtonsVisibility(!inputOngoing)
                btnAddPart.toggleVisibilityEFAB(!inputOngoing)
            }
        })

        binding.btnAddPart.setOnClickListener {
            tempPart = Part(
                name = resources.getString(R.string.exercise),
                type = "exercise",
                duration = 5,
                setBreak = 20
            )
            binding.edtPartType.setAdapter(partTypeArrayAdapter)
            viewModel.setInputOngoing(true)
        }

        binding.btnStartTabata.setOnClickListener {
            val intent0 = Intent(this, TabataSessionActivity::class.java).apply {
                putExtra(Constants.TABATA_KEY, tabata)
                putExtra(Constants.PREP_KEY, tabata.defPrep)
                putExtra(Constants.ROUNDS_KEY, tabata.defRounds)
                putExtra(Constants.RESUMED_KEY, false)
            }

            startActivity(intent0)
        }

        binding.btnResumeTabata.setOnClickListener {
            val intent1 = Intent(this, TabataSessionActivity::class.java).apply {
                putExtra(Constants.TABATA_KEY, tabata)
                putExtra(Constants.PREP_KEY, tabata.state[5])
                putExtra(Constants.ROUNDS_KEY, tabata.state[4])
                putExtra(Constants.RESUMED_KEY, true)
            }
            startActivity(intent1)
        }

        setImgOnClickListeners()

        binding.edtPartType.setAdapter(partTypeArrayAdapter)

        binding.edtPartType.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val exerciseOrBreak = s.toString() == resources.getString(R.string.exercise) || s.toString() == resources.getString(R.string.breakTxt)
                binding.apply {
                    setupPartDuration.toggleVisibility(exerciseOrBreak)
                    plusPartCycles.toggleVisibility(!exerciseOrBreak)
                    minusPartCycles.toggleVisibility(!exerciseOrBreak)
                    setupBreakSet.toggleVisibility(!exerciseOrBreak)
                    edtPartName.isEnabled = exerciseOrBreak
                }
                if (exerciseOrBreak) {
                    binding.tilPartName.hint = resources.getString(R.string.name)
                    binding.tilPartDuration.hint = resources.getString(R.string.duration)
                    if (!isEdited) {
                        edtPartName.setText(s.toString())
                        edtPartName.requestFocus()
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                    }
                    binding.edtPartName.setSelection(binding.edtPartName.text.toString().length)
                } else {
                    binding.tilPartName.hint = resources.getString(R.string.cycles)
                    binding.tilPartDuration.hint = resources.getString(R.string.breakAfterSet)
                    binding.edtPartName.setText(if (!isEdited) "5" else viewModel.parts.value!![editedPosition].duration.toString())
                    Helpers.hideKeyboard(applicationContext, binding.edtPartName)
                }
            }
        })

        binding.imgFinishInput.setOnClickListener {
            viewModel.setInputOngoing(false)
            val imgID = when (binding.edtPartType.text.toString()) {
                resources.getString(R.string.exercise) -> R.drawable.ic_exercise
                resources.getString(R.string.breakTxt) -> R.drawable.ic_break
                resources.getString(R.string.setMarker) -> R.drawable.ic_set_marker
                else -> R.drawable.ic_hexagon_single_empty
            }
            val type = when (binding.edtPartType.text.toString()) {
                resources.getString(R.string.exercise) -> "exercise"
                resources.getString(R.string.breakTxt) -> "break"
                resources.getString(R.string.setMarker) -> "set marker"
                else -> "exercise"
            }
            val duration = if (binding.edtPartType.text.toString() != resources.getString(R.string.setMarker)) {
                Helpers.textViewToSecs(edtPartDuration)
            } else {
                binding.edtPartName.text.toString().toInt()
            }
            val partName = if (binding.edtPartType.text.toString() != resources.getString(R.string.setMarker)) {
                if (binding.edtPartName.text.toString().isEmpty()) {
                    binding.edtPartType.text.toString()
                } else {
                    binding.edtPartName.text.toString()
                }
            } else {
                resources.getString(R.string.setMarker)
            }
            val setBreak = if (binding.edtPartType.text.toString() == resources.getString(R.string.setMarker)) {
                Helpers.textViewToSecs(binding.edtPartDuration, true)
            } else {
                0
            }
            if (!isEdited) {
                val newPart = Part(Utils.getInstance(this).getPartsID(), partName, type, duration, imgID, 0, setBreak)
                viewModel.addNewPart(newPart)
                adapter.notifyItemInserted(viewModel.parts.value!!.lastIndex)
                if (!btnStartTabata.isShown) btnStartTabata.show()
            } else {
                viewModel.parts.value?.let {
                    it[editedPosition].name = partName
                    it[editedPosition].type = type
                    it[editedPosition].duration = duration
                    it[editedPosition].imgID = imgID
                    it[editedPosition].setBreak = setBreak
                }
                Helpers.modifiedTabata = tabata
                adapter.notifyItemChanged(editedPosition)
                canStart = viewModel.calculateDuration()
                isEdited = false
            }
        }
    }

    private fun toggleStartingButtonsVisibility(extraCondition: Boolean = true) {
        binding.btnStartTabata.toggleVisibilityEFAB(extraCondition && viewModel.parts.value!!.isNotEmpty() && canStart)
        binding.btnResumeTabata.toggleVisibilityEFAB(extraCondition && viewModel.parts.value!!.isNotEmpty() && canStart && tabata.state[0] != 0)
    }

    override fun onItemDoubleTap(position: Int) {
        isEdited = true
        editedPosition = position
        tempPart = viewModel.parts.value!![position]
        viewModel.setInputOngoing(true)
        when (viewModel.parts.value!![position].type) {
            "set marker" -> {
                binding.edtPartName.setText(viewModel.parts.value!![position].duration.toString())
                binding.edtPartDuration.setText(Helpers.secsToString(viewModel.parts.value!![position].setBreak))
            }
            else -> {
                binding.edtPartName.setText(viewModel.parts.value!![position].name)
                binding.edtPartDuration.setText(Helpers.secsToString(viewModel.parts.value!![position].duration))
            }

        }
        binding.edtPartType.setText(
            when (viewModel.parts.value!![position].type) {
                "exercise" -> resources.getText(R.string.exercise)
                "break" -> resources.getText(R.string.breakTxt)
                "set marker" -> resources.getText(R.string.setMarker)
                else -> resources.getText(R.string.exercise)
            }, false
        )
    }

    private var simpleCallback: ItemTouchHelper.SimpleCallback =
        object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                    ItemTouchHelper.START or ItemTouchHelper.END, ItemTouchHelper.RIGHT
        ) {
            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                if (viewHolder != null) {
                    viewHolder.itemView.isPressed = true
                }
                super.onSelectedChanged(viewHolder, actionState)
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                viewHolder.itemView.isPressed = false
                super.clearView(recyclerView, viewHolder)
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                if (btnResumeTabata.isShown) btnResumeTabata.hide()
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                viewModel.swapParts(fromPosition, toPosition)
//                Collections.swap(viewModel.parts.value!!, fromPosition, toPosition)
                adapter.notifyItemMoved(fromPosition, toPosition)
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val deletedPart = viewModel.parts.value!![position]
                viewModel.removePart(position)
                adapter.notifyItemRemoved(position)
                adapter.notifyItemRangeChanged(position, 1)
                Snackbar.make(partsRV, resources.getString(R.string.partDeleted), Snackbar.LENGTH_LONG)
                    .setAction(resources.getString(R.string.undo)) {
                        viewModel.addNewPart(deletedPart, position)
                        adapter.notifyItemInserted(position)
                    }
                    .setActionTextColor(ContextCompat.getColor(applicationContext, R.color.purple_500))
                    .show()
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val itemHeight = itemView.bottom - itemView.top
                    val isCanceled = dX == 0f && !isCurrentlyActive
                    val deleteIcon = ContextCompat.getDrawable(applicationContext, R.drawable.ic_delete_swipe)
                    val intrinsicWidth = deleteIcon!!.intrinsicWidth
                    val intrinsicHeight = deleteIcon.intrinsicHeight
                    val background = ContextCompat.getDrawable(applicationContext, R.drawable.ic_background)

                    if (isCanceled) {
                        clearCanvas(c, itemView.left + dX, itemView.top.toFloat(), itemView.right + dX, itemView.bottom.toFloat())
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        return
                    }

                    background!!.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt() + 30, itemView.bottom)
                    background.draw(c)

                    val deleteIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                    val deleteIconMargin = (itemHeight - intrinsicHeight) / 2
                    val deleteIconLeft = itemView.left + deleteIconMargin
                    val deleteIconRight = itemView.left + deleteIconMargin + intrinsicWidth
                    val deleteIconBottom = deleteIconTop + intrinsicHeight

                    deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
                    deleteIcon.draw(c)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

            fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
                val clearPaint = Paint().apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                }
                c?.drawRect(left, top, right, bottom, clearPaint)
            }
        }

    private fun setImgOnClickListeners() {

        binding.imgCloseInput.setOnClickListener {
            viewModel.setInputOngoing(false)
            isEdited = false
        }

        binding.setupPreparation.setOnClickListener {
            handleTimeSelection(binding.txtPreparation)
        }

        binding.setupBreakSet.setOnClickListener {
            handleTimeSelection(binding.edtPartDuration)
        }

        binding.setupPartDuration.setOnClickListener {
            handleTimeSelection(binding.edtPartDuration)
        }

        binding.minusPartCycles.setOnClickListener {
            if (binding.edtPartName.text.toString().toInt() != 1)
                binding.edtPartName.setText((binding.edtPartName.text.toString().toInt() - 1).toString())
        }
        binding.plusPartCycles.setOnClickListener {
            binding.edtPartName.setText((binding.edtPartName.text.toString().toInt() + 1).toString())
        }

        binding.minusRounds.setOnClickListener {
            if (viewModel.rounds.value != 1) {
//                binding.txtRounds.text = (binding.txtRounds.text.toString().toInt() - 1).toString()
                viewModel.setRounds(viewModel.rounds.value?.minus(1))
//                binding.txtTotalDuration.text = Helpers.secsToString(
//                    binding.txtRounds.text.toString().toInt() * viewModel.duration.value!! + Helpers.textViewToSecs(binding.txtPreparation)
//                )
            }
        }

        binding.plusRounds.setOnClickListener {
            viewModel.setRounds(viewModel.rounds.value?.plus(1))
        }
    }

    @SuppressLint("SetTextI18n")
    private fun handleTimeSelection(textView: TextView) {
        val d = AlertDialog.Builder(this, R.style.DefaultAlertDialogTheme)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.time_picker, null)
        d.setTitle(R.string.duration)
        d.setView(dialogView)
        d.setCancelable(false)
        d.setIcon(R.drawable.ic_timer)
        val numberPickerSecs = dialogView.numberPickerSecs
        numberPickerSecs.minValue = 0
        numberPickerSecs.maxValue = 59
        numberPickerSecs.value = Helpers.textViewToSecs(textView) % 60
        try {
            val field = NumberPicker::class.java.getDeclaredField("mInputText")
            field.isAccessible = true
            val inputText = field[numberPickerSecs] as EditText
            inputText.visibility = View.INVISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
        numberPickerSecs.wrapSelectorWheel = false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            numberPickerSecs.textSize = 80F
        }
        numberPickerSecs.setFormatter { i -> String.format("%02d", i) }
        val numberPickerMins = dialogView.numberPickerMins
        numberPickerMins.minValue = 0
        numberPickerMins.maxValue = 5
        numberPickerMins.value = Helpers.textViewToSecs(textView) / 60
        try {
            val field = NumberPicker::class.java.getDeclaredField("mInputText")
            field.isAccessible = true
            val inputText = field[numberPickerMins] as EditText
            inputText.visibility = View.INVISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
        numberPickerMins.wrapSelectorWheel = false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            numberPickerMins.textSize = 80F
        }
        d.setPositiveButton("Set") { _, _ ->
            textView.text = "${numberPickerMins.value}:${String.format("%02d", numberPickerSecs.value)}"
            if (textView == binding.txtPreparation) viewModel.setPreparation(Helpers.textViewToSecs(textView))
        }
        d.setNegativeButton(resources.getString(R.string.cancel), null)
        d.show()
    }
}