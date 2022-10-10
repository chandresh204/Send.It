package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chad.orionsoft.sendit.databinding.ActivitySendAudioBinding
import java.io.File

class SendActivityAudio : AppCompatActivity() {

    private val audioList=ArrayList<AudioObject>()
    private lateinit var aAdapter:AudioAdapter
    private var selectedAudioFiles=0
    private var selectedSize:Long=0

    private val artMap=HashMap<Long,Bitmap>()
    private lateinit var sView: SearchView
    private lateinit var binding: ActivitySendAudioBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendAudioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        generateAudioList()
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

    private fun generateAudioList() {
        val audioURI= MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val albumURI=MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

        val aCursor= contentResolver.query(audioURI,null,null,null,null)
        val alCursor=contentResolver.query(albumURI,null,null,null,null)

        if(aCursor!=null && aCursor.moveToFirst()) {

            val idIndex=aCursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val titleIndex=aCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistIndex=aCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val durationIndex=aCursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val pathIndex=aCursor.getColumnIndex(MediaStore.Audio.Media.DATA)

            val albumIdIndex1=aCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            val albumIdIndex2=alCursor!!.getColumnIndex(MediaStore.Audio.Albums._ID)
            val albumArtIndex=alCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)

            do {
                val id=aCursor.getLong(idIndex)
                val title=aCursor.getString(titleIndex)
                val artists=aCursor.getString(artistIndex)
                val duration=aCursor.getInt(durationIndex)
                val path=aCursor.getString(pathIndex)

                val albumId1=aCursor.getString(albumIdIndex1)
                var alCursorPosition=0

                if(alCursor.moveToFirst()) {
                    do {
                        val albumId2=alCursor.getString(albumIdIndex2)
                        if(albumId1==albumId2) {
                            alCursorPosition=alCursor.position
                            break
                        }
                    } while (alCursor.moveToNext())
                }

                alCursor.moveToPosition(alCursorPosition)
                val albumArt=alCursor.getString(albumArtIndex)

                audioList.add(AudioObject(id,title,artists,duration,path,albumArt,false))

            } while (aCursor.moveToNext())
        }
        aCursor?.close()
        alCursor?.close()
        audioList.sortBy { it.title.lowercase() }
        binding.audioRecyclerView.layoutManager= LinearLayoutManager(applicationContext)
        aAdapter=AudioAdapter(audioList)
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
            selectedSize+=i.fileSize
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

    fun sendAudioNow(v:View) {
        v.id
        if(selectedAudioFiles==0) {
            Toast.makeText(applicationContext,"Nothing selected",Toast.LENGTH_SHORT).show()
            return
        }
        if(!Connection.connection) {
            val i=Intent(applicationContext,ConnectionCheck::class.java)
            i.putExtra(ConnectionCheck.MODE_INTENT_STRING,ConnectionCheck.SENDER_MODE)
            startActivity(i)
            return
        }
        if(selectedSize > Connection.freeSpace) {
            Toast.makeText(applicationContext,resources.getString(R.string.no_space_error),Toast.LENGTH_SHORT).show()
            return
        }
        val sendList=ArrayList<SendObject>()
        for(i in audioList) {
            if(i.isSelected) {
                sendList.add(SendObject(i.audioFile.name,i.path))
            }
        }
        StaticList.sendList=sendList
        startActivity(Intent(applicationContext,SendNow::class.java))
        clearAll()
    }

    fun showPopUpMenu(v:View) {
        val themeCtx= ContextThemeWrapper(applicationContext,R.style.SenderTheme_NoActionBar)
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

    fun goBack(v:View) {
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
    inner class AudioAdapter(val aList:ArrayList<AudioObject>) : RecyclerView.Adapter<AudioAdapter.AudioHolder>() {

        val fullList=ArrayList<AudioObject>().apply {
            addAll(aList)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioHolder {
            return AudioHolder(layoutInflater.inflate(R.layout.audio_item_layout,parent,false))
        }

        override fun getItemCount(): Int = aList.size

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
                    selectedSize-=aList[position].fileSize
                    updateBelowBar()
                } else {
                    aList[position].isSelected=true
                    audioItemLayout.background=resources.getDrawable(R.drawable.item_background_select)
                    titleText.isSelected=true
                    artistText.isSelected=true
                    selectedAudioFiles++
                    selectedSize+=aList[position].fileSize
                    updateBelowBar()
                }
            }

            if(aList[position].artist.contains("unknown")) {
                artistText.visibility= View.GONE
            } else {
                artistText.visibility=View.VISIBLE
                artistText.text=aList[position].artist
            }
            albumArt.setImageDrawable(resources.getDrawable(R.drawable.music_icon_small))
            if(aList[position].albumArt!=null) {
                if(artMap[aList[position].id] !=null) {
                    albumArt.setImageBitmap(artMap[aList[position].id])
                } else {
                    val artBitmap= BitmapFactory.decodeFile(aList[position].albumArt)
                    val bitmap= Bitmap.createScaledBitmap(artBitmap,100,100,false)
                    albumArt.setImageBitmap(bitmap)
                    artMap[aList[position].id] = bitmap
                }
            }
            infoText.text="${Connection.formatDuration(aList[position].duration)}  ${Connection.formatDataString(aList[position].fileSize,' ')}"

            mediaPlay.setOnClickListener {
                val i= Intent(applicationContext,AudioPlayer::class.java)
                i.putExtra("title",aList[position].title)
                i.putExtra("artist",aList[position].artist)
                i.putExtra("albumArt",aList[position].albumArt)
                i.putExtra("path",aList[position].path)
                i.putExtra("duration",aList[position].duration)
                startActivity(i)
            }
        }

        inner class AudioHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val audioItemLayout:LinearLayout=itemView.findViewById(R.id.audio_item_layout)
            val titleText:TextView=itemView.findViewById(R.id.media_title)
            val infoText:TextView=itemView.findViewById(R.id.media_info)
            val artistText:TextView=itemView.findViewById(R.id.media_artist)
            val albumArt:ImageView=itemView.findViewById(R.id.album_art)
            val mediaPlay:Button=itemView.findViewById(R.id.media_play_button)
        }

        val filter=object: Filter() {

            override fun performFiltering(p0: CharSequence?): FilterResults {
                val filteredList=ArrayList<AudioObject>()
                val queryText=p0.toString().toLowerCase()
                for(i in fullList) {
                    if(i.title.toLowerCase().trim().contains(queryText) || i.artist.toLowerCase().trim().contains(queryText)) {
                        filteredList.add(i)
                    }
                }
                val filterResults=FilterResults()
                filterResults.values=filteredList
                return filterResults
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                aList.clear()
                aList.addAll(p1?.values as ArrayList<AudioObject>)
                if(aList.size==0)
                    binding.noMediaFoundText.visibility=View.VISIBLE
                else
                    binding.noMediaFoundText.visibility=View.GONE
                notifyDataSetChanged()
            }

        }
    }

    inner class AudioObject(val id:Long,val title:String, val artist:String,val duration:Int,val path:String,val albumArt:String?,var isSelected:Boolean) {

        val audioFile= File(path)
        val fileSize=audioFile.length()

    }
}
