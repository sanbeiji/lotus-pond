package com.example.lotuspondreader.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DictDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<DictEntry>)

    @Query("SELECT * FROM dictionary_entries WHERE traditional = :word")
    suspend fun lookupWord(word: String): List<DictEntry>

    @Query("SELECT COUNT(*) FROM dictionary_entries")
    suspend fun count(): Int

    @Query("DELETE FROM dictionary_entries")
    suspend fun clearAll()
}
