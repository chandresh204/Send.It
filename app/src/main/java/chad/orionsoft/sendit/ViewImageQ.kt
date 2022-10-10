package chad.orionsoft.sendit

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import chad.orionsoft.sendit.databinding.ActivityViewImageBinding

class ViewImageQ : AppCompatActivity() {

    private lateinit var binding: ActivityViewImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val uriStr=intent.getStringExtra("uri")
        val uri = Uri.parse(uriStr)
        val dataSize= intent.getLongExtra("size",0L)
        if (dataSize >= Connection.maxImageMemAllowed) {
            Toast.makeText(applicationContext, "Image too large to preview", Toast.LENGTH_SHORT).show()
            val info= Connection.formatDataString(dataSize,' ')
            binding.imageInfo.text=info
            binding.imageTitle.text= intent.getStringExtra("title")
            return
        }
        val imageBitMap= BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
        binding.fullImage.setImageBitmap(imageBitMap)
        try {
            val width=imageBitMap.width
            val height=imageBitMap.height
            val info="$width x $height ${Connection.formatDataString(dataSize,' ')}"
            binding.imageInfo.text=info
            binding.imageTitle.text= intent.getStringExtra("title")
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Error loading image", Toast.LENGTH_SHORT).show()
        }

    }

    fun goBack(v: View) {
        v.id
        finish()
    }
}