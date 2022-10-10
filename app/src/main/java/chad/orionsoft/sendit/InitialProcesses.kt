package chad.orionsoft.sendit

import android.content.Context
import android.graphics.drawable.Drawable
import kotlinx.coroutines.*
import java.io.File

class InitialProcesses {

    companion object {

        val appList = ArrayList<AppObject>()
        val appListWithSystem = ArrayList<AppObject>()
        var isAppListPrepared = false
     //   var currentLoading = 0
    //    private val thumbDir = File(Environment.getExternalStorageDirectory(), "Send_it/.thumbnails")
   //     private val imagesList= ArrayList<ImageObject>()

        suspend fun createAppArraysAsync(ctx: Context) =
            coroutineScope {
                async(Dispatchers.IO) {
                    val appsAll = ctx.packageManager.getInstalledApplications(0)
                    for (i in appsAll) {
                        val appName = i.loadLabel(ctx.packageManager) as String
                        val appSource = i.sourceDir
                        val packageName = i.packageName
                        val icon = i.loadIcon(ctx.packageManager)
                        if (appSource.contains("/system/")) {
                            appListWithSystem.add(AppObject(appName, packageName, icon, appSource, false))
                        } else {
                            appList.add(AppObject(appName, packageName, icon, appSource, false))
                        }
                    }
                    appList.sortBy { it.appName.lowercase() }
                    appListWithSystem.sortBy { it.appName.lowercase() }
                    isAppListPrepared = true
                }
            }

   /*     fun startThumbGeneration(ctx: Context) {

            Toast.makeText(ctx,"Thumbs generation started",Toast.LENGTH_SHORT).show()
            if (!thumbDir.exists())
                thumbDir.mkdirs()
            getImages(ctx)
            GlobalScope.launch(Dispatchers.Main) {
                for (i in 0 until imagesList.size) {
                    currentLoading++
                    waitForthumbsAsync().await()
                    async {
                        generateOneThumbAsync(imagesList[i].id, imagesList[i].title, imagesList[i].imageDATA)
                    }
                }
            }
        }


        private fun getImages(ctx:Context) {
            val imageURI= MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val curser= ctx.contentResolver.query(imageURI,null,null,null,null)
            if(curser!=null && curser.moveToFirst()) {

                var count=0
                val indexID=curser.getColumnIndex(MediaStore.Images.Media._ID)
                val indexDATa=curser.getColumnIndex(MediaStore.Images.Media.DATA)
                val titleIndex=curser.getColumnIndex(MediaStore.Images.Media.TITLE)

                do {
                    val thisID=curser.getLong(indexID)
                    val data=curser.getString(indexDATa)
                    val title=curser.getString(titleIndex)
                    count++
                    imagesList.add(ImageObject(thisID, data, title))
                } while (curser.moveToNext())
            }
            imagesList.sortByDescending { imageObject -> imageObject.lastModified }
        }


        private suspend fun waitForthumbsAsync() =
            coroutineScope {
                async(Dispatchers.IO) {
                    while(currentLoading>5) ;
                }
            }

        private suspend fun generateOneThumbAsync(id:Long, name:String, path:String) =
            coroutineScope {
                async(Dispatchers.IO) {
                    val fileName="$id:$name"
                    val thumbFile=File(thumbDir,fileName)
                    if(!thumbFile.exists()) {
                        val bitmap= BitmapFactory.decodeFile(path)
                        val asRation=bitmap.width.toFloat()/bitmap.height.toFloat()
                        val newHeight=300/asRation
                        val smallMap= Bitmap.createScaledBitmap(bitmap,300,newHeight.toInt(),false)
                        smallMap.compress(Bitmap.CompressFormat.JPEG,90,thumbFile.outputStream())
                    }
                    currentLoading--
                }
            }  */

        class AppObject (val appName:String, val appPackage:String, val appIcon: Drawable, val appSource:String, var isSelected:Boolean) {
            val objectSize= File(appSource).length()
        }

    /*    class ImageObject(val id:Long, private val imageDATA: String, val title:String) {

            private val imageFile=File(imageDATA)
            val size=imageFile.length()
            val lastModified=imageFile.lastModified()
        }  */
    }
}