package com.example.studywaveadmin.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.studywaveadmin.databinding.PlaylistItemBinding
import com.example.studywaveadmin.model.PlayListModel

class PlayListAdapter(
    private val playList:ArrayList<PlayListModel>
):RecyclerView.Adapter<PlayListAdapter.PlayListViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayListViewHolder {
        val binding = PlaylistItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return PlayListViewHolder(binding)
    }

    override fun getItemCount(): Int = playList.size

    override fun onBindViewHolder(holder: PlayListViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class PlayListViewHolder(private val binding: PlaylistItemBinding):RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val list = playList[position]
            binding.titleItem.text = list.title
        }
    }
}