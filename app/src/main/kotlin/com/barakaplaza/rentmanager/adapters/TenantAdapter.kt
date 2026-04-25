package com.barakaplaza.rentmanager.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.barakaplaza.rentmanager.R
import com.barakaplaza.rentmanager.models.Tenant

class TenantAdapter(
    private val tenants: List<Tenant>,
    private val onPayClick: (Tenant) -> Unit,
    private val onHistoryClick: (Tenant) -> Unit
) : RecyclerView.Adapter<TenantAdapter.TenantViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TenantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tenant, parent, false)
        return TenantViewHolder(view)
    }

    override fun onBindViewHolder(holder: TenantViewHolder, position: Int) {
        val tenant = tenants[position]
        holder.tvName.text   = tenant.name
        holder.tvHouse.text  = "House: ${tenant.houseNumber}"
        holder.tvPhone.text  = "📞 ${tenant.phone}"
        holder.tvMoveIn.text = "Move-in: ${tenant.moveInDate}"
        holder.tvStatus.text = if (tenant.isActive) "✅ Active" else "❌ Inactive"
        holder.tvStatus.setTextColor(
            holder.itemView.context.getColor(
                if (tenant.isActive) R.color.status_active else R.color.status_inactive
            )
        )
        holder.tvAvatar.text = tenant.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        holder.btnPay.setOnClickListener { onPayClick(tenant) }
        holder.btnHistory.setOnClickListener { onHistoryClick(tenant) }
    }

    override fun getItemCount() = tenants.size

    class TenantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView   = itemView.findViewById(R.id.tvName)
        val tvHouse: TextView  = itemView.findViewById(R.id.tvHouse)
        val tvPhone: TextView  = itemView.findViewById(R.id.tvPhone)
        val tvMoveIn: TextView = itemView.findViewById(R.id.tvMoveIn)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvAvatar: TextView = itemView.findViewById(R.id.tvAvatar)
        val btnPay: Button     = itemView.findViewById(R.id.btnPay)
        val btnHistory: Button = itemView.findViewById(R.id.btnHistory)
    }
}
