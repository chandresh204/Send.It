package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chad.orionsoft.sendit.databinding.ActivitySendImagesBinding
import kotlinx.coroutines.*
import java.io.File

class SendActivityImagesQ : AppCompatActivity() {

    private val imagesList=ArrayList<ImageObjectQ>()
    lateinit var adapter:IAdapterQ
    var selectedImages=0
    var selectedSize:Long=0

    private lateinit var binding: ActivitySendImagesBinding

    @TargetApi(Build.VERSION_CODES.Q)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_images)
        setOutFileDir(applicationContext)
        if(!outfileDir.exists()) {
            outfileDir.mkdirs()
        }
        getImagesQ()
        updateBelowBar()
        binding.imageRecyclerView.layoutManager= GridLayoutManager(applicationContext,3)
        adapter=IAdapterQ(imagesList)
        binding.imageRecyclerView.adapter=adapter
        binding.toolbarTextImages.text="Send.it - ${imagesList.size} images"
    }

    private fun getImagesQ() {
        val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val iCursor = contentResolver.query(imageUri, null, null, null, null)

        if(iCursor!=null && iCursor.moveToFirst()) {
            val idIndex = iCursor.getColumnIndex(MediaStore.Images.Media._ID)
            val titleIndex = iCursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeIndex = iCursor.getColumnIndex(MediaStore.Images.Media.SIZE)
            val modifyIndex = iCursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)

            do {
                val id = iCursor.getLong(idIndex)
                val uri = ContentUris.withAppendedId(imageUri, id)
                val title = iCursor.getString(titleIndex)
                val size = iCursor.getLong(sizeIndex)
                val modified = iCursor.getLong(modifyIndex)
                imagesList.add(ImageObjectQ(uri, title, size, modified, false))
            } while (iCursor.moveToNext())
        }
        iCursor?.close()
        imagesList.sortByDescending { imageObjectQ -> imageObjectQ.lastModified }
    }

    @SuppressLint("SetTextI18n")
    private fun updateBelowBar() {
        if(Connection.connection) {
            if(selectedSize>Connection.freeSpace) {
                binding.imageBottomInfo.setBackgroundColor(Color.RED)
            } else {
                binding.imageBottomInfo.setBackgroundColor(Color.rgb(0x65,0x1f,0xff))
            }
            binding.imageBottomInfo.text=
                "Selected $selectedImages : ${Connection.formatDataString(selectedSize,' ')}" +
                        " , free: ${Connection.formatDataString(Connection.freeSpace,' ')}"
        } else {
            binding.imageBottomInfo.text=
                "Selected $selectedImages : ${Connection.formatDataString(selectedSize,' ')}"
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    fun sendImages(v:View) {
        v.id
        if(selectedSize == 0L) {
            Toast.makeText(applicationContext,"Nothing Selected", Toast.LENGTH_SHORT).show()
            return
        }
        if(!Connection.connection) {
            val i= Intent(applicationContext,ConnectionCheck::class.java)
            i.putExtra(ConnectionCheck.MODE_INTENT_STRING,ConnectionCheck.SENDER_MODE)
            startActivity(i)
            return
        }
        if(selectedSize > Connection.freeSpace) {
            Toast.makeText(applicationContext, resources.getString(R.string.no_space_error), Toast.LENGTH_SHORT).show()
            return
        }
        val sendImages = ArrayList<SendObjectQ>()
        for (i in imagesList) {
            if (i.isSelected) {
                sendImages.add(SendObjectQ(i.title, i.uri, i.size))
            }
        }
        StaticList.sendListQ = sendImages
        startActivity(Intent(applicationContext,SendNowQ::class.java))
        for(i in imagesList) {
            i.isSelected=false
            selectedImages=0
            selectedSize=0
        }
        adapter.notifyDataSetChanged()
        updateBelowBar()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun showPopUpMenu(v: View) {
        val themeCtx= ContextThemeWrapper(applicationContext,R.style.SenderTheme_NoActionBar)
        val popup= PopupMenu(themeCtx,v)
        popup.inflate(R.menu.sendit_image_menu)
        popup.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.select_all_images -> {
                    selectedSize=0
                    selectedImages=0
                    for(i in imagesList) {
                        i.isSelected=true
                        selectedImages++
                        selectedSize+=i.size
                    }
                    adapter.notifyDataSetChanged()
                    updateBelowBar()
                }
                R.id.clear_selected_images -> {
                    for(i in imagesList) {
                        i.isSelected=false
                        selectedImages=0
                        selectedSize=0
                    }
                    adapter.notifyDataSetChanged()
                    updateBelowBar()
                }
            }
            true
        }
        popup.show()
    }

    fun goBack(v: View) {
        v.id
        onBackPressed()
    }

    override fun onResume() {
        updateBelowBar()
        super.onResume()
    }

    inner class IAdapterQ(private val images:ArrayList<ImageObjectQ>) : RecyclerView.Adapter<IAdapterQ.IHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IHolder =
            IHolder(layoutInflater.inflate(R.layout.item_layout_image,parent,false))

        override fun getItemCount(): Int = images.size

        @RequiresApi(Build.VERSION_CODES.Q)
        override fun onBindViewHolder(holder: IHolder, position: Int) {
            val image=images[position]
            val imageView=holder.imageView
            val eyeImage=holder.eyeImage
            val selectionMark=holder.selectionMark

            try {
                CoroutineScope(Dispatchers.Main).launch {
                    val bMap = loadThumbnailAsync(image.uri).await()
                    imageView.setImageBitmap(bMap)
                    imageView.animate().apply {
                        alpha(1f)
                        duration = 500
                    }
                }
            } catch (e:Exception) {
                imageView.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        this@SendActivityImagesQ.resources,
                        R.drawable.image_icon_small,
                        this@SendActivityImagesQ.theme))
            }
            eyeImage.setOnClickListener {
                val i= Intent(applicationContext, ViewImageQ::class.java)
                i.putExtra("uri",image.uri.toString())
                i.putExtra("size", image.size)
                i.putExtra("title", image.title)
                startActivity(i)
            }

            if(images[position].isSelected) {
                selectionMark.visibility= View.VISIBLE
            } else {
                selectionMark.visibility= View.GONE
            }

            imageView.setOnClickListener {
                if(image.isSelected) {
                    image.isSelected=false
                    selectionMark.visibility= View.GONE
                    selectedImages--
                    selectedSize-=image.size
                } else {
                    image.isSelected=true
                    selectionMark.visibility= View.VISIBLE
                    selectedImages++
                    selectedSize+=image.size
                }
                updateBelowBar()
            }
        }

        inner class IHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView =itemView.findViewById(R.id.imageView)
            val eyeImage: ImageView =itemView.findViewById(R.id.eye_image)
            val selectionMark: ImageView =itemView.findViewById(R.id.selection_mark)
        }
    }

    companion object {
        lateinit var outfileDir: File

        fun setOutFileDir(ctx: Context) {
            outfileDir = ctx.getExternalFilesDir("Thumbnails")!!
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun loadThumbnailAsync(uri:Uri) : Deferred<Bitmap> =
        coroutineScope {
            async (Dispatchers.IO){
                return@async try {
                    contentResolver.loadThumbnail(uri, Size(300,300), null)
                } catch (e: Exception) {
                    BitmapFactory.decodeResource(resources, R.drawable.broken_image)
                }
            }
        }

    inner class ImageObjectQ(val uri: Uri, val title:String,val size:Long,val lastModified:Long, var isSelected: Boolean)
}