package com.example.lotuspondreader.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

object DictionaryImporter {
    private const val TAG = "DictionaryImporter"

    suspend fun importIfNeeded(context: Context, database: StoryDatabase) = withContext(Dispatchers.IO) {
        val dictDao = database.dictDao()
        val count = try {
            dictDao.count()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking dict count", e)
            0
        }

        if (count >= 120000) {
            Log.d(TAG, "Dictionary already populated with $count entries.")
            return@withContext
        }

        Log.d(TAG, "Populating dictionary from assets (current count: $count)...")
        try {
            database.withTransaction {
                // Clear any partial imports first
                dictDao.clearAll()

                context.assets.open("cedict.bin").use { inputStream ->
                    GZIPInputStream(inputStream).use { gzipStream ->
                        InputStreamReader(gzipStream, Charsets.UTF_8).use { reader ->
                            BufferedReader(reader).use { bufferedReader ->
                                val batch = mutableListOf<DictEntry>()
                                var line = bufferedReader.readLine()
                                while (line != null) {
                                    val parts = line.split('\t')
                                    if (parts.size >= 4) {
                                        batch.add(
                                            DictEntry(
                                                traditional = parts[0],
                                                simplified = parts[1],
                                                pinyin = parts[2],
                                                english = parts[3]
                                            )
                                        )
                                    }
                                    if (batch.size >= 5000) {
                                        dictDao.insertAll(batch)
                                        batch.clear()
                                    }
                                    line = bufferedReader.readLine()
                                }
                                if (batch.isNotEmpty()) {
                                    dictDao.insertAll(batch)
                                }
                            }
                        }
                    }
                }
            }
            val finalCount = dictDao.count()
            Log.d(TAG, "Dictionary population complete. Final count: $finalCount")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import dictionary", e)
        }
    }
}
