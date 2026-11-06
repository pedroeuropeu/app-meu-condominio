package br.com.condovitta.app.feature.rotina

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.condovitta.app.domain.ConsultaPlaca
import br.com.condovitta.app.domain.GasMes
import br.com.condovitta.app.domain.Livro
import br.com.condovitta.app.domain.MinhaUnidade
import br.com.condovitta.app.domain.NovoRegistro
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.RotinaRepository
import br.com.condovitta.app.domain.SentidoGaragem
import br.com.condovitta.app.domain.TipoRegistro
import br.com.condovitta.app.domain.VeiculosPortaria
import br.com.condovitta.app.feature.portaria.Loadable
import br.com.condovitta.app.ui.Messenger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private suspend fun <T> MutableStateFlow<Loadable<T>>.load(block: suspend () -> Outcome<T>) {
    update { it.copy(loading = true, error = null) }
    value = when (val r = block()) {
        is Outcome.Ok -> Loadable(r.value, loading = false)
        is Outcome.Err -> Loadable(value.data, false, r.error.message)
    }
}

/** Livro da portaria: ocorrências, rondas, chaves e prestadores do turno. */
@HiltViewModel
class LivroViewModel @Inject constructor(private val repo: RotinaRepository, private val messenger: Messenger) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<Livro>())
    val state: StateFlow<Loadable<Livro>> = _state.asStateFlow()
    val salvando = MutableStateFlow(false)
    val erro = MutableStateFlow<String?>(null)
    val salvo = MutableStateFlow(0)

    fun refresh() = viewModelScope.launch { _state.load { repo.livro() } }

    fun registrar(tipo: TipoRegistro, texto: String, avisarSindico: Boolean) {
        if (salvando.value) return
        salvando.value = true
        erro.value = null
        viewModelScope.launch {
            when (val r = repo.registrar(NovoRegistro(tipo, texto, avisarSindico = avisarSindico))) {
                is Outcome.Ok -> { salvo.value++; refresh(); messenger.show("Registrado no livro" + if (avisarSindico) " · síndico avisado" else "") }
                is Outcome.Err -> erro.value = r.error.message
            }
            salvando.value = false
        }
    }

    /** apagar sem "tem certeza?": some da lista e só vai para o servidor se não desfizer */
    fun excluir(id: Long) {
        val atual = _state.value.data ?: return
        _state.value = Loadable(atual.copy(registros = atual.registros.filterNot { it.id == id }), loading = false)
        messenger.deferred("Registro apagado", undo = { refresh() }, commit = { repo.excluirRegistro(id) })
    }

    fun devolverChave(id: Long) {
        viewModelScope.launch {
            when (val r = repo.devolverChave(id)) {
                is Outcome.Ok -> { refresh(); messenger.show("Chave devolvida") }
                is Outcome.Err -> messenger.show(r.error.message)
            }
        }
    }
}

/** Garagem: veículos dos moradores, consulta de placa e entradas/saídas manuais. */
@HiltViewModel
class VeiculosViewModel @Inject constructor(private val repo: RotinaRepository, private val messenger: Messenger) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<VeiculosPortaria>())
    val state: StateFlow<Loadable<VeiculosPortaria>> = _state.asStateFlow()
    val consulta = MutableStateFlow<Loadable<ConsultaPlaca>?>(null)

    fun refresh() = viewModelScope.launch { _state.load { repo.veiculos() } }

    fun consultar(placa: String) {
        if (placa.filter { it.isLetterOrDigit() }.length < 7) { consulta.value = null; return }
        consulta.value = Loadable(loading = true)
        viewModelScope.launch {
            consulta.value = when (val r = repo.consultarPlaca(placa)) {
                is Outcome.Ok -> Loadable(r.value, loading = false)
                is Outcome.Err -> Loadable(null, false, r.error.message)
            }
        }
    }

    fun registrar(placa: String, sentido: SentidoGaragem) {
        viewModelScope.launch {
            when (val r = repo.movimento(placa, sentido, "Garagem")) {
                is Outcome.Ok -> { refresh(); messenger.show("${if (sentido == SentidoGaragem.ENTRADA) "Entrada" else "Saída"} registrada · ${r.value.placa}") }
                is Outcome.Err -> messenger.show(r.error.message)
            }
        }
    }
}

/** Leitura de gás do mês: o porteiro lança o m³ de cada unidade; o consumo entra no boleto. */
@HiltViewModel
class GasViewModel @Inject constructor(private val repo: RotinaRepository, private val messenger: Messenger) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<GasMes>())
    val state: StateFlow<Loadable<GasMes>> = _state.asStateFlow()

    fun refresh() = viewModelScope.launch { _state.load { repo.gas() } }

    fun lancar(unidadeId: Long, texto: String, ok: () -> Unit) {
        val mes = _state.value.data?.mes ?: return
        val valor = texto.replace(',', '.').toBigDecimalOrNull()
        if (valor == null || valor.signum() < 0) { messenger.show("Digite a leitura do medidor (ex.: 152,345)."); return }
        viewModelScope.launch {
            when (val r = repo.lancarGas(mes, unidadeId, valor)) {
                is Outcome.Ok -> {
                    ok()
                    _state.update { s -> s.copy(data = s.data?.let { g ->
                        val unidades = g.unidades.map { if (it.unidadeId == unidadeId) r.value else it }
                        g.copy(unidades = unidades, lidas = unidades.count { it.lida }, alertas = unidades.count { it.alerta != null })
                    }) }
                    messenger.show(r.value.alerta?.let { "${r.value.unidade}: $it" } ?: "Leitura de ${r.value.unidade} lançada")
                }
                is Outcome.Err -> messenger.show(r.error.message)
            }
        }
    }
}

/** Minha unidade: moradores, pets e funcionários domésticos (o titular cadastra). */
@HiltViewModel
class MinhaUnidadeViewModel @Inject constructor(private val repo: RotinaRepository, private val messenger: Messenger) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<MinhaUnidade>())
    val state: StateFlow<Loadable<MinhaUnidade>> = _state.asStateFlow()

    fun refresh() = viewModelScope.launch { _state.load { repo.minhaUnidade() } }

    fun adicionarPet(nome: String, especie: String, raca: String, porte: String?) = viewModelScope.launch {
        when (val r = repo.adicionarPet(nome, especie, raca, porte)) {
            is Outcome.Ok -> { refresh(); messenger.show("${r.value.nome} cadastrado(a)") }
            is Outcome.Err -> messenger.show(r.error.message)
        }
    }

    fun adicionarDomestico(nome: String, documento: String, funcao: String, dias: String) = viewModelScope.launch {
        when (val r = repo.adicionarDomestico(nome, documento, funcao, dias)) {
            is Outcome.Ok -> { refresh(); messenger.show("${r.value.nome} cadastrado(a)") }
            is Outcome.Err -> messenger.show(r.error.message)
        }
    }

    fun removerPet(id: Long) {
        val atual = _state.value.data ?: return
        val pet = atual.pets.firstOrNull { it.id == id } ?: return
        _state.value = Loadable(atual.copy(pets = atual.pets - pet), loading = false)
        messenger.deferred("${pet.nome} removido(a)", undo = { refresh() }, commit = { repo.removerPet(id) })
    }

    fun removerDomestico(id: Long) {
        val atual = _state.value.data ?: return
        val d = atual.domesticos.firstOrNull { it.id == id } ?: return
        _state.value = Loadable(atual.copy(domesticos = atual.domesticos - d), loading = false)
        messenger.deferred("${d.nome} removido(a)", undo = { refresh() }, commit = { repo.removerDomestico(id) })
    }
}

