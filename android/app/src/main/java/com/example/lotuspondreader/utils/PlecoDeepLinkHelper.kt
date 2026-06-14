package com.example.lotuspondreader.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object PlecoDeepLinkHelper {

    fun openPleco(context: Context, word: String) {
        val encodedWord = Uri.encode(word)
        val plecoUri = "plecoapi://x-callback-url/s?q=$encodedWord"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(plecoUri)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Fallback: Launch Play Store to download Pleco
            val playStoreUri = "market://details?id=com.pleco.chinesesystem"
            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(playStoreIntent)
            } catch (e2: ActivityNotFoundException) {
                // Fallback 2: Open Play Store web link in browser
                val webPlayStoreUri = "https://play.google.com/store/apps/details?id=com.pleco.chinesesystem"
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webPlayStoreUri)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
        }
    }
}
