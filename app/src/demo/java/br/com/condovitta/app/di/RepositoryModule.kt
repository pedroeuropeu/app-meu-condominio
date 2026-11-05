package br.com.condovitta.app.di

import br.com.condovitta.app.data.demo.DemoAuthRepository
import br.com.condovitta.app.data.demo.DemoContextRepository
import br.com.condovitta.app.domain.AuthRepository
import br.com.condovitta.app.domain.ContextRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Flavor demo: dados de exemplo no aparelho, sem servidor. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun auth(impl: DemoAuthRepository): AuthRepository
    @Binds abstract fun portaria(impl: br.com.condovitta.app.data.demo.DemoPortariaRepository): br.com.condovitta.app.domain.PortariaRepository
    @Binds abstract fun morador(impl: br.com.condovitta.app.data.demo.DemoMoradorRepository): br.com.condovitta.app.domain.MoradorRepository
    @Binds abstract fun reservas(impl: br.com.condovitta.app.data.demo.DemoReservasRepository): br.com.condovitta.app.domain.ReservasRepository
    @Binds abstract fun context(impl: DemoContextRepository): ContextRepository
}
