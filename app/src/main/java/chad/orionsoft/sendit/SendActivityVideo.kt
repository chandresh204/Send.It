package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ThumbnailUtils
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chad.orionsoft.sendit.databinding.ActivitySendVideoBinding
import kotlinx.coroutines.*
import java.io.File
import java.lang.Exception

class SendActivityVideo : AppCompatActivity() {

    private lateinit var vAdapter:VideoAdapter
    private val videosList=ArrayList<VideoObject>()
    private val thumbMap=HashMap<Long,Bitmap>()
    private var selectedVideoFiles=0
    private var selectedSize:Long=0

    private lateinit var sView:SearchView
    private lateinit var binding: ActivitySendVideoBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        generateVideoList()
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

    private fun generateVideoList() {

        val vURI= MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val vCursor=contentResolver.query(vURI,null,null,null,null)

        if(vCursor!=null && vCursor.moveToFirst()) {

            val idIndex=vCursor.getColumnIndex(MediaStore.Video.Media._ID)
            val titleIndex=vCursor.getColumnIndex(MediaStore.Video.Media.TITLE)
            val pathIndex=vCursor.getColumnIndex(MediaStore.Video.Media.DATA)
            val durationIndex=vCursor.getColumnIndex(MediaStore.Video.Media.DURATION)

            do {
                val id=vCursor.getLong(idIndex)
                val title=vCursor.getString(titleIndex)
                val path=vCursor.getString(pathIndex)
                val duration=vCursor.getInt(durationIndex)
                val thumbData="unknown"

                videosList.add(VideoObject(id,title,path,thumbData,duration,false))
            } while (vCursor.moveToNext())
        }
        vCursor?.close()
        videosList.sortBy { it.title.lowercase() }
        binding.videoRecyclerView.layoutManager= LinearLayoutManager(applicationContext)
        vAdapter=VideoAdapter(videosList)
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

    private fun selectAll() {
        selectedSize=0
        selectedVideoFiles=0
        for(i in videosList) {
            i.isSelected=true
            selectedVideoFiles++
            selectedSize+=i.fileSize
        }
        updateBelowBar()
        vAdapter.notifyDataSetChanged()
    }

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
        if(selectedSize>Connection.freeSpace) {
            Toast.makeText(applicationContext, resources.getString(R.string.no_space_error), Toast.LENGTH_SHORT).show()
            return
        }
        val sendList=ArrayList<SendObject>()
        for(i in videosList) {
            if(i.isSelected) {
                sendList.add(SendObject(i.videoFile.name,i.path))
            }
        }
        StaticList.sendList=sendList
        startActivity(Intent(applicationContext,SendNow::class.java))
        clearAll()


    }

    override fun onResume() {
        updateBelowBar()
        super.onResume()
    }

    fun goBack(v:View) {
        v.id
        onBackPressed()
    }

    fun showPopUpMenu(v:View) {
        val themeCtx=ContextThemeWrapper(applicationContext,R.style.SenderTheme_NoActionBar)
        val popupMenu=PopupMenu(themeCtx,v)
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

    inner class VideoAdapter(val videos:ArrayList<VideoObject>) : RecyclerView.Adapter<VideoAdapter.VideoHolder>() {

        val fullList=ArrayList<VideoObject>().apply {
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
                        Connection.formatDataString(videos[position].fileSize,' ')

            videoThumb.setImageDrawable(resources.getDrawable(R.drawable.video_icon_small))
            if(thumbMap[videos[position].id] !=null) {
                videoThumb.setImageBitmap(thumbMap[videos[position].id])
            } else {
                GlobalScope.launch(Dispatchers.Main) {
                    val id=videos[position].id
                    val thumbnail=loadThumbnailAsync(videos[position].path).await()
                    videoThumb.setImageBitmap(thumbnail)
                    thumbMap[id] = thumbnail
                }
            }


            if(videos[position].isSelected) {
                videoItemLayout.background=resources.getDrawable(R.drawable.item_background_select)
                videoTitle.isSelected=true
            } else {
                videoItemLayout.background=resources.getDrawable(R.drawable.item_background)
                videoTitle.isSelected=false
            }

            videoItemLayout.setOnClickListener {
                if(videos[position].isSelected) {
                    videos[position].isSelected=false
                    videoItemLayout.background=resources.getDrawable(R.drawable.item_background)
                    videoTitle.isSelected=false
                    selectedSize-=videos[position].fileSize
                    selectedVideoFiles--
                    updateBelowBar()
                } else {
                    videos[position].isSelected=true
                    videoItemLayout.background=resources.getDrawable(R.drawable.item_background_select)
                    videoTitle.isSelected=true
                    selectedVideoFiles++
                    selectedSize+=videos[position].fileSize
                    updateBelowBar()
                }
            }

            videoPlayButton.setOnClickListener {
                val i= Intent(applicationContext,VideoPlayer::class.java)
                i.putExtra("title",videos[position].title)
                i.putExtra("path",videos[position].path)
                i.putExtra("duration",videos[position].duration)
                startActivity(i)
            }
        }

        val filter=object: Filter() {

            override fun performFiltering(p0: CharSequence?): FilterResults {
                val filteredList=ArrayList<VideoObject>()
                val queryText=p0.toString().lowercase()
                for(i in fullList) {
                    if(i.title.lowercase().trim().contains(queryText)) {
                        filteredList.add(i)
                    }
                }
                val filterResults=FilterResults()
                filterResults.values=filteredList
                return filterResults
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                videos.clear()
                videos.addAll(p1?.values as ArrayList<VideoObject>)
                if(videos.size==0)
                    binding.noMediaFoundText.visibility=View.VISIBLE
                else
                    binding.noMediaFoundText.visibility=View.GONE
                notifyDataSetChanged()
            }

        }
        inner class VideoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val videoItemLayout:LinearLayout=itemView.findViewById(R.id.video_item_layout)
            val videoTitle:TextView=itemView.findViewById(R.id.video_title)
            val videoInfo:TextView=itemView.findViewById(R.id.video_info)
            val videoThumb:ImageView=itemView.findViewById(R.id.video_thumbnail)
            val videoPlayButton:Button=itemView.findViewById(R.id.video_play_button)
        }
    }

    suspend fun loadThumbnailAsync(path:String) : Deferred<Bitmap> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async try {
                    ThumbnailUtils.createVideoThumbnail(path,MediaStore.Video.Thumbnails.MICRO_KIND)!!
                } catch (e: Exception) {
                    BitmapFactory.decodeResource(resources, R.drawable.video_icon_small)
                }
            }
        }

    inner class VideoObject(val id:Long, val title:String,val path:String,val thumbPath:String,val duration:Int,var isSelected:Boolean) {
        val videoFile=File(path)
        val fileSize= videoFile.length()
    }
}
