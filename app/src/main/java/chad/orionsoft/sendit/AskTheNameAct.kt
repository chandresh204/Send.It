package chad.orionsoft.sendit

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import chad.orionsoft.sendit.databinding.ActivityAskTheNameBinding

class AskTheNameAct : AppCompatActivity() {

    private lateinit var prefs:SharedPreferences
    private lateinit var editor:SharedPreferences.Editor
    private var nextAct:Int=0
    private lateinit var binding: ActivityAskTheNameBinding
    private lateinit var nameEditText : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAskTheNameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        nameEditText = binding.nameEditText
        prefs=getSharedPreferences("prefs", Context.MODE_PRIVATE)
        nextAct=intent.getIntExtra("nextAct",0)
        val newUser=prefs.getBoolean("newUser",true)
        val typeface=Typeface.createFromAsset(assets,"fonts/opensans-regular.ttf")
        nameEditText.typeface=typeface
        if(!newUser) {
            goToNextActivity()
        }
    }

    fun setName(v: View) {
        v.id
        if(nameEditText.text.isNullOrEmpty()) {
            nameEditText.error="invalid"
        } else {
            val username=nameEditText.text.toString()
            editor=prefs.edit()
            editor.putString("username",username)
            editor.putBoolean("newUser",false)
            editor.apply()
            Toast.makeText(applicationContext,"Name is set To: $username",Toast.LENGTH_SHORT).show()
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),0)
                } else {
                    goToNextActivity()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
           goToNextActivity()
        } else {
            finish()
        }
    }

    private fun goToNextActivity() {
        when(nextAct) {
            0 -> {
                startActivity(Intent(applicationContext,MainActivity::class.java))
                finish()
            }
            1 -> {
                startActivity(Intent(applicationContext,SendActivityEX::class.java))
                finish()
            }
        }
    }
}
