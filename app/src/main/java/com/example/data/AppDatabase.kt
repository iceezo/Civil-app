package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun getProjectById(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectByIdDirect(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)
}

@Dao
interface CalculationDao {
    @Query("SELECT * FROM calculation_records WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getCalculationsForProject(projectId: Long): Flow<List<CalculationRecordEntity>>

    @Query("SELECT * FROM calculation_records ORDER BY timestamp DESC LIMIT 20")
    fun getRecentCalculations(): Flow<List<CalculationRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculation(record: CalculationRecordEntity): Long

    @Query("DELETE FROM calculation_records WHERE id = :id")
    suspend fun deleteCalculationById(id: Long)
}

@Dao
interface DrawingDao {
    @Query("SELECT * FROM drawings WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun getDrawingsForProject(projectId: Long): Flow<List<DrawingEntity>>

    @Query("SELECT * FROM drawings WHERE id = :id LIMIT 1")
    fun getDrawingById(id: Long): Flow<DrawingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawing(drawing: DrawingEntity): Long

    @Update
    suspend fun updateDrawing(drawing: DrawingEntity)

    @Query("DELETE FROM drawings WHERE id = :id")
    suspend fun deleteDrawingById(id: Long)
}

@Dao
interface BOQDao {
    @Query("SELECT * FROM boq_items WHERE projectId = :projectId ORDER BY id ASC")
    fun getBOQItems(projectId: Long): Flow<List<BOQItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBOQItems(items: List<BOQItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBOQItem(item: BOQItemEntity): Long

    @Update
    suspend fun updateBOQItem(item: BOQItemEntity)

    @Query("DELETE FROM boq_items WHERE id = :id")
    suspend fun deleteBOQItem(id: Long)

    @Query("DELETE FROM boq_items WHERE projectId = :projectId")
    suspend fun clearBOQForProject(projectId: Long)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getMessagesForProject(projectId: Long): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE projectId = :projectId")
    suspend fun clearChatForProject(projectId: Long)
}

@Database(
    entities = [
        ProjectEntity::class,
        CalculationRecordEntity::class,
        DrawingEntity::class,
        BOQItemEntity::class,
        ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun calculationDao(): CalculationDao
    abstract fun drawingDao(): DrawingDao
    abstract fun boqDao(): BOQDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "civiai_engineer.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
