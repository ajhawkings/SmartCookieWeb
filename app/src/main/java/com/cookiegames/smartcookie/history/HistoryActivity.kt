/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Created by CookieJarApps 10/01/2020 */


package com.cookiegames.smartcookie.history

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import butterknife.ButterKnife
import com.cookiegames.smartcookie.AppTheme
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.database.HistoryEntry
import com.cookiegames.smartcookie.database.history.HistoryRepository
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.dialog.LightningDialogBuilder
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.utils.RecyclerItemClickListener
import com.cookiegames.smartcookie.utils.ThemeUtils
import com.huxq17.download.DownloadProvider.context
import java.text.DateFormat
import java.util.*
import javax.inject.Inject


class HistoryActivity : AppCompatActivity(), SearchView.OnQueryTextListener {
    @JvmField
    @Inject
    var mUserPreferences: UserPreferences? = null

    @JvmField
    @Inject
    var dialogBuilder: LightningDialogBuilder? = null

    lateinit var list: RecyclerView
    lateinit var arrayAdapter: CustomAdapter
    lateinit var historyList: List<HistoryEntry>


    @Inject internal lateinit var historyRepository: HistoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(this)

        val color: Int
        if (mUserPreferences!!.useTheme === AppTheme.LIGHT) {
            setTheme(R.style.Theme_SettingsTheme)
            color = ThemeUtils.getPrimaryColor(this)
            window.setBackgroundDrawable(ColorDrawable(color))
        } else if (mUserPreferences!!.useTheme === AppTheme.DARK) {
            setTheme(R.style.Theme_SettingsTheme_Dark)
            color = ThemeUtils.getPrimaryColor(this)
            window.setBackgroundDrawable(ColorDrawable(color))
        } else {
            setTheme(R.style.Theme_SettingsTheme_Black)
            color = ThemeUtils.getPrimaryColor(this)
            window.setBackgroundDrawable(ColorDrawable(color))
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        ButterKnife.bind(this)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        list = findViewById<RecyclerView>(R.id.history)
        val linearLayoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        historyRepository
                .lastHundredVisitedHistoryEntries()
                .subscribe { list ->
                    historyList = list
                }

        //val downloadAdapter = HistoryAdapter(map, downloadInfoList)
        arrayAdapter = CustomAdapter(historyList)
        list.layoutManager = linearLayoutManager
        list?.adapter = arrayAdapter

        list.addOnItemTouchListener(
                RecyclerItemClickListener(context, list, object : RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View?, position: Int) {
                        val i = Intent(ACTION_VIEW, historyList[position].url.toUri())
                        i.setData(Uri.parse(historyList[position].url))
                        i.setPackage(context!!.packageName)
                        startActivity(i, null)
                    }

                    override fun onLongItemClick(view: View?, position: Int) {
                        dialogBuilder!!.showLongPressedHistoryLinkDialog(this@HistoryActivity, historyList[position].url)
                    }
                })
        )

        /*//Sort download list if need.
        Collections.sort(downloadInfoList) { o1, o2 -> (o1.createTime - o2.createTime).toInt() }
        list.layoutManager = linearLayoutManager
        activity_download.xmldownloadAdapter = DownloadActivity.DownloadAdapter(map, downloadInfoList)
        list.adapter = downloadAdapter
        downloadObserver.enable()*/

        //downloadAdapter!!.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            else -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    fun dataChanged() {
        historyRepository
                .lastHundredVisitedHistoryEntries()
                .subscribe { list ->
                    historyList = list
                }
        arrayAdapter = CustomAdapter(historyList)
        list?.adapter = arrayAdapter
        arrayAdapter.notifyDataSetChanged()
    }

    class CustomAdapter(private val dataSet: List<HistoryEntry>) :
            RecyclerView.Adapter<CustomAdapter.ViewHolder>() {


        /**
         * Provide a reference to the type of views that you are using
         * (custom ViewHolder).
         */
        class ViewHolder(view: View, private val dataSet: List<HistoryEntry>) : RecyclerView.ViewHolder(view) {
            val textView: TextView
            val historyUrl: TextView
            val historyDate: TextView

            init {
                // Define click listener for the ViewHolder's View.
                textView = view.findViewById(R.id.historyTitle)
                historyUrl = view.findViewById(R.id.historyUrl)
                historyDate = view.findViewById(R.id.historyTime)
            }

           /*override fun onClick(v: View?) {
                val i = Intent(ACTION_VIEW, dataSet[layoutPosition].url.toUri())
                i.setData(Uri.parse(dataSet[adapterPosition].url))
                i.setPackage(v!!.context!!.packageName)
                startActivity(v.context, i, null)
            }*/
        }

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            // Create a new view, which defines the UI of the list item
            val view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.history_row, viewGroup, false)

            return ViewHolder(view, dataSet)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

            // Get element from your dataset at this position and replace the
            // contents of the view with that element
            viewHolder.textView.text = dataSet[position].title
            viewHolder.historyUrl.text = dataSet[position].url
            viewHolder.historyDate.text = getDateTime(dataSet[position].lastTimeVisited)
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = dataSet.size

        private fun getDateTime(s: Long): String? {
            try {
                val sdf = DateFormat.getDateTimeInstance()
                val netDate = Date(s)
                return sdf.format(netDate)
            } catch (e: Exception) {
                return e.toString()
            }
        }

    }



    override fun onQueryTextSubmit(query: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        TODO("Not yet implemented")
    }

}