package chad.orionsoft.sendit

import java.io.File

class SendObject(val name:String,val path:String) {
    val file= File(path)
    val size=file.length()
}