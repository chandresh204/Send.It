package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chad.orionsoft.sendit.databinding.ActivitySendStorageBinding
import java.lang.Exception

class SendActivityStorage : AppCompatActivity() {

    private val fList = ArrayList<FileObjectQ>()
    private lateinit var fAdapter: FileAdapter
    private var selectedFiles = 0
    private var selectedSize = 0L

    private lateinit var binding: ActivitySendStorageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendStorageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.storageRecycler.layoutManager = LinearLayoutManager(applicationContext)
        binding.storageRecycler.recycledViewPool.setMaxRecycledViews(0,0)

        val activityLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val dataInt = result.data

                val resolver = contentResolver

                if (dataInt?.clipData != null) {
                    for (i in 0 until dataInt.clipData!!.itemCount) {
                        val clipItem = dataInt.clipData!!.getItemAt(i)
                        val uri = clipItem.uri
                        val cur = resolver.query(uri, null, null, null, null)
                        if (cur!!.moveToFirst()) {
                            val titleIndex = cur.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                            val sizeIndex = cur.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                            val title = cur.getString(titleIndex)
                            val size = cur.getString(sizeIndex)
                            fList.add(FileObjectQ(title, uri, size.toLong(), true))
                            selectedFiles ++
                            selectedSize += size.toLong()
                        }
                    }
                    updateBelowBar()
                    fAdapter = FileAdapter(fList)
                    binding.storageRecycler.adapter = fAdapter
                    return@registerForActivityResult
                }
                val cur = resolver.query(dataInt?.data!!, null, null, null, null)
                if (cur!!.moveToFirst()) {
                    val titleIndex = cur.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val sizeIndex = cur.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                    val title = cur.getString(titleIndex)
                    val size = cur.getString(sizeIndex)
                    fList.add(FileObjectQ(title, dataInt.data!!, size.toLong(), true))
                    selectedFiles ++
                    selectedSize += size.toLong()
                    updateBelowBar()
                }
                fAdapter = FileAdapter(fList)
                binding.storageRecycler.adapter = fAdapter
            }
        }

        binding.openFileButton.setOnClickListener {
            val fileChooser = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            activityLauncher.launch(Intent.createChooser(fileChooser, "Select files"))
        }

    }

    fun startSending(v:View) {
        v.id
        if (!Connection.connection) {
            val i = Intent(applicationContext, ConnectionCheck::class.java)
            i.putExtra(ConnectionCheck.MODE_INTENT_STRING, ConnectionCheck.SENDER_MODE)
            startActivity(i)
            return
        }
        if (selectedSize > Connection.freeSpace) {
            Toast.makeText(applicationContext,resources.getString(R.string.no_space_error),Toast.LENGTH_SHORT).show()
            return
        }
        val sentItems = ArrayList<SendObjectQ>()
        for (file in fList) {
            if (file.isSelected) {
                sentItems.add(SendObjectQ(file.fileName, file.uri, file.size))
            }
        }
        StaticList.sendListQ = sentItems
        startActivity(Intent(applicationContext,SendNowQ::class.java))
        fList.clear()
        selectedFiles = 0
        selectedSize = 0L
        fAdapter.notifyDataSetChanged()
    }

    @TargetApi(Build.VERSION_CODES.M)
    @SuppressLint("SetTextI18n")
    private fun updateBelowBar() {
        var freeSpaceStr =""
        if(Connection.connection) {
            freeSpaceStr = " free : ${Connection.formatDataString(Connection.freeSpace,' ')}"
        }
        binding.storageBelowBarText.text = "Selected $selectedFiles : ${Connection.formatDataString(selectedSize, ' ')}," +
                freeSpaceStr
        if(selectedSize > Connection.freeSpace) {
            binding.storageBelowBarText.setBackgroundColor(Color.RED)
        } else {
            binding.storageBelowBarText.setBackgroundColor(resources.getColor(R.color.VioletPrimary,resources.newTheme()))
        }
        if (selectedFiles==0) {
            binding.sendStorageSelected.visibility = View.GONE
        } else {
            binding.sendStorageSelected.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        updateBelowBar()
        super.onResume()
    }

    inner class FileAdapter(val fList:ArrayList<FileObjectQ>) : RecyclerView.Adapter<FileAdapter.FileHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileHolder {
            return FileHolder(layoutInflater.inflate(R.layout.item_layout_file, parent, false))
        }

        override fun getItemCount(): Int = fList.size

        override fun onBindViewHolder(holder: FileHolder, position: Int) {
            val fObject = fList[position]
            val itemlayout = holder.itemLayout
            val iconImageView = holder.iconImageView
            val titleTextView = holder.titleTextView
            val uriTextView = holder.uriTextView
            val sizeTextView = holder.sizeTextView
            val viewImage = holder.viewImage
            titleTextView.text = fObject.fileName
            uriTextView.text = fObject.uri.toString()
            sizeTextView.text = Connection.formatDataString(fObject.size, ' ')
            iconImageView.setImageDrawable(Connection.findICONSmall(applicationContext,fObject.fileName))
            if (fObject.isSelected) {
                itemlayout.background = resources.getDrawable(R.drawable.item_background_select)
            } else {
                itemlayout.background = resources.getDrawable(R.drawable.item_background)
            }
            itemlayout.setOnClickListener {
                if(fObject.isSelected) {
                    fObject.isSelected = false
                    itemlayout.background = resources.getDrawable(R.drawable.item_background)
                    selectedFiles --
                    selectedSize -= fObject.size
                    updateBelowBar()
                } else {
                    fObject.isSelected = true
                    itemlayout.background = resources.getDrawable(R.drawable.item_background_select)
                    selectedFiles ++
                    selectedSize += fObject.size
                    updateBelowBar()
                }
            }
            viewImage.setOnClickListener {
                val uri= fObject.uri
                val mime=contentResolver.getType(uri)
                val i= Intent().apply {
                    setDataAndType(uri,mime)
                    action = Intent.ACTION_VIEW
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    startActivity(i)
                } catch (e:Exception) {
                    Toast.makeText(applicationContext,"Can't open this file",Toast.LENGTH_SHORT).show()
                }
            }
        }

        inner class FileHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
            val itemLayout:LinearLayout = itemView.findViewById(R.id.file_bar_layout)
            val iconImageView:ImageView = itemView.findViewById(R.id.file_icon)
            val titleTextView:TextView = itemView.findViewById(R.id.file_title)
            val uriTextView:TextView = itemView.findViewById(R.id.file_path)
            val sizeTextView:TextView = itemView.findViewById(R.id.file_prop)
            val viewImage:ImageView = itemView.findViewById(R.id.file_open_button)
        }

    }

    class FileObjectQ(val fileName:String, val uri: Uri, val size:Long, var isSelected:Boolean)
}
