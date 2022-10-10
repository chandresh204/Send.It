package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.ContentUris
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
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chad.orionsoft.sendit.databinding.ActivitySendAudioBinding
import kotlinx.coroutines.*
import java.lang.Exception

class SendActivityAudioQ : AppCompatActivity() {

    private val audioList=ArrayList<AudioObjectQ>()
    private lateinit var aAdapter:AudioAdapterQ
    private var selectedAudioFiles=0
    private var selectedSize:Long=0

    private lateinit var sView: SearchView
    private lateinit var binding: ActivitySendAudioBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendAudioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        generateAudioListQ()
        updateBelowBar()
        sView= binding.audioSearchView
        sView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(p0: String?): Boolean {
                aAdapter.filter.filter(p0)
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                aAdapter.filter.filter(p0)
                return true
            }
        })
        if(audioList.size==1) {
            binding.toolbarTextAudio.text="Send.it - ${audioList.size} file"
        } else {
            binding.toolbarTextAudio.text="Send.it - ${audioList.size} files"
        }
        binding.toolbarTextAudio.setOnClickListener {  }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun generateAudioListQ() {
        val audioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val albumUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

        val aCursor = contentResolver.query(audioUri,null,null,null,null)
        val alCursor = contentResolver.query(albumUri,null,null,null,null)

        if (aCursor!=null && aCursor.moveToFirst()) {

            val idIndex = aCursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val alIndex1 = aCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            val titleIndex = aCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistIndex = aCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val durationIndex = aCursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val sizeIndex = aCursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
            val nameIndex = aCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)


            do {
                val alId1 = aCursor.getLong(alIndex1)
                var artUri = Uri.parse("null")
                if (alCursor!=null && alCursor.moveToFirst()) {
                    val alIndex2 = alCursor.getColumnIndex(MediaStore.Audio.Albums._ID)
                    do {
                        val alId2 = alCursor.getLong(alIndex2)
                        if (alId1 == alId2) {
                            artUri= ContentUris.withAppendedId(albumUri, alId2)
                            break
                        }
                    } while (alCursor.moveToNext())
                }
                val uri = ContentUris.withAppendedId(audioUri, aCursor.getLong(idIndex))
                val title = aCursor.getString(titleIndex)
                val artist = aCursor.getString(artistIndex)
                val duration = aCursor.getInt(durationIndex)
                val size = aCursor.getLong(sizeIndex)
                val name = aCursor.getString(nameIndex)
                audioList.add(AudioObjectQ(uri,name,title,artist.toString(),duration,artUri,size,false))
            } while (aCursor.moveToNext())
        }
        aCursor?.close()
        alCursor?.close()
        audioList.sortBy { it.title.lowercase() }
        binding.audioRecyclerView.layoutManager= LinearLayoutManager(applicationContext)
        aAdapter=AudioAdapterQ(audioList)
        binding.audioRecyclerView.adapter=aAdapter
    }

    @SuppressLint("SetTextI18n")
    private fun updateBelowBar() {
        if(Connection.connection) {
            if(selectedSize>Connection.freeSpace) {
                binding.audioBottomInfo.setBackgroundColor(Color.RED)
            } else {
                binding.audioBottomInfo.setBackgroundColor(Color.rgb(0x65,0x1f,0xff))
            }
            binding.audioBottomInfo.text=
                "Selected $selectedAudioFiles : ${Connection.formatDataString(selectedSize,' ')}" +
                        " , free: ${Connection.formatDataString(Connection.freeSpace,' ')}"
        } else {
            binding.audioBottomInfo.text=
                "Selected $selectedAudioFiles : ${Connection.formatDataString(selectedSize,' ')}"
        }
    }

    private fun selectAll() {
        selectedAudioFiles=0
        selectedSize=0
        for(i in audioList) {
            i.isSelected=true
            selectedAudioFiles++
            selectedSize+=i.size
        }
        updateBelowBar()
        aAdapter.notifyDataSetChanged()
    }

    private fun clearAll() {
        for(i in audioList) {
            i.isSelected=false
            selectedAudioFiles=0
            selectedSize=0
        }
        updateBelowBar()
        aAdapter.notifyDataSetChanged()
    }

   fun sendAudioNow(v: View) {
        v.id
        if(selectedAudioFiles==0) {
            Toast.makeText(applicationContext,"Nothing selected", Toast.LENGTH_SHORT).show()
            return
        }
        if(!Connection.connection) {
            val i= Intent(applicationContext,ConnectionCheck::class.java)
            i.putExtra(ConnectionCheck.MODE_INTENT_STRING,ConnectionCheck.SENDER_MODE)
            startActivity(i)
            return
        }
        if(selectedSize > Connection.freeSpace) {
            Toast.makeText(applicationContext,resources.getString(R.string.no_space_error), Toast.LENGTH_SHORT).show()
            return
        }
        val sendListQ=ArrayList<SendObjectQ>()
        for(i in audioList) {
            if(i.isSelected) {
                sendListQ.add(SendObjectQ(i.name, i.uri, i.size))
            }
        }
        StaticList.sendListQ=sendListQ
        startActivity(Intent(applicationContext,SendNowQ::class.java))
        clearAll()
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

    fun goBack(v: View) {
        onBackPressed()
    }

    override fun onResume() {
        updateBelowBar()
        super.onResume()
    }

    override fun onBackPressed() {
        if(sView.isIconified) {
            finish()
        } else {
            sView.setQuery("",true)
            sView.isIconified=true
        }
    }
    inner class AudioAdapterQ(val aList:ArrayList<AudioObjectQ>) : RecyclerView.Adapter<AudioAdapterQ.AudioHolder>() {

        val fullList=ArrayList<AudioObjectQ>().apply {
            addAll(aList)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioHolder {
            return AudioHolder(layoutInflater.inflate(R.layout.audio_item_layout,parent,false))
        }

        override fun getItemCount(): Int = aList.size

        @RequiresApi(Build.VERSION_CODES.Q)
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: AudioHolder, position: Int) {
            val audioItemLayout=holder.audioItemLayout
            val titleText=holder.titleText
            val infoText=holder.infoText
            val artistText=holder.artistText
            val albumArt=holder.albumArt
            val mediaPlay=holder.mediaPlay
            titleText.text=aList[position].title

            if(aList[position].isSelected) {
                audioItemLayout.background=resources.getDrawable(R.drawable.item_background_select)
                titleText.isSelected=true
                artistText.isSelected=true
            } else {
                audioItemLayout.background=resources.getDrawable(R.drawable.item_background)
                titleText.isSelected=false
                artistText.isSelected=false
            }
            audioItemLayout.setOnClickListener {
                if(aList[position].isSelected) {
                    aList[position].isSelected=false
                    audioItemLayout.background=resources.getDrawable(R.drawable.item_background)
                    titleText.isSelected=false
                    artistText.isSelected=false
                    selectedAudioFiles--
                    selectedSize-=aList[position].size
                    updateBelowBar()
                } else {
                    aList[position].isSelected=true
                    audioItemLayout.background=resources.getDrawable(R.drawable.item_background_select)
                    titleText.isSelected=true
                    artistText.isSelected=true
                    selectedAudioFiles++
                    selectedSize+=aList[position].size
                    updateBelowBar()
                }
            }

            if(aList[position].artist.contains("unknown")) {
                artistText.visibility= View.GONE
            } else {
                artistText.visibility= View.VISIBLE
                artistText.text=aList[position].artist
            }

            GlobalScope.launch (Dispatchers.Main) {
                val bMap = loadThumbnail(aList[position].albumArt).await()
                albumArt.setImageBitmap(bMap)
            }
            infoText.text="${Connection.formatDuration(aList[position].duration)}  ${Connection.formatDataString(aList[position].size,' ')}"

            mediaPlay.setOnClickListener {
                val i= Intent(applicationContext,AudioPlayerQ::class.java)
                i.putExtra("title",aList[position].title)
                i.putExtra("artist",aList[position].artist)
                i.putExtra("albumArt",aList[position].albumArt.toString())
                i.putExtra("uri",aList[position].uri.toString())
                i.putExtra("duration",aList[position].duration)
                startActivity(i)
            }
        }

        inner class AudioHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val audioItemLayout: LinearLayout =itemView.findViewById(R.id.audio_item_layout)
            val titleText: TextView =itemView.findViewById(R.id.media_title)
            val infoText: TextView =itemView.findViewById(R.id.media_info)
            val artistText: TextView =itemView.findViewById(R.id.media_artist)
            val albumArt: ImageView =itemView.findViewById(R.id.album_art)
            val mediaPlay: Button =itemView.findViewById(R.id.media_play_button)
        }

        val filter=object: Filter() {

            override fun performFiltering(p0: CharSequence?): FilterResults {
                val filteredList=ArrayList<AudioObjectQ>()
                val queryText=p0.toString().toLowerCase()
                for(i in fullList) {
                    if(i.title.toLowerCase().trim().contains(queryText) || i.artist.toLowerCase().trim().contains(queryText)) {
                        filteredList.add(i)
                    }
                }
                val filterResults= FilterResults()
                filterResults.values=filteredList
                return filterResults
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                aList.clear()
                aList.addAll(p1?.values as ArrayList<AudioObjectQ>)
                if(aList.size==0)
                    binding.noMediaFoundText.visibility= View.VISIBLE
                else
                    binding.noMediaFoundText.visibility= View.GONE
                notifyDataSetChanged()
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun loadThumbnail(albumArt: Uri) : Deferred<Bitmap> =
        coroutineScope {
            async (Dispatchers.IO) {
                return@async try {
                    contentResolver.loadThumbnail(albumArt, Size(100,100), null)
                } catch (e: Exception) {
                    BitmapFactory.decodeResource(resources, R.drawable.music_icon_small)
                }
            }
        }

    inner class AudioObjectQ(val uri:Uri, val name:String, val title:String, val artist:String, val duration:Int,val albumArt:Uri,val size:Long, var isSelected:Boolean)
}