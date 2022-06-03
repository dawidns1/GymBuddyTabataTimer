package com.example.gymbuddy_tabatatimer.recyclerViewAdapters

import android.annotation.SuppressLint
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.gymbuddy_tabatatimer.helpers.Helpers
import com.example.gymbuddy_tabatatimer.model.Part
import com.example.gymbuddy_tabatatimer.R
import kotlinx.android.synthetic.main.item_list_part.view.*

class PartsRVAdapter(
    private val parts: ArrayList<Part>,
    private val doubleTapListener: OnItemDoubleTapListener
) :
    RecyclerView.Adapter<PartsRVAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.item_list_part,
            parent,
            false
        )

        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ci = parts[position]
        holder.txtPartName.text = if(ci.type !="set marker") ci.name else holder.parent.resources.getString(R.string.setMarker)

        holder.txtPartDuration.text = if (ci.type != "set marker") {
            Helpers.secsToString(ci.duration)
        } else {
            if(ci.setBreak==0){
            ci.duration.toString()
            }else{
                "${Helpers.secsToString(ci.setBreak)} / ${ci.duration}"
            }
        }
        holder.imgPart.setImageResource(ci.imgID)
        holder.txtPartID.text = ci.id.toString()

    }

    override fun getItemCount(): Int {
        return parts.size
    }
    @SuppressLint("ClickableViewAccessibility")
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var txtPartName: TextView = itemView.txtPartName
        var txtPartDuration:TextView = itemView.txtPartDuration
        var imgPart:ImageView = itemView.imgPart
        var txtPartID:TextView = itemView.txtPartID
        var parent:CardView=itemView.parentPartRV

        init {
//            btnMenuPart.setOnClickListener {
//                val popupMenu = PopupMenu(it.context, it)
//                popupMenu.inflate(R.menu.popup_menu_tabata)
//                popupMenu.setOnMenuItemClickListener { item ->
//                    when (item.itemId) {
//                        R.id.menuEdit -> {
//                            Toast.makeText(it.context, "To be done", Toast.LENGTH_SHORT).show()
//                            true
//                        }
//                        R.id.menuDelete -> {
//                            Toast.makeText(it.context, "To be done", Toast.LENGTH_SHORT).show()
//                            true
//                        }
//                        else -> false
//                    }
//                }
//                popupMenu.show()
//            }

            parent.setOnTouchListener(object : View.OnTouchListener {
                val gestureDetector = GestureDetector(object :
                    GestureDetector.SimpleOnGestureListener() {

                    override fun onDoubleTap(e: MotionEvent?): Boolean {
                        if (adapterPosition != RecyclerView.NO_POSITION)
                            doubleTapListener.onItemDoubleTap(adapterPosition)
                        return super.onDoubleTap(e)
                    }

                    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                        return super.onSingleTapConfirmed(e)
                    }
                })

                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    gestureDetector.onTouchEvent(event)
                    return true
                }
            })
        }


    }

    interface OnItemDoubleTapListener {
        fun onItemDoubleTap(position: Int)
    }

}