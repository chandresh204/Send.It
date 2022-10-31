package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.*
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chad.orionsoft.sendit.databinding.ActivitySendImagesBinding
import kotlinx.coroutines.*
import java.io.File

class SendActivityImages : AppCompatActivity() {

    private val imagesList=ArrayList<ImageObject>()
    lateinit var adapter:IAdapter
    var selectedImages=0
    var selectedSize:Long=0
    private lateinit var outfileDir : File
    private lateinit var thumbHandler : Handler

    private lateinit var binding: ActivitySendImagesBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendImagesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        outfileDir = File(applicationContext.getExternalFilesDir("Images"), "Thumbnails")
        if(!outfileDir.exists()) {
            outfileDir.mkdirs()
        }
        getImages()
        thumbHandler = Handler(mainLooper) {
            val pos = it.obj as Int
            adapter.notifyItemChanged(pos)
            true
        }
        updateBelowBar()
        CoroutineScope(Dispatchers.Main).launch {
            getThumbnailsAsync().await()
        }
        binding.imageRecyclerView.layoutManager= GridLayoutManager(applicationContext,3)
        binding.imageRecyclerView.recycledViewPool.setMaxRecycledViews(0,0)
        adapter=IAdapter(imagesList)
        binding.imageRecyclerView.adapter=adapter
        binding.toolbarTextImages.text="Send.it - ${imagesList.size} images"
    }

    private fun getImages() {
        val imageURI= MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val cursor= contentResolver.query(imageURI,null,null,null,null)
        if(cursor!=null && cursor.moveToFirst()) {

            var count=0
            val indexID=cursor.getColumnIndex(MediaStore.Images.Media._ID)
            val indexDATa=cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            val titleIndex=cursor.getColumnIndex(MediaStore.Images.Media.TITLE)

            do {
                val thisID=cursor.getLong(indexID)
                val data=cursor.getString(indexDATa)
                val title=cursor.getString(titleIndex)
                count++
                imagesList.add(ImageObject(thisID,data,title,false, "null"))
            } while (cursor.moveToNext())
        }
        cursor?.close()
        imagesList.sortByDescending { imageObject -> imageObject.lastModified }
    }

    private suspend fun getThumbnailsAsync() =
        coroutineScope {
            async(Dispatchers.IO) {
                for (i in 0 until imagesList.size) {
                    if (imagesList[i].imageFile.length() >= Connection.maxImageMemAllowed) {
                        continue
                    }
                    val outFile = File(outfileDir, imagesList[i].imageFile.name)
                    if (!outFile.exists()) {
                        val mainImage = BitmapFactory.decodeFile(imagesList[i].imageDATA)
                        if(mainImage != null) {
                            val asRation=mainImage.width.toFloat()/mainImage.height.toFloat()
                            val newHeight=300/asRation
                            val bMap = Bitmap.createScaledBitmap(mainImage, 300, newHeight.toInt(), false)
                            bMap.compress(Bitmap.CompressFormat.JPEG, 90, outFile.outputStream())
                        }
                    }
                    imagesList[i].thumbData = outFile.path
                    thumbHandler.obtainMessage(0, i).sendToTarget()
                }
            }
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

    fun sendImages(v:View) {
        v.id
        if(selectedSize == 0L) {
            Toast.makeText(applicationContext,"Nothing Selected",Toast.LENGTH_SHORT).show()
            return
        }
        if(!Connection.connection) {
            val i=Intent(applicationContext,ConnectionCheck::class.java)
            i.putExtra(ConnectionCheck.MODE_INTENT_STRING,ConnectionCheck.SENDER_MODE)
            startActivity(i)
            return
        }
        if(selectedSize > Connection.freeSpace) {
            Toast.makeText(applicationContext, resources.getString(R.string.no_space_error),Toast.LENGTH_SHORT).show()
            return
        }
        val sendImages=ArrayList<SendObject>()
        for(i in imagesList) {
            if(i.isSelected) {
                sendImages.add(SendObject(i.imageFile.name,i.imageDATA))
            }
        }
        StaticList.sendList=sendImages
        startActivity(Intent(applicationContext,SendNow::class.java))
        for(i in imagesList) {
            i.isSelected=false
            selectedImages=0
            selectedSize=0
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun showPopUpMenu(v:View) {
        val themeCtx=ContextThemeWrapper(applicationContext,R.style.SenderTheme_NoActionBar)
        val popup=PopupMenu(themeCtx,v)
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

    fun goBack(v:View) {
        v.id
        onBackPressed()
    }

    override fun onResume() {
        updateBelowBar()
        super.onResume()
    }

    inner class IAdapter(private val images:ArrayList<ImageObject>) : RecyclerView.Adapter<IAdapter.IHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IHolder =
            IHolder(layoutInflater.inflate(R.layout.item_layout_image,parent,false))

        override fun getItemCount(): Int = images.size

        override fun onBindViewHolder(holder: IHolder, position: Int) {
            val image=images[position]
            val imageView=holder.imageView
            val eyeImage=holder.eyeImage
            val selectionMark=holder.selectionMark

            if (images[position].thumbData != "null") {
                CoroutineScope(Dispatchers.Main).launch {
                    val bMap = getThumbnailAsync(images[position].thumbData).await()
                    imageView.setImageBitmap(bMap)
                    imageView.animate().apply {
                        alpha(1f)
                        duration = 500
                    }
                }
            }

            eyeImage.setOnClickListener {
                val i= Intent(applicationContext, ViewImage::class.java)
                i.putExtra("path",image.imageDATA)
                startActivity(i)
            }

            if(images[position].isSelected) {
                selectionMark.visibility= View.VISIBLE
            } else {
                selectionMark.visibility=View.GONE
            }

            imageView.setOnClickListener {
                if(image.isSelected) {
                    image.isSelected=false
                    selectionMark.visibility=View.GONE
                    selectedImages--
                    selectedSize-=image.size
                } else {
                    image.isSelected=true
                    selectionMark.visibility=View.VISIBLE
                    selectedImages++
                    selectedSize+=image.size
                }
                updateBelowBar()
            }
        }

        inner class IHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView:ImageView=itemView.findViewById(R.id.imageView)
            val eyeImage:ImageView=itemView.findViewById(R.id.eye_image)
            val selectionMark:ImageView=itemView.findViewById(R.id.selection_mark)
        }
    }

    private suspend fun getThumbnailAsync(thumbData: String) : Deferred<Bitmap> =
        coroutineScope {
            async (Dispatchers.IO) {
                return@async BitmapFactory.decodeFile(thumbData)
            }
        }

    inner class ImageObject(val id:Long ,val imageDATA: String,val title:String,var isSelected:Boolean, var thumbData:String) {

        val imageFile=File(imageDATA)
        val size=imageFile.length()
   //    val thumbName="$id:$title"
        val lastModified=imageFile.lastModified()
    }
}
