package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.storage.StorageManager
import android.util.Log
import android.view.View
import android.widget.Toast
import chad.orionsoft.sendit.databinding.ActivityCreateConnectionReceiverBinding
import chad.orionsoft.sendit.databinding.DeviceConnectedLayoutBinding
import kotlinx.coroutines.*
import java.io.File
import java.lang.Exception
import java.net.DatagramPacket
import java.net.DatagramSocket

class CreateConnectionReceiver : AppCompatActivity() {

    private var keepSearching=false
    private var test=""

    private lateinit var operationHandler:Handler
    private lateinit var headingHandler:Handler
    private lateinit var uiCodeHandler:Handler
    private lateinit var binding: ActivityCreateConnectionReceiverBinding
    private lateinit var binding2 : DeviceConnectedLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateConnectionReceiverBinding.inflate(layoutInflater)
        binding2 = DeviceConnectedLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        keepSearching = true
        initializeHandlers()
        binding.receiverCancelButton.setOnClickListener {
            keepSearching = false
            finish()
        }
        CoroutineScope(Dispatchers.Main).launch {
            val operation = waitSenderAsync().await()
            if (operation.contains("connected")) {
                setContentView(binding2.root)
                binding2.connectionNameText.text=Connection.partnerName
                uiCodeHandler.postDelayed( {
                    Connection.connection=true
                    if(Connection.isScoped) {
                        startActivity(Intent(applicationContext,ReceiveNowQ::class.java))
                    } else {
                        startActivity(Intent(applicationContext,ReceiveNow::class.java))
                    }
                    finish()
                },1000)
            } else if(operation.contains("Error")) {
                Toast.makeText(applicationContext,operation,Toast.LENGTH_SHORT).show()
                Log.e("Receiver", operation)
            }
            finish()
            }
    }

    @SuppressLint("SetTextI18n")
    private fun initializeHandlers() {
        headingHandler = Handler(mainLooper) {
            val msg = it.obj as String
            binding.receiverConnectionHeading.text = msg
            true
        }
        uiCodeHandler = Handler(mainLooper) {
            isCodeSet = false
            binding.receiverConnectProgressbar.visibility = View.GONE
            binding.receiverConnectButton.visibility = View.VISIBLE
            binding.receiverEnterCodeEdit.visibility = View.VISIBLE
            binding.receiverEnterCodeInfo.visibility = View.VISIBLE
            binding.receiverConnectButton.setOnClickListener {
                if (binding.receiverEnterCodeEdit.text.isNullOrEmpty()) {
                    Toast.makeText(applicationContext,"please enter code to connect", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                RES_CODE = binding.receiverEnterCodeEdit.text.toString()
                isCodeSet = true
            }
            true
        }

        operationHandler= Handler(mainLooper) {
            val msg=it.obj as String
            test+="\n$msg"
            binding.dynamicInfo.text=test
            Log.d("Operation", msg)
            true
        }
    }

    companion object {
     //   val parentJob= Job()
        var isCodeSet = false
        var RES_CODE =""
        const val APP_CODE = Connection.APP_CODE
        const val PORT = Connection.receiverPort
        const val SEARCH_CODE = Connection.SEARCH_CODE
        var MY_NAME = "$APP_CODE/*${Connection.username}*/"
        const val NEXT_SIG_END = Connection.NEXT_SIG_END
        const val CONNECT_SIG = Connection.CONNECT_SIG
        const val ASK_CODE_SIG = Connection.ASK_CODE_SIG
        const val CONN_OK = Connection.CONN_OK
        const val OLD_VER = Connection.OLD_VER
        const val DEVICE_DETAILS = Connection.DEVICE_DETAILS
    }

    private fun deviceInfoString() : String {
        Connection.freeSpace = getFreeSpace()
        return "$DEVICE_DETAILS#${Connection.myRandomId}/*${Connection.freeSpace}*/${Connection.THIS_VER}@"
    }

    private fun getFreeSpace() : Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val stManager = getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            stManager.getFreeBytes(StorageManager.UUID_DEFAULT)
        } else {
            val file = File(Environment.getExternalStorageDirectory(),"Send_it")
            file.freeSpace
        }

    }

    private suspend fun waitSenderAsync() : Deferred<String> =
            coroutineScope {
                async (Dispatchers.IO) {
                    var res=""
                    try {
                        // prepare datagram socket
                        val recSocket = DatagramSocket(PORT)
                        recSocket.soTimeout = 60000
                        CoroutineScope(Dispatchers.IO).launch {
                            watchReceiverSocketAsync().await()
                            if(!recSocket.isClosed) {
                   //             recSocket.close()
                                Log.d("Receiver", "Socket closed")
                            }
                        }
                        var inStep1 = true
                        var buff= ByteArray(1024)
                        var receivePacket = DatagramPacket(buff, buff.size)
                        var senderName=""

                        // in step 1
                        while(inStep1) {
                            Log.d("Receiver", "Searching")
                            recSocket.receive(receivePacket)
                            if (!keepSearching) {
                                break
                            }
                            val senderAddress = receivePacket.address
                            val senderPort = receivePacket.port
                            val got = String(buff, 0, receivePacket.length)
                            operationHandler.obtainMessage(0, "received: $got").sendToTarget()
                            if(got.contains(SEARCH_CODE)) {
                                val present = MY_NAME.toByteArray()
                                val sendPacket = DatagramPacket(present, present.size, senderAddress, senderPort)
                                recSocket.send(sendPacket)
                                operationHandler.obtainMessage(0, "sent present").sendToTarget()
                            }
                            if(got.contains(NEXT_SIG_END)) {
                                val nameStartIndex = got.indexOf("#")
                                val nameEndIndex = got.indexOf("/*")
                                senderName = got.substring(nameStartIndex+1, nameEndIndex)
                                operationHandler.obtainMessage(0, "Sender: $senderName").sendToTarget()
                                inStep1 = false
                            }
                        }

                        if (!keepSearching) {
                            return@async "Stopped"
                        }

                        //in step 2
                        headingHandler.obtainMessage(0, "connecting  to $senderName").sendToTarget()
                        operationHandler.obtainMessage(0, "In step 2").sendToTarget()
                        buff = ByteArray(1024)
                        receivePacket = DatagramPacket(buff, buff.size)
                        recSocket.receive(receivePacket)
                        val senderAddress = receivePacket.address
                        val senderPort = receivePacket.port
                        val got = String(buff, 0, receivePacket.length)
                        operationHandler.obtainMessage(0, "got: $got").sendToTarget()

                        if (got.contains(CONNECT_SIG)) {
                            // send mac, free space and version details
                            val res1= deviceInfoString().toByteArray()
                            val sendPacket = DatagramPacket(res1, res1.size, senderAddress, senderPort)
                            recSocket.send(sendPacket)
                            operationHandler.obtainMessage(0, "Device details sent: ${String(res1)}").sendToTarget()
                            buff = ByteArray(1024)
                            val receivePacket1 = DatagramPacket(buff, buff.size)
                            recSocket.receive(receivePacket1)
                            val got1 = String(buff, 0, receivePacket1.length)
                            operationHandler.obtainMessage(0, "on connect signal :$got1").sendToTarget()
                            if (got1.contains(ASK_CODE_SIG)) {
                                operationHandler.obtainMessage(0, "code will be asked").sendToTarget()
                                uiCodeHandler.obtainMessage(0,0).sendToTarget()
                                while(!isCodeSet) { }
                                val codeStr = "$APP_CODE&$RES_CODE/*${getFreeSpace()}*/"
                                val codeBytes = codeStr.toByteArray()
                                Log.d("Operation", "sending code $codeStr")
                                val sendPacket1 = DatagramPacket(codeBytes, codeBytes.size, senderAddress, senderPort)
                                recSocket.send(sendPacket1)
                                buff = ByteArray(1024)
                                val receivePacket2 = DatagramPacket(buff, buff.size)
                                recSocket.receive(receivePacket2)
                                val got2 = String(buff, 0, receivePacket2.length)
                                Log.d("Operation", "got response $got2")
                                if(got2.contains(CONN_OK)) {
                                    operationHandler.obtainMessage(0, "connected").sendToTarget()
                                    Connection.connection = true
                                    Connection.partnerName = senderName
                                    Connection.partnerAddress = senderAddress
                                    res = "connected"
                                } else {
                                    operationHandler.obtainMessage(0, "Wrong code sent by you").sendToTarget()
                                    res = "Error : Wrong code received"
                                }

                            }
                            if (got1.contains(CONN_OK)) {
                                operationHandler.obtainMessage(0, "connected").sendToTarget()
                                Connection.partnerName = senderName
                                Connection.partnerAddress = senderAddress
                                res = "connected"
                            }
                            if (got1.contains(OLD_VER)) {
                                operationHandler.obtainMessage(0, "You are using old version").sendToTarget()
                                res = "Error : You are using older version"
                            }
                        }

                    } catch (e:Exception) {
                        res = "Error: $e"
                    }
                    return@async res
                }
            }

    private suspend fun watchReceiverSocketAsync() =
        coroutineScope {
            async(Dispatchers.IO) {
                while(keepSearching) {
                    delay(2000)
                }
                Log.d("Receiver", "Socket can be closed")
            }
        }
}
