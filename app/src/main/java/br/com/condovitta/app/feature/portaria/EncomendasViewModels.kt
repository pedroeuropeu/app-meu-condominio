package br.com.condovitta.app.feature.portaria

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.condovitta.app.data.demo.DemoCodigo
import br.com.condovitta.app.domain.Destinatarios
import br.com.condovitta.app.domain.Encomenda
import br.com.condovitta.app.domain.EncomendasPortaria
import br.com.condovitta.app.domain.Entrega
import br.com.condovitta.app.domain.NovaEncomenda
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Portaria
import br.com.condovitta.app.domain.PortariaRepository
import br.com.condovitta.app.domain.TamanhoEncomenda
import br.com.condovitta.app.domain.UnidadeResumo
import br.com.condovitta.app.ui.Messenger
import br.com.condovitta.app.ui.navigation.EncomendaDetalhe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FiltroEncomendas { AGUARDANDO, ENTREGUES }

@HiltViewModel
class EncomendasViewModel @Inject constructor(private val repo: PortariaRepository) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<EncomendasPortaria>())
    val state: StateFlow<Loadable<EncomendasPortaria>> = _state.asStateFlow()
    val filtro = MutableStateFlow(FiltroEncomendas.AGUARDANDO)

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            _state.value = when (val r = repo.encomendas()) {
                is Outcome.Ok -> Loadable(r.value, loading = false)
                is Outcome.Err -> Loadable(_state.value.data, false, r.error.message)
            }
        }
    }
}

/** Detalhe da encomenda: foto da chegada, avisar de novo e registrar a retirada (código ou terceiro + assinatura). */
@HiltViewModel
class EncomendaViewModel @Inject constructor(
    private val repo: PortariaRepository,
    private val messenger: Messenger,
    saved: SavedStateHandle
) : ViewModel() {
    /** argumento da rota [EncomendaDetalhe] */
    val id: Long = checkNotNull(saved.get<Long>("id"))

    private val _state = MutableStateFlow(Loadable<Encomenda>())
    val state: StateFlow<Loadable<Encomenda>> = _state.asStateFlow()
    val foto = MutableStateFlow<ByteArray?>(null)
    val destinatarios = MutableStateFlow<Destinatarios?>(null)
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    /** erro dentro da folha de entrega (código errado, falta assinatura...) */
    val entregaErro = MutableStateFlow<String?>(null)
    /** a folha de entrega fecha quando a entrega dá certo */
    val entregue = MutableStateFlow(0)

    /** só no demo: o código que o morador teria no app */
    val codigoDemo: String? get() = (repo as? DemoCodigo)?.codigo(id)

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = repo.encomendas()) {
                is Outcome.Ok -> {
                    val e = (r.value.aguardando + r.value.entregues).firstOrNull { it.id == id }
                        ?: _state.value.data?.takeIf { it.id == id }
                    _state.value = if (e != null) Loadable(e, loading = false) else Loadable(null, false, "Esta encomenda não está mais na lista da portaria.")
                    if (e != null) {
                        if (e.temFoto && foto.value == null) (repo.fotoEncomenda(id) as? Outcome.Ok)?.let { foto.value = it.value }
                        if (destinatarios.value == null) (repo.destinatarios(e.unidadeId) as? Outcome.Ok)?.let { destinatarios.value = it.value }
                    }
                }
                is Outcome.Err -> _state.value = Loadable(_state.value.data, false, r.error.message)
            }
        }
    }

    fun avisar() = run("Lembrete enviado ao morador") { repo.avisar(id) }

    fun entregar(entrega: Entrega) {
        if (_busy.value) return
        _busy.value = true
        entregaErro.value = null
        viewModelScope.launch {
            when (val r = repo.entregar(id, entrega)) {
                is Outcome.Ok -> {
                    _state.value = Loadable(r.value, loading = false)
                    entregue.value++
                    messenger.undoable("Entrega registrada · ${r.value.unidade}", restore = {
                        when (val d = repo.desfazerEntrega(id)) {
                            is Outcome.Ok -> { _state.value = Loadable(d.value, loading = false); Outcome.Ok(Unit) }
                            is Outcome.Err -> d
                        }
                    })
                }
                is Outcome.Err -> entregaErro.value = r.error.message
            }
            _busy.value = false
        }
    }

    private fun run(texto: String, block: suspend () -> Outcome<Encomenda>) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            when (val r = block()) {
                is Outcome.Ok -> { _state.value = Loadable(r.value, loading = false); messenger.show("$texto · ${r.value.unidade}") }
                is Outcome.Err -> messenger.show(r.error.message)
            }
            _busy.value = false
        }
    }
}

data class NovaEncomendaUi(
    val unidade: UnidadeResumo? = null,
    val destinatarios: List<String> = emptyList(),
    val destinatario: String? = null,
    val transportadora: String? = null,
    val tamanho: TamanhoEncomenda? = TamanhoEncomenda.PEQUENO,
    val local: String? = null,
    val perecivel: Boolean = false,
    val rastreio: String = "",
    val foto: ByteArray? = null,
    val saving: Boolean = false,
    val error: String? = null
) {
    val valido: Boolean get() = unidade != null && transportadora != null
}

/** O fluxo-estrela: unidade → transportadora → foto → onde guardar. Gera o código de retirada e avisa o morador. */
@HiltViewModel
class NovaEncomendaViewModel @Inject constructor(
    private val repo: PortariaRepository,
    private val messenger: Messenger
) : ViewModel() {
    private val _ui = MutableStateFlow(NovaEncomendaUi())
    val ui: StateFlow<NovaEncomendaUi> = _ui.asStateFlow()
    val unidades = MutableStateFlow<List<UnidadeResumo>>(emptyList())
    val done = MutableStateFlow(false)

    init {
        viewModelScope.launch { (repo.unidades() as? Outcome.Ok)?.let { unidades.value = it.value } }
    }

    fun update(f: (NovaEncomendaUi) -> NovaEncomendaUi) = _ui.update { f(it).copy(error = null) }

    fun unidade(u: UnidadeResumo) {
        _ui.update { it.copy(unidade = u, destinatarios = listOfNotNull(u.responsavel), destinatario = u.responsavel, error = null) }
        viewModelScope.launch {
            (repo.destinatarios(u.id) as? Outcome.Ok)?.value?.let { d ->
                _ui.update { s -> if (s.unidade?.id != u.id) s else s.copy(destinatarios = d.moradores.ifEmpty { s.destinatarios }, destinatario = s.destinatario ?: d.moradores.firstOrNull()) }
            }
        }
    }

    fun perecivel(v: Boolean) = update { it.copy(perecivel = v, local = if (v) Portaria.GELADEIRA else it.local.takeIf { l -> l != Portaria.GELADEIRA }) }

    fun salvar() {
        val s = _ui.value
        if (!s.valido || s.saving) return
        _ui.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val form = NovaEncomenda(s.unidade!!.id, s.destinatario, s.transportadora!!, s.tamanho, s.local, s.perecivel, s.rastreio)
            when (val r = repo.registrarEncomenda(form, s.foto)) {
                is Outcome.Ok -> {
                    val e = r.value
                    messenger.deferredUndo("Encomenda registrada · ${e.unidade} avisado") { repo.excluirEncomenda(e.id) }
                    done.value = true
                }
                is Outcome.Err -> _ui.update { it.copy(saving = false, error = r.error.message) }
            }
        }
    }
}

/** "Desfazer" de algo que já foi gravado: só chama [undo] se a pessoa tocar. */
private fun Messenger.deferredUndo(text: String, undo: suspend () -> Outcome<Unit>) = undoable(text, restore = undo)
