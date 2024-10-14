package jarvay.workpaper.data.rule

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules")
    fun findAll(): List<RuleWithRelation>

    @Query("SELECT * FROM rules")
    fun findAllFlow(): Flow<List<RuleWithRelation>>

    @Query("SELECT * FROM rules WHERE id= :id ")
    fun findByIdFlow(id: Long): Flow<Rule>

    @Query("SELECT * FROM rules WHERE id= :id ")
    fun findById(id: Long): RuleWithRelation?

    @Query("SELECT * FROM rules WHERE id= :id ")
    fun findFlowById(id: Long): Flow<RuleWithRelation>?

    @Insert
    suspend fun insert(item: Rule): Long

    @Update
    suspend fun update(item: Rule)

    @Delete
    suspend fun delete(item: Rule)

    @Transaction
    @Query(
        """SELECT * FROM rules
                
                """
    )
    fun test(): List<RuleWithRelation>
}