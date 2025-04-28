package com.example.railoptima.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.railoptima.R
import com.example.railoptima.model.Resource

class ResourceAdapter(private val resources: MutableList<Resource>) :
    RecyclerView.Adapter<ResourceAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val typeTv: TextView = itemView.findViewById(R.id.tv_resource_type)
        val idTv: TextView = itemView.findViewById(R.id.tv_resource_id)
        val statusTv: TextView = itemView.findViewById(R.id.tv_status)
        val updateBtn: Button = itemView.findViewById(R.id.btn_update)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_resource, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val resource = resources[position]
        holder.typeTv.text = resource.type
        holder.idTv.text = resource.id
        holder.statusTv.text = resource.status

        holder.updateBtn.setOnClickListener {
            val dialogView = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.dialog_update_resource, null)
            dialogView.findViewById<TextView>(R.id.tv_dialog_content).text =
                "Update status for ${resource.type} ${resource.id}"
            val statusSpinner = dialogView.findViewById<Spinner>(R.id.sp_dialog_status)
            val statuses = arrayOf("Available", "Occupied", "Maintenance")
            statusSpinner.adapter = ArrayAdapter(
                holder.itemView.context,
                android.R.layout.simple_spinner_item,
                statuses
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            statusSpinner.setSelection(statuses.indexOf(resource.status))

            val dialog = AlertDialog.Builder(holder.itemView.context)
                .setView(dialogView)
                .create()
            dialogView.findViewById<Button>(R.id.btn_dialog_cancel).setOnClickListener { dialog.dismiss() }
            dialogView.findViewById<Button>(R.id.btn_dialog_update).setOnClickListener {
                resource.status = statusSpinner.selectedItem.toString()
                notifyItemChanged(position)
                dialog.dismiss()
            }
            dialog.show()
        }
    }

    override fun getItemCount(): Int = resources.size

    fun addResource(resource: Resource) {
        resources.add(resource)
        notifyItemInserted(resources.size - 1)
    }
}