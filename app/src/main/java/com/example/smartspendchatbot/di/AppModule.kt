package com.example.smartspendchatbot.di

import android.content.Context
import androidx.room.Room
import com.example.smartspendchatbot.data.local.BudgetDao
import com.example.smartspendchatbot.data.local.ExpenseDao
import com.example.smartspendchatbot.data.local.LocalDatabase
import com.example.smartspendchatbot.data.repository.BudgetRepository
import com.example.smartspendchatbot.ai.GoogleAiHelper // Import the renamed helper
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext app: Context
    ): LocalDatabase =
        Room.databaseBuilder(app, LocalDatabase::class.java, "smart_spend_db")
            // TODO: Implement proper database migrations instead of fallbackToDestructiveMigration.
            // This is crucial for production apps to avoid data loss during schema updates.
            // Example: .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration(false) // Removed: Avoid using this in production
            .build()

    @Provides
    fun provideBudgetDao(db: LocalDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideExpenseDao(db: LocalDatabase): ExpenseDao = db.expenseDao()

    @Provides
    @Singleton
    fun provideBudgetRepository(
        budgetDao: BudgetDao,
        expenseDao: ExpenseDao
    ): BudgetRepository = BudgetRepository(budgetDao, expenseDao)

    // Provide the renamed GoogleAiHelper
    @Provides
    @Singleton
    fun provideGoogleAiHelper(): GoogleAiHelper = GoogleAiHelper()
}
