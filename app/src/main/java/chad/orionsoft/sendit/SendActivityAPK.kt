package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import chad.orionsoft.sendit.InitialProcesses.Companion
import chad.orionsoft.sendit.databinding.ActivitySendApkBinding

class SendActivityAPK : AppCompatActivity() {

    private var appArray=ArrayList<Companion.AppObject>()
    private var showingSystem=false
    private var isSearchViewOn =false
    private var selectedAPPs=0
    private var selectedSize:Long=0
    private var isPrepared=false

    private lateinit var aAdapter:AppAdapter
    private lateinit var sView:SearchView

    private lateinit var belowBarHandler:Handler
    private lateinit var binding: ActivitySendApkBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendApkBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prepareRecyclerView()
        initializeHandlers()
        sView= binding.apkSearchView
        sView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(p0: String?): Boolean {
                aAdapter.filter.filter(p0)
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                aAdapter.filter.filter(p0)
                return true
            }
        })
        binding.noticeText.animate().apply {
            duration=5000
            alpha(0f)
        }
        belowBarHandler.postDelayed( {
            binding.noticeText.visibility=View.GONE
        },5000)
        binding.toolbarTextApk.text="Send.it - ${InitialProcesses.appList.size} APKs"
    }

    private fun initializeHandlers() {

        belowBarHandler= Handler(mainLooper) {
            val msg=it.obj as String
            showTextOnBelowBar(msg)
            true
        }
    }

    fun sendAPKs(v:View) {
        v.id
        if(selectedSize == 0L) {
            Toast.makeText(applicationContext,"Nothing selected",Toast.LENGTH_SHORT).show()
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
        val selectedApps=ArrayList<SendObject>()
        for(i in appArray) {
            if(i.isSelected) {
                selectedApps.add(SendObject("${i.appName}.apk",i.appSource))
            }
        }
        StaticList.sendList=selectedApps
        startActivity(Intent(applicationContext,SendNow::class.java))
        clearAll()
    }

    private fun prepareRecyclerView() {
        isPrepared=false
        appArray.clear()
        showTextOnBelowBar("Searching for APKs, please Wait....")
        binding.appListLoadingProgressbar.visibility=View.VISIBLE
        CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {
            waitForArrayToPrepareAsync().await()
            binding.appRecyclerView.layoutManager=LinearLayoutManager(applicationContext)
            binding.appRecyclerView.recycledViewPool.setMaxRecycledViews(0,0)
            aAdapter=AppAdapter(applicationContext,appArray)
            binding.appRecyclerView.adapter=aAdapter
            binding.appListLoadingProgressbar.visibility=View.GONE
            selectedAPPs=0
            showTextOnBelowBar("No apps Selected")
            clearAll()
        }
    }

    private suspend fun waitForArrayToPrepareAsync() =
        coroutineScope {
            async(Dispatchers.IO) {
                while(!InitialProcesses.isAppListPrepared) ;
                if(showingSystem) {
                    appArray.addAll(InitialProcesses.appListWithSystem)
                } else {
                    appArray.addAll(InitialProcesses.appList)
                }
            }
        }

    private fun selectAll() {
        selectedAPPs=0
        selectedSize=0
      for(i in appArray) {
          i.isSelected=true
          selectedAPPs++
          selectedSize+=i.objectSize
      }
        aAdapter.notifyDataSetChanged()
        showSelectedNumbers()
    }

    private fun clearAll() {
        if(isSearchViewOn) {
            isSearchViewOn=false
            sView.setQuery("",true)
            sView.isIconified=true
        }
        for(i in appArray) {
            i.isSelected=false
            selectedAPPs=0
            selectedSize=0
        }
        aAdapter.notifyDataSetChanged()
        showSelectedNumbers()
    }

    private fun showTextOnBelowBar(text:String) {
        binding.belowBarText.text=text
    }

    @SuppressLint("SetTextI18n")
    fun showPopUpMenu(v:View) {
        val themeCtx=ContextThemeWrapper(applicationContext,R.style.SenderTheme_NoActionBar)
        val popupMenu=PopupMenu(themeCtx,v)
        popupMenu.inflate(R.menu.sendit_apk_menu)
        popupMenu.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.show_system -> {
                    if(showingSystem) {
                        showingSystem=false
                        it.title="show system apps"
                        binding.toolbarTextApk.text="Send.it - ${InitialProcesses.appList.size} APKs"
                    } else {
                        showingSystem=true
                        it.title = "hide system apps"
                        binding.toolbarTextApk.text="Send.it - ${InitialProcesses.appListWithSystem.size} APKs"
                    }
                    clearAll()
                    prepareRecyclerView()
                }
                R.id.select_all_apks -> {
                    selectAll()
                }
                R.id.clear_selected -> {
                    clearAll()
                }
            }
            true
        }
        popupMenu.show()
    }

    @SuppressLint("SetTextI18n")
    private fun showSelectedNumbers() {
        if(Connection.connection) {
            if(selectedSize>Connection.freeSpace) {
                binding.belowBarText.setBackgroundColor(Color.RED)
            } else {
                binding.belowBarText.setBackgroundColor(Color.rgb(0x65,0x1f,0xff))
            }
            binding.belowBarText.text=
                "Selected $selectedAPPs : ${Connection.formatDataString(selectedSize,' ')}" +
                        " , free: ${Connection.formatDataString(Connection.freeSpace,' ')}"
        } else {
            binding.belowBarText.text=
                "Selected $selectedAPPs : ${Connection.formatDataString(selectedSize,' ')}"
        }
    }

    override fun onBackPressed() {
        if(sView.isIconified) {
            finish()
        } else {
            sView.setQuery("",true)
            sView.isIconified=true
        }
    }

    fun goBack(v:View) {
        v.id
        onBackPressed()
    }

    override fun onResume() {
        showSelectedNumbers()
        super.onResume()
    }

    inner class AppAdapter(private val ctx: Context,val appList:ArrayList<Companion.AppObject>) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

        val fullAppList=ArrayList<Companion.AppObject>().apply {
            addAll(appList)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            return AppViewHolder(LayoutInflater.from(ctx).inflate(R.layout.item_layout_media,parent,false))
        }

        override fun getItemCount(): Int = appList.size

        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            val appInfo = appList[position]
            val appItemLayout = holder.appItemLayout
            val icon = holder.icon
            val appName = holder.appName
            val packageName = holder.packageName
            packageName.visibility=View.VISIBLE
            val appSize=holder.appSize
            appName.text = appInfo.appName
            packageName.text = appInfo.appPackage
            appSize.text=Connection.formatDataString(appInfo.objectSize,' ')
            icon.setImageDrawable(appInfo.appIcon)
            if (appInfo.isSelected) {
                appItemLayout.setBackgroundResource(R.drawable.item_background_select)
                appName.isSelected=true
                packageName.isSelected=true
            }
            appItemLayout.setOnClickListener {
                if (appInfo.isSelected) {
                    appInfo.isSelected = false
                    selectedAPPs--
                    selectedSize-=appInfo.objectSize
                    appName.isSelected=false
                    packageName.isSelected=false
                    appItemLayout.setBackgroundResource(R.drawable.item_background)
                } else {
                    appInfo.isSelected = true
                    selectedAPPs++
                    appName.isSelected=true
                    packageName.isSelected=true
                    selectedSize+=appInfo.objectSize
                    appItemLayout.setBackgroundResource(R.drawable.item_background_select)
                }
                    showSelectedNumbers()
            }
        }

        val filter=object:Filter() {

            override fun performFiltering(p0: CharSequence?): FilterResults {
                val filteredList=ArrayList<Companion.AppObject>()
                for(i in fullAppList) {
                    if(i.appName.lowercase().trim().contains(p0!!)) {
                        filteredList.add(i)
                    }
                }
                val filterResults=FilterResults()
                filterResults.values=filteredList
                return filterResults
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                appList.clear()
                appList.addAll(p1?.values as ArrayList<Companion.AppObject>)
                if(appList.size==0)
                    binding.noAppFoundText.visibility=View.VISIBLE
                else
                    binding.noAppFoundText.visibility=View.GONE
                notifyDataSetChanged()
            }

        }

        inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val appItemLayout:LinearLayout=itemView.findViewById(R.id.item_bar_layout)
            val icon:ImageView=itemView.findViewById(R.id.item_icon)
            val appName:TextView=itemView.findViewById(R.id.item_title)
            val packageName:TextView=itemView.findViewById(R.id.item_descr)
            val appSize:TextView=itemView.findViewById(R.id.item_prop)
        }
    }

}