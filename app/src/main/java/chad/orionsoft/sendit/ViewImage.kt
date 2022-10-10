package chad.orionsoft.sendit

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import chad.orionsoft.sendit.databinding.ActivityViewImageBinding
import java.io.File

class ViewImage : AppCompatActivity() {

    private lateinit var binding: ActivityViewImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val path=intent.getStringExtra("path")
        val imageFile=File(path!!)
        val dataSize=imageFile.length()
        if (dataSize >= Connection.maxImageMemAllowed) {
            val info= Connection.formatDataString(dataSize,' ')
            binding.imageInfo.text=info
            binding.imageTitle.text=imageFile.name
            Toast.makeText(applicationContext, "Image is too large to preview", Toast.LENGTH_SHORT).show()
            return
        }
        val imageBitMap = BitmapFactory.decodeFile(path) ?: return
        binding.fullImage.setImageBitmap(imageBitMap)
        val width=imageBitMap.width
        val height=imageBitMap.height
        val info="$width x $height ${Connection.formatDataString(dataSize,' ')}"
        binding.imageInfo.text=info
        binding.imageTitle.text=imageFile.name
    }

    fun goBack(v: View) {
        v.id
        finish()
    }
}
