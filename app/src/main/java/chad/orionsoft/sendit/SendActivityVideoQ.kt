package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.ContentUris
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chad.orionsoft.sendit.databinding.ActivitySendVideoBinding
import kotlinx.coroutines.*
import java.lang.Exception

@TargetApi(29)
class SendActivityVideoQ : AppCompatActivity() {

    private lateinit var vAdapter:VideoAdapterQ
    private val videosList=ArrayList<VideoObjectQ>()
    private var selectedVideoFiles=0
    private var selectedSize:Long=0

    private lateinit var sView: SearchView
    private lateinit var binding: ActivitySendVideoBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        generateVideoListQ()
        sView= binding.videoSearchView
        sView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(p0: String?): Boolean {
                vAdapter.filter.filter(p0)
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                vAdapter.filter.filter(p0)
                return true
            }
        })
        updateBelowBar()
        if(videosList.size==1) {
            binding.toolbarTextVideo.text="Send.it - ${videosList.size} video"
        } else {
            binding.toolbarTextVideo.text="Send.it - ${videosList.size} videos"
        }
    }

    private fun generateVideoListQ() {

        val vURI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val vCursor = contentResolver.query(vURI,null,null,null,null)

        if (vCursor!=null && vCursor.moveToFirst()) {

            val idIndex = vCursor.getColumnIndex(MediaStore.Video.Media._ID)
            val titleIndex = vCursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            val durationIndex = vCursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            val sizeIndex = vCursor.getColumnIndex(MediaStore.Video.Media.SIZE)

            do {
                val id = vCursor.getLong(idIndex)
                val uri = ContentUris.withAppendedId(vURI, id)
                val title = vCursor.getString(titleIndex)
                val duration = vCursor.getInt(durationIndex)
                val size = vCursor.getLong(sizeIndex)
                videosList.add(VideoObjectQ(uri,title,duration,size,false))
            } while (vCursor.moveToNext())
        }

        vCursor?.close()
        videosList.sortBy { it.title.lowercase() }
        binding.videoRecyclerView.layoutManager= LinearLayoutManager(applicationContext)
        vAdapter=VideoAdapterQ(videosList)
        binding.videoRecyclerView.adapter=vAdapter
        binding.videoRecyclerView.recycledViewPool.setMaxRecycledViews(0,0)
    }

    @SuppressLint("SetTextI18n")
    private fun updateBelowBar() {
        if(Connection.connection) {
            if(selectedSize>Connection.freeSpace) {
                binding.videoBottomInfo.setBackgroundColor(Color.RED)
            } else {
                binding.videoBottomInfo.setBackgroundColor(Color.rgb(0x65,0x1f,0xff))
            }
            binding.videoBottomInfo.text=
                "Selected $selectedVideoFiles : ${Connection.formatDataString(selectedSize,' ')}" +
                        " , free: ${Connection.formatDataString(Connection.freeSpace,' ')}"
        } else {
            binding.videoBottomInfo.text=
                "Selected $selectedVideoFiles : ${Connection.formatDataString(selectedSize,' ')}"
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun selectAll() {
        selectedSize=0
        selectedVideoFiles=0
        for(i in videosList) {
            i.isSelected=true
            selectedVideoFiles++
            selectedSize+=i.size
        }
        updateBelowBar()
        vAdapter.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearAll() {
        for(i in videosList) {
            i.isSelected=false
            selectedVideoFiles=0
            selectedSize=0
        }
        updateBelowBar()
        vAdapter.notifyDataSetChanged()
    }

    fun sendVideoNow(v:View) {
        v.id
        if (selectedSize == 0L) {
            Toast.makeText(applicationContext,"Nothing Selected", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Connection.connection) {
            val i= Intent(applicationContext,ConnectionCheck::class.java)
            i.putExtra(ConnectionCheck.MODE_INTENT_STRING,ConnectionCheck.SENDER_MODE)
            startActivity(i)
            return
        }
        if(selectedSize>Connection.freeSpace) {
            Toast.makeText(applicationContext, resources.getString(R.string.no_space_error), Toast.LENGTH_SHORT).show()
            return
        }
        val sendList = ArrayList<SendObjectQ>()
        for (i in videosList) {
            if (i.isSelected) {
                sendList.add(SendObjectQ(i.title, i.uri, i.size))
            }
        }
        StaticList.sendListQ = sendList
        startActivity(Intent(applicationContext,SendNowQ::class.java))
        clearAll()
    }

    override fun onResume() {
        updateBelowBar()
        super.onResume()
    }

    fun goBack(v: View) {
        v.id
        onBackPressed()
    }

    fun showPopUpMenu(v: View) {
        val themeCtx= ContextThemeWrapper(applicationContext,R.style.SenderTheme_NoActionBar)
        val popupMenu= PopupMenu(themeCtx,v)
        popupMenu.inflate(R.menu.sendit_media_menu)
        popupMenu.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.select_all_media -> {
                    selectAll()
                }
                R.id.clear_selected_media -> {
                    clearAll()
                }
            }
            true
        }
        popupMenu.show()
    }

    override fun onBackPressed() {
        if(sView.isIconified) {
            finish()
        } else {
            sView.setQuery("",true)
            sView.isIconified=true
        }
    }

    inner class VideoAdapterQ(val videos:ArrayList<VideoObjectQ>) : RecyclerView.Adapter<VideoAdapterQ.VideoHolder>() {

        val fullList=ArrayList<VideoObjectQ>().apply {
            addAll(videos)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
            return VideoHolder(layoutInflater.inflate(R.layout.video_item_layout,parent,false))
        }

        override fun getItemCount(): Int = videos.size

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: VideoHolder, position: Int) {
            val videoItemLayout=holder.videoItemLayout
            val videoTitle=holder.videoTitle
            val videoInfo=holder.videoInfo
            val videoThumb=holder.videoThumb
            val videoPlayButton=holder.videoPlayButton

            videoTitle.text=videos[position].title
            videoInfo.text=
                "${Connection.formatDuration(videos[position].duration)}  " +
                        Connection.formatDataString(videos[position].size,' ')

            try {
                CoroutineScope(Dispatchers.Main).launch {
                    val bMap = loadThumbnailAsync(videos[position].uri).await()
                    videoThumb.setImageBitmap(bMap)
                }
            } catch (ex: Exception) {
                videoThumb.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        this@SendActivityVideoQ.resources,
                        R.drawable.video_icon_small,
                        this@SendActivityVideoQ.theme
                    ))
            }

            if(videos[position].isSelected) {
                videoItemLayout.background=
                    ResourcesCompat.getDrawable(
                        this@SendActivityVideoQ.resources,
                        R.drawable.item_background_select,
                        this@SendActivityVideoQ.theme
                    )
                videoTitle.isSelected=true
            } else {
                videoItemLayout.background =
                    ResourcesCompat.getDrawable(
                        this@SendActivityVideoQ.resources,
                        R.drawable.item_background,
                        this@SendActivityVideoQ.theme
                    )
                videoTitle.isSelected=false
            }

            videoItemLayout.setOnClickListener {
                if(videos[position].isSelected) {
                    videos[position].isSelected=false
                    videoItemLayout.background=
                        ResourcesCompat.getDrawable(
                            this@SendActivityVideoQ.resources,
                            R.drawable.item_background,
                            this@SendActivityVideoQ.theme
                        )
                    videoTitle.isSelected=false
                    selectedSize-=videos[position].size
                    selectedVideoFiles--
                    updateBelowBar()
                } else {
                    videos[position].isSelected=true
                    videoItemLayout.background=
                        ResourcesCompat.getDrawable(
                            this@SendActivityVideoQ.resources,
                            R.drawable.item_background_select,
                            this@SendActivityVideoQ.theme
                        )
                    videoTitle.isSelected=true
                    selectedVideoFiles++
                    selectedSize+=videos[position].size
                    updateBelowBar()
                }
            }

            videoPlayButton.setOnClickListener {
                val i= Intent(applicationContext,VideoPlayerQ::class.java)
                i.putExtra("title",videos[position].title)
                i.putExtra("uri",videos[position].uri.toString())
                i.putExtra("duration",videos[position].duration)
                startActivity(i)
            }
        }

        val filter=object: Filter() {

            override fun performFiltering(p0: CharSequence?): FilterResults {
                val filteredList=ArrayList<VideoObjectQ>()
                val queryText=p0.toString().lowercase()
                for(i in fullList) {
                    if(i.title.lowercase().trim().contains(queryText)) {
                        filteredList.add(i)
                    }
                }
                val filterResults= FilterResults()
                filterResults.values=filteredList
                return filterResults
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                videos.clear()
                videos.addAll(p1?.values as ArrayList<VideoObjectQ>)
                if(videos.size==0)
                    binding.noMediaFoundText.visibility= View.VISIBLE
                else
                    binding.noMediaFoundText.visibility= View.GONE
                notifyDataSetChanged()
            }

        }
        inner class VideoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val videoItemLayout: LinearLayout =itemView.findViewById(R.id.video_item_layout)
            val videoTitle: TextView =itemView.findViewById(R.id.video_title)
            val videoInfo: TextView =itemView.findViewById(R.id.video_info)
            val videoThumb: ImageView =itemView.findViewById(R.id.video_thumbnail)
            val videoPlayButton: Button =itemView.findViewById(R.id.video_play_button)
        }
    }

    private suspend fun loadThumbnailAsync(uri:Uri): Deferred<Bitmap> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async try {
                    contentResolver.loadThumbnail(uri, Size(100,100), null)
                } catch (e: Exception) {
                    BitmapFactory.decodeResource(resources, R.drawable.video_icon_small)
                }
            }
        }

    inner class VideoObjectQ(val uri: Uri, val title:String,val duration: Int,val size:Long, var isSelected: Boolean)

}