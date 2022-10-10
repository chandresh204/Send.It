package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.content.*
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import chad.orionsoft.sendit.databinding.ActivityConnectionCheckBinding
import java.lang.reflect.Method
import java.math.BigInteger
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteOrder

class ConnectionCheck : AppCompatActivity() {

    private lateinit var wManager:WifiManager
    private lateinit var apMethod: Method
    private var apState:Int=0
    private lateinit var wifiStateChangeReceiver:BroadcastReceiver
    private var deviceMode:Int=0
    private lateinit var binding: ActivityConnectionCheckBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectionCheckBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_connection_check)
        deviceMode=intent.getIntExtra(MODE_INTENT_STRING,-1)
        wManager= applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        apMethod=wManager.javaClass.getMethod("getWifiApState")
        apState=apMethod.invoke(wManager) as Int
        updateUI(UI_NOTCONNECTED)
        checkNetwork()

        wifiStateChangeReceiver=object: BroadcastReceiver() {

            override fun onReceive(p0: Context?, p1: Intent?) {
                checkNetwork()
            }

        }
        registerReceiver(wifiStateChangeReceiver, IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION))
        registerReceiver(wifiStateChangeReceiver, IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED"))
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(mode:Int) {
        when(mode) {
            UI_CONNECTED -> {
                binding.connectionInfo.text=resources.getString(R.string.wlan_connection_found)
                binding.hotspotButton.visibility= View.GONE
                binding.wifiButton.visibility=View.GONE
                binding.createHotspotInfo.visibility=View.GONE
                binding.createWifiInfo.visibility=View.GONE
                binding.nextButton.visibility=View.VISIBLE
                binding.nextButton.setOnClickListener {
                    when(deviceMode) {
                        SENDER_MODE -> {
                            startActivity(Intent(applicationContext,CreateConnectionSender::class.java))
                            finish()
                        }
                        RECEIVER_MODE -> {
                            startActivity(Intent(applicationContext,CreateConnectionReceiver::class.java))
                            finish()
                        }
                        else -> {
                            Toast.makeText(applicationContext,"an error occurred",Toast.LENGTH_LONG).show()
                            finish()
                        }
                    }
                }
            }
            UI_NOTCONNECTED -> {
                binding.connectionInfo.text=resources.getString(R.string.wlan_connection_required)
                binding.hotspotButton.visibility=View.VISIBLE
                binding.hotspotButton.setOnClickListener {
                    val i=Intent(Intent.ACTION_MAIN,null)
                    val cName=ComponentName("com.android.settings","com.android.settings.TetherSettings")
                    i.component = cName
                    startActivity(i)
                    Toast.makeText(applicationContext,"Please turn on Mobile Hotspot",Toast.LENGTH_LONG).show()
                }
                binding.wifiButton.visibility=View.VISIBLE
                binding.wifiButton.setOnClickListener {
                    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
                binding.createHotspotInfo.visibility=View.VISIBLE
                binding.createWifiInfo.visibility=View.VISIBLE
                binding.nextButton.visibility=View.GONE
                binding.createWifiInfo.text=resources.getString(R.string.connect_wifi_info)
            }
            UI_NOTCONNECTED_ON_WIFI -> {
                binding.connectionInfo.text=getString(R.string.wlan_connection_required)
                binding.hotspotButton.visibility=View.VISIBLE
                binding.hotspotButton.setOnClickListener {
                    val i=Intent(Intent.ACTION_MAIN,null)
                    val cName=ComponentName("com.android.settings","com.android.settings.TetherSettings")
                    i.component = cName
                    startActivity(i)
                    Toast.makeText(applicationContext,"Please turn on Mobile Hotspot",Toast.LENGTH_LONG).show()
                }
                binding.wifiButton.visibility=View.VISIBLE
                binding.wifiButton.setOnClickListener {
                    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
                binding.wifiButton.text="Setup wifi"
                binding.createHotspotInfo.visibility=View.VISIBLE
                binding.createWifiInfo.visibility=View.VISIBLE
                binding.nextButton.visibility=View.GONE
                binding.createWifiInfo.text="wifi is enabled but not connected to any network"
            }
        }
    }

    private fun checkNetwork() : Boolean {
        var connection=false
        updateUI(UI_NOTCONNECTED)
        apState=apMethod.invoke(wManager) as Int
        if(apState==11) {
            if(wManager.isWifiEnabled) {
                val supState = wManager.connectionInfo.supplicantState
                if (supState == SupplicantState.COMPLETED) {
                    connection = true
                    val broadAddr = getBroadcastAddressWifi()
                    if (broadAddr != "null") {
                        Connection.BROAD_ADDR = broadAddr
                        // Toast.makeText(applicationContext, "Broadcast address: $broadAddr", Toast.LENGTH_SHORT).show()
                    }
                    updateUI(UI_CONNECTED)
                } else {
                    updateUI(UI_NOTCONNECTED_ON_WIFI)
                }
            }
        } else if(apState==13) {
            connection=true
            updateUI(UI_CONNECTED)
        }
        return connection
    }

    override fun onResume() {
        super.onResume()
        checkNetwork()
    }

    private fun getBroadcastAddressWifi(): String {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        var connInfo = wm.connectionInfo.ipAddress
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            connInfo = Integer.reverseBytes(connInfo)
        }
        val ipByteArray = BigInteger.valueOf(connInfo.toLong()).toByteArray()
        return try {
            val ipString = InetAddress.getByAddress(ipByteArray).hostAddress
            val lastIndexOfDot = ipString.lastIndexOf('.')
            ipString.substring(0, lastIndexOfDot) + ".255"
        } catch (e: UnknownHostException) {
            "null"
        }
    }

    companion object {
        const val UI_NOTCONNECTED=0
        const val UI_CONNECTED=1
        const val UI_NOTCONNECTED_ON_WIFI=2
        const val SENDER_MODE=0
        const val RECEIVER_MODE=1
        const val MODE_INTENT_STRING="connection_mode"
    }
}
