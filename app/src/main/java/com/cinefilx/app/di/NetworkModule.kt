package com.cinefilx.app.di

import android.content.Context
import androidx.room.Room
import com.cinefilx.app.BuildConfig
import com.cinefilx.app.data.local.WatchlistDatabase
import com.cinefilx.app.data.local.WatchlistDao
import com.cinefilx.app.data.remote.EztvApiService
import com.cinefilx.app.data.remote.TmdbApiService
import com.cinefilx.app.data.remote.YtsApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // ── TMDB ──────────────────────────────────────────────────────────────────
    @Provides
    @Singleton
    @Named("tmdb")
    fun provideTmdbRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.TMDB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTmdbApiService(@Named("tmdb") retrofit: Retrofit): TmdbApiService {
        return retrofit.create(TmdbApiService::class.java)
    }

    // ── YTS ───────────────────────────────────────────────────────────────────
    @Provides
    @Singleton
    @Named("yts")
    fun provideYtsRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://yts.mx/api/v2/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideYtsApiService(@Named("yts") retrofit: Retrofit): YtsApiService {
        return retrofit.create(YtsApiService::class.java)
    }

    // ── EZTV ─────────────────────────────────────────────────────────────────
    @Provides
    @Singleton
    @Named("eztv")
    fun provideEztvRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://eztv.re/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideEztvApiService(@Named("eztv") retrofit: Retrofit): EztvApiService {
        return retrofit.create(EztvApiService::class.java)
    }

    // ── Room DB ───────────────────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideWatchlistDatabase(@ApplicationContext context: Context): WatchlistDatabase {
        return Room.databaseBuilder(
            context,
            WatchlistDatabase::class.java,
            "cinefilx_watchlist.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideWatchlistDao(db: WatchlistDatabase): WatchlistDao {
        return db.watchlistDao()
    }
}
