package chad.orionsoft.sendit

import java.io.File

class FileObject(val filePath:String,var isSelected:Boolean) {
    val file= File(filePath)
    val fileName:String=file.name
    val extension:String=file.extension
    val fileSize=file.length()
}