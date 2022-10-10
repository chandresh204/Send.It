package chad.orionsoft.sendit

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import chad.orionsoft.sendit.databinding.ActivityEditorBinding

class EditorActivity : AppCompatActivity() {

    private lateinit var prefs:SharedPreferences
    private lateinit var editor:SharedPreferences.Editor
    private lateinit var binding: ActivityEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs=getSharedPreferences(Connection.PREFS, Context.MODE_PRIVATE)

        val username=prefs.getString(Connection.PREFS_USERNAME,"")
        binding.usernameEditText.setText(username)

        val maxMemAllowed = prefs.getInt(MAX_IMAGE_MEM, 10)
        binding.maxMemEdit.setText(maxMemAllowed.toString())
    }

    fun finishEdit(v: View) {
        v.id
        if(binding.usernameEditText.text.toString().isEmpty()) {
            binding.usernameEditText.error="Invalid"
            return
        }
        if (binding.maxMemEdit.text.toString().toInt() < 2 || binding.maxMemEdit.text.toString().toInt() > 20) {
            binding.maxMemEdit.error = "Should be between 2 and 20"
            return
        }
        val newUsername = binding.usernameEditText.text.toString()
        editor=prefs.edit()
        editor.putString(Connection.PREFS_USERNAME,newUsername).apply()
        editor.putInt(MAX_IMAGE_MEM, binding.maxMemEdit.text.toString().toInt()).apply()
        Connection.maxImageMemAllowed = binding.maxMemEdit.text.toString().toInt() * 1024 * 1024
        Toast.makeText(applicationContext,"Values updated",Toast.LENGTH_SHORT).show()
        finish()
    }

    fun cancelEdit(v:View) {
        v.id
        finish()
    }

    companion object {
        const val MAX_IMAGE_MEM = "maxImageMem"
    }
}
