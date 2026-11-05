package br.com.condovitta.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import br.com.condovitta.app.BuildConfig
import br.com.condovitta.app.core.common.SystemTimeProvider
import br.com.condovitta.app.core.common.TimeProvider
import br.com.condovitta.app.core.security.KeystoreTokenCipher
import br.com.condovitta.app.core.security.SessionStore
import br.com.condovitta.app.core.security.TokenCipher
import br.com.condovitta.app.data.remote.ApiJson
import br.com.condovitta.app.data.remote.AuthInterceptor
import br.com.condovitta.app.data.remote.CondominioContext
import br.com.condovitta.app.data.remote.CondovittaApi
import br.com.condovitta.app.data.remote.PublicApi
import br.com.condovitta.app.data.remote.SessionEvents
import br.com.condovitta.app.data.remote.TokenAuthenticator
import br.com.condovitta.app.data.remote.TokenRefresher
import br.com.condovitta.app.data.remote.UserAgentInterceptor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

/** Escopo que vive enquanto o app vive (sessão, sincronizações). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppScope

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun timeProvider(impl: SystemTimeProvider): TimeProvider
    @Binds abstract fun tokenCipher(impl: KeystoreTokenCipher): TokenCipher
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton @AppScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides @Singleton @Named("session")
    fun sessionDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("session") }

    @Provides @Singleton @Named("settings")
    fun settingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun sessionEvents(): SessionEvents = SessionEvents()

    @Provides @Singleton
    fun condominioContext(): CondominioContext = CondominioContext()

    private fun baseClient(): OkHttpClient.Builder = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(UserAgentInterceptor(UserAgentInterceptor.value(BuildConfig.VERSION_NAME, android.os.Build.VERSION.RELEASE.orEmpty(), android.os.Build.MODEL.orEmpty())))
        .apply {
            if (BuildConfig.DEBUG) {
                // só no debug, sem corpo e sem o token
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC; redactHeader("Authorization") })
            }
        }

    @Provides @Singleton @Named("public")
    fun publicClient(): OkHttpClient = baseClient().build()

    @Provides @Singleton
    fun tokenRefresher(@Named("public") client: OkHttpClient, store: SessionStore, time: TimeProvider): TokenRefresher =
        TokenRefresher(client, BuildConfig.KEYCLOAK_TOKEN_URL, BuildConfig.KEYCLOAK_CLIENT_ID, store, time)

    @Provides @Singleton @Named("auth")
    fun authClient(store: SessionStore, refresher: TokenRefresher, events: SessionEvents, time: TimeProvider, condominio: CondominioContext): OkHttpClient =
        baseClient()
            .addInterceptor(AuthInterceptor(store, refresher, time, condominio))
            .authenticator(TokenAuthenticator(store, refresher, events))
            .build()

    private fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides @Singleton
    fun publicApi(@Named("public") client: OkHttpClient): PublicApi = retrofit(client).create(PublicApi::class.java)

    @Provides @Singleton
    fun portariaApi(@Named("auth") client: OkHttpClient): br.com.condovitta.app.data.remote.PortariaApi =
        retrofit(client).create(br.com.condovitta.app.data.remote.PortariaApi::class.java)

    @Provides @Singleton
    fun condovittaApi(@Named("auth") client: OkHttpClient): CondovittaApi = retrofit(client).create(CondovittaApi::class.java)
}
