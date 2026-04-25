package com.barakaplaza.rentmanager.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.barakaplaza.rentmanager.R
import com.barakaplaza.rentmanager.models.Payment

class PaymentAdapter(
    private val payments: List<Payment>,
    private val onReceiptClick: (Payment) -> Unit
) : RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_payment, parent, false)
        return PaymentViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        val payment = payments[position]
        holder.tvTenantName.text = payment.tenantName
        holder.tvHouse.text      = "House ${payment.houseNumber}"
        holder.tvAmount.text     = "KSh ${String.format("%,.0f", payment.amount)}"
        holder.tvDate.text       = payment.paymentDate
        holder.tvRef.text        = "Ref: ${payment.referenceNumber}"
        holder.tvMonth.text      = "${payment.paymentMonth} ${payment.paymentYear}"

        val statusColor = holder.itemView.context.getColor(
            when (payment.status) {
                Payment.STATUS_CONFIRMED -> R.color.status_active
                Payment.STATUS_PENDING   -> R.color.status_pending
                else                     -> R.color.status_inactive
            }
        )
        holder.tvStatus.text = payment.status
        holder.tvStatus.setTextColor(statusColor)

        holder.tvMethodBadge.text = when (payment.paymentMethod) {
            Payment.METHOD_MPESA     -> "📱 M-PESA"
            Payment.METHOD_CASH      -> "💵 CASH"
            Payment.METHOD_PAYBILL   -> "🏦 PAYBILL"
            Payment.METHOD_STK_PUSH  -> "📲 STK PUSH"
            else                     -> payment.paymentMethod
        }

        holder.cardView.setOnClickListener { onReceiptClick(payment) }
    }

    override fun getItemCount() = payments.size

    class PaymentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView        = itemView.findViewById(R.id.cardView)
        val tvTenantName: TextView    = itemView.findViewById(R.id.tvTenantName)
        val tvHouse: TextView         = itemView.findViewById(R.id.tvHouse)
        val tvAmount: TextView        = itemView.findViewById(R.id.tvAmount)
        val tvDate: TextView          = itemView.findViewById(R.id.tvDate)
        val tvRef: TextView           = itemView.findViewById(R.id.tvRef)
        val tvStatus: TextView        = itemView.findViewById(R.id.tvStatus)
        val tvMonth: TextView         = itemView.findViewById(R.id.tvMonth)
        val tvMethodBadge: TextView   = itemView.findViewById(R.id.tvMethodBadge)
    }
}
