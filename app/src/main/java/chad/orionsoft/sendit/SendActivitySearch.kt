package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import chad.orionsoft.sendit.databinding.ActivitySendSearchBinding
import kotlinx.coroutines.*
import java.io.File

class SendActivitySearch : AppCompatActivity() {

    private val searchedFiles=ArrayList<FileObject>()
    private lateinit var sAdapter:FileRecyAdapter
    private lateinit var binding: ActivitySendSearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        belowBarText = binding.searchBottomInfo
        nothingFoundText = binding.nothingFoundInSearchText
        if(Connection.lastSearched.isNotEmpty()) {
            binding.searchEditText.setText(Connection.lastSearched)
            prepareRecyclerViewForResults(Connection.lastSearched)
        }
    }

    fun startSearching(v: View) {
        v.id
        toggleNothingFoundText(false)
        val query = binding.searchEditText.text.toString()
        if(query.isNotEmpty())
            prepareRecyclerViewForResults(query)
    }


    private fun prepareRecyclerViewForResults(query:String) {
        searchedFiles.clear()
        selectedFiles = 0
        selectedSize = 0
        updateBelowBar()
        binding.searchProgressBar.visibility = View.VISIBLE
        GlobalScope.launch(Dispatchers.Main) {
            getSearchResultsAsync(File(Environment.getExternalStorageDirectory(), ""), query).await()
            binding.searchProgressBar.visibility = View.GONE
            if (searchedFiles.size == 0) {
                toggleNothingFoundText(true)
                binding.searchBottomInfo.visibility = View.GONE
                binding.sendSearchedFileButton.visibility = View.GONE
            } else {
                toggleNothingFoundText(false)
                binding.searchBottomInfo.visibility = View.VISIBLE
                binding.sendSearchedFileButton.visibility = View.VISIBLE
            }
            binding.searchRecyclerView.layoutManager = LinearLayoutManager(applicationContext)
            sAdapter = FileRecyAdapter(searchedFiles, applicationContext, FileRecyAdapter.MODE_SEARCH)
            binding.searchRecyclerView.adapter = sAdapter
        }
    }

    fun sendSelectedSearchedFiles(v:View){
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
            Toast.makeText(applicationContext, resources.getString(R.string.no_space_error), Toast.LENGTH_SHORT).show()
            return
        }
        val sendList=ArrayList<SendObject>()
        for(i in searchedFiles) {
            if(i.isSelected) {
                sendList.add(SendObject(i.fileName,i.filePath))
            }
        }
        StaticList.sendList=sendList
        startActivity(Intent(applicationContext,SendNow::class.java))
    }

    override fun onBackPressed() {
        Connection.lastSearched = binding.searchEditText.text.toString()
        super.onBackPressed()
    }

    private fun makeSearchResults(dir:File,query: String) {
        val list=dir.listFiles()
        var noMedia=false
        for(name in dir.list()) {
            if(name==".nomedia") {
                noMedia=true
                break
            }
        }
        if(!noMedia) {
            for(i in list) {
                if(i.isDirectory && !i.isHidden) {
                    makeSearchResults(i,query)
                } else{
                    if(i.name.trim().toLowerCase().contains(query.toLowerCase()) && !i.isHidden) {
                        searchedFiles.add(FileObject(i.path,false))
                    }
                }
            }
        }
    }

    private suspend fun getSearchResultsAsync(dir: File, query: String) =
        coroutineScope {
            async(Dispatchers.IO) {
                makeSearchResults(dir,query)
            }
        }

    companion object {
        var selectedSize:Long=0
        var selectedFiles=0
        lateinit var belowBarText: TextView
        lateinit var nothingFoundText: TextView

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
