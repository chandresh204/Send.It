package chad.orionsoft.sendit

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView

class FileRecyAdapter(private val files:ArrayList<FileObject>,private val ctx: Context,private val mode:Int) :
    RecyclerView.Adapter<FileRecyAdapter.FileHolder>() {

    val fullList=ArrayList<FileObject>().apply {
        addAll(files)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileHolder {
        return FileHolder(LayoutInflater.from(ctx).inflate(R.layout.item_layout_file,parent,false))
    }

    override fun getItemCount(): Int = files.size

    override fun onBindViewHolder(holder: FileHolder, position: Int) {
        val fileLayout=holder.fileLayout
        val fileIcon=holder.fileIcon
        val fileName=holder.fileName
        val filePath=holder.filePath
        val fileSize=holder.fileSize
        val viewFile=holder.viewFile
        val file=files[position]
        fileIcon.setImageDrawable(Connection.findICONSmall(ctx,file.fileName))
        fileName.text=file.fileName
        filePath.text=file.filePath
        fileSize.text=Connection.formatDataString(file.fileSize,' ')
        if(file.isSelected) {
            fileName.isSelected=true
            filePath.isSelected=true
            fileLayout.background=ctx.resources.getDrawable(R.drawable.item_background_select)
        } else {
            fileName.isSelected=false
            filePath.isSelected=false
            fileLayout.background=ctx.resources.getDrawable(R.drawable.item_background)
        }
        fileLayout.setOnClickListener {
            if(file.isSelected) {
                file.isSelected=false
                fileName.isSelected=false
                filePath.isSelected=false
                fileLayout.background=ctx.resources.getDrawable(R.drawable.item_background)
                when(mode) {
                    MODE_PDF -> {
                        SendActivityPDF.selectedFiles--
                        SendActivityPDF.selectedSize-=file.fileSize
                        SendActivityPDF.updateBelowBar()
                    }
                    MODE_OFFICE -> {
                        SendActivityOffice.selectedFiles--
                        SendActivityOffice.selectedSize-=file.fileSize
                        SendActivityOffice.updateBelowBar()
                    }
                    MODE_SEARCH -> {
                        SendActivitySearch.selectedFiles--
                        SendActivitySearch.selectedSize-=file.fileSize
                        SendActivitySearch.updateBelowBar()
                    }
                }
            } else {
                file.isSelected=true
                fileName.isSelected=true
                filePath.isSelected=true
                fileLayout.background=ctx.resources.getDrawable(R.drawable.item_background_select)
                when(mode) {
                    MODE_PDF -> {
                        SendActivityPDF.selectedFiles++
                        SendActivityPDF.selectedSize+=file.fileSize
                        SendActivityPDF.updateBelowBar()
                    }
                    MODE_OFFICE -> {
                        SendActivityOffice.selectedFiles++
                        SendActivityOffice.selectedSize+=file.fileSize
                        SendActivityOffice.updateBelowBar()
                    }
                    MODE_SEARCH -> {
                        SendActivitySearch.selectedFiles++
                        SendActivitySearch.selectedSize+=file.fileSize
                        SendActivitySearch.updateBelowBar()
                    }
                }
            }
        }
        viewFile.setOnClickListener {
            val uri=FileProvider.getUriForFile(ctx,ctx.packageName+".provider",file.file)
            val mime=ctx.contentResolver.getType(uri)
            val i= Intent().apply {
                setDataAndType(uri,mime)
                action = Intent.ACTION_VIEW
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(i)
        }
    }

    val filter=object: Filter() {

        override fun performFiltering(p0: CharSequence?): FilterResults {
            val filteredList=ArrayList<FileObject>()
            val queryText=p0.toString().lowercase()
            for(i in fullList) {
                if(i.fileName.lowercase().trim().contains(queryText)) {
                    filteredList.add(i)
                }
            }
            val filterResults=FilterResults()
            filterResults.values=filteredList
            return filterResults
        }

        override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
            files.clear()
            files.addAll(p1?.values as ArrayList<FileObject>)
            when(mode) {
                MODE_PDF -> {
                    if(files.size==0)
                        SendActivityPDF.toggleNothingFoundText(true)
                    else
                        SendActivityPDF.toggleNothingFoundText(false)
                }
                MODE_OFFICE -> {
                    if(files.size==0)
                        SendActivityOffice.toggleNothingFoundText(true)
                    else
                        SendActivityOffice.toggleNothingFoundText(false)
                }
            }
            notifyDataSetChanged()
        }

    }

    inner class FileHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileLayout:LinearLayout=itemView.findViewById(R.id.file_bar_layout)
        val fileIcon:ImageView=itemView.findViewById(R.id.file_icon)
        val fileName:TextView=itemView.findViewById(R.id.file_title)
        val filePath:TextView=itemView.findViewById(R.id.file_path)
        val fileSize:TextView=itemView.findViewById(R.id.file_prop)
        val viewFile:ImageView=itemView.findViewById(R.id.file_open_button)
    }

    companion object {
        const val MODE_PDF=0
        const val MODE_OFFICE=1
        const val MODE_SEARCH=2
    }
}