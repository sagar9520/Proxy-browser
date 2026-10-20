package com.myfuturecode.proxygo

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxAdView
import com.applovin.mediation.ads.MaxAppOpenAd
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.mediation.ads.MaxRewardedAd
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Ek hi jagah se saare ads. Remote config "ads.network" = "admob" ya "max" se network chunte hain,
 * unit ids bhi config se aate hain. Ads band karne ho to "ads.enabled": false.
 */
object Ads {
    private lateinit var app: Context
    private var maxReady = false
    private var actionCount = 0
    var showingFullScreen = false
        private set

    private val cfg get() = RemoteConfig.current.ads
    /** Sirf debug build ke emulator test me ads band (intent extra "ci_no_ads"). */
    var ciNoAds = false
    private val adsOn get() = cfg.enabled && !ciNoAds
    private val useMax get() = cfg.network == "max" && maxReady
    private fun on(unit: String) = adsOn && unit.isNotBlank()

    // AdMob
    private var gInter: InterstitialAd? = null
    private var gReward: RewardedAd? = null
    private var gOpen: AppOpenAd? = null
    private var gOpenLoading = false

    // MAX
    private var mInter: MaxInterstitialAd? = null
    private var mReward: MaxRewardedAd? = null
    private var mOpen: MaxAppOpenAd? = null
    private var pendingMaxDone: (() -> Unit)? = null
    private var pendingMaxReward: (() -> Unit)? = null
    private var maxRewardEarned = false

    fun init(ctx: Context) {
        app = ctx.applicationContext
        MobileAds.initialize(app) {}
        if (BuildConfig.APPLOVIN_SDK_KEY.isNotBlank()) {
            val init = AppLovinSdkInitializationConfiguration.builder(BuildConfig.APPLOVIN_SDK_KEY, app)
                .setMediationProvider(AppLovinMediationProvider.MAX)
                .build()
            AppLovinSdk.getInstance(app).initialize(init) { maxReady = true }
        }
    }

    fun preload(activity: Activity) {
        if (!adsOn) return
        if (useMax) preloadMax(activity) else preloadAdmob()
    }

    private fun req() = AdRequest.Builder().build()

    private fun preloadAdmob() {
        val u = cfg.admob
        if (on(u.interstitial) && gInter == null) InterstitialAd.load(app, u.interstitial, req(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { gInter = ad }
            override fun onAdFailedToLoad(e: LoadAdError) { gInter = null }
        })
        if (on(u.rewarded) && gReward == null) RewardedAd.load(app, u.rewarded, req(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { gReward = ad }
            override fun onAdFailedToLoad(e: LoadAdError) { gReward = null }
        })
        loadAppOpen()
    }

    fun loadAppOpen() {
        if (!cfg.appOpen || useMax) return
        val id = cfg.admob.appOpen
        if (!on(id) || gOpen != null || gOpenLoading) return
        gOpenLoading = true
        AppOpenAd.load(app, id, req(), object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) { gOpen = ad; gOpenLoading = false }
            override fun onAdFailedToLoad(e: LoadAdError) { gOpenLoading = false }
        })
    }

    val appOpenReady: Boolean
        get() = if (useMax) mOpen?.isReady == true else gOpen != null

    /** Splash aur app wapas khulne par. */
    fun showAppOpen(activity: Activity, done: () -> Unit) {
        if (!adsOn || !cfg.appOpen || showingFullScreen) { done(); return }
        if (useMax) {
            val ad = mOpen
            if (ad?.isReady == true) { pendingMaxDone = done; showingFullScreen = true; ad.showAd() } else done()
            return
        }
        val ad = gOpen ?: run { loadAppOpen(); done(); return }
        gOpen = null
        ad.fullScreenContentCallback = fullScreen { loadAppOpen(); done() }
        showingFullScreen = true
        ad.show(activity)
    }

    /** Har N-th action par interstitial (N = ads.interstitial_every). */
    fun maybeInterstitial(activity: Activity, done: () -> Unit = {}) {
        actionCount++
        val every = cfg.interstitialEvery.coerceAtLeast(1)
        if (actionCount % every == 0) showInterstitial(activity, done) else done()
    }

    fun showInterstitial(activity: Activity, done: () -> Unit = {}) {
        if (!adsOn) { done(); return }
        if (useMax) {
            val ad = mInter
            if (ad?.isReady == true) { pendingMaxDone = done; showingFullScreen = true; ad.showAd() } else { ad?.loadAd(); done() }
            return
        }
        val ad = gInter ?: run { preloadAdmob(); done(); return }
        gInter = null
        ad.fullScreenContentCallback = fullScreen { preloadAdmob(); done() }
        showingFullScreen = true
        ad.show(activity)
    }

    val rewardedAvailable get() = adsOn && (if (useMax) mReward?.isReady == true else gReward != null)

    /** Reward mila to onReward, warna onFail (ad load nahi hua). */
    fun showRewarded(activity: Activity, onReward: () -> Unit, onFail: () -> Unit) {
        if (!adsOn) { onReward(); return }
        if (useMax) {
            val ad = mReward
            if (ad?.isReady == true) {
                maxRewardEarned = false; pendingMaxReward = onReward; pendingMaxDone = {}
                showingFullScreen = true; ad.showAd()
            } else { ad?.loadAd(); onFail() }
            return
        }
        val ad = gReward ?: run { preloadAdmob(); onFail(); return }
        gReward = null
        var earned = false
        ad.fullScreenContentCallback = fullScreen { preloadAdmob(); if (earned) onReward() }
        showingFullScreen = true
        ad.show(activity) { earned = true }
    }

    private fun fullScreen(after: () -> Unit) = object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() { showingFullScreen = false; after() }
        override fun onAdFailedToShowFullScreenContent(e: AdError) { showingFullScreen = false; after() }
    }

    // ---------- AppLovin MAX ----------
    private val maxListener = object : MaxRewardedAdListener {
        override fun onAdLoaded(ad: MaxAd) {}
        override fun onAdDisplayed(ad: MaxAd) {}
        override fun onAdClicked(ad: MaxAd) {}
        override fun onAdLoadFailed(unit: String, e: MaxError) {}
        override fun onAdHidden(ad: MaxAd) { finishMax(); reloadMax(ad) }
        override fun onAdDisplayFailed(ad: MaxAd, e: MaxError) { finishMax(); reloadMax(ad) }
        override fun onUserRewarded(ad: MaxAd, reward: MaxReward) { maxRewardEarned = true }
    }

    private fun finishMax() {
        showingFullScreen = false
        if (maxRewardEarned) pendingMaxReward?.invoke()
        pendingMaxDone?.invoke()
        pendingMaxDone = null; pendingMaxReward = null; maxRewardEarned = false
    }

    private fun reloadMax(ad: MaxAd) {
        when (ad.adUnitId) {
            mInter?.adUnitId -> mInter?.loadAd()
            mReward?.adUnitId -> mReward?.loadAd()
            mOpen?.adUnitId -> mOpen?.loadAd()
        }
    }

    private fun preloadMax(activity: Activity) {
        val u = cfg.max
        if (on(u.interstitial) && mInter == null) mInter = MaxInterstitialAd(u.interstitial, activity).apply {
            setListener(maxListener); loadAd()
        }
        if (on(u.rewarded) && mReward == null) mReward = MaxRewardedAd.getInstance(u.rewarded, activity).apply {
            setListener(maxListener); loadAd()
        }
        if (cfg.appOpen && on(u.appOpen) && mOpen == null) mOpen = MaxAppOpenAd(u.appOpen, activity).apply {
            setListener(maxListener); loadAd()
        }
    }

    /** Banner (50dp) ya MREC (300x250). */
    @Composable
    fun Banner(modifier: Modifier = Modifier, mrec: Boolean = false) {
        if (!adsOn) return
        val width = LocalConfiguration.current.screenWidthDp
        if (useMax) {
            val id = cfg.max.banner
            if (id.isBlank()) return
            AndroidView(modifier = modifier.fillMaxWidth(), factory = { ctx ->
                val v = if (mrec) MaxAdView(id, MaxAdFormat.MREC, ctx) else MaxAdView(id, ctx)
                val h = (if (mrec) 250 else 50) * ctx.resources.displayMetrics.density
                v.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h.toInt())
                v.loadAd(); v
            })
            return
        }
        val id = cfg.admob.banner
        if (id.isBlank()) return
        AndroidView(modifier = modifier.fillMaxWidth(), factory = { ctx ->
            AdView(ctx).apply {
                adUnitId = id
                setAdSize(if (mrec) AdSize.MEDIUM_RECTANGLE else AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, width))
                loadAd(req())
            }
        })
    }
}
