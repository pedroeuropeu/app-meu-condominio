package br.com.condovitta.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import br.com.condovitta.app.core.common.TimeProvider
import br.com.condovitta.app.core.security.TokenCipher
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/** "Hoje" fixo: quarta, 4 de novembro de 2026 (o "hoje" do prototype/data.js). */
class FixedTime(var date: LocalDate = LocalDate.of(2026, 11, 4), var millis: Long = 1_793_786_400_000L) : TimeProvider {
    override fun today(): LocalDate = date
    override fun now(): LocalDateTime = date.atTime(10, 0)
    override fun epochMillis(): Long = millis
    fun advance(ms: Long) { millis += ms }
}

/** Cifra de teste (XOR): o Keystore só existe no aparelho; o teste instrumentado cobre o real. */
class FakeCipher : TokenCipher {
    override fun encrypt(plain: ByteArray): ByteArray = plain.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
    override fun decrypt(data: ByteArray): ByteArray = encrypt(data)
}

fun tempDataStore(name: String = "test"): DataStore<Preferences> {
    val dir: File = Files.createTempDirectory("ds").toFile()
    return PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + SupervisorJob())) { File(dir, "$name.preferences_pb") }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

fun epochOf(dt: LocalDateTime): Long = dt.toInstant(ZoneOffset.UTC).toEpochMilli()
