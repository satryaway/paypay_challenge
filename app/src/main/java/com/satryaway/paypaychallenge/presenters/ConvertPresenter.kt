package com.satryaway.paypaychallenge.presenters

import android.content.Context
import com.satryaway.paypaychallenge.repos.LiveRepository
import com.satryaway.paypaychallenge.utils.CacheUtils
import com.satryaway.paypaychallenge.utils.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ConvertPresenter {
    private var view: View? = null
    private val liveRepository = LiveRepository()

    var currencyList = arrayListOf<String>()
    var currentCurrency = "USD"
    var currentNominal = 1f

    var currencyMap = hashMapOf<String, Float>()

    fun attachView(view: View) {
        this.view = view
    }

    fun detachView() {
        this.view = null
    }

    fun requestRate(context: Context) {
        val cache = CacheUtils.get(context)
        if (cache?.isCurrencyExpired() == true) {
            GlobalScope.launch(Dispatchers.IO) {
                val result = liveRepository.live()
                result.quotes?.let {
                    cache.saveCurrencies(it) { currencyMap, isSaved ->
                        if (isSaved) {
                            this@ConvertPresenter.currencyMap = currencyMap
                            view?.onFetchedCurrency(StringUtils.getCurrenciesValue(currencyMap))
                        } else {
                            view?.onFailedSavingCurrency("Failed to Store Data")
                        }
                    }
                }
            }
        } else {
            cache?.initCurrencies {
                this@ConvertPresenter.currencyMap = it
                view?.onFetchedCurrency(StringUtils.getCurrenciesValue(it))
            }
        }
    }

    fun convert() {
        if (currentNominal <= 0) {
            view?.showErrorMessage("Please Input Correct Value")
        } else {
            view?.setConversionValue()
        }
    }

    fun getCollectedList(): ArrayList<String> {
        val list = arrayListOf<String>()
        currencyMap.forEach {
            val text = "${StringUtils.getCurrencyInitial(it.key)};${it.value}"
            list.add(text)
        }

        return list
    }

    fun getSourceRate(): Float {
        return currencyMap[currentCurrency] ?: 1f
    }

    interface View {
        fun setConversionValue()
        fun showErrorMessage(message: String)
        fun onFetchedCurrency(currenciesValue: ArrayList<String>)
        fun onFailedSavingCurrency(message: String)
    }
}