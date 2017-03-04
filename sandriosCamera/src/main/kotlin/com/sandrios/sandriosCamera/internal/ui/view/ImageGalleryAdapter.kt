package com.sandrios.sandriosCamera.internal.ui.view

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import com.bumptech.glide.Glide
import com.sandrios.sandriosCamera.R

import java.io.File
import java.util.ArrayList

/**
 * Created by TedPark on 2016. 8. 30..
 */
class ImageGalleryAdapter(internal var context: Context) : RecyclerView.Adapter<ImageGalleryAdapter.GalleryViewHolder>() {


    internal var pickerTiles: ArrayList<PickerTile>
    internal var onItemClickListener: OnItemClickListener? = null

    init {

        pickerTiles = ArrayList<PickerTile>()

        var imageCursor: Cursor? = null
        try {
            val columns = arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.ImageColumns.ORIENTATION)
            val orderBy = MediaStore.Images.Media.DATE_ADDED + " DESC"


            imageCursor = context.applicationContext.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, orderBy)
            //imageCursor = sContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, orderBy);
            if (imageCursor != null) {
                var count = 0
                while (imageCursor.moveToNext()) {
                    val imageLocation = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA))
                    val imageFile = File(imageLocation)
                    pickerTiles.add(PickerTile(Uri.fromFile(imageFile)))
                    count++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (imageCursor != null && !imageCursor.isClosed) {
                imageCursor.close()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val view = View.inflate(context, R.layout.image_item, null)
        return GalleryViewHolder(view)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {

        val pickerTile = getItem(position)

        val uri = pickerTile.imageUri
        Glide.with(context)
                .load(uri)
                .thumbnail(0.1f)
                .dontAnimate()
                .centerCrop()
                .placeholder(ContextCompat.getDrawable(context, R.drawable.ic_gallery))
                .error(ContextCompat.getDrawable(context, R.drawable.ic_error))
                .into(holder.iv_thumbnail)

        if (onItemClickListener != null) {
            holder.itemView.setOnClickListener { onItemClickListener!!.onItemClick(holder.itemView, position) }
        }
    }

    override fun getItemCount(): Int {
        return pickerTiles.size
    }

    fun getItem(position: Int): PickerTile {
        return pickerTiles[position]
    }

    fun setOnItemClickListener(
            onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)
    }


    class PickerTile internal constructor(val imageUri: Uri) {

        override fun toString(): String {
            return "ImageTile: " + imageUri
        }

    }

    internal inner class GalleryViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        var iv_thumbnail: ImageView

        init {
            iv_thumbnail = view.findViewById(R.id.image) as ImageView
        }
    }
}