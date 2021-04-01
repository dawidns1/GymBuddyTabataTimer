package com.example.gymbuddy_tabatatimer

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_parts.*
import kotlinx.android.synthetic.main.item_list_part.*
import kotlinx.android.synthetic.main.time_picker.view.*
import java.util.*
import kotlin.collections.ArrayList

class PartsActivity : AppCompatActivity(), PartsRVAdapter.OnItemDoubleTapListener {
    lateinit var tabata: Tabata
    lateinit var adapter: PartsRVAdapter
    private var parts = ArrayList<Part>()
    private var isEdited = false
    private var editedPosition = 0
    private var initialDuration = 0
    private var duration = 0

    override fun onBackPressed() {
        super.onBackPressed()
        if (initialDuration != tabata.durationTotal) Helpers.modifiedTabata = tabata
        finish()
    }

    override fun onResume() {
        val tabatas=Utils.getInstance(this).getAllTabatas()
//        Toast.makeText(this, "${tabatas!![0].state[0]}", Toast.LENGTH_SHORT).show()
        if (tabatas != null) {
            for(t in tabatas){
                if(t.id==tabata.id){
                    tabata.state=t.state
//                    Toast.makeText(this, "${tabata.state[0]}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        if(tabata.state[0]==0){
            btnResumeTabata.hide()
        }else{
            btnResumeTabata.show()
        }
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_GymBuddyTabataTimer)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parts)

        val intent = intent
        tabata = intent.getSerializableExtra(Constants.TABATA_KEY) as Tabata
        initialDuration = tabata.durationTotal
        Helpers.setupActionBar(tabata.name, "", supportActionBar!!, this)
        parts = tabata.parts

//        Toast.makeText(this, "${tabata.state[0]}", Toast.LENGTH_SHORT).show()

        if (parts.isEmpty()) btnStartTabata.visibility = View.GONE
        if(tabata.state[0]!=0) btnResumeTabata.visibility=View.VISIBLE

        txtPreparation.text = Helpers.secsToString(tabata.defPrep)
        txtRounds.text = tabata.defRounds.toString()

        calculateDuration()

        adapter = PartsRVAdapter(parts, this)
        partsRV.adapter = adapter
        partsRV.layoutManager = LinearLayoutManager(this)

        val itemTouchHelper = ItemTouchHelper(simpleCallback)
        itemTouchHelper.attachToRecyclerView(partsRV)

        btnAddPart.setOnClickListener {
            addTabataView.visibility = View.VISIBLE
            startingDetails.visibility = View.GONE
            btnAdd.text = resources.getString(R.string.add)
        }

        btnStartTabata.setOnClickListener {
            tabata.defPrep = Helpers.textViewToSecs(txtPreparation)
            tabata.defRounds = txtRounds.text.toString().toInt()
            Utils.getInstance(this).updateTabata(tabata)
            val intent0 = Intent(this, TabataSessionActivity::class.java)
            intent0.putExtra(Constants.TABATA_KEY, tabata)
            intent0.putExtra(Constants.PREP_KEY, tabata.defPrep)
            intent0.putExtra(Constants.ROUNDS_KEY, tabata.defRounds)
            intent0.putExtra(Constants.RESUMED_KEY,false)
            startActivity(intent0)
        }

        btnResumeTabata.setOnClickListener {
            val intent1 = Intent(this, TabataSessionActivity::class.java)
            intent1.putExtra(Constants.TABATA_KEY, tabata)
            intent1.putExtra(Constants.PREP_KEY, tabata.state[5])
            intent1.putExtra(Constants.ROUNDS_KEY, tabata.state[4])
            intent1.putExtra(Constants.RESUMED_KEY,true)
            startActivity(intent1)
        }

        setImgOnClickListeners()

        spinnerPartType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        edtPartName.setText(resources.getString(R.string.exercise))
                        edtPartName.visibility = View.VISIBLE
                        partNameTxt.text = resources.getString(R.string.name)
                        partDurationTxt.text = resources.getString(R.string.duration)
                        txtPartDurationA.visibility = View.VISIBLE
                        setupPartDuration.visibility = View.VISIBLE
                        txtPartCycles.visibility = View.GONE
                        plusPartCycles.visibility = View.GONE
                        minusPartCycles.visibility = View.GONE
                        txtBreakSet.visibility = View.GONE
                        setupBreakSet.visibility = View.GONE
                    }
                    1 -> {
                        edtPartName.setText(resources.getString(R.string.breakTxt))
                        edtPartName.visibility = View.VISIBLE
                        partNameTxt.text = resources.getString(R.string.name)
                        partDurationTxt.text = resources.getString(R.string.duration)
                        txtPartDurationA.visibility = View.VISIBLE
                        setupPartDuration.visibility = View.VISIBLE
                        txtPartCycles.visibility = View.GONE
                        plusPartCycles.visibility = View.GONE
                        minusPartCycles.visibility = View.GONE
                        txtBreakSet.visibility = View.GONE
                        setupBreakSet.visibility = View.GONE
                    }
                    2 -> {
                        txtPartDurationA.visibility = View.GONE
                        edtPartName.visibility = View.GONE
                        partNameTxt.text = resources.getString(R.string.cycles)
                        partDurationTxt.text = resources.getString(R.string.breakAfterSet)
                        setupPartDuration.visibility = View.GONE
                        txtPartCycles.visibility = View.VISIBLE
                        plusPartCycles.visibility = View.VISIBLE
                        minusPartCycles.visibility = View.VISIBLE
                        txtBreakSet.visibility = View.VISIBLE
                        setupBreakSet.visibility = View.VISIBLE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        btnAdd.setOnClickListener {
            addTabataView.visibility = View.GONE
            startingDetails.visibility = View.VISIBLE
            btnAddPart.show()
            btnStartTabata.show()
            val imgID = when (spinnerPartType.selectedItem.toString()) {
                resources.getString(R.string.exercise) -> R.drawable.ic_exercise
                resources.getString(R.string.breakTxt) -> R.drawable.ic_break
                resources.getString(R.string.setMarker) -> R.drawable.ic_set_marker
                else -> R.drawable.ic_hexagon_single_empty
            }
            val type=when (spinnerPartType.selectedItem.toString()) {
                resources.getString(R.string.exercise) -> "exercise"
                resources.getString(R.string.breakTxt) -> "break"
                resources.getString(R.string.setMarker) -> "set marker"
                else -> "exercise"
            }
            val duration = if (spinnerPartType.selectedItem.toString() != resources.getString(R.string.setMarker)) {
                Helpers.textViewToSecs(txtPartDurationA)
            } else {
                txtPartCycles.text.toString().toInt()
            }
            val partName = if (spinnerPartType.selectedItem.toString() != resources.getString(R.string.setMarker)) {
                if (edtPartName.text.isEmpty()) {
                    spinnerPartType.selectedItem.toString()
                } else {
                    edtPartName.text.toString()
                }
            } else {
                resources.getString(R.string.setMarker)
            }
            val setBreak = if (spinnerPartType.selectedItem.toString() == resources.getString(R.string.setMarker)) {
                Helpers.textViewToSecs(txtBreakSet)
            } else {
                0
            }
            if (!isEdited) {
                val newPart = Part(Utils.getInstance(this).getPartsID()!!, partName,type, duration, imgID,0, setBreak)
                parts.add(newPart)
                adapter.notifyItemInserted(parts.lastIndex)
                Utils.getInstance(this).addPartToTabata(tabata, newPart)
                if (!btnStartTabata.isShown) btnStartTabata.show()
            } else {
                parts[editedPosition].name = partName
                parts[editedPosition].type = spinnerPartType.selectedItem.toString()
                parts[editedPosition].durartion = duration
                parts[editedPosition].imgID = imgID
                parts[editedPosition].setBreak = setBreak
                adapter.notifyItemChanged(editedPosition)
                Utils.getInstance(this).updatePart(parts[editedPosition])
                isEdited = false
            }
            calculateDuration()
        }
    }

    override fun onItemDoubleTap(position: Int) {

        isEdited = true
        editedPosition = position
        btnAdd.text = resources.getString(R.string.save)
        addTabataView.visibility = View.VISIBLE
        startingDetails.visibility = View.GONE

        when (parts[position].type) {
            resources.getString(R.string.setMarker) -> {
                txtPartCycles.text = parts[position].durartion.toString()
                txtBreakSet.text = Helpers.secsToString(parts[position].setBreak)
            }
            else -> txtPartDurationA.text = Helpers.secsToString(parts[position].durartion)

        }
        spinnerPartType.setSelection(
            when (parts[position].type) {
                resources.getString(R.string.exercise) -> 0
                resources.getString(R.string.breakTxt) -> 1
                resources.getString(R.string.setMarker) -> 2
                else -> 0
            }
        )
        edtPartName.setText(parts[position].name)
    }

    private fun calculateDuration() {
        val durations = ArrayList<Int>()
        durations.add(0)
        var setBreak=0
        val cycles = ArrayList<Int>()

        for (i in parts.indices) {
            if (parts[i].type == resources.getString(R.string.setMarker)) {
                if (i + 1 <= parts.lastIndex && parts[i + 1].type == resources.getString(R.string.setMarker)) {
                    continue
                } else {
                    cycles.add(parts[i].durartion)
                    setBreak+=parts[i].setBreak
                    durations.add(0)
                }
            } else {
                durations[durations.lastIndex] += parts[i].durartion
            }
        }

        duration = 0

        if (cycles.isEmpty()) duration = durations[durations.lastIndex]
        else {
            for (i in cycles.indices) {
                duration += cycles[i] * durations[i]
            }
            if (durations.size > cycles.size) {
                duration += durations[durations.lastIndex]
            }
        }

        duration+=setBreak

        tabata.durationTotal = duration
        Utils.getInstance(this).updateTabata(tabata)
        txtRoundDuration.text = Helpers.secsToString(duration)
        txtTotalDuration.text = Helpers.secsToString(
            txtRounds.text.toString().toInt() * duration + Helpers.textViewToSecs(txtPreparation)
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

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                Collections.swap(parts, fromPosition, toPosition)
                calculateDuration()
                adapter.notifyItemMoved(fromPosition, toPosition)
                Utils.getInstance(this@PartsActivity)
                    .updateTabatasParts(tabata, parts)
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                Utils.getInstance(this@PartsActivity).deletePartFromTabata(parts[position])
                val deletedPart = parts[position]
                parts.removeAt(position)
                if (parts.isEmpty()) btnStartTabata.hide()
                calculateDuration()
                adapter.notifyItemRemoved(position)
                adapter.notifyItemRangeChanged(position, 1)
                Snackbar.make(partsRV, resources.getString(R.string.partDeleted), Snackbar.LENGTH_LONG)
                    .setAction(resources.getString(R.string.undo)) {
                        parts.add(position, deletedPart)
                        calculateDuration()
                        adapter.notifyItemInserted(position)
                        Utils.getInstance(this@PartsActivity).updateTabatasParts(tabata, parts)
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

        imgCloseInput.setOnClickListener {
            addTabataView.visibility = View.GONE
            startingDetails.visibility = View.VISIBLE
            btnAddPart.show()
            btnStartTabata.show()
            isEdited = false
        }

        setupPreparation.setOnClickListener {
            handleTimeSelection(txtPreparation)
        }

        setupBreakSet.setOnClickListener {
            handleTimeSelection(txtBreakSet)
        }

        setupPartDuration.setOnClickListener {
            handleTimeSelection(txtPartDurationA)
        }

        minusPartCycles.setOnClickListener {
            if (txtPartCycles.text.toString().toInt() != 1)
                txtPartCycles.text = (txtPartCycles.text.toString().toInt() - 1).toString()
        }
        plusPartCycles.setOnClickListener {
            txtPartCycles.text = (txtPartCycles.text.toString().toInt() + 1).toString()
        }

        minusRounds.setOnClickListener {
            if (txtRounds.text.toString().toInt() != 1) {
                txtRounds.text = (txtRounds.text.toString().toInt() - 1).toString()
                txtTotalDuration.text = Helpers.secsToString(
                    txtRounds.text.toString().toInt() * duration + Helpers.textViewToSecs(
                        txtPreparation
                    )
                )
            }
        }

        plusRounds.setOnClickListener {
            txtRounds.text = (txtRounds.text.toString().toInt() + 1).toString()
            txtTotalDuration.text = Helpers.secsToString(
                txtRounds.text.toString()
                    .toInt() * duration + Helpers.textViewToSecs(txtPreparation)
            )
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
            if (textView == txtPreparation) txtTotalDuration.text = Helpers.secsToString(txtRounds.text.toString().toInt() * duration + Helpers.textViewToSecs(txtPreparation))
        }
        d.setNegativeButton(resources.getString(R.string.cancel), null)
        d.show()
    }
}