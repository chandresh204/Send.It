package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chad.orionsoft.sendit.databinding.ActivitySendExBinding
import java.io.File

class SendActivityEX : AppCompatActivity() {

    private val fileListMain = ArrayList<FileToSend>()
    private val uriListMain = ArrayList<UriToSend>()
    private val displayList = ArrayList<DisplayObject>()
    private var totalSize = 0L
    private var sendMode = URI_MODE

    private lateinit var binding: ActivitySendExBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendExBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (!Connection.connection) {
            val i = Intent(applicationContext, ConnectionCheck::class.java)
            i.putExtra(ConnectionCheck.MODE_INTENT_STRING, ConnectionCheck.SENDER_MODE)
            startActivity(i)
        }
        if (intent.clipData != null) {
            val cData = intent.clipData
            var info = ""
            for (i in 0 until cData!!.itemCount) {
                val data = cData.getItemAt(i)
                val uri = data.uri
                info += "$uri\n"
                if (uri.toString().contains("content:/")) {

                    val cur = contentResolver.query(uri, null, null,null,null)
                    if (cur != null && cur.moveToFirst()) {
                        val titleIndex = cur.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                        val sizeIndex = cur.getColumnIndex(MediaStore.Images.Media.SIZE)
                        val title = cur.getString(titleIndex)
                        val size = cur.getLong(sizeIndex)
                        uriListMain.add(UriToSend(title, uri, size))
                    }
                    cur?.close()
                    sendMode = URI_MODE
                }
                if (uri!!.toString().contains("file:/")) {
                    val path = uri.path
                    val lastIndex = path!!.lastIndexOf("/")
                    val title = path.substring(lastIndex + 1)
                    info += "filename :$title\npath: $path\n\n"
                    fileListMain.add(FileToSend(title,path))
                    sendMode = FILE_MODE
                }
            }
            binding.exRecycler.layoutManager = LinearLayoutManager(applicationContext)
            binding.exRecycler.recycledViewPool.setMaxRecycledViews(0,0)
            if (sendMode == FILE_MODE) {
                for (file in fileListMain) {
                    displayList.add(DisplayObject(file.title, file.path, file.filesize))
                }
                binding.exRecycler.adapter = FileListAdapter(displayList)
            }
            if (sendMode == URI_MODE) {
                for (uriO in uriListMain) {
                    displayList.add(DisplayObject(uriO.title, uriO.uri.toString(), uriO.size))
                }
                binding.exRecycler.adapter = FileListAdapter(displayList)
            }
        }
        binding.exBelowBar.isSelected = true
        updateBelowBar()
    }

    fun sendNow(v:View) {
        v.id
        if(totalSize > Connection.freeSpace) {
            Toast.makeText(applicationContext,"Receiver does not have that much space, see the red banner below",Toast.LENGTH_SHORT).show()
            return
        }
        if (sendMode == FILE_MODE) {
            val sending = ArrayList<SendObject>()
            for ( file in fileListMain) {
                val name = file.title
                val path =file.path
                sending.add(SendObject(name,path))
            }
            StaticList.sendList = sending
            startActivity(Intent(applicationContext,SendNow::class.java))
            finish()
            return
        }
        if (sendMode == URI_MODE) {
            val sending = ArrayList<SendObjectQ>()
            for (uriO in uriListMain) {
                sending.add(SendObjectQ(uriO.title, uriO.uri, uriO.size))
            }
            StaticList.sendListQ = sending
            startActivity(Intent(applicationContext, SendNowQ::class.java))
            finish()
            return
        }

    }

    override fun onResume() {
        updateBelowBar()
        super.onResume()
    }

    @SuppressLint("SetTextI18n")
    private fun updateBelowBar() {
        totalSize = 0L
        for (item in displayList) {
            totalSize+=item.size
        }
        if(Connection.connection) {
            if(totalSize > Connection.freeSpace) {
                binding.exBelowBar.setBackgroundColor(Color.RED)
            } else {
                binding.exBelowBar.setBackgroundColor(
                    ResourcesCompat.getColor(this@SendActivityEX.resources,
                        R.color.VioletPrimary,
                        this@SendActivityEX.theme))
            }
            binding.exBelowBar.text = "Sending to :${Connection.partnerName}, Total ${displayList.size}: ${Connection.formatDataString(totalSize,' ')}, " +
                    "free: ${Connection.formatDataString(Connection.freeSpace,' ')}"
            return
        }
        binding.exBelowBar.text = "Total ${displayList.size}: ${Connection.formatDataString(totalSize,' ')}"
        return
    }

    inner class FileListAdapter(private val fileList : ArrayList<DisplayObject>) : RecyclerView.Adapter<FileListAdapter.FileListHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileListHolder {
            return FileListHolder(layoutInflater.inflate(R.layout.item_layout_file,parent,false))
        }

        override fun getItemCount(): Int = fileList.size

        @SuppressLint("NotifyDataSetChanged")
        override fun onBindViewHolder(holder: FileListHolder, position: Int) {
            val fileIcon= holder.fileIcon
            val fileNameText = holder.fileNameText
            val fileInfoText= holder.fileInfoText
            val fileSizeText = holder.fileSizeText
            val cancelButton =holder.cancelButton
            fileIcon.setImageDrawable(Connection.findICONSmall(applicationContext,fileList[position].title))
            fileNameText.text = fileList[position].title
            fileInfoText.text = fileList[position].info
            fileSizeText.text = Connection.formatDataString(fileList[position].size,' ')
            cancelButton.setImageDrawable(
                ResourcesCompat.getDrawable(
                    this@SendActivityEX.resources,
                    R.drawable.cancel_icon,
                    this@SendActivityEX.theme))
            cancelButton.setOnClickListener {
                if (sendMode == FILE_MODE) {
                    fileListMain.removeAt(position)
                } else {
                    uriListMain.removeAt(position)
                }
                displayList.removeAt(position)
                notifyDataSetChanged()
                updateBelowBar()
            }
        }

        inner class FileListHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val fileIcon: ImageView = itemView.findViewById(R.id.file_icon)
            val fileNameText:TextView = itemView.findViewById(R.id.file_title)
            val fileInfoText:TextView = itemView.findViewById(R.id.file_path)
            val fileSizeText:TextView = itemView.findViewById(R.id.file_prop)
            val cancelButton: ImageView = itemView.findViewById(R.id.file_open_button)
        }
    }

    inner class FileToSend(val title:String, val path:String) {
        val file = File(path)
        val filesize = file.length()
    }

    inner class UriToSend (val title:String, val uri: Uri, val size:Long)

    inner class DisplayObject(val title: String, val info: String, val size:Long)

    companion object {
        const val FILE_MODE = 0
        const val URI_MODE = 1
    }
}