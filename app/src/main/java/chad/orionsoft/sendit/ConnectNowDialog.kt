package chad.orionsoft.sendit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View

class ConnectNowDialog : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect_now_dialog)
    }

    fun connectNow(v: View) {
        v.id
        startActivity(Intent(applicationContext,SendOptions::class.java))
        Handler(mainLooper).postDelayed({
            val i=Intent(applicationContext,ConnectionCheck::class.java)
            i.putExtra(ConnectionCheck.MODE_INTENT_STRING,ConnectionCheck.SENDER_MODE)
            startActivity(i)
        },200)
        finish()
    }

    fun connectLater(v:View) {
        v.id
        startActivity(Intent(applicationContext,SendOptions::class.java))
        finish()
    }

}
