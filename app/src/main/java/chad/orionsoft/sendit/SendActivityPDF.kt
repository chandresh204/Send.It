package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.PopupMenu
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import chad.orionsoft.sendit.databinding.ActivitySendPdfBinding
import java.io.File

class SendActivityPDF : AppCompatActivity() {

    private val pdfFiles=ArrayList<FileObject>()
    private lateinit var sView:SearchView
    private lateinit var pAdapter:FileRecyAdapter

    private lateinit var binding: ActivitySendPdfBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)
        belowBarText = binding.pdfBottomInfo
        nothingFoundText = binding.nothingFoundText
        updateBelowBar()
        getPDFs(File(Environment.getExternalStorageDirectory(),""))
        pdfFiles.sortBy { it.fileName }
        if(pdfFiles.size==0) {
            toggleNothingFoundText(true)
        }
        binding.pdfRecyclerView.layoutManager=LinearLayoutManager(applicationContext)
        pAdapter= FileRecyAdapter(pdfFiles,this,FileRecyAdapter.MODE_PDF)
        binding.pdfRecyclerView.adapter=pAdapter
        binding.pdfRecyclerView.recycledViewPool.setMaxRecycledViews(0,0)
        sView= binding.pdfSearchView
        sView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(p0: String?): Boolean {
                pAdapter.filter.filter(p0)
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                pAdapter.filter.filter(p0)
                return true
            }
        })
        binding.toolbarText.text="Send.it - ${pdfFiles.size} PDFs"
        binding.toolbarText.setOnClickListener {  }
        binding.sendSelectedPdfButton.setOnLongClickListener {
            Toast.makeText(applicationContext,"This app is created by 204chandresh@gmail.com",Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun getPDFs(dir: File) {
        val list=dir.listFiles() ?: emptyArray()
        var noMedia=false
        for(name in (dir.list() ?: emptyArray())) {
            if(name==".nomedia") {
                noMedia=true
                break
            }
        }
        if(!noMedia) {
            for(i in list) {
                if(i.isDirectory && !i.isHidden) {
                    getPDFs(i)
                }
                if(i.extension=="pdf") {
                    pdfFiles.add(FileObject(i.path,false))
                }
            }
        }
    }

    fun sendSelectedPDFNow(v: View) {
        v.id
        if(selectedSize == 0L) {
            Toast.makeText(applicationContext,"Nothing Selected",Toast.LENGTH_SHORT).show()
            return
        }
        if(!Connection.connection) {
            val i=Intent(applicationContext,ConnectionCheck::class.java)
            i.putExtra(ConnectionCheck.MODE_INTENT_STRING,ConnectionCheck.SENDER_MODE)
            startActivity(i)
            return
        }
        if( selectedSize > Connection.freeSpace) {
            Toast.makeText(applicationContext,resources.getString(R.string.no_space_error),Toast.LENGTH_SHORT).show()
            return
        }
        val sendList=ArrayList<SendObject>()
        for(i in pdfFiles) {
            if(i.isSelected) {
                sendList.add(SendObject(i.fileName,i.filePath))
            }
        }
        StaticList.sendList=sendList
        startActivity(Intent(applicationContext,SendNow::class.java))
        clearAll()
    }

    fun goBack(v:View) {
        v.id
        onBackPressed()
    }

    override fun onResume() {
        updateBelowBar()
        super.onResume()
    }

    fun showPopUpMenu(v:View) {
        val themeCtx=ContextThemeWrapper(applicationContext,R.style.SenderTheme)
        val popupMenu=PopupMenu(themeCtx,v)
        popupMenu.inflate(R.menu.sendit_media_menu)
        popupMenu.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.select_all_media -> {
                    selectAll()
                }
                R.id.clear_selected_media -> {
                    clearAll()
                }
            }
            true
        }
        popupMenu.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun selectAll() {
        selectedSize=0
        selectedFiles=0
        for(i in pdfFiles) {
            i.isSelected=true
            selectedFiles++
            selectedSize+=i.fileSize
        }
        updateBelowBar()
        pAdapter.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearAll() {
        for(i in pdfFiles) {
            if(i.isSelected) {
                i.isSelected=false
                selectedFiles--
                selectedSize-=i.fileSize
            }
        }
        updateBelowBar()
        pAdapter.notifyDataSetChanged()
    }

    override fun onBackPressed() {
        if(sView.isIconified) {
            selectedFiles=0
            selectedSize=0
            finish()
        } else {
            sView.setQuery("",true)
            sView.isIconified=true
        }
    }

    companion object {

        var selectedSize:Long=0
        var selectedFiles=0
        @SuppressLint("StaticFieldLeak")
        lateinit var belowBarText:TextView
        @SuppressLint("StaticFieldLeak")
        lateinit var nothingFoundText:TextView

        @SuppressLint("SetTextI18n")
        fun updateBelowBar() {

            if(Connection.connection) {
                if(selectedSize >Connection.freeSpace) {
                    belowBarText.setBackgroundColor(Color.RED)
                } else {
                    belowBarText.setBackgroundColor(Color.rgb(0x65,0x1f,0xff))
                }
                belowBarText.text=
                    "Selected $selectedFiles : ${Connection.formatDataString(selectedSize,' ')}" +
                            " , free: ${Connection.formatDataString(Connection.freeSpace,' ')}"
            } else {
                belowBarText.text=
                    "Selected $selectedFiles : ${Connection.formatDataString(selectedSize,' ')}"
            }
        }

        fun toggleNothingFoundText(show:Boolean) {
            if(show) {
                nothingFoundText.visibility=View.VISIBLE
            } else {
                nothingFoundText.visibility=View.GONE
            }
        }
    }
}
