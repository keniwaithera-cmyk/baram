package com.barakaplaza.rentmanager.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.barakaplaza.rentmanager.R
import com.barakaplaza.rentmanager.models.House

class HouseAdapter(
    private val houses: List<House>,
    private val onEditClick: (House) -> Unit
) : RecyclerView.Adapter<HouseAdapter.HouseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HouseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_house, parent, false)
        return HouseViewHolder(view)
    }

    override fun onBindViewHolder(holder: HouseViewHolder, position: Int) {
        val house = houses[position]
        holder.tvHouseNumber.text = "House ${house.houseNumber}"
        holder.tvType.text        = house.type
        holder.tvFloor.text       = house.floor
        holder.tvRent.text        = "KSh ${String.format("%,.0f", house.monthlyRent)}/mo"

        val occupied = house.isOccupied
        holder.tvStatus.text = if (occupied) "🔴 Occupied" else "🟢 Vacant"
        holder.tvStatus.setTextColor(
            holder.itemView.context.getColor(
                if (occupied) R.color.status_inactive else R.color.status_active
            )
        )
        holder.cardView.setCardBackgroundColor(
            holder.itemView.context.getColor(
                if (occupied) R.color.house_occupied_bg else R.color.house_vacant_bg
            )
        )
        holder.btnEdit.setOnClickListener { onEditClick(house) }
    }

    override fun getItemCount() = houses.size

    class HouseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView     = itemView.findViewById(R.id.cardView)
        val tvHouseNumber: TextView = itemView.findViewById(R.id.tvHouseNumber)
        val tvType: TextView        = itemView.findViewById(R.id.tvType)
        val tvFloor: TextView       = itemView.findViewById(R.id.tvFloor)
        val tvRent: TextView        = itemView.findViewById(R.id.tvRent)
        val tvStatus: TextView      = itemView.findViewById(R.id.tvStatus)
        val btnEdit: Button         = itemView.findViewById(R.id.btnEdit)
    }
}
