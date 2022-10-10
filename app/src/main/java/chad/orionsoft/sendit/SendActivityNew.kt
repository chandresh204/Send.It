package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import chad.orionsoft.sendit.databinding.ActivitySendNewBinding

class SendActivityNew : AppCompatActivity() {

    private lateinit var binding: ActivitySendNewBinding
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendNewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.senderBelowBarText.text=
                "sending to: ${Connection.partnerName} , ${Connection.partnerAddress}"
        binding.senderBelowBarText.isSelected= true
    }

    fun goBack(v: View) {
        v.id
        onBackPressed()
    }
}
