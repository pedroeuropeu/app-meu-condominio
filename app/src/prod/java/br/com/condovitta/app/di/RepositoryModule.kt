package br.com.condovitta.app.di

import br.com.condovitta.app.data.repository.RemoteAuthRepository
import br.com.condovitta.app.data.repository.RemoteContextRepository
import br.com.condovitta.app.domain.AuthRepository
import br.com.condovitta.app.domain.ContextRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Flavors prod e local: dados da API do gestao-condominio. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun auth(impl: RemoteAuthRepository): AuthRepository
    @Binds abstract fun portaria(impl: br.com.condovitta.app.data.repository.RemotePortariaRepository): br.com.condovitta.app.domain.PortariaRepository
    @Binds abstract fun morador(impl: br.com.condovitta.app.data.repository.RemoteMoradorRepository): br.com.condovitta.app.domain.MoradorRepository
    @Binds abstract fun reservas(impl: br.com.condovitta.app.data.repository.RemoteReservasRepository): br.com.condovitta.app.domain.ReservasRepository
    @Binds abstract fun notificacoes(impl: br.com.condovitta.app.data.repository.RemoteNotificacoesRepository): br.com.condovitta.app.domain.NotificacoesRepository
    @Binds abstract fun rotina(impl: br.com.condovitta.app.data.repository.RemoteRotinaRepository): br.com.condovitta.app.domain.RotinaRepository
    @Binds abstract fun context(impl: RemoteContextRepository): ContextRepository
}
