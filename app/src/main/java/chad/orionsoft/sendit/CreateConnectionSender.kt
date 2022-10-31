package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.telephony.TelephonyManager
import android.view.View
import android.widget.Button
import android.widget.Toast
import chad.orionsoft.sendit.databinding.ActivityCreateConnectionBinding
import chad.orionsoft.sendit.databinding.DeviceConnectedLayoutBinding
import kotlinx.coroutines.*
import org.json.JSONArray
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.*

class CreateConnectionSender : AppCompatActivity() {

    private var code=""
    private val receiverPort=4444
    private var step1 = false
    private lateinit var receiverHandler: Handler
    private lateinit var backToNormalHandler: Handler
    private lateinit var showCodeHandler: Handler
    private var receiverAddress=""
    private var searching = false
    private lateinit var binding: ActivityCreateConnectionBinding
    private lateinit var binding2 : DeviceConnectedLayoutBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateConnectionBinding.inflate(layoutInflater)
        binding2 = DeviceConnectedLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setDataDir(applicationContext)
    /*    testHandler=Handler {
            val msg=it.obj as String
            Toast.makeText(applicationContext,msg,Toast.LENGTH_SHORT).show()
            true
        }
        handler = Handler {
            val msg = it.obj as String
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
            true
        }  */
        showCodeHandler = Handler(mainLooper) {
            val code = it.obj as String
            binding.codeText.visibility= View.VISIBLE
            binding.codeText.text = code
            binding.receiverBtnLayout.removeAllViews()
            binding.buttonRelativeLayout.visibility = View.GONE
            binding.connectSendHeading.text = "Enter Code"
            binding.connectSendText.text = "ask receiver to enter this code to connect"
            true
        }
        receiverHandler = Handler(mainLooper) {
            val info = it.obj as JSONArray
            val name = info.getString(0)
            val address = info.getString(1)
            val btn = Button(applicationContext).apply {
                setBackgroundResource(R.drawable.item_background_select)
                text = name
            }
            binding.codeText.visibility= View.GONE
            binding.receiverBtnLayout.addView(btn)
            binding.connectSendHeading.text = "Searching for Receiver"
            binding.connectSendText.text = "tap to connect"
            btn.setOnClickListener {
                Connection.partnerName = name
                receiverAddress = address.substring(1)
                Connection.partnerAddress = InetAddress.getByName(receiverAddress)
                binding.connectSendHeading.text ="Connecting"
                binding.connectSendText.text = " Connecting to ${Connection.partnerName}\n Please wait..."
                step1 = false
                binding.receiverBtnLayout.removeAllViews()
            }
            true
        }
        backToNormalHandler = Handler(mainLooper) {
            val errMsg = it.obj as String
            binding.codeText.visibility = View.GONE
            binding.receiverBtnLayout.removeAllViews()
            binding.buttonRelativeLayout.visibility = View.VISIBLE
            binding.connectSendHeading.text = "Searching for Receiver"
            binding.connectSendText.text = errMsg
            true
        }

        binding.cancelButton.setOnClickListener {
            if (searching) {
                searching = false
                parentJob.cancel()
            }
        }

        CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main + parentJob) {
            val searchBytes = ("$APP_CODE/*$SEARCH_CODE*/").toByteArray()
            val conRes=testConnectionAsync(searchBytes,100).await()
            if(conRes.contains("error")) {
                Toast.makeText(applicationContext, conRes,Toast.LENGTH_SHORT).show()
                binding.buttonRelativeLayout.visibility = View.GONE
                finish()
            } else {
                startSearching()
                binding.cancelButton.setOnClickListener {
                    android.os.Process.killProcess(android.os.Process.myPid())
                }
            }
        }
    }

    private fun startSearching() {
        code=generateCode()
        CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main + parentJob) {
            while(!Connection.connection) {
                // setContentView(R.layout.activity_sender)
                val res = broadcastSearchAsync().await()
                Toast.makeText(applicationContext,res, Toast.LENGTH_LONG).show()
                // handler.obtainMessage(0, "End: $res").sendToTarget()
            }
            setContentView(binding2.root)
            binding2.connectionNameText.text = Connection.partnerName
            backToNormalHandler.postDelayed( {
                finish()
            },2000)
        }
    }

    private fun generateCode() : String {
        val code1=Random().nextInt(10)
        val code2=Random().nextInt(10)
        val code3=Random().nextInt(10)
        val code4=Random().nextInt(10)
        val code="$code1$code2$code3$code4"
        binding.codeText.text=code
        return code
    }

    companion object {
        val parentJob= Job()
        lateinit var data1Dir: File
        fun setDataDir(ctx: Context) {
            data1Dir = ctx.getExternalFilesDir( "MAC")!!
        }
        const val SEARCH_CODE = Connection.SEARCH_CODE
        const val APP_CODE = Connection.APP_CODE
        var BROAD_ADDR = Connection.BROAD_ADDR
        const val PORT = Connection.receiverPort
        const val NEXT_SIG_END = Connection.NEXT_SIG_END
        const val CONNECT_SIG = Connection.CONNECT_SIG
        const val OLD_VER = Connection.OLD_VER
        const val CONN_OK = Connection.CONN_OK
     //   const val DEVICE_DETAILS = Connection.DEVICE_DETAILS
        const val ASK_CODE_SIG = Connection.ASK_CODE_SIG
        const val WRONG_CODE = Connection.WRONG_CODE
    }

    override fun onDestroy() {
        parentJob.cancel()
 //       android.os.Process.killProcess(android.os.Process.myPid())
        super.onDestroy()
    }

    override fun onBackPressed() {
        parentJob.cancel()
  //      android.os.Process.killProcess(android.os.Process.myPid())
        super.onBackPressed()
    }

    private fun getMobileDataState(): Int {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.dataState
    }

    private suspend fun testConnectionAsync(bytes:ByteArray, timeOut:Int) : Deferred<String> =
        coroutineScope {
            async(Dispatchers.IO) {
                var res =""
                if (Connection.BROAD_ADDR != "null") {
                    try {
                        //prepare socket
                        val sendSocket=DatagramSocket()
                        val sendPacket=DatagramPacket(bytes,bytes.size, InetAddress.getByName(BROAD_ADDR),receiverPort)
                        sendSocket.soTimeout=timeOut

                        //send broadcast
                        sendSocket.send(sendPacket)

                        //if successful then close
                        res="send successfully"
                        sendSocket.close()
                    } catch (e:Exception) {
                        res="error: $e"
                    }
                } else {
                    val mobileState = getMobileDataState()
                    if (mobileState== 0) {
                        for (i in 0..255) {
                            val dSocket = DatagramSocket()
                            try {
                                val address = InetAddress.getByName("192.168.$i.255")
                                val sendPacket = DatagramPacket(bytes, bytes.size, address, 4444)
                                dSocket.send(sendPacket)
                                BROAD_ADDR = "192.168.$i.255"
                                searching = true
                                dSocket.soTimeout = 2000
                                while (searching) {
                                    try {
                                        dSocket.send(sendPacket)
                                        val buff = ByteArray(100)
                                        val receivePacket = DatagramPacket(buff, buff.size)
                                        dSocket.receive(receivePacket)
                                  //      val str = String(buff, 0, receivePacket.length)
                                        res ="send successfully"
                                        break
                                    } catch (e:Exception) { }
                                }
                                break
                            } catch (e:Exception) {
                                res = "error: User Interrupt"
                            }
                        }
                    } else {
                        val dSocket = DatagramSocket()
                        var searching = true
                        dSocket.soTimeout = 1000
                        while(searching)
                            try {
                                for (i in 0..255) {
                                    val address = InetAddress.getByName("192.168.$i.255")
                                    val sendPacket = DatagramPacket(bytes, bytes.size, address, 4444)
                                    dSocket.send(sendPacket)
                                    //   handler.obtainMessage(0, "192.168.$i.255 searching").sendToTarget()
                                }
                                val buff= ByteArray(100)
                                val receivePacket = DatagramPacket(buff, buff.size)
                                dSocket.receive(receivePacket)
                                val add = receivePacket.address
                                val resAddress = add.toString().substring(1)
                                val finalIndexOfDot = resAddress.lastIndexOf('.')
                                val broadAddr = resAddress.substring(0, finalIndexOfDot) + ".255"
                                BROAD_ADDR = broadAddr
                                searching = false
                                res = "send successfully"
                            } catch (e:Exception) { }
                    }
                }
                return@async res
            }
        }

    private  suspend fun broadcastSearchAsync() : Deferred<String> =
            coroutineScope {
                async (Dispatchers.IO) {
                    var res:String
                    try {
                        val sendSocket = DatagramSocket()
                        val searchBytes = ("$APP_CODE/*$SEARCH_CODE*/").toByteArray()
                        val searchPacket = DatagramPacket(searchBytes, searchBytes.size, InetAddress.getByName(
                            BROAD_ADDR), PORT)
                        val receiverList = ArrayList<String>()
                        step1 = true
                        while (step1) {
                            try {
                                sendSocket.soTimeout = 4000
                                // handler.obtainMessage(0, "Search packet sent").sendToTarget()
                                sendSocket.send(searchPacket)
                                val buff= ByteArray(1024)
                                val receivePacket = DatagramPacket(buff, buff.size)
                                sendSocket.receive(receivePacket)
                                val resp = String(buff,0, buff.size)
                                // handler.obtainMessage(0, "received: $resp").sendToTarget()
                                if (resp.contains(APP_CODE)) {
                                    val firstNameIndex = resp.indexOf("/*")
                                    val lastIndex = resp.indexOf("*/")
                                    val deviceName = resp.substring(firstNameIndex+2, lastIndex)
                                    if (!receiverList.contains(deviceName)) {
                                        val jArray = JSONArray()
                                        jArray.put(0,deviceName)
                                        jArray.put(1, receivePacket.address.toString())
                                        receiverList.add(deviceName)
                                        receiverHandler.obtainMessage(0, jArray).sendToTarget()
                                    }
                                }
                                delay(2000)
                            } catch (e:Exception) {
                                // handler.obtainMessage(0, "no response").sendToTarget()
                            }
                        }

                        //in step 2
                        // handler.obtainMessage(0, "Entering in step 2").sendToTarget()
                        val nextSigBytes = "$APP_CODE#${Connection.username}$NEXT_SIG_END".toByteArray()
                        val sendPacket = DatagramPacket(nextSigBytes, nextSigBytes.size, InetAddress.getByName(
                            BROAD_ADDR), PORT)
                        sendSocket.send(sendPacket)
                        // handler.obtainMessage(0, "next step signal send").sendToTarget()
                        delay(1000)
                        val connSignal = CONNECT_SIG.toByteArray()
                        val sendPacket1 = DatagramPacket(connSignal, connSignal.size, InetAddress.getByName(receiverAddress), PORT)
                        sendSocket.send(sendPacket1)
                        // handler.obtainMessage(0, "Conn signal send to $receiverAddress").sendToTarget()


                        // in step 3 accept mac address and check if is in list
                        val buff= ByteArray(1024)
                        val receivePacket = DatagramPacket(buff, buff.size)
                        sendSocket.receive(receivePacket)
                        val connSig = String(buff,0,receivePacket.length)
                        val freeIndex = connSig.indexOf("/*")
                        val verIndex = connSig.indexOf("*/")
                        val endIndex = connSig.indexOf("@")
                        val mac = connSig.substring(0, freeIndex)
                        val free = connSig.substring(freeIndex+2, verIndex)
                        Connection.freeSpace = free.toLong()
                        val ver = connSig.substring(verIndex+2, endIndex)
                        // handler.obtainMessage(0, "mac received $mac").sendToTarget()
                        var oldDevice =false
                        if(data1Dir.list()!=null && data1Dir.list()!!.isNotEmpty()) {
                            if(data1Dir.list()!!.contains(Connection.partnerName)) {
                                val macFile = File(data1Dir,Connection.partnerName)
                                val fileReader = macFile.inputStream()
                                val buff1 = ByteArray(1024)
                                val read = fileReader.read(buff1)
                                val macStr = String(buff1, 0, read)
                                if (macStr == mac) {
                                    // handler.obtainMessage(0, "this is an old device").sendToTarget()
                                    if (ver < Connection.MIN_VER_NEED) {
                                        val oldSignal = OLD_VER.toByteArray()
                                        val sendPacket2 = DatagramPacket(oldSignal,oldSignal.size, InetAddress.getByName(receiverAddress),
                                            PORT)
                                        sendSocket.send(sendPacket2)
                                        backToNormalHandler.obtainMessage(0, "Receiver using older version.. try again!").sendToTarget()
                                    } else {
                                        val okSignal = CONN_OK.toByteArray()
                                        val sendPacket2 = DatagramPacket(okSignal,okSignal.size, InetAddress.getByName(receiverAddress),
                                            PORT)
                                        sendSocket.send(sendPacket2)
                                        Connection.connection = true
                                        Connection.partnerAddress = InetAddress.getByName(receiverAddress)
                                    }
                                    oldDevice = true
                                }
                        //       else { handler.obtainMessage(0, "This is new device").sendToTarget() }
                            }
                        }
                        if(!oldDevice) {
                            //ask for code
                            val askCodeBytes = ASK_CODE_SIG.toByteArray()
                            val sendPacket2 = DatagramPacket(askCodeBytes, askCodeBytes.size, InetAddress.getByName(receiverAddress), PORT)
                            sendSocket.send(sendPacket2)
                            // handler.obtainMessage(0, "code :").sendToTarget()

                            // show code----
                            val conCode = generateCode()
                            showCodeHandler.obtainMessage(0, conCode).sendToTarget()


                            val buff1 = ByteArray(1024)
                            sendSocket.soTimeout = 30000
                            val receivePacket2 = DatagramPacket(buff1, buff1.size)
                            sendSocket.receive(receivePacket2)
                            val codeStr = String(buff1,0,receivePacket2.length)
                            val codeIndex = codeStr.indexOf("&")
                            val freeIndex1 = codeStr.indexOf("/*")
                            // val lastIndex = codeStr.indexOf("*/")
                            val code = codeStr.substring(codeIndex+1,freeIndex1)
                            // val free = codeStr.substring(freeIndex1+2,lastIndex)
                            if (code == conCode) {
                                // handler.obtainMessage(0, "right code received, free: ${formatDataString(free.toLong(),' ')}").sendToTarget()
                                val okSignal = CONN_OK.toByteArray()
                                val sendPacket3 = DatagramPacket(okSignal,okSignal.size, InetAddress.getByName(receiverAddress),
                                    PORT)
                                sendSocket.send(sendPacket3)
                                val file = File(data1Dir, Connection.partnerName)
                                file.createNewFile()
                                val oStream = file.outputStream()
                                val macBytes = mac.toByteArray()
                                oStream.write(macBytes)
                                oStream.flush()
                                oStream.close()
                                Connection.connection = true
                                Connection.partnerAddress = InetAddress.getByName(receiverAddress)
                            } else {
                                // handler.obtainMessage(0, "wrong code received, please try again").sendToTarget()
                                val wrongSignal = WRONG_CODE.toByteArray()
                                val sendPacket4 = DatagramPacket(wrongSignal, wrongSignal.size, InetAddress.getByName(receiverAddress),
                                    PORT)
                                sendSocket.send(sendPacket4)
                                backToNormalHandler.obtainMessage(0, "Wrong code sent by receiver.., Try again!").sendToTarget()
                            }
                        }
                        res = "Complete"
                        sendSocket.close()
                    } catch (e:Exception) {
                        res = "Error :$e"
                    }
                    return@async res
                }
            }
}