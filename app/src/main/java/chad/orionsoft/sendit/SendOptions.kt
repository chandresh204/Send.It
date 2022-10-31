package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chad.orionsoft.sendit.databinding.ActivitySendOptionsBinding
import kotlinx.coroutines.*
import java.io.File
import java.net.Socket

class SendOptions : AppCompatActivity() {

    private val optionList=ArrayList<SendOption>()
    private lateinit var binding: ActivitySendOptionsBinding
    private lateinit var mainScope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendOptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mainScope = CoroutineScope(Dispatchers.Main)
        generateOptions()
        binding.sendOptionsBelowBar.isSelected=true
    }

    private fun generateOptions() {
     /*   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Connection.isScoped) {
                optionList.add(SendOption("IMAGES",resources.getDrawable(R.drawable.image_icon,resources.newTheme())))
                optionList.add(SendOption("AUDIO",resources.getDrawable(R.drawable.music_icon,resources.newTheme())))
                optionList.add(SendOption("VIDEO",resources.getDrawable(R.drawable.video_icon,resources.newTheme())))
                optionList.add(SendOption("APK",resources.getDrawable(R.drawable.apk_icon,resources.newTheme())))
                optionList.add(SendOption("SELECT", resources.getDrawable(R.drawable.open_storage, resources.newTheme())))
                optionList.add(SendOption("RECEIVE",resources.getDrawable(R.drawable.receive_icon_small,resources.newTheme())))
            } else {
                optionList.add(SendOption("IMAGES",resources.getDrawable(R.drawable.image_icon,resources.newTheme())))
                optionList.add(SendOption("AUDIO",resources.getDrawable(R.drawable.music_icon,resources.newTheme())))
                optionList.add(SendOption("VIDEO",resources.getDrawable(R.drawable.video_icon,resources.newTheme())))
                optionList.add(SendOption("APK",resources.getDrawable(R.drawable.apk_icon,resources.newTheme())))
                optionList.add(SendOption("OFFICE",resources.getDrawable(R.drawable.doc_icon,resources.newTheme())))
                optionList.add(SendOption("PDF",resources.getDrawable(R.drawable.pdf_icon,resources.newTheme())))
                optionList.add(SendOption("SEARCH",resources.getDrawable(R.drawable.search_icon,resources.newTheme())))
                optionList.add(SendOption("SELECT", resources.getDrawable(R.drawable.open_storage, resources.newTheme())))
                optionList.add(SendOption("RECEIVE",resources.getDrawable(R.drawable.receive_icon_small,resources.newTheme())))
            }
        } else {
            optionList.add(SendOption("IMAGES",resources.getDrawable(R.drawable.image_icon)))
            optionList.add(SendOption("AUDIO",resources.getDrawable(R.drawable.music_icon)))
            optionList.add(SendOption("VIDEO",resources.getDrawable(R.drawable.video_icon)))
            optionList.add(SendOption("APK",resources.getDrawable(R.drawable.apk_icon)))
            optionList.add(SendOption("OFFICE",resources.getDrawable(R.drawable.doc_icon)))
            optionList.add(SendOption("PDF",resources.getDrawable(R.drawable.pdf_icon)))
            optionList.add(SendOption("SEARCH",resources.getDrawable(R.drawable.search_icon)))
            optionList.add(SendOption("SELECT", resources.getDrawable(R.drawable.open_storage)))
            optionList.add(SendOption("RECEIVE",resources.getDrawable(R.drawable.receive_icon_small)))
        }  */

        if (Connection.isScoped) {
            optionList.add(SendOption("IMAGES",getDrawableResource(R.drawable.image_icon)))
            optionList.add(SendOption("AUDIO",getDrawableResource(R.drawable.music_icon)))
            optionList.add(SendOption("VIDEO",getDrawableResource(R.drawable.video_icon)))
            optionList.add(SendOption("APK",getDrawableResource(R.drawable.apk_icon)))
            optionList.add(SendOption("SELECT", getDrawableResource(R.drawable.open_storage)))
            optionList.add(SendOption("RECEIVE", getDrawableResource(R.drawable.receive_icon_small)))
        } else {
            optionList.add(SendOption("IMAGES", getDrawableResource(R.drawable.image_icon)))
            optionList.add(SendOption("AUDIO", getDrawableResource(R.drawable.music_icon)))
            optionList.add(SendOption("VIDEO", getDrawableResource(R.drawable.video_icon)))
            optionList.add(SendOption("APK", getDrawableResource(R.drawable.apk_icon)))
            optionList.add(SendOption("OFFICE", getDrawableResource(R.drawable.doc_icon)))
            optionList.add(SendOption("PDF", getDrawableResource(R.drawable.pdf_icon)))
            optionList.add(SendOption("SEARCH", getDrawableResource(R.drawable.search_icon)))
            optionList.add(SendOption("SELECT", getDrawableResource(R.drawable.open_storage)))
            optionList.add(SendOption("RECEIVE", getDrawableResource(R.drawable.receive_icon_small)))
        }


        binding.sendOptionRecyclerView.layoutManager=GridLayoutManager(applicationContext,3)
        binding.sendOptionRecyclerView.adapter=OAdapter(optionList)
    }

    private fun getDrawableResource(resId: Int): Drawable {
        return ResourcesCompat.getDrawable(resources, resId, theme)
            ?: return ResourcesCompat.getDrawable(
                resources,
                R.drawable.sendit_icon_new_small,
                theme
            )!!
    }

    inner class OAdapter(private val oList:ArrayList<SendOption>) : RecyclerView.Adapter<OAdapter.OHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OHolder {
            return OHolder(layoutInflater.inflate(R.layout.send_option_layout,parent,false))
        }

        override fun getItemCount(): Int = oList.size

        override fun onBindViewHolder(holder: OHolder, position: Int) {
            val optionIcon=holder.optionIcon
            val optionText=holder.optionText
            optionIcon.setImageDrawable(oList[position].icon)
            optionText.text=oList[position].title
            optionIcon.setOnClickListener {
                when(oList[position].title) {
                    "IMAGES" -> {
                        if (Connection.isScoped) {
                            startActivity(Intent(applicationContext,SendActivityImagesQ::class.java))
                        } else {
                            startActivity(Intent(applicationContext, SendActivityImages::class.java))
                        }
                    }
                    "AUDIO" -> {
                        if (Connection.isScoped) {
                            startActivity(Intent(applicationContext,SendActivityAudioQ::class.java))
                        } else {
                            startActivity(Intent(applicationContext,SendActivityAudio::class.java))
                        }
                    }
                    "VIDEO" -> {
                        if (Connection.isScoped) {
                            startActivity(Intent(applicationContext,SendActivityVideoQ::class.java))
                        } else {
                            startActivity(Intent(applicationContext,SendActivityVideo::class.java))
                        }
                    }
                    "APK" -> {
                        startActivity(Intent(applicationContext,SendActivityAPK::class.java))
                    }
                    "OFFICE" -> {
                        startActivity(Intent(applicationContext,SendActivityOffice::class.java))
                    }
                    "PDF" -> {
                        startActivity(Intent(applicationContext,SendActivityPDF::class.java))
                    }
                    "SELECT" -> {
                        startActivity(Intent(applicationContext,SendActivityStorage::class.java))
                    }
                    "SEARCH" -> {
                        startActivity(Intent(applicationContext,SendActivitySearch::class.java))
                    }
                    "RECEIVE" -> {
                        if(!Connection.connection) {
                            Toast.makeText(applicationContext,"you're not connected",Toast.LENGTH_SHORT).show()
                            startActivity(Intent(applicationContext,MainActivity::class.java))
                            finish()
                            return@setOnClickListener
                        }
                        val builder = AlertDialog.Builder(this@SendOptions)
                        builder.setMessage("Are you really want to switch?")
                        builder.setNegativeButton("NO") { _, _ ->
                            //Do nothing
                        }
                        builder.setPositiveButton("YES") { _, _ ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val sm = getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                                val myFreeSpace = sm.getFreeBytes(StorageManager.UUID_DEFAULT)
                                val interchangeMsg = "${Connection.APP_CODE}/*${Connection.MODE_INTERCHANGE}*/$myFreeSpace#"
                                mainScope.launch {
                                    val resp = sendMessageAsync(interchangeMsg).await()
                                    Toast.makeText(applicationContext,resp,Toast.LENGTH_SHORT).show()
                                    if(resp==Connection.RECEIVER_OK) {
                                        Connection.freeSpace = sm.getFreeBytes(StorageManager.UUID_DEFAULT)
                                        startActivity(Intent(applicationContext,ReceiveNowQ::class.java))
                                        finish()
                                    }
                                }
                            } else {
                                val myFreeSpace = File(Environment.getExternalStorageDirectory(),"").freeSpace
                                val interchangeMsg="${Connection.APP_CODE}/*${Connection.MODE_INTERCHANGE}*/$myFreeSpace#"
                                mainScope.launch {
                                    val resp = sendMessageAsync(interchangeMsg).await()
                                    Toast.makeText(applicationContext,resp,Toast.LENGTH_SHORT).show()
                                    if(resp==Connection.RECEIVER_OK) {
                                        Connection.freeSpace = File(Environment.getExternalStorageDirectory(),"").freeSpace
                                        startActivity(Intent(applicationContext,ReceiveNow::class.java))
                                        finish()
                                    }
                                }
                            }
                        }
                        val aDialog = builder.create()
                        aDialog.setTitle("Start Receiving Mode now?")
                        aDialog.show()
                    }
                }
            }
        }

        inner class OHolder(itemView:View) : RecyclerView.ViewHolder(itemView) {
            val optionIcon:ImageView=itemView.findViewById(R.id.option_icon)
            val optionText:TextView=itemView.findViewById(R.id.option_text)
        }

    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        if(Connection.connection) {
            binding.sendOptionsBelowBar.text =
                "sending to : ${Connection.partnerName} , ${Connection.partnerAddress}"
        } else {
            binding.sendOptionsBelowBar.text =
                "Not connected to receiver"
        }
    }

    override fun onBackPressed() {
        confirmExitDialog()
    }

    private fun confirmExitDialog() {
        val builder = AlertDialog.Builder(this@SendOptions)
        builder.apply {
            setMessage("Are you sure you want to stop sending?")
            setNegativeButton("NO") { _, _ ->
                // do nothing
            }
            setPositiveButton("YES") { _, _ ->
                if(!Connection.connection) {
                    finishAffinity()
                    return@setPositiveButton
                }
                mainScope.launch {
                    val msg="${Connection.APP_CODE}/*${Connection.MODE_STOP}*/"
                    val res =sendMessageAsync(msg).await()
                    if(res==Connection.RECEIVER_OK) {
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                }
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private suspend fun sendMessageAsync(msg:String) : Deferred<String> =
        coroutineScope {
            async (Dispatchers.IO){
                val msgBytes = msg.toByteArray()
                val socket = Socket(Connection.partnerAddress,Connection.receiverPort)
                Connection.mode = Connection.MODE_SENDER
                val sendStream=socket.getOutputStream()
                val receiveStream=socket.getInputStream()
                sendStream.write(msgBytes,0,msgBytes.size)
                while(receiveStream.available()==0) ;
                val buff=ByteArray(50)
                val read=receiveStream.read(buff)
                return@async String(buff,0,read)
            }
        }

    inner class SendOption(val title:String,val icon: Drawable)
}
