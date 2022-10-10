package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.usage.StorageStatsManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.storage.StorageManager
import android.provider.MediaStore
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chad.orionsoft.sendit.databinding.ActivityReceiveNewBinding
import chad.orionsoft.sendit.databinding.ContentReceiveActivityNewBinding
import chad.orionsoft.sendit.databinding.ItemSendingLayoutBinding
import kotlinx.coroutines.*
import java.lang.Runnable
import java.net.ServerSocket
import java.net.Socket

class ReceiveNowQ : AppCompatActivity() {

    private val receiverPort=4444
    private var receivedItemsCount=0
    private var keepReceiving=false
    private var totalReceivedBytes:Long=0
    private var currentFileSize:Long=0
    private var operations=""
    private var receivedBytesFromHalfSecond:Long=0
    private var operationTime=0f
    private var itemArray=ArrayList<ReceivedObject>()

    private lateinit var iAdapter:ItemAdapter
    private lateinit var onTouchListener: View.OnTouchListener
    private lateinit var operationHandler: Handler
    private lateinit var layoutChangeHandler: Handler
    private lateinit var filenameHandler: Handler
    private lateinit var statusHandler: Handler
    private lateinit var timeAndSpeedCountHandler: Handler
    private lateinit var timeAndSpeedRunnable:Runnable
    private lateinit var addItemInRecyclerView: Handler

    private lateinit var binding: ActivityReceiveNewBinding
    private lateinit var binding2 : ContentReceiveActivityNewBinding
    private lateinit var binding3 : ItemSendingLayoutBinding

    @TargetApi(Build.VERSION_CODES.Q)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiveNewBinding.inflate(layoutInflater)
        binding2  = binding.contentReceiverActivity
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setSupportActionBar(binding.toolbar)
        binding2.receiverBelowBarText.isSelected = true
        binding2.receiverBelowBarText.text=
            "receiving from :${Connection.partnerName} , free: ${Connection.formatDataString(getFreeSpace(),' ')}"
        initializeHandler()
        GlobalScope.launch(Dispatchers.Main + parentJob) {
            val operation=receiveOnTCPAsync().await()
            operationHandler.obtainMessage(0,operation).sendToTarget()
            if(operation.contains("error")) {
                layoutChangeHandler.obtainMessage(0, ERROR_LAYOUT).sendToTarget()
                return@launch
            }
            if(operation.contains(Connection.MODE_INTERCHANGE)) {
                Toast.makeText(applicationContext,"Sender has switched position, now you can send..", Toast.LENGTH_LONG).show()
                startActivity(Intent(applicationContext,SendOptions::class.java))
                finish()
                return@launch
            }
            if(operation.contains(Connection.MODE_STOP)) {
                Toast.makeText(applicationContext,"Sender left, you can exit now...", Toast.LENGTH_SHORT).show()
            }
        }

        var currentY=0f

        onTouchListener= View.OnTouchListener { view, motionEvent ->

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
                        view.visibility= View.GONE
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

        binding2.receivedItemRecyclerView.layoutManager= LinearLayoutManager(applicationContext)
        binding2.receivedItemRecyclerView.recycledViewPool.setMaxRecycledViews(0,0)
        iAdapter=ItemAdapter(itemArray)
        binding2.receivedItemRecyclerView.adapter=iAdapter
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("SetTextI18n")
    private fun initializeHandler() {

        binding3 = binding2.itemReceivingLayout
        operationHandler= Handler(mainLooper) {
            val msg=it.obj as String
            operations+="\n$msg"
            binding2.receiverOperations.text=operations
            true
        }

        layoutChangeHandler= Handler(mainLooper) {
            when(it.obj as Int) {
                SHOW_LAYOUT -> {
                    binding3.root.visibility= View.VISIBLE
                    binding3.sendingStopButton.visibility = View.GONE
                    binding3.imageBackground.setBackgroundColor(Color.rgb(0x15,0xa9,0x62))
                    Animator.moveUP_IN(binding3.root,300)
                    binding3.sendingThumbnail.setOnClickListener(null)
                    binding3.sendingFileStatus.visibility= View.VISIBLE
                    binding3.fileSkippingButton.visibility= View.GONE
                    binding3.root.setOnTouchListener(null)
                }
                FINISH_LAYOUT -> {
                    binding3.root.setOnTouchListener(onTouchListener)
                    timeAndSpeedCountHandler.removeCallbacks(timeAndSpeedRunnable)
                    binding3.sendingFileStatus.visibility= View.GONE
                    Animator.flipDrawable(binding3.sendingThumbnail,
                        resources.getDrawable(R.drawable.finished_icon,resources.newTheme()) ,200)
                    binding3.sendingThumbnail.setOnClickListener {
                        Animator.moveDown_OUT(binding3.root,300)
                        operationHandler.postDelayed({
                            binding3.root.visibility = View.GONE
                        },300)
                    }
                    binding3.sendingTime.text="$operationTime s\n${Connection.formatDataString(totalReceivedBytes,'\n')}"
                    binding3.sendingFilename.text="Total Item received: $receivedItemsCount"
                    binding3.sendingSpeed.text="average\n ${Connection.formatDataString((totalReceivedBytes/operationTime).toLong(),' ')}ps"
                    binding2.receiverBelowBarText.text=
                        "receiving from :${Connection.partnerName} , free: ${Connection.formatDataString(getFreeSpace(),' ')}"
                }
                ERROR_LAYOUT -> {
                    timeAndSpeedCountHandler.removeCallbacks(timeAndSpeedRunnable)
                    binding3.sendingFileStatus.visibility= View.GONE
                    binding3.root.setBackgroundColor(Color.RED)
                    binding3.sendingFilename.text="An error occured. receiver will terminate in 5 seconds"
                    statusHandler.postDelayed({
                        //     android.os.Process.killProcess(android.os.Process.myPid())
                    },5000)
                }
            }
            true
        }

        filenameHandler= Handler(mainLooper) {
            val name=it.obj as String
            binding3.sendingFilename.text = name
            Animator.flipDrawable(binding3.sendingThumbnail,Connection.findICON(applicationContext,name),200)
            true
        }

        statusHandler= Handler(mainLooper) {
            val received=it.obj as Long
            binding3.sendingFileStatus.text="${Connection.formatDataString(received,' ')} / ${Connection.formatDataString(currentFileSize,' ')}"
            val progress=(received*100/currentFileSize).toInt()
            binding3.sendingProgress.progress=progress
            true
        }

        addItemInRecyclerView= Handler(mainLooper) {
            //   val file=it.obj as File
            //    itemArray.add(ReceivedObject(file,false))
            binding2.startupText.visibility= View.GONE
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

    @TargetApi(Build.VERSION_CODES.O)
    private fun getFreeSpace() : Long {
        val stManager = getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
        return stManager.getFreeBytes(StorageManager.UUID_DEFAULT)
    }

    /* private fun provideFile(filename:String) : File {
        var exists=false
        val fList=appDir.list()
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
    } */

    inner class ItemAdapter(private var items:ArrayList<ReceivedObject>) : RecyclerView.Adapter<ItemAdapter.ItemHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
            ItemHolder(layoutInflater.inflate(R.layout.item_layout_media,parent,false))

        override fun getItemCount(): Int = items.size

        @RequiresApi(Build.VERSION_CODES.Q)
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            val itemName=holder.itemName
            val itemLayout=holder.itemLayout
            val itemIcon=holder.itemIcon
            val itemSize=holder.itemSize
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if(items[position].error) {
                    itemLayout.background =
                        resources.getDrawable(R.drawable.item_background_error, resources.newTheme())
                }
            } else {
                if(items[position].error) {
                    itemLayout.background = resources.getDrawable(R.drawable.item_background_error, theme)
                }
            }
            if(position==items.lastIndex) {
                Animator.moveRight_IN(itemLayout,300)
            }
            itemName.text=items[position].filename
            if(items[position].error) {
                if(!items[position].deleted) {
                    itemSize.text="unfinished - ${Connection.formatDataString(items[position].size,' ')} , tap to delete"
                    itemLayout.setOnClickListener {
                        // TODO ("Delete unfinished file here")
                        if(!items[position].deleted) {
                            val operation = contentResolver.delete(items[position].fileUri, null, null)
                            if (operation == 1) {
                                itemSize.text = "deleted"
                                items[position].deleted = true
                            } else {
                                itemSize.text = "unable to delete"
                            }
                        }
                    }
                } else {
                    itemSize.text="deleted"
                    itemLayout.setOnClickListener(null)
                }
            } else {
                when(items[position].type) {
                    Connection.TYPE_IMAGE -> {
                        itemSize.text="${Connection.formatDataString(items[position].size,' ')} - saved in Pictures"
                    }
                    Connection.TYPE_VIDEO -> {
                        itemSize.text= "${Connection.formatDataString(items[position].size,' ')} - saved in Movies"
                    }
                    Connection.TYPE_AUDIO -> {
                        itemSize.text= "${Connection.formatDataString(items[position].size,' ')} - saved in Music"
                    }
                    Connection.TYPE_OTHER -> {
                        itemSize.text= "${Connection.formatDataString(items[position].size,' ')} - saved in Downloads"
                    }
                }
                itemLayout.setOnClickListener {
                    Toast.makeText(applicationContext,"open the file", Toast.LENGTH_SHORT).show()
                    val mime = contentResolver.getType(items[position].fileUri)
                    val i= Intent().apply {
                        setDataAndType(items[position].fileUri,mime)
                        action = Intent.ACTION_VIEW
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(i)
                }
            }
            itemIcon.setImageDrawable(Connection.findICONSmall(applicationContext,items[position].filename))
        }

        inner class ItemHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            val itemName: TextView =itemView.findViewById(R.id.item_title)
            val itemLayout: LinearLayout =itemView.findViewById(R.id.item_bar_layout)
            val itemIcon: ImageView =itemView.findViewById(R.id.item_icon)
            val itemSize: TextView =itemView.findViewById(R.id.item_prop)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun receiveOnTCPAsync() : Deferred<String> =
        coroutineScope {
            async(Dispatchers.IO + parentJob) {
                var res=""
                keepReceiving=true
                try {
                    while(keepReceiving) {
                        //create server socket
                        val serverSocket= ServerSocket(receiverPort)
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

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun receiveFileOnTCP(receiveSocket: Socket, count:Int) : String {
        var res=""
        val receiverOKBytes = Connection.RECEIVER_OK.toByteArray()

        //show item_receving_layout
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
                // val newFile = provideFile(filename)
                val newFileUri = getNewFileUri(filename)
                val outputStream= contentResolver.openOutputStream(newFileUri!!)

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
                    outputStream?.write(buff1,0,read)
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
                val bytes= "${Connection.ON_RECEIVE_RESPONSE}/*${getFreeSpace()}*/".toByteArray()
                sendStream.write(bytes,0,bytes.size)
                operationHandler.obtainMessage(0,"final response sent").sendToTarget()
                statusHandler.obtainMessage(0,receivedBytes).sendToTarget()
                operationHandler.obtainMessage(0,"finished:$receivedBytes").sendToTarget()
                outputStream?.flush()
                outputStream?.close()

                //update media on device
                when(Connection.getContentTypeFromName(filename)) {
                    Connection.TYPE_IMAGE -> {
                        val newValue = ContentValues()
                        newValue.put(MediaStore.Images.Media.IS_PENDING, 0)
                        contentResolver.update(newFileUri, newValue, null, null)
                    }
                    Connection.TYPE_VIDEO -> {
                        val newValue = ContentValues()
                        newValue.put(MediaStore.Video.Media.IS_PENDING, 0)
                        contentResolver.update(newFileUri, newValue, null, null)
                    }
                    Connection.TYPE_AUDIO -> {
                        val newValue = ContentValues()
                        newValue.put(MediaStore.Audio.Media.IS_PENDING, 0)
                        contentResolver.update(newFileUri, newValue, null ,null)
                    }
                    Connection.TYPE_OTHER -> {
                        val newValue = ContentValues()
                        newValue.put(MediaStore.Downloads.IS_PENDING, 0)
                        contentResolver.update(newFileUri, newValue, null ,null)
                    }
                }

                // update received list
                itemArray.add(ReceivedObject(filename, receivedBytes, newFileUri, error))
                addItemInRecyclerView.obtainMessage(0, "").sendToTarget()

                /*sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(newFile)))
                itemArray.add(ReceivedObject(newFile,error))
                addItemInRecyclerView.obtainMessage(0,newFile).sendToTarget() */
            }
            operationHandler.obtainMessage(0,"Totalreceived:${Connection.formatDataString(totalReceivedBytes,' ')}").sendToTarget()
            res="okdone"
        } catch (e:Exception) {
            res = "Error:$e"
        }

        //show layout changes on finished
        layoutChangeHandler.obtainMessage(0,2).sendToTarget()

        return res
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getNewFileUri(filename:String) : Uri? {

        val type = Connection.getContentTypeFromName(filename)

        var itemUri:Uri? = Uri.parse("Helllo")
        when(type) {
            Connection.TYPE_IMAGE -> {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/*")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                itemUri = contentResolver.insert(collection, values)
            }
            Connection.TYPE_VIDEO -> {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/*")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }

                val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                itemUri = contentResolver.insert(collection, values)
            }
            Connection.TYPE_AUDIO -> {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/*")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }

                val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                itemUri = contentResolver.insert(collection, values)
            }
            Connection.TYPE_OTHER -> {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, filename)
                    put(MediaStore.Downloads.MIME_TYPE, "*/*")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                itemUri = contentResolver.insert(collection, values)
            }
        }
        operationHandler.obtainMessage(0,"new Uri :$itemUri, filetype: $type").sendToTarget()
        return itemUri
    }

    /* private fun receiveFileOnTCP(receiveSocket: Socket, count:Int) : String {
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
    } */

    private fun confirmExitDialog() {
        val builder = AlertDialog.Builder(this@ReceiveNowQ)
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

    inner class ReceivedObject(val filename:String, val size:Long, val fileUri:Uri, val error:Boolean) {
        val type = Connection.getContentTypeFromName(filename)
        var deleted = false
    }

    companion object {
        val parentJob= Job()
        const val SHOW_LAYOUT=1
        const val FINISH_LAYOUT=2
        const val ERROR_LAYOUT=3
    }
}