package com.example.gymbuddy_tabatatimer.recyclerViewAdapters

import android.annotation.SuppressLint
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import com.example.gymbuddy_tabatatimer.helpers.Helpers
import com.example.gymbuddy_tabatatimer.R
import com.example.gymbuddy_tabatatimer.model.Tabata
import kotlinx.android.synthetic.main.item_list_tabata.view.*

class TabatasRVAdapter(
    val tabatas: ArrayList<Tabata>,
    private val listener: (position: Int) -> Unit,
    private val doubleTapListener: (position: Int) -> Unit
) : RecyclerView.Adapter<TabatasRVAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_list_tabata, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ci = tabatas[position]
        holder.txtTabataName.text = ci.name
        holder.txtTabataDuration.text = Helpers.secsToString(ci.durationTotal)
        holder.txtTabataID.text = ci.id.toString()

    }

    override fun getItemCount(): Int {
        return tabatas.size
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var txtTabataName = itemView.txtTabataName
        var txtTabataDuration = itemView.txtTabataDuration
        var parent = itemView.parentRV
        var txtTabataID = itemView.txtTabataID

        init {
            parent.setOnClickListener(this)

            parent.setOnTouchListener(object : View.OnTouchListener {
                val gestureDetector = GestureDetector(object :
                    GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent?): Boolean {
                        if (adapterPosition != RecyclerView.NO_POSITION)
                            doubleTapListener.invoke(adapterPosition)
                        return super.onDoubleTap(e)
                    }

                    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                        if (adapterPosition != RecyclerView.NO_POSITION)
                            parent.isPressed = true
                        listener.invoke(adapterPosition)
                        return super.onSingleTapConfirmed(e)
                    }
                })

                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    gestureDetector.onTouchEvent(event)
                    return true
                }
            })

        }

        override fun onClick(v: View?) {
            if (adapterPosition != RecyclerView.NO_POSITION)
                listener.invoke(adapterPosition)
        }

    }
//
//    interface OnItemDoubleTapListener {
//        fun onItemDoubleTap(position: Int)
//    }
//
//    interface OnItemClickListener {
//        fun onItemClick(position: Int)
//    }
}