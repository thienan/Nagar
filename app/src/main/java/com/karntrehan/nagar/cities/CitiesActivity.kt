package com.karntrehan.nagar.cities

import android.arch.lifecycle.LifecycleActivity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import com.karntrehan.nagar.R
import com.karntrehan.nagar.cities.viewmodel.CitiesViewModel
import com.karntrehan.nagar.databinding.ActivityCitiesBinding


class CitiesActivity : LifecycleActivity() {

    lateinit var binding: ActivityCitiesBinding

    val citiesViewModel: CitiesViewModel by lazy {
        ViewModelProviders.of(this).get(CitiesViewModel::class.java)
    }

    internal val citiesAdapter: CitiesAdapter by lazy { CitiesAdapter(context) }

    val context: Context by lazy { this }

    val layoutManager: LinearLayoutManager by lazy {
        binding.rvCities.layoutManager as LinearLayoutManager
    }

    //Control the paginated scroll from calling the viewmodel recursively for next items
    var canLoadMore = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_cities)
        binding.rvCities.setHasFixedSize(true)

        binding.rvCities.adapter = citiesAdapter

        //Add the scroll listener to the recyclerview to load new elements from
        // viewmodel when last element of already loaded set is visible on screen
        binding.rvCities.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                val lastPosition = layoutManager.findLastVisibleItemPosition()
                if (citiesAdapter.itemCount > 1 && lastPosition == citiesAdapter.itemCount - 1
                        && canLoadMore) {
                    //Load next set of cities
                    citiesViewModel.setInput(lastPosition)
                    canLoadMore = false
                }
            }
        })

        //Observe emission of values from the cities LiveData and
        // dispatch data to the adapter
        citiesViewModel.cities.observe(this, Observer { citiesList ->
            val lastVisi = layoutManager.findLastVisibleItemPosition()

            hideSRLLoader()

            if (citiesList != null && citiesList.isNotEmpty()) {
                canLoadMore = true
                citiesAdapter.addItems(if (lastVisi < 0) 0 else lastVisi, citiesList)
            }
        })

        //Observe the emission of the load status LiveData and
        // hide the loaderView when data is completely
        citiesViewModel.loadStatus.observe(this, Observer { loading ->
            if (!loading!!)
                citiesAdapter.removeLoader()
        })


        //Observe the errorStatus stream for server sync and display the error as a toast
        citiesViewModel.errorStatus.observe(this, Observer { errorStatus ->
            citiesAdapter.removeLoader()
            hideSRLLoader()
            Toast.makeText(context, errorStatus, Toast.LENGTH_LONG).show()
        })

        //Clear displayed data and db. Load data afresh from server
        binding.srlCities.setOnRefreshListener({
            citiesAdapter.clearAll()
            citiesViewModel.refreshDb()
        })

    }

    //Control the visibility of the SwipeRefreshLayout
    private fun hideSRLLoader() {
        if (binding.srlCities.isRefreshing)
            binding.srlCities.isRefreshing = false
    }

    override fun onResume() {
        super.onResume()
        //Load the first set of cities
        citiesViewModel.setInput(0)
    }
}
