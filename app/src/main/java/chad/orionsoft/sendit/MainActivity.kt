package chad.orionsoft.sendit

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.TranslateAnimation
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.FileProvider
import chad.orionsoft.sendit.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        Connection.username = prefs.getString("username", "unknown") as String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Connection.isScoped = true
        }
        Handler(mainLooper).postDelayed({
            Animator.moveUP_IN(binding.optionsLayout.root,700)
            binding.optionsLayout.root.visibility=View.VISIBLE
            TranslateAnimation(0f,0f,0f,-100f).apply {
                duration=700
                binding.senditBanner.startAnimation(this)
            }
            Handler(mainLooper).postDelayed({
                binding.senditBanner.translationY=-100f
            },690)
        },2000)
        generateRandomIDandUsername()
        startAsyncInitialProcess()
        binding.mainMenu.setOnClickListener {
            val popupMenu = PopupMenu(applicationContext, it)
            menuInflater.inflate(R.menu.main_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.menu_app_send -> {
                        val sourceFile = File(applicationInfo.sourceDir)
                        val targetFile = File(applicationContext.getExternalFilesDir("source"),
                            "send.it ${resources.getString(R.string.app_version)}.apk")
                        if (sourceFile.copyRecursively(targetFile, true)) {
                            val apkUri = FileProvider.getUriForFile(applicationContext,
                                "$packageName.provider", targetFile)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, apkUri)
                            }
                            startActivity(Intent.createChooser(sendIntent, "send Via: "))
                            Toast.makeText(applicationContext, "Bluetooth is recommended", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(applicationContext, "Failed to create target file..", Toast.LENGTH_SHORT).show()
                        }
                    }
                    R.id.menu_about -> {
                        startActivity(Intent(applicationContext, AboutActivity::class.java))
                    }
                }
                true
            }
            popupMenu.show()
        }
    }

    fun startSendActivity(v: View) {
        v.id
        startActivity(Intent(applicationContext, ConnectNowDialog::class.java))
    }

    fun startReceiveActivity(v: View) {
        v.id
        val i=Intent(applicationContext, ConnectionCheck::class.java)
        i.putExtra(ConnectionCheck.MODE_INTENT_STRING,ConnectionCheck.RECEIVER_MODE)
        startActivity(i)
        // startActivity(Intent(applicationContext,ReceiveNowQ::class.java))
    }

    fun startSettingActivity(v: View) {
        v.id
        startActivity(Intent(applicationContext, EditorActivity::class.java))
    }

    private fun generateRandomIDandUsername() {
        val prefs = getSharedPreferences(Connection.PREFS, Context.MODE_PRIVATE)
        var rId = prefs.getString(Connection.PREFS_RID,"null")
        if (rId == "null") {
            rId = (Random.nextInt(10000000, 99999999)).toString()
            val editor = prefs.edit()
            editor.putString(Connection.PREFS_RID, rId).apply()
        }
        Connection.myRandomId = rId!!
        // Toast.makeText(applicationContext,"my rId: ${Connection.myRandomId}", Toast.LENGTH_LONG).show()
        val username=prefs.getString(Connection.PREFS_USERNAME,"")
        val fullName = "$username - ${Build.MANUFACTURER}:${Build.MODEL}"
        Connection.username = fullName
        // get max image memory allowed
        Connection.maxImageMemAllowed = prefs.getInt(EditorActivity.MAX_IMAGE_MEM, 10) * 1024 *1024
    }

    private fun startAsyncInitialProcess() {
        GlobalScope.launch(Dispatchers.IO) {
      //      InitialProcesses.startThumbGeneration(applicationContext)
            InitialProcesses.createAppArraysAsync(applicationContext)
        }
    }

    override fun onBackPressed() {
        finishAffinity()
    }

}
