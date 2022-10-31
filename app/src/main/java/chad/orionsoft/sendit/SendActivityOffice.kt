package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.PopupMenu
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.LinearLayoutManager
import chad.orionsoft.sendit.databinding.ActivitySendOfficeBinding
import java.io.File

class SendActivityOffice : AppCompatActivity() {

    private val officeFiles=ArrayList<FileObject>()
    private lateinit var sView: SearchView
    private lateinit var oAdapter:FileRecyAdapter

    private lateinit var binding: ActivitySendOfficeBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendOfficeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        belowBarText = binding.officeBottomInfo
        nothingFoundText = binding.nothingFoundText
        updateBelowBar()
        getOffices(File(Environment.getExternalStorageDirectory(),""))
        officeFiles.sortBy { it.fileName }
        if(officeFiles.size==0) {
            toggleNothingFoundText(true)
        }
        binding.officeRecyclerView.layoutManager= LinearLayoutManager(applicationContext)
        oAdapter= FileRecyAdapter(officeFiles,this,FileRecyAdapter.MODE_OFFICE)
        binding.officeRecyclerView.adapter=oAdapter
        binding.officeRecyclerView.recycledViewPool.setMaxRecycledViews(0,0)
        sView = binding.officeSearchView
        sView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(p0: String?): Boolean {
                oAdapter.filter.filter(p0)
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                oAdapter.filter.filter(p0)
                return true
            }
        })
        binding.officeToolbar.setOnClickListener { }
        binding.toolbarTextOffice.text="Send.it - ${officeFiles.size} Files"
    }

    private fun getOffices(dir: File) {
        val list = dir.listFiles() ?: emptyArray()
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
                    getOffices(i)
                }
                when(i.extension) {
                    "doc","docx","docm","dotx","dotm","docb","odt","ott","odm",
                        "xls","xlsx","xlt","xlsb","xst","xla","ods","ots",
                    "ppt","pot","pps","pptx","pptm","potx","ppsx","sldx","sldm","odp","otp" -> {
                        officeFiles.add(FileObject(i.path,false))
                    }
                }
            }
        }
    }

    fun sendSelectedOfficeNow(v:View) {
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
        if(selectedSize > Connection.freeSpace) {
            Toast.makeText(applicationContext,resources.getString(R.string.no_space_error),Toast.LENGTH_SHORT).show()
            return
        }
        val sendList = ArrayList<SendObject>()
        for (i in officeFiles) {
            if (i.isSelected) {
                sendList.add(SendObject(i.fileName, i.filePath))
            }
        }
        StaticList.sendList = sendList
        startActivity(Intent(applicationContext, SendNow::class.java))
        clearAll()
    }

    fun showPopUpMenu(v:View) {
        val menuCtx=ContextThemeWrapper(applicationContext,R.style.SenderTheme_NoActionBar)
        val popupMenu=PopupMenu(menuCtx,v)
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
        for(i in officeFiles) {
            i.isSelected=true
            selectedFiles++
            selectedSize+=i.fileSize
        }
        updateBelowBar()
        oAdapter.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearAll() {
        for(i in officeFiles) {
            if(i.isSelected) {
                i.isSelected=false
                selectedFiles--
                selectedSize-=i.fileSize
            }
        }
        updateBelowBar()
        oAdapter.notifyDataSetChanged()
    }

    fun goBack(v:View) {
        v.id
        onBackPressed()
    }

    override fun onBackPressed() {
        if(sView.isIconified) {
            finish()
            selectedFiles=0
            selectedSize=0
        } else {
            sView.setQuery("",true)
            sView.isIconified=true
        }
    }

    override fun onResume() {
        updateBelowBar()
        super.onResume()
    }

    companion object {

        var selectedSize:Long=0
        var selectedFiles=0
        @SuppressLint("StaticFieldLeak")
        lateinit var belowBarText: TextView
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
