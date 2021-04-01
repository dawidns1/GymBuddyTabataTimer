package com.example.gymbuddy_tabatatimer

import android.app.AlertDialog
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private var tabatas = ArrayList<Tabata>()
    private lateinit var adapter: TabatasRVAdapter
    private var doubleBackToExitPressedOnce = false

    override fun onResume() {
        if (Helpers.modifiedTabata != null) {
            adapter.apply {
                tabatas.clear()
                tabatas.addAll(Utils.getInstance(this@MainActivity).getAllTabatas()!!)
                notifyDataSetChanged()
            }
//            tabatasRV.layoutManager = LinearLayoutManager(this)
//            adapter = TabatasRVAdapter(tabatas, this, this)
//            tabatasRV.adapter = adapter
            true
        }

        super.onResume()
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, resources.getString(R.string.pressBackToExit), Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_GymBuddyTabataTimer)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Helpers.setupActionBar(resources.getString(R.string.tabataTimer), "", supportActionBar!!, this)

        tabatas = initSampleTabatas()
        tabatasRV.layoutManager = LinearLayoutManager(this)
        adapter = TabatasRVAdapter(tabatas = tabatas, listener = { onItemClick(it) }, doubleTapListener = { onItemDoubleTap(it) })
        tabatasRV.adapter = adapter
//        tabatasRV.setHasFixedSize(false)
        val itemTouchHelper = ItemTouchHelper(simpleCallback)
        itemTouchHelper.attachToRecyclerView(tabatasRV)

        btnAddTabata.setOnClickListener {
            handleAddTabata()
        }
//
//        for(t in tabatas){
//            Toast.makeText(this, t.durationTotal.toString(), Toast.LENGTH_SHORT).show()
//        }
    }

    private fun handleAddTabata(name: String = "", edited: Boolean = false, position: Int = 0) {
        val tabataName = EditText(this)
        tabataName.setText(name)
        tabataName.setSelection(tabataName.text.length)
        tabataName.setTextColor(Color.WHITE)
        tabataName.requestFocus()
        AlertDialog.Builder(this@MainActivity, R.style.DefaultAlertDialogTheme)
                .setTitle(resources.getString(R.string.addNewTabata))
                .setView(tabataName)
                .setIcon(R.drawable.ic_add_tabata)
                .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                    if (tabataName.text.isEmpty()) {
                        Toast.makeText(this, resources.getString(R.string.insertName), Toast.LENGTH_SHORT).show()
                        handleAddTabata(name, edited, position)
                    } else {
                        if (!edited) {
                            val newTabata = Tabata(
                                    Utils.getInstance(this).getTabatasID()!!,
                                    tabataName.text.toString()
                            )
                            Utils.getInstance(this).addTabata(newTabata)
                            tabatas.add(newTabata)
                            adapter.notifyItemInserted(tabatas.lastIndex)
                        } else {
                            tabatas[position].name = tabataName.text.toString()
                            adapter.notifyItemChanged(position)
                            Utils.getInstance(this).updateTabatas(tabatas)
                        }
                        Helpers.hideKeyboard(applicationContext, tabataName)
                    }
                }
                .setNegativeButton(resources.getString(R.string.cancel), null)
                .show()
        tabataName.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    private fun onItemClick(position: Int) {
//        tabatas = Utils.getInstance(this).getAllTabatas()!!
//        adapter.notifyDataSetChanged()
        val intent = Intent(this, PartsActivity::class.java)
        intent.putExtra(Constants.TABATA_KEY, tabatas[position])
        startActivity(intent)
    }

    private fun onItemDoubleTap(position: Int) {
        handleAddTabata(tabatas[position].name, true, position)
    }

    private var simpleCallback: ItemTouchHelper.SimpleCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                    ItemTouchHelper.START or ItemTouchHelper.END, ItemTouchHelper.RIGHT
    ) {

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            if (viewHolder != null) {
                viewHolder.itemView.isPressed = true
            }
            super.onSelectedChanged(viewHolder, actionState)
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
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
            tabatas = Utils.getInstance(this@MainActivity).getAllTabatas()!!
            Collections.swap(tabatas, fromPosition, toPosition)
            adapter.notifyItemMoved(fromPosition, toPosition)
            Utils.getInstance(this@MainActivity).updateTabatas(tabatas)
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            Utils.getInstance(this@MainActivity).deleteTabata(tabatas[position])
            val deletedTabata = tabatas[position]
            tabatas.removeAt(position)
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, 1)
            Snackbar.make(tabatasRV, resources.getString(R.string.tabataDeleted), Snackbar.LENGTH_LONG)
                    .setAction(resources.getString(R.string.undo)) {
                        tabatas.add(position, deletedTabata)
                        adapter.notifyItemInserted(position)
                        Utils.getInstance(this@MainActivity).updateTabatas(tabatas)
                    }
                    .setActionTextColor(ContextCompat.getColor(applicationContext, R.color.purple_500))
                    .show()
        }

        override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                val itemView = viewHolder.itemView
                val itemHeight = itemView.bottom - itemView.top
                val isCanceled = dX == 0f && !isCurrentlyActive
                val deleteIcon =
                        ContextCompat.getDrawable(applicationContext, R.drawable.ic_delete_swipe)
                val intrinsicWidth = deleteIcon!!.intrinsicWidth
                val intrinsicHeight = deleteIcon.intrinsicHeight
                val background =
                        ContextCompat.getDrawable(applicationContext, R.drawable.ic_background)

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

    private fun initSampleTabatas(): ArrayList<Tabata> {
        val sampleTabatas = Utils.getInstance(this).getAllTabatas()
        return sampleTabatas!!
    }
}