package br.com.condovitta.app.data.repository

import br.com.condovitta.app.data.remote.DomesticoPedidoDto
import br.com.condovitta.app.data.remote.LeituraFormDto
import br.com.condovitta.app.data.remote.MovimentoFormDto
import br.com.condovitta.app.data.remote.PetPedidoDto
import br.com.condovitta.app.data.remote.RegistroFormDto
import br.com.condovitta.app.data.remote.RotinaApi
import br.com.condovitta.app.data.remote.apiCall
import br.com.condovitta.app.data.remote.apiUnit
import br.com.condovitta.app.data.remote.toDomain
import br.com.condovitta.app.data.remote.toVeiculo
import br.com.condovitta.app.domain.ConsultaPlaca
import br.com.condovitta.app.domain.Domestico
import br.com.condovitta.app.domain.GasMes
import br.com.condovitta.app.domain.LeituraGas
import br.com.condovitta.app.domain.Livro
import br.com.condovitta.app.domain.MinhaUnidade
import br.com.condovitta.app.domain.MovimentoGaragem
import br.com.condovitta.app.domain.NovoRegistro
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Pet
import br.com.condovitta.app.domain.RegistroLivro
import br.com.condovitta.app.domain.RotinaRepository
import br.com.condovitta.app.domain.SentidoGaragem
import br.com.condovitta.app.domain.VeiculosPortaria
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteRotinaRepository @Inject constructor(private val api: RotinaApi) : RotinaRepository {
    override suspend fun livro(): Outcome<Livro> = apiCall { api.livro().toDomain() }
    override suspend fun registrar(form: NovoRegistro): Outcome<RegistroLivro> =
        apiCall { api.registrar(RegistroFormDto(form.tipo.name, form.texto.trim(), form.unidadeId, form.avisarSindico)).toDomain() }
    override suspend fun excluirRegistro(id: Long): Outcome<Unit> = apiUnit { api.excluirRegistro(id) }
    override suspend fun devolverChave(id: Long): Outcome<Unit> = apiCall { api.devolverChave(id); Unit }

    override suspend fun veiculos(): Outcome<VeiculosPortaria> = apiCall { api.veiculos().let { d -> VeiculosPortaria(d.moradores.map { it.toVeiculo() }, d.movimento.map { it.toDomain() }) } }
    override suspend fun consultarPlaca(placa: String): Outcome<ConsultaPlaca> = apiCall {
        api.placa(placa.uppercase().filter { it.isLetterOrDigit() }).let { d -> ConsultaPlaca(d.placa, d.moradores.map { it.toVeiculo() }, d.visitantes.map { it.toDomain() }) }
    }
    override suspend fun movimento(placa: String, sentido: SentidoGaragem, portao: String?): Outcome<MovimentoGaragem> =
        apiCall { api.movimento(MovimentoFormDto(placa.uppercase().filter { it.isLetterOrDigit() }, sentido.name, portao)).toDomain() }

    override suspend fun gas(mes: String?): Outcome<GasMes> = apiCall { api.gas(mes).toDomain() }
    override suspend fun lancarGas(mes: String, unidadeId: Long, leitura: BigDecimal): Outcome<LeituraGas> = apiCall { api.lancarGas(mes, unidadeId, LeituraFormDto(leitura)).toDomain() }

    override suspend fun minhaUnidade(): Outcome<MinhaUnidade> = apiCall { api.minhaUnidade().toDomain() }
    override suspend fun adicionarPet(nome: String, especie: String?, raca: String?, porte: String?): Outcome<Pet> =
        apiCall { api.adicionarPet(PetPedidoDto(nome.trim(), especie?.ifBlank { null }, raca?.ifBlank { null }, porte?.ifBlank { null })).toDomain() }
    override suspend fun removerPet(id: Long): Outcome<Unit> = apiUnit { api.removerPet(id) }
    override suspend fun adicionarDomestico(nome: String, documento: String?, funcao: String?, diasHorarios: String?): Outcome<Domestico> =
        apiCall { api.adicionarDomestico(DomesticoPedidoDto(nome.trim(), documento?.ifBlank { null }, funcao?.ifBlank { null }, diasHorarios?.ifBlank { null })).toDomain() }
    override suspend fun removerDomestico(id: Long): Outcome<Unit> = apiUnit { api.removerDomestico(id) }
}
