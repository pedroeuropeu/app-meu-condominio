package br.com.condovitta.app.feature.portaria

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.condovitta.app.core.common.TimeProvider
import br.com.condovitta.app.domain.AcessoListas
import br.com.condovitta.app.domain.Bloqueado
import br.com.condovitta.app.domain.Busca
import br.com.condovitta.app.domain.NovaVisita
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Painel
import br.com.condovitta.app.domain.PortariaRepository
import br.com.condovitta.app.domain.TipoVisitante
import br.com.condovitta.app.domain.UnidadeResumo
import br.com.condovitta.app.domain.ViaLiberacao
import br.com.condovitta.app.domain.Visita
import br.com.condovitta.app.ui.Messenger
import br.com.condovitta.app.ui.navigation.VisitanteDetalhe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/** Painel do turno: números, esperados, quem está dentro e a busca rápida (morador, unidade ou placa). */
@OptIn(FlowPreview::class)
@HiltViewModel
class PainelViewModel @Inject constructor(private val repo: PortariaRepository) : ViewModel() {

    private val _painel = MutableStateFlow(Loadable<Painel>())
    val painel: StateFlow<Loadable<Painel>> = _painel.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _busca = MutableStateFlow<Loadable<Busca>?>(null)
    val busca: StateFlow<Loadable<Busca>?> = _busca.asStateFlow()

    init {
        viewModelScope.launch {
            _query.debounce(300).distinctUntilChanged().collect { buscar(it) }
        }
    }

    private suspend fun buscar(q: String) {
        if (q.trim().length < 2) {
            _busca.value = null
            return
        }
        _busca.value = Loadable(_busca.value?.data, loading = true)
        _busca.value = when (val r = repo.busca(q)) {
            is Outcome.Ok -> Loadable(r.value, loading = false)
            is Outcome.Err -> Loadable(null, loading = false, error = r.error.message)
        }
    }

    fun onQuery(q: String) {
        _query.value = q
    }

    /** [silent]: atualização automática do turno, sem esconder o que já está na tela */
    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _painel.update { it.copy(loading = true, error = null) }
            // a busca aberta também se atualiza (alguém entrou ou saiu enquanto isso)
            if (_busca.value != null) buscar(_query.value)
            when (val r = repo.painel()) {
                is Outcome.Ok -> _painel.value = Loadable(r.value, loading = false)
                is Outcome.Err -> _painel.update { if (silent && it.data != null) it.copy(loading = false) else Loadable(it.data, false, r.error.message) }
            }
        }
    }
}

enum class FiltroVisitas { ESPERADOS, DENTRO, HISTORICO }

@HiltViewModel
class VisitantesViewModel @Inject constructor(
    private val repo: PortariaRepository,
    private val messenger: Messenger,
    saved: SavedStateHandle
) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<AcessoListas>())
    val state: StateFlow<Loadable<AcessoListas>> = _state.asStateFlow()
    /** o botão "No condomínio" do Início abre já nesse filtro (argumento da rota PortVisitantes) */
    val filtro = MutableStateFlow(if (saved.get<Boolean>("dentro") == true) FiltroVisitas.DENTRO else FiltroVisitas.ESPERADOS)
    /** visitante com ação em andamento (o botão da linha mostra que está trabalhando) */
    val ocupado = MutableStateFlow<Long?>(null)

    /** "Liberar" da lista: entrada (com a foto, se tirou) e o aviso com Desfazer. */
    fun liberar(v: Visita, foto: ByteArray?) {
        if (ocupado.value != null) return
        ocupado.value = v.visitanteId
        viewModelScope.launch {
            val (r, fotoFalhou) = repo.entrarComFoto(v.visitanteId, null, foto)
            when (r) {
                is Outcome.Ok -> {
                    val acesso = r.value.acessoId
                    val texto = "Entrada liberada · ${v.nome}" + if (fotoFalhou) " (a foto não foi enviada)" else ""
                    if (acesso != null) messenger.undoable(texto, restore = { repo.desfazer(acesso) }, onRestored = ::refresh) else messenger.show(texto)
                    refresh()
                }
                is Outcome.Err -> messenger.show(r.error.message)
            }
            ocupado.value = null
        }
    }

    /** "Saída" da lista de quem está no condomínio. */
    fun saida(v: Visita) {
        val acesso = v.acessoId ?: return
        if (ocupado.value != null) return
        ocupado.value = v.visitanteId
        viewModelScope.launch {
            when (val r = repo.saida(acesso)) {
                is Outcome.Ok -> { messenger.undoable("Saída registrada · ${v.nome}", restore = { repo.desfazer(acesso) }, onRestored = ::refresh); refresh() }
                is Outcome.Err -> messenger.show(r.error.message)
            }
            ocupado.value = null
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            _state.value = when (val r = repo.acesso()) {
                is Outcome.Ok -> Loadable(r.value, loading = false)
                is Outcome.Err -> Loadable(_state.value.data, false, r.error.message)
            }
        }
    }
}

/**
 * Detalhe de um visitante e as ações da guarita: liberar entrada, consultar o morador, recusar e registrar saída. Cada
 * ação mostra o aviso com "Desfazer" (POST /acessos/{id}/desfazer).
 */
@HiltViewModel
class VisitanteViewModel @Inject constructor(
    private val repo: PortariaRepository,
    private val messenger: Messenger,
    saved: SavedStateHandle
) : ViewModel() {
    /** argumento da rota [VisitanteDetalhe] (lido pela chave: funciona também nos testes) */
    private val visitanteId: Long = checkNotNull(saved.get<Long>("visitanteId"))

    private val _state = MutableStateFlow(Loadable<Visita>())
    val state: StateFlow<Loadable<Visita>> = _state.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            _state.value = when (val r = repo.acesso()) {
                is Outcome.Ok -> r.value.todas().firstOrNull { it.visitanteId == visitanteId }
                    ?.also { v -> carregarFoto(v) }
                    ?.let { Loadable(it, loading = false) } ?: Loadable(null, false, "Este visitante não está mais na lista de hoje.")
                is Outcome.Err -> Loadable(_state.value.data, false, r.error.message)
            }
        }
    }

    /** foto de quem entrou (quando a portaria tirou) */
    val foto = MutableStateFlow<ByteArray?>(null)

    fun liberar(via: ViaLiberacao? = null, foto: ByteArray? = null) = acao("Entrada liberada") {
        val (r, fotoFalhou) = repo.entrarComFoto(visitanteId, via, foto)
        if (fotoFalhou) messenger.show("A foto não foi enviada; a entrada foi liberada.")
        if (r is Outcome.Ok && foto != null && !fotoFalhou) this.foto.value = foto
        r
    }
    private fun carregarFoto(v: Visita) {
        val acesso = v.acessoId ?: return
        if (!v.temFoto || foto.value != null) return
        viewModelScope.launch { (repo.verFotoAcesso(acesso) as? Outcome.Ok)?.value?.let { foto.value = it } }
    }

    fun consultar() = acao("Pedido enviado ao morador", undo = false) { repo.consultar(visitanteId) }
    fun recusar() = _state.value.data?.acessoId?.let { id -> acao("Entrada recusada") { repo.recusar(id) } }
    fun saida() = _state.value.data?.acessoId?.let { id -> acao("Saída registrada") { repo.saida(id) } }

    private fun acao(texto: String, undo: Boolean = true, block: suspend () -> Outcome<Visita>) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            when (val r = block()) {
                is Outcome.Ok -> {
                    val v = r.value
                    _state.value = Loadable(v, loading = false)
                    val acesso = v.acessoId
                    if (undo && acesso != null) messenger.undoable("$texto · ${v.nome}", restore = { repo.desfazer(acesso) }, onRestored = ::refresh)
                    else messenger.show("$texto · ${v.nome}")
                }
                is Outcome.Err -> messenger.show(r.error.message)
            }
            _busy.value = false
        }
    }
}

data class NovaVisitaUi(
    val unidade: UnidadeResumo? = null,
    val condominio: Boolean = false,
    val nome: String = "",
    val tipo: TipoVisitante = TipoVisitante.VISITA,
    val documento: String = "",
    val telefone: String = "",
    val placa: String = "",
    val obs: String = "",
    val saving: Boolean = false,
    val error: String? = null
) {
    val valido: Boolean get() = nome.trim().length >= 3 && (unidade != null || condominio)
}

/** Cadastro rápido de quem chegou sem pré-autorização: libera na hora ou pede ao morador pelo app. */
@HiltViewModel
class NovaVisitaViewModel @Inject constructor(
    private val repo: PortariaRepository,
    private val messenger: Messenger,
    private val time: TimeProvider
) : ViewModel() {
    private val _ui = MutableStateFlow(NovaVisitaUi())
    val ui: StateFlow<NovaVisitaUi> = _ui.asStateFlow()
    val unidades = MutableStateFlow<List<UnidadeResumo>>(emptyList())

    /** fecha a tela quando der certo (com o id do visitante) */
    val done = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch { (repo.unidades() as? Outcome.Ok)?.let { unidades.value = it.value } }
    }

    fun update(f: (NovaVisitaUi) -> NovaVisitaUi) = _ui.update { f(it).copy(error = null) }

    /** [consultar] = pergunta ao morador pelo app; senão a portaria libera agora (pelo interfone). */
    fun salvar(consultar: Boolean) {
        val s = _ui.value
        if (!s.valido || s.saving) return
        _ui.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val agora = time.now().toLocalTime().withSecond(0).withNano(0)
            val fim = agora.plusHours(4).let { if (it.isBefore(agora)) LocalTime.of(23, 59) else it }
            val form = NovaVisita(
                nome = s.nome, tipo = s.tipo, unidadeId = s.unidade?.id, documento = s.documento, telefone = s.telefone,
                placa = s.placa, obs = s.obs, inicio = agora, fim = fim, consultar = consultar && s.unidade != null
            )
            val criada = repo.novaVisita(form)
            if (criada is Outcome.Err) {
                _ui.update { it.copy(saving = false, error = criada.error.message) }
                return@launch
            }
            val v = (criada as Outcome.Ok).value
            if (form.consultar) {
                messenger.show("Pedido enviado ao morador do ${v.unidade} · ${v.nome}")
                done.value = v.visitanteId
                return@launch
            }
            when (val r = repo.entrada(v.visitanteId, ViaLiberacao.INTERFONE)) {
                is Outcome.Ok -> {
                    val acesso = r.value.acessoId
                    if (acesso != null) messenger.undoable("Entrada liberada · ${v.nome}", restore = { repo.desfazer(acesso) })
                    else messenger.show("Entrada liberada · ${v.nome}")
                    done.value = v.visitanteId
                }
                // cadastrou mas não entrou (ex.: bloqueado): fica na lista de esperados
                is Outcome.Err -> _ui.update { it.copy(saving = false, error = r.error.message) }
            }
        }
    }
}

@HiltViewModel
class BloqueadosViewModel @Inject constructor(private val repo: PortariaRepository) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<List<Bloqueado>>())
    val state: StateFlow<Loadable<List<Bloqueado>>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            _state.value = when (val r = repo.bloqueados()) {
                is Outcome.Ok -> Loadable(r.value, loading = false)
                is Outcome.Err -> Loadable(null, false, r.error.message)
            }
        }
    }
}

/** Esperados: os que ainda vão entrar. */
fun List<Visita>.esperadosAgora() = filter { it.status.pendente }
