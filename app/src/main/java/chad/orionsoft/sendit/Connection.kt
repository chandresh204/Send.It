package chad.orionsoft.sendit

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import java.io.File
import java.net.InetAddress
import java.util.*
import kotlin.math.roundToLong

class Connection {

    companion object {
        const val APP_CODE="sendit004"
        const val MIN_VER_NEED= "210"
        const val THIS_VER = "221"
        const val RECEIVER_OK="${APP_CODE}_OK"
        const val ON_RECEIVE_RESPONSE="${APP_CODE}_received" //+ "/*freeSpace*/
        const val SENDING_STOP="sender_stop"
        const val saveDir="send_it"
        const val receiverPort=4444
        const val MODE_SENDER=0
        const val SEARCH_CODE = "Searching"
        var BROAD_ADDR = "null"
        const val NEXT_SIG_END = "/*ENTER_STEP2*/"
        const val CONNECT_SIG= "$APP_CODE/*CONN_NOW*/"
        const val ASK_CODE_SIG = "$APP_CODE/*ASK_CODE*/"
        const val CONN_OK = "$APP_CODE/*CONN_OK*/"
        const val WRONG_CODE = "$APP_CODE/*WRONG*/"
        const val OLD_VER = "$APP_CODE/*OLD*/"
        const val PREFS = "prefs"
        const val PREFS_USERNAME = "username"
        const val PREFS_RID = "randomId"
        const val DEVICE_DETAILS = "DEVICEDETAILS"
        private const val MODE_RECEIVER=1
        const val MODE_FILES="files" //+ "#count"
        const val MODE_INTERCHANGE="interchange"
        const val MODE_STOP = "stop"
        const val TYPE_IMAGE = 1
        const val TYPE_AUDIO = 2
        const val TYPE_VIDEO = 3
        const val TYPE_OTHER = 0

        var connection=false
        var myRandomId =""
 //       var broadcastAddress:InetAddress=InetAddress.getByName("192.168.72.255")
        var partnerAddress:InetAddress=InetAddress.getByName("")
        var isScoped = false
        var username=""
        var partnerName=""
        var freeSpace:Long=0
        var lastSearched=""
        var mode= MODE_RECEIVER
        var maxImageMemAllowed = 10 * 1024 * 1024

        //added for notification
        const val NOTIFICATION_CHANNEL_ID = "send_it"
        const val NOTIFICATION_CHANNEL = "send_it_channel"


        fun findICON(ctx: Context, fileName:String) : Drawable? {
            val iconResource:Int=

            when(File(fileName).extension.lowercase()) {
                //set Image icon
                "jpg","jpeg","tiff","png","gif","bmp" -> {
                    R.drawable.image_icon
                }

                //set audio icon
                "mp3","aac","flac","m4a","wma","wav","aax","act","aiff","alac","amr","ape","au","raw" -> {
                    R.drawable.music_icon
                }

                //set video icon
                "mp4","webm","avi","mkv","flv","vob","ogg","wmv","m4v","mpg","mpeg","3gp","f4v" -> {
                    R.drawable.video_icon
                }

                //set apk icon
                "apk" -> {
                    R.drawable.apk_icon
                }

                //set document icon
                "doc","docx","docm","dotx","dotm","docb","odt","ott","odm" -> {
                    R.drawable.doc_icon
                }

                //set sheet icon
                "xls","xlsx","xlt","xlsb","xst","xla","ods","ots" -> {
                    R.drawable.sheet_icon
                }

                //set presentation icon
                "ppt","pot","pps","pptx","pptm","potx","ppsx","sldx","sldm","odp","otp" -> {
                    R.drawable.presentation_icon
                }

                //set pdf icon
                "pdf" -> {
                    R.drawable.pdf_icon
                }

                //set archive icon
                "zip","rar","7z","tar","war","iso","gz","xz","lz","lzo","jar" -> {
                    R.drawable.archive_icon
                }

                //set windows icon
                "exe","msi" -> {
                    R.drawable.windows_software_icon
                }

                //set debian icon
                "deb" -> {
                    R.drawable.debian_icon
                }

                //set Text Icon
                "txt","c","c++","java","py","kt" -> {
                    R.drawable.text_icon
                }

                //set unknown icon
                else -> {
                    R.drawable.file_unknown_icon
                }
            }
         /*   return if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.LOLLIPOP) {
                ctx.resources.getDrawable(iconResource,ctx.resources.newTheme())
            } else {
                ctx.resources.getDrawable(iconResource)
            }  */
            return ResourcesCompat.getDrawable(ctx.resources ,iconResource, ctx.resources.newTheme())

     /*       if(fileName.toLowerCase().contains(".apk")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    drawable=ctx.resources.getDrawable(R.drawable.apk_icon,ctx.resources.newTheme())
                } else {
                    drawable=ctx.resources.getDrawable(R.drawable.apk_icon)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    drawable=ctx.resources.getDrawable(R.drawable.file_unknown_icon,ctx.resources.newTheme())
                } else {
                    drawable=ctx.resources.getDrawable(R.drawable.file_unknown_icon)
                }
            }
            return drawable  */
        }

        fun findICONSmall(ctx:Context,fileName:String) : Drawable? {
            val iconResource:Int=

            when(File(fileName).extension.lowercase()) {
                //set Image icon
                "jpg","jpeg","tiff","png","gif","bmp" -> {
                    R.drawable.image_icon_small
                }

                //set audio icon
                "mp3","aac","flac","m4a","wma","wav","aax","act","aiff","alac","amr","ape","au","raw" -> {
                    R.drawable.music_icon_small
                }

                //set video icon
                "mp4","webm","avi","mkv","flv","vob","ogg","wmv","m4v","mpg","mpeg","3gp","f4v" -> {
                    R.drawable.video_icon_small
                }

                //set apk icon
                "apk" -> {
                    R.drawable.apk_icon_small
                }

                //set pdf icon
                "pdf" -> {
                    R.drawable.pdf_icon_small
                }

                //set document icon
                "doc","docx","docm","dotx","dotm","docb","odt","ott","odm" -> {
                    R.drawable.doc_icon_small
                }

                //set sheet icon
                "xls","xlsx","xlt","xlsb","xst","xla","ods","ots" -> {
                    R.drawable.sheet_icon_small
                }

                //set presentation icon
                "ppt","pot","pps","pptx","pptm","potx","ppsx","sldx","sldm","odp","otp" -> {
                    R.drawable.presentation_icon_small
                }

                //set windows icon
                "exe","msi" -> {
                    R.drawable.windows_software_icon_small
                }

                //set debian icon
                "deb" -> {
                    R.drawable.debian_icon_small
                }

                //set archive icon
                "zip","rar","7z","tar","war","iso","gz","xz","lz","lzo","jar" -> {
                    R.drawable.archive_icon_small
                }

                //set Text Icon
                "txt","c","c++","java","py","kt" -> {
                    R.drawable.text_icon_small
                }

                //set unknown icon
                else -> {
                    R.drawable.file_unknown_icon_small
                }
            }
            return ResourcesCompat.getDrawable(ctx.resources, iconResource, ctx.resources.newTheme())

        /*    var drawable:Drawable
            if( File(fileName).extension.contains("apk")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    drawable=ctx.resources.getDrawable(R.drawable.apk_icon_small,ctx.resources.newTheme())
                } else {
                    drawable=ctx.resources.getDrawable(R.drawable.apk_icon_small)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    drawable=ctx.resources.getDrawable(R.drawable.file_unknown_icon_small,ctx.resources.newTheme())
                } else {
                    drawable=ctx.resources.getDrawable(R.drawable.file_unknown_icon_small)
                }
            }
            return drawable  */
        }

        fun getContentTypeFromName(filename:String) : Int {

            return when(File(filename).extension.lowercase()) {

                //set Image type
                "jpg","jpeg","tiff","png","gif","bmp" -> {
                    TYPE_IMAGE
                }

                //set audio type
                "mp3","aac","flac","m4a","wma","wav","aax","act","aiff","alac","amr","ape","au","raw" -> {
                    TYPE_AUDIO
                }

                //set video type
                "mp4","webm","avi","mkv","flv","vob","ogg","wmv","m4v","mpg","mpeg","3gp","f4v" -> {
                    TYPE_VIDEO
                }

                else -> TYPE_OTHER
            }
        }

        fun formatDataString(bytes: Long,separatorChar:Char): String {
            val str:String
            when {
                bytes < 1024 -> str = "$bytes${separatorChar}B"
                bytes in 1024..1048575 -> {
                    val inKB = (bytes / 1024)
                    str = "$inKB${separatorChar}kB"
                }
                bytes in 1048576..1073741823 -> {
                    val inMB1 = (bytes.toFloat() / 1048576)
                    val inMB2 = inMB1 * 100
                    val inMB3 = inMB2.roundToLong()
                    val inMB = (inMB3.toFloat() / 100)
                    str = "$inMB${separatorChar}MB"
                }
                else -> {
                    val inGB1 = (bytes.toFloat() / 1073741824)
                    val inGB2 = inGB1 * 100
                    val inGB3 = inGB2.roundToLong()
                    val inGB = (inGB3.toFloat() / 100)
                    str = "$inGB${separatorChar}GB"
                }
            }
            return str
        }

        fun formatDuration(duration: Int): String {
            val cal = Calendar.getInstance()
            cal.timeInMillis = duration.toLong() - cal.timeZone.rawOffset
            val formatter = Formatter()
            val hr = cal.get(Calendar.HOUR)
            return if (hr > 0) {
                formatter.format("%tH:%tM:%tS", cal, cal, cal).toString()
            } else {
                formatter.format("%tM:%tS", cal, cal).toString()
            }
        }
    }

    // sender Pattern : APP_CODE;senderName*/
    //receiver Pattern: APP_CODE;receiverName/*code*/
    //sender mode pattern: APP_CODE/*mode*/
    //sender file pattern: filename/*filesize*/
    //on received response pattern: ON_RECEIVE_RESPONSE/*freespaceinLong*/
}