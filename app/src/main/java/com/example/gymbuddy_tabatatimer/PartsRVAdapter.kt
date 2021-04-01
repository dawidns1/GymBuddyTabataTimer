package com.example.gymbuddy_tabatatimer

import android.annotation.SuppressLint
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_list_part.view.*

class PartsRVAdapter(
    private val parts: ArrayList<Part>,
    private val doubleTapListener: PartsRVAdapter.OnItemDoubleTapListener
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
            Helpers.secsToString(ci.durartion)
        } else {
            if(ci.setBreak==0){
            ci.durartion.toString()
            }else{
                "${Helpers.secsToString(ci.setBreak)} / ${ci.durartion}"
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
        var txtPartName = itemView.txtPartName
        var txtPartDuration = itemView.txtPartDuration
        var imgPart = itemView.imgPart
        var btnMenuPart = itemView.btnMenuPart
        var txtPartID = itemView.txtPartID
        var parent=itemView.parentPartRV

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