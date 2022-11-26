package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chad.orionsoft.sendit.databinding.ActivityReceiveNewBinding
import chad.orionsoft.sendit.databinding.ContentReceiveActivityNewBinding
import chad.orionsoft.sendit.databinding.ItemSendingLayoutBinding
import kotlinx.coroutines.*
import java.io.File
import java.lang.Runnable
import java.net.ServerSocket
import java.net.Socket
import kotlin.collections.ArrayList

class ReceiveNow : AppCompatActivity() {

    private val receiverPort=4444
    private var receivedItemsCount=0
    private var keepReceiving=false
    private var totalReceivedBytes:Long=0
    private var currentFileSize:Long=0
    private var operations=""
    private var receivedBytesFromHalfSecond:Long=0
    private var operationTime=0f
    private var itemArray=ArrayList<ReceivedObject>()

    private lateinit var appDir:File
    private lateinit var iAdapter:ItemAdapter
    private lateinit var onTouchListener: View.OnTouchListener
    private lateinit var operationHandler: Handler
    private lateinit var layoutChangeHandler:Handler
    private lateinit var filenameHandler:Handler
    private lateinit var statusHandler:Handler
    private lateinit var timeAndSpeedCountHandler:Handler
    private lateinit var timeAndSpeedRunnable:Runnable
    private lateinit var addItemInRecyclerView:Handler

    private lateinit var binding : ActivityReceiveNewBinding
    private lateinit var binding2 : ContentReceiveActivityNewBinding
    private lateinit var binding3 : ItemSendingLayoutBinding
    private lateinit var mainScope: CoroutineScope

    //for notification
    private lateinit var nManager : NotificationManager
    private var notificationId = 10
    private var notificationFileName = "Receiving"

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiveNewBinding.inflate(layoutInflater)
        binding2 = binding.contentReceiverActivity
        setContentView(binding.root)
        mainScope = CoroutineScope(Dispatchers.Main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setSupportActionBar(binding.toolbar)
        appDir= File(Environment.getExternalStorageDirectory(),"Send_it")
        if(!appDir.exists())
            appDir.mkdirs()
        binding2.receiverBelowBarText.isSelected = true
        binding2.receiverBelowBarText.text=
                "receiving from :${Connection.partnerName} , free: ${Connection.formatDataString(appDir.freeSpace,' ')}"
        initializeHandler()
        createNotificationChannel()
        mainScope.launch(Dispatchers.Main + parentJob) {
            val operation=receiveOnTCPAsync().await()
            operationHandler.obtainMessage(0,operation).sendToTarget()
            if(operation.contains("error")) {
                layoutChangeHandler.obtainMessage(0, ERROR_LAYOUT).sendToTarget()
                return@launch
            }
            if(operation.contains(Connection.MODE_INTERCHANGE)) {
                Toast.makeText(applicationContext,"Sender has switched position, now you can send..",Toast.LENGTH_LONG).show()
                startActivity(Intent(applicationContext,SendOptions::class.java))
                finish()
                return@launch
            }
            if(operation.contains(Connection.MODE_STOP)) {
                Toast.makeText(applicationContext,"Sender left, you can exit now....",Toast.LENGTH_SHORT).show()
            }
        }

        var currentY=0f

        onTouchListener=View.OnTouchListener { view, motionEvent ->

            when(motionEvent.action) {

                MotionEvent.ACTION_DOWN -> {
                    currentY=motionEvent.rawY
                }

                MotionEvent.ACTION_MOVE -> {
                    val translation=motionEvent.rawY - currentY
                    if(translation>0)
                        view.translationY=translation
                }

                MotionEvent.ACTION_UP -> {
                    val lastTranslation=view.translationY
                    view.translationY=0f
                    if(lastTranslation>250) {
                        view.visibility=View.GONE
                    } else {
                        TranslateAnimation(0f,0f,lastTranslation,0f).apply {
                            duration=200
                            view.startAnimation(this)

                        }
                    }
                }

            }
            true
        }

        binding2.receivedItemRecyclerView.layoutManager=LinearLayoutManager(applicationContext)
        binding2.receivedItemRecyclerView.recycledViewPool.setMaxRecycledViews(0,0)
        iAdapter=ItemAdapter(itemArray)
        binding2.receivedItemRecyclerView.adapter=iAdapter
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    private fun initializeHandler() {

        binding3 = binding2.itemReceivingLayout
        operationHandler= Handler(mainLooper) {
            val msg=it.obj as String
            operations+="\n$msg"
            binding2.receiverOperations.text=operations
            true
        }

        layoutChangeHandler = Handler(mainLooper) {
            when(it.obj as Int) {
                SHOW_LAYOUT -> {
                    binding3.root.visibility= View.VISIBLE
                    binding3.imageBackground.setBackgroundColor(Color.rgb(0x15,0xa9,0x62))
                    Animator.moveUP_IN(binding3.root,300)
                    binding3.sendingThumbnail.setOnClickListener(null)
                    binding3.sendingFileStatus.visibility=View.VISIBLE
                    binding3.fileSkippingButton.visibility=View.GONE
                    binding3.root.setOnTouchListener(null)
                }
                FINISH_LAYOUT -> {
                    binding3.root.setOnTouchListener(onTouchListener)
                    timeAndSpeedCountHandler.removeCallbacks(timeAndSpeedRunnable)
                    binding3.sendingFileStatus.visibility=View.GONE
                 /*   val doneDrawable=  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        resources.getDrawable(R.drawable.finished_icon,resources.newTheme())
                    } else {
                        resources.getDrawable(R.drawable.finished_icon)
                    }  */
                    val doneDrawable= ResourcesCompat.getDrawable(this@ReceiveNow.resources, R.drawable.finished_icon,
                        this@ReceiveNow.resources.newTheme())
                    Animator.flipDrawable(binding3.sendingThumbnail,doneDrawable,200)
                    binding3.sendingThumbnail.setOnClickListener {
                        Animator.moveDown_OUT(binding3.root,300)
                        operationHandler.postDelayed({
                            binding3.root.visibility=View.GONE
                        },300)
                    }
                    binding3.sendingTime.text="$operationTime s\n${Connection.formatDataString(totalReceivedBytes,'\n')}"
                    binding3.sendingFilename.text="Total Item received: $receivedItemsCount"
                    binding3.sendingSpeed.text="average\n ${Connection.formatDataString((totalReceivedBytes/operationTime).toLong(),' ')}ps"
                    binding2.receiverBelowBarText.text=
                        "receiving from :${Connection.partnerName} , free: ${Connection.formatDataString(appDir.freeSpace,' ')}"
                }
                ERROR_LAYOUT -> {
                    timeAndSpeedCountHandler.removeCallbacks(timeAndSpeedRunnable)
                    binding3.sendingFileStatus.visibility=View.GONE
                    binding3.root.setBackgroundColor(Color.RED)
                    binding3.sendingFilename.text="An error occured. receiver will terminate in 5 seconds"
                    statusHandler.postDelayed({
                   //     android.os.Process.killProcess(android.os.Process.myPid())
                    },5000)
                }
            }
            true
        }

        filenameHandler=Handler(mainLooper) {
            val name=it.obj as String
            binding3.sendingFilename.text=name
            Animator.flipDrawable(
                binding3.sendingThumbnail, Connection.findICON(applicationContext,name),200)
            notificationFileName = name
            notificationId++
            true
        }

        statusHandler=Handler(mainLooper) {
            val received=it.obj as Long
            val progressText = "${Connection.formatDataString(received,' ')} / ${Connection.formatDataString(currentFileSize,' ')}"
            binding3.sendingFileStatus.text= progressText
            val progress=(received*100/currentFileSize).toInt()
            binding3.sendingProgress.progress=progress
            updateNotification(progress, progressText)
            true
        }

        addItemInRecyclerView=Handler(mainLooper) {
         //   val file=it.obj as File
        //    itemArray.add(ReceivedObject(file,false))
            binding2.startupText.visibility=View.GONE
            iAdapter.notifyDataSetChanged()
            binding2.receivedItemRecyclerView.smoothScrollToPosition(iAdapter.itemCount-1)
            true
        }

        timeAndSpeedCountHandler= Handler(mainLooper)
        timeAndSpeedRunnable=object: Runnable {

            override fun run() {
                val diff=(totalReceivedBytes-receivedBytesFromHalfSecond)*2
                binding3.sendingSpeed.text="${Connection.formatDataString(diff,'\n')}ps"
                binding3.sendingTime.text="$operationTime s\n${Connection.formatDataString(totalReceivedBytes,'\n')}"
                receivedBytesFromHalfSecond=totalReceivedBytes
                operationTime+=0.5f
                timeAndSpeedCountHandler.postDelayed(this,500)
            }

        }

    }

    private fun provideFile(filename:String) : File {
        var exists=false
        val fList = appDir.list() ?: emptyArray()
        for(i in fList.indices) {
            if(filename==fList[i])
                exists=true
        }
        return if(exists) {
            val lastIndexOfName = filename.lastIndexOf(".")
            val file1= filename.substring(0,lastIndexOfName)
            val ext = filename.substring(lastIndexOfName)
            File(saveDir,"$file1(1)$ext")
            provideFile("$file1(1)$ext")
        } else {
            File(saveDir,filename)
        }
    }

    inner class ItemAdapter(private var items:ArrayList<ReceivedObject>) : RecyclerView.Adapter<ItemAdapter.ItemHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
            ItemHolder(layoutInflater.inflate(R.layout.item_layout_media,parent,false))

        override fun getItemCount(): Int = items.size

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            val itemName=holder.itemName
            val itemLayout=holder.itemLayout
            val itemIcon=holder.itemIcon
            val itemSize=holder.itemSize
         /*   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if(items[position].error) {
                    itemLayout.background =
                        resources.getDrawable(R.drawable.item_background_error, resources.newTheme())
                }
            } else {
                if(items[position].error) {
                    itemLayout.background = resources.getDrawable(R.drawable.item_background_error)
                }
            }  */
            if(items[position].error) {
                itemLayout.background =
                    ResourcesCompat.getDrawable(this@ReceiveNow.resources,
                        R.drawable.item_background_error, this@ReceiveNow.resources.newTheme())
            }
            if(position==items.lastIndex) {
                Animator.moveRight_IN(itemLayout,300)
            }
            itemName.text=items[position].itemName
            if(items[position].error) {
                if(!items[position].deleted) {
                    itemSize.text="unfinished - ${Connection.formatDataString(items[position].itemSize,' ')} , tap to delete"
                    itemLayout.setOnClickListener {
                        if(items[position].itemFile.delete()) {
                            itemSize.text="deleted"
                            items[position].deleted=true
                        } else {
                            itemSize.text="unable to delete"
                        }
                    }
                } else {
                    itemSize.text="deleted"
                    itemLayout.setOnClickListener(null)
                }
            } else {
                itemSize.text=Connection.formatDataString(items[position].itemSize,' ')
                itemLayout.setOnClickListener {
                    Toast.makeText(applicationContext,"open the file",Toast.LENGTH_SHORT).show()
                    val uri=FileProvider.getUriForFile(applicationContext, "$packageName.provider",items[position].itemFile)
                    val mime=contentResolver.getType(uri)
                    val i=Intent().apply {
                        setDataAndType(uri,mime)
                        action = Intent.ACTION_VIEW
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(i)
                }
            }
            itemIcon.setImageDrawable(Connection.findICONSmall(applicationContext,items[position].itemName))
        }

        inner class ItemHolder(itemView:View): RecyclerView.ViewHolder(itemView) {
            val itemName:TextView=itemView.findViewById(R.id.item_title)
            val itemLayout:LinearLayout=itemView.findViewById(R.id.item_bar_layout)
            val itemIcon:ImageView=itemView.findViewById(R.id.item_icon)
            val itemSize:TextView=itemView.findViewById(R.id.item_prop)
        }
    }

    private suspend fun receiveOnTCPAsync() : Deferred<String> =
            coroutineScope {
                async(Dispatchers.IO) {
                    var res=""
                    keepReceiving=true
                    try {
                        while(keepReceiving) {
                            //create server socket
                            val serverSocket=ServerSocket(receiverPort)
                            val receiveSocket=serverSocket.accept()
                            val recStream=receiveSocket.getInputStream()
                            val sendStream=receiveSocket.getOutputStream()

                            val receiveOKBytes= Connection.RECEIVER_OK.toByteArray()

                            operationHandler.obtainMessage(0,"Socket created").sendToTarget()

                            operationHandler.obtainMessage(0,"receiving data").sendToTarget()
                            while(recStream.available()==0) ;

                            //get receiving mode
                            val buff=ByteArray(50)
                            val buffSize=recStream.read(buff)
                            val modeString=String(buff,0,buffSize)
                            operationHandler.obtainMessage(0,"received:$modeString").sendToTarget()
                            if(modeString.contains("${Connection.APP_CODE}/*")) {
                                val codeSeparatorIndex=modeString.indexOf("/*")
                                val lastIndex=modeString.indexOf("*/")
                                val modeCode=modeString.substring(codeSeparatorIndex+2,lastIndex)
                                operationHandler.obtainMessage(0,"modeCode:$modeCode").sendToTarget()

                                if(modeCode.contains(Connection.MODE_FILES)) {
                                    val countSeparatorIndex=modeCode.indexOf("#")
                                    val count=Integer.parseInt(modeCode.substring(countSeparatorIndex+1))
                                    operationHandler.obtainMessage(0,"receiving:$count files").sendToTarget()
                                    sendStream.write(receiveOKBytes,0,receiveOKBytes.size)
                                    operationTime+=0.5f
                                    timeAndSpeedCountHandler.postDelayed(timeAndSpeedRunnable,500)
                                    val filesOperation=receiveFileOnTCP(receiveSocket,count)
                                    operationHandler.obtainMessage(0,"file operation completed: $filesOperation").sendToTarget()
                                    if(filesOperation.contains("error")) {
                                        layoutChangeHandler.obtainMessage(0, ERROR_LAYOUT).sendToTarget()
                                    }
                                }
                                if(modeCode.contains(Connection.MODE_INTERCHANGE)) {
                                    keepReceiving = false
                                    val freeSpaceStart = modeString.indexOf("*/")
                                    val freeSpaceEnd = modeString.indexOf("#")
                                    Connection.freeSpace = modeString.substring(freeSpaceStart+2,freeSpaceEnd).toLong()
                                    sendStream.write(receiveOKBytes,0,receiveOKBytes.size)
                                    res=Connection.MODE_INTERCHANGE
                                }
                                if(modeCode.contains(Connection.MODE_STOP)) {
                                    keepReceiving = false
                                    sendStream.write(receiveOKBytes,0,receiveOKBytes.size)
                                    res=Connection.MODE_STOP
                                }
                            } else {
                                continue
                            }
                            serverSocket.close()
                            receiveSocket.close()
                        }
                    }catch (e:Exception) {
                        res="error:$e"
                    }
                    return@async res
                }
            }

    private fun receiveFileOnTCP(receiveSocket: Socket,count:Int) : String {
        var res:String
        val receiverOKBytes= Connection.RECEIVER_OK.toByteArray()

        //show item_receiving_layout
        layoutChangeHandler.obtainMessage(0,1).sendToTarget()

        try {
            val recStream = receiveSocket.getInputStream()
            val sendStream = receiveSocket.getOutputStream()

            //start receiving files
            for(i in 0 until count) {
                //receive filename and size
                operationHandler.obtainMessage(0,"enter loop in ${i+1} time").sendToTarget()
                while(recStream.available()==0)  ;
                val buff=ByteArray(1024)
                val infoRead=recStream.read(buff)
                val infoString=String(buff,0,infoRead)
                val separatorIndex=infoString.indexOf("/*")
                val lastIndex=infoString.indexOf("*/")
                val filename=infoString.substring(0,separatorIndex)
                val fileSize=infoString.substring(separatorIndex+2,lastIndex).toLong()
                if(filename== Connection.SENDING_STOP && fileSize==(-222).toLong()) {
                    operationHandler.obtainMessage(0,"sender stopped").sendToTarget()
                    break
                }
                operationHandler.obtainMessage(0,"FileAvailable: $filename: ${Connection.formatDataString(fileSize,' ')}").sendToTarget()
                sendStream.write(receiverOKBytes,0,receiverOKBytes.size)
                currentFileSize=fileSize
                filenameHandler.obtainMessage(0,filename).sendToTarget()
                statusHandler.obtainMessage(0,fileSize).sendToTarget()

                //receive file
                val newFile = provideFile(filename)
                val fileOutputStream=newFile.outputStream()

                val continueReceiving= true
                var receivedBytes:Long=0
                var loops=0
                var error=true

                while(continueReceiving) {

                    loops++
                    var stopWaiting = false
                    operationHandler.postDelayed( {
                        stopWaiting = true
                    },4000)
                    while (recStream.available()==0) {
                        if(stopWaiting)
                            break
                    }
                    if(stopWaiting)
                        break
                    val buff1= ByteArray(recStream.available())
                    val read= recStream.read(buff1)
                    fileOutputStream.write(buff1,0,read)
                    totalReceivedBytes += read
                    receivedBytes += read
                    if(loops % 200 == 0) {
                        statusHandler.obtainMessage(0,receivedBytes).sendToTarget()
                    }
                    if(receivedBytes == fileSize) {
                        statusHandler.obtainMessage(0,receivedBytes).sendToTarget()
                        receivedItemsCount++
                        error=false
                        break
                    }
                }

                if(error) {
                    operationHandler.obtainMessage(0,"unfinished file").sendToTarget()
                }

                //send final response
                val bytes= "${Connection.ON_RECEIVE_RESPONSE}/*${saveDir.freeSpace}*/".toByteArray()
                sendStream.write(bytes,0,bytes.size)
                operationHandler.obtainMessage(0,"final response sent").sendToTarget()
                statusHandler.obtainMessage(0,receivedBytes).sendToTarget()
                operationHandler.obtainMessage(0,"finished:$receivedBytes").sendToTarget()
                fileOutputStream.close()

                //update media on device
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(newFile)))
                itemArray.add(ReceivedObject(newFile,error))
                addItemInRecyclerView.obtainMessage(0,newFile).sendToTarget()
            }
            operationHandler.obtainMessage(0,"Totalreceived:${Connection.formatDataString(totalReceivedBytes,' ')}").sendToTarget()
            res="okdone"

        } catch (e:Exception) {
            res="error:$e"
        }

        //show layout changes on finished
        layoutChangeHandler.obtainMessage(0,2).sendToTarget()

        return res
    }

    private fun confirmExitDialog() {
        val builder = AlertDialog.Builder(this@ReceiveNow)
        builder.apply {
            setMessage("Are you sure you want to stop the process?")
            setNegativeButton("NO") { _, _ ->
                //do nothing
            }
            setPositiveButton("YES") { _, _ ->
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
        val dialog = builder.create()
        dialog.setTitle("Sure to Exit?")
        dialog.show()
    }

    override fun onBackPressed() {
        confirmExitDialog()
    }

    private fun createNotificationChannel() {
        nManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(Connection.NOTIFICATION_CHANNEL_ID,
                Connection.NOTIFICATION_CHANNEL, NotificationManager.IMPORTANCE_DEFAULT)
            nManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun updateNotification(progress: Int, contentName: String) {
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(
                this, Connection.NOTIFICATION_CHANNEL_ID).apply {
                setSmallIcon(R.drawable.sendit_icon_new_small)
                setProgress(100, progress, false)
                setContentTitle(notificationFileName)
                setContentText(contentName)
            }.build()
        } else {
            Notification.Builder(this).apply {
                setSmallIcon(R.drawable.sendit_icon_new_small)
                setProgress(100, progress, false)
                setContentTitle(notificationFileName)
                setContentText(contentName)
            }.build()
        }
        notification.contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, ReceiveNow::class.java),
            PendingIntent.FLAG_IMMUTABLE)
        nManager.notify(notificationId, notification)
    }

    inner class ReceivedObject(val itemFile: File, val error:Boolean) {
        val itemSize=itemFile.length()
        val itemName:String=itemFile.name
        var deleted=false
    }
    companion object {
        val parentJob= Job()
        val saveDir= File(Environment.getExternalStorageDirectory(), Connection.saveDir)
        const val SHOW_LAYOUT=1
        const val FINISH_LAYOUT=2
        const val ERROR_LAYOUT=3
    }
}