package com.example.pucematch.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object para operaciones sobre la tabla de perfiles.
 * Expone Flow para que la UI reaccione reactivamente (UDF).
 */
@Dao
interface UserDao {

    @Query("SELECT * FROM profiles")
    fun getAllProfiles(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM profiles")
    suspend fun getProfilesList(): List<StudentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<StudentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: StudentEntity)

    @Query("UPDATE profiles SET isMatched = :isMatched WHERE id = :id")
    suspend fun updateMatchStatus(id: String, isMatched: Boolean)

    @Query("DELETE FROM profiles")
    suspend fun deleteAllProfiles()

    @androidx.room.Transaction
    suspend fun clearAndInsert(profiles: List<StudentEntity>) {
        deleteAllProfiles()
        insertProfiles(profiles)
    }
}
