package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chad.orionsoft.sendit.databinding.ActivitySendNowBinding
import chad.orionsoft.sendit.databinding.ItemSendingLayoutBinding
import kotlinx.coroutines.*
import java.lang.Exception
import java.lang.Runnable
import java.net.Socket
import kotlin.math.roundToInt

class SendNow : AppCompatActivity() {

    private var operation=""
    private var currentFileSize:Long=0
    private var itemSentAndRemoved=false
    private var operationTime=0f
    private var totalSentBytes:Long=0
    private var totalSentFiles=0
    private var sentBytesFromHalfSecond:Long=0
    private var stopRightNow=false
    private var skipFile=false

    private lateinit var sendList:ArrayList<SendObject>
    private lateinit var sAdapter:SendAdapter
    private lateinit var operationHandler: Handler
    private lateinit var layoutChangeHandler:Handler
    private lateinit var fileNameHandler:Handler
    private lateinit var fileIconHandler:Handler
    private lateinit var fileSentUpdate:Handler
    private lateinit var removeItemFromRecyclerView:Handler
    private lateinit var timeAndSpeedCountHandler:Handler
    private lateinit var timeAndSpeedRunnable:Runnable

    private lateinit var binding: ActivitySendNowBinding
    private lateinit var binding2 : ItemSendingLayoutBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendNowBinding.inflate(layoutInflater)
        binding2 = binding.itemSendingLayout
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        sendList=StaticList.sendList
        initializeHandlers()
        binding.sendNowRecyclerView.layoutManager=LinearLayoutManager(applicationContext)
        sAdapter=SendAdapter(sendList)
        binding.sendNowRecyclerView.adapter=sAdapter
        binding.sendNowRecyclerView.recycledViewPool.setMaxRecycledViews(0,0)
        CoroutineScope(Dispatchers.Main).launch {
            val res=sendFilesOnTCPAsync(sendList).await()
            Toast.makeText(applicationContext,res,Toast.LENGTH_SHORT).show()
            if(res.contains("error")) {
                layoutChangeHandler.obtainMessage(0, ERROR_LAYOUT).sendToTarget()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initializeHandlers() {

        operationHandler= Handler(mainLooper) {
            val msg=it.obj as String
            operation+="$msg\n"
            binding.operationText.text=operation
            Log.d("Operation", msg)
            true
        }

        layoutChangeHandler=Handler(mainLooper) {
            when(it.obj as Int) {
                SHOW_LAYOUT -> {

                }
                FINISH_LAYOUT -> {
                    binding2.sendingFileStatus.visibility=View.GONE
                    binding2.fileSkippingButton.visibility=View.GONE
                    binding2.sendingStopButton.visibility=View.GONE
                    val averageSpeed= (totalSentBytes / operationTime).roundToInt()
                    binding2.sendingSpeed.text="average: \n${Connection.formatDataString(averageSpeed.toLong(),' ')}ps"
                /*    val finishIcon= if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        resources.getDrawable(R.drawable.finished_icon,resources.newTheme())
                    } else {
                        resources.getDrawable(R.drawable.finished_icon)
                    }  */
                    val finishIcon = ResourcesCompat.getDrawable(
                        this@SendNow.resources,
                        R.drawable.finished_icon,
                        this@SendNow.theme
                    )
                    Animator.flipDrawable(binding2.sendingThumbnail,finishIcon,200)
                    binding2.sendingThumbnail.setOnClickListener {
                        finish()
                    }
                    binding2.sendingTime.text="$operationTime s\n${Connection.formatDataString(totalSentBytes,'\n')}"
                }
                ERROR_LAYOUT -> {
                    binding2.sendingFilename.text="An Error occurred, the App will be stopped in 5 seconds"
                    binding2.root.setBackgroundColor(Color.RED)
                    fileNameHandler.postDelayed({
                        android.os.Process.killProcess(android.os.Process.myPid())
                    },5000)

                }
            }
            true
        }

        fileNameHandler=Handler(mainLooper) {
            val msg=it.obj as String
            binding2.sendingFilename.text=msg
            true
        }

        fileIconHandler=Handler(mainLooper) {
            val drawable=it.obj as Drawable
            Animator.flipDrawable(binding2.sendingThumbnail,drawable,200)
            true
        }

        fileSentUpdate=Handler(mainLooper) {
            val length=it.obj as Long
            val update="${Connection.formatDataString(length,' ')} / ${Connection.formatDataString(currentFileSize,' ')}"
            binding2.sendingFileStatus.text=update
            //update progress-bar
            val progress:Int=(length*100/currentFileSize).toInt()
            binding2.sendingProgress.progress=progress
            true
        }

        removeItemFromRecyclerView = Handler(mainLooper) {
            it.obj as Int
            sendList.removeAt(0)
            sAdapter.notifyItemRemoved(0)
            itemSentAndRemoved=true
            true
        }

        timeAndSpeedCountHandler= Handler(mainLooper)
        timeAndSpeedRunnable=object: Runnable {

            override fun run() {
                val diff=(totalSentBytes-sentBytesFromHalfSecond)*2
                binding2.sendingSpeed.text="${Connection.formatDataString(diff,'\n')}ps"
                binding2.sendingTime.text="$operationTime s\n${Connection.formatDataString(totalSentBytes,'\n')}"
                sentBytesFromHalfSecond=totalSentBytes
                operationTime+=0.5f
                timeAndSpeedCountHandler.postDelayed(this,500)
            }

        }
    }

    fun stopSending(v:View) {
        v.id
        Toast.makeText(applicationContext,"sending canceled", Toast.LENGTH_SHORT).show()

        skipFile=true
        stopRightNow=true
    }

    fun skipFile(v:View) {
        v.id
        Toast.makeText(applicationContext,"file Skipped", Toast.LENGTH_SHORT).show()
        skipFile=true
    }

    inner class SendAdapter(private val sList:ArrayList<SendObject>) : RecyclerView.Adapter<SendAdapter.SendHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SendHolder {
            return SendHolder(layoutInflater.inflate(R.layout.item_layout_media,parent,false))
        }

        override fun getItemCount(): Int = sList.size

        override fun onBindViewHolder(holder: SendHolder, position: Int) {
            val itemTitle=holder.itemTitle
            val itemSize=holder.itemSize
            val itemIcon=holder.itemIcon
            itemTitle.text=sList[position].name
            itemSize.text=Connection.formatDataString(sList[position].size,' ')
            itemIcon.setImageDrawable(Connection.findICONSmall(applicationContext,sList[position].name))
        }

        inner class SendHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val itemIcon:ImageView=itemView.findViewById(R.id.item_icon)
            val itemTitle:TextView=itemView.findViewById(R.id.item_title)
            val itemSize:TextView=itemView.findViewById(R.id.item_prop)
        }
    }

    private suspend fun sendFilesOnTCPAsync(selectedItems:ArrayList<SendObject>) : Deferred<String> =
        coroutineScope {
            async(Dispatchers.IO) {
                var res:String
                val sendObjects=ArrayList<SendObject>()
                sendObjects.addAll(selectedItems)
                try {

                    //create Socket
                    val socket= Socket(Connection.partnerAddress, Connection.receiverPort)
                    Connection.mode= Connection.MODE_SENDER
                    val sendStream=socket.getOutputStream()
                    val receiveStream=socket.getInputStream()
                    operationHandler.obtainMessage(0,"socket created").sendToTarget()

                    //send items mode
                    var notGoodResponse=true
                    while(notGoodResponse) {
                        val modeBytes="${Connection.APP_CODE}/*${Connection.MODE_FILES}#${sendObjects.size}*/".toByteArray()
                        sendStream.write(modeBytes,0,modeBytes.size)
                        operationHandler.obtainMessage(0,"modeFile sent").sendToTarget()

                        //wait for response
                        while(receiveStream.available()==0) ;
                        val buff=ByteArray(50)
                        val read=receiveStream.read(buff)
                        val modeRes=String(buff,0,read)
                        operationHandler.obtainMessage(0,"response received: $modeRes").sendToTarget()
                        notGoodResponse = !modeRes.contains(Connection.RECEIVER_OK)
                    }

                    operationTime+=0.5f
                    timeAndSpeedCountHandler.postDelayed(timeAndSpeedRunnable,500)

                    //startSendingFiles
                    stopRightNow=false
     //               layoutChangeHandler.obtainMessage(0, SHOW_LAYOUT).sendToTarget()
                    for(i in 0 until sendObjects.size) {

                        val filename= sendObjects[i].name
                        fileNameHandler.obtainMessage(0, filename).sendToTarget()
                        fileIconHandler.obtainMessage(0, Connection.findICON(applicationContext,filename)).sendToTarget()
                        currentFileSize= sendObjects[i].size
                        var sentBytes:Long=0
                        fileSentUpdate.obtainMessage(0,sentBytes).sendToTarget()

                        //break if stop button pressed
                        if(stopRightNow) {
                            val stopBytes="${Connection.SENDING_STOP}/*-222*/".toByteArray()
                            sendStream.write(stopBytes,0,stopBytes.size)
                            operationHandler.obtainMessage(0,"stop message sent").sendToTarget()
                            break
                        }

                        //send filename and size
                        val filenameBytes="$filename/*$currentFileSize*/".toByteArray()
                        sendStream.write(filenameBytes,0,filenameBytes.size)
                        operationHandler.obtainMessage(0,"filename and size sent").sendToTarget()
                        //wait for the response
                        val buff=ByteArray(50)
                        while(receiveStream.available()==0) ;
                        val responseSize=receiveStream.read(buff)
                        val responseString=String(buff,0,responseSize)
                        operationHandler.obtainMessage(0,"response received:$responseString").sendToTarget()
                        if(responseString.contains(Connection.RECEIVER_OK)) {
                            operationHandler.obtainMessage(0,"receiver ready to accept").sendToTarget()
                            val stream= sendObjects[i].file.inputStream()
                            Log.d("Operation", "Stream available: ${stream.available()}")
                            var loops=0
                            //start sending stream
                            skipFile=false


                            // stream.available = 0 for large this needs to be changed.
                      /*      while(stream.available()>0 && !skipFile) {
                                val fileBuff=ByteArray(4096)
                                val bytesRead=stream.read(fileBuff)
                                sendStream.write(fileBuff,0,bytesRead)
                                totalSentBytes+=bytesRead
                                sentBytes+=bytesRead
                                loops++
                                if(loops%200==0) {
                                    //     operationHandler.obtainMessage(0,"sent:${formatDataString(sentBytes,' ')}").sendToTarget()
                                    //update UI
                                    fileSentUpdate.obtainMessage(0,sentBytes).sendToTarget()
                                }
                            } */

                            while (!skipFile) {
                                val fileBuff= ByteArray(4096)
                                val bytesRead = stream.read(fileBuff)
                                if (bytesRead <= 0) break
                                sendStream.write(fileBuff,0, bytesRead)
                                totalSentBytes += bytesRead
                                sentBytes += bytesRead
                                loops++
                                if(loops%200 == 0) {
                                    //     operationHandler.obtainMessage(0,"sent:${formatDataString(sentBytes,' ')}").sendToTarget()
                                    //update UI
                                    fileSentUpdate.obtainMessage(0,sentBytes).sendToTarget()
                                }
                            }


                            fileSentUpdate.obtainMessage(0,sentBytes).sendToTarget()
                            operationHandler.obtainMessage(0,"file sent : $sentBytes").sendToTarget()
                        } else {
                            continue
                        }
                        //wait for final response
                        var finalResponse=""
                        operationHandler.obtainMessage(0,"waiting for final response").sendToTarget()
                        while(!finalResponse.contains(Connection.ON_RECEIVE_RESPONSE)) {
                            while(receiveStream.available()==0) ;
                            operationHandler.obtainMessage(0,"available in final: ${receiveStream.available()}").sendToTarget()
                            val buff1=ByteArray(200)
                            receiveStream.read(buff1,0,buff1.size)
                            finalResponse=String(buff1,0,buff1.size)
                            operationHandler.obtainMessage(0,"received in final: $finalResponse").sendToTarget()
                        }
                        val separatorIndex=finalResponse.indexOf("/*")
                        val finalIndex=finalResponse.indexOf("*/")
                        val freeSpaceStr=finalResponse.substring(separatorIndex+2,finalIndex)
                        Connection.freeSpace=freeSpaceStr.toLong()

                        if(!stopRightNow && !skipFile)
                            totalSentFiles++

                        removeItemFromRecyclerView.obtainMessage(0,i).sendToTarget()
                    }
                    fileNameHandler.obtainMessage(0,"file sent: $totalSentFiles").sendToTarget()
                    layoutChangeHandler.obtainMessage(0, FINISH_LAYOUT).sendToTarget()
                    res="OKdone"
                    socket.close()
                    operationHandler.obtainMessage(0,"socket closed").sendToTarget()
                    timeAndSpeedCountHandler.removeCallbacks(timeAndSpeedRunnable)
                } catch (e: Exception) {
                    res="Error:$e"
                }
                return@async res
            }
        }

    companion object {
        const val SHOW_LAYOUT=1
        const val FINISH_LAYOUT=2
        const val ERROR_LAYOUT=3
    }
}