package br.com.condovitta.app.feature.servicos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.condovitta.app.domain.DocParaAssinar
import br.com.condovitta.app.domain.EstadoPlenario
import br.com.condovitta.app.domain.Evidencias
import br.com.condovitta.app.domain.GovernancaRepository
import br.com.condovitta.app.domain.MeusDocumentos
import br.com.condovitta.app.domain.MinhasAssembleias
import br.com.condovitta.app.domain.OpcaoVoto
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Poderes
import br.com.condovitta.app.domain.Representante
import br.com.condovitta.app.domain.ResultadoAssinatura
import br.com.condovitta.app.feature.portaria.Loadable
import br.com.condovitta.app.ui.Messenger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/* ------------------------------------------------------------------ documentos */

@HiltViewModel
class DocumentosViewModel @Inject constructor(private val repo: GovernancaRepository, private val messenger: Messenger) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<MeusDocumentos>())
    val state: StateFlow<Loadable<MeusDocumentos>> = _state.asStateFlow()
    val baixando = MutableStateFlow<String?>(null)

    fun refresh() = viewModelScope.launch { _state.carregar { repo.documentos() } }

    fun oficial(id: Long, abrir: (ByteArray) -> Unit) = viewModelScope.launch { baixar(messenger, baixando, "o$id", { repo.oficialPdf(id) }, abrir) }
    fun pdf(id: Long, abrir: (ByteArray) -> Unit) = viewModelScope.launch { baixar(messenger, baixando, "d$id", { repo.documentoPdf(id) }, abrir) }
}

@HiltViewModel
class DocumentoViewModel @Inject constructor(saved: SavedStateHandle, private val repo: GovernancaRepository, private val messenger: Messenger) : ViewModel() {
    val id: Long = saved.get<Long>("id") ?: 0L
    private val _state = MutableStateFlow(Loadable<DocParaAssinar>())
    val state: StateFlow<Loadable<DocParaAssinar>> = _state.asStateFlow()
    val baixando = MutableStateFlow<String?>(null)
    val envio = Envio()
    /** e-mail para onde o código foi (null = ainda não pediu) */
    val codigoPara = MutableStateFlow<String?>(null)
    val resultado = MutableStateFlow<ResultadoAssinatura?>(null)
    val evidencias = MutableStateFlow<Evidencias?>(null)
    val recusou = MutableStateFlow(false)

    fun refresh() = viewModelScope.launch { _state.carregar { repo.documento(id) } }

    fun pdf(abrir: (ByteArray) -> Unit) = viewModelScope.launch { baixar(messenger, baixando, "pdf", { repo.documentoPdf(id) }, abrir) }

    fun pedirCodigo() = viewModelScope.launch {
        when (val r = repo.enviarCodigo(id)) {
            is Outcome.Ok -> { codigoPara.value = r.value; messenger.show("Código enviado para ${r.value}") }
            is Outcome.Err -> messenger.show(r.error.message)
        }
    }

    fun assinar(png: ByteArray?, codigo: String) {
        val d = _state.value.data ?: return
        when {
            png == null -> { envio.erro.value = "Desenhe a sua assinatura no quadro."; return }
            d.exigeCodigo && codigo.length != 6 -> { envio.erro.value = "Digite o código de 6 dígitos que enviamos por e-mail."; return }
        }
        viewModelScope.launch {
            envio.rodar({ repo.assinar(id, png!!, codigo.takeIf { d.exigeCodigo }) }) {
                resultado.value = it
                messenger.show("Documento assinado")
                refresh()
            }
        }
    }

    fun recusar(motivo: String) {
        if (motivo.trim().length < 5) { envio.erro.value = "Conte o motivo da recusa."; return }
        viewModelScope.launch {
            envio.rodar({ repo.recusar(id, motivo) }) {
                recusou.value = true
                messenger.show("Recusa registrada · o solicitante foi avisado")
            }
        }
    }

    fun carregarEvidencias() = viewModelScope.launch {
        when (val r = repo.evidencias(id)) {
            is Outcome.Ok -> evidencias.value = r.value
            is Outcome.Err -> messenger.show(r.error.message)
        }
    }
}

/* ------------------------------------------------------------------ assembleias */

@HiltViewModel
class AssembleiasViewModel @Inject constructor(private val repo: GovernancaRepository, private val messenger: Messenger) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<MinhasAssembleias>())
    val state: StateFlow<Loadable<MinhasAssembleias>> = _state.asStateFlow()
    val baixando = MutableStateFlow<String?>(null)
    val representantes = MutableStateFlow<List<Representante>>(emptyList())
    val envio = Envio()

    fun refresh() = viewModelScope.launch { _state.carregar { repo.assembleias() } }

    fun edital(id: Long, abrir: (ByteArray) -> Unit) = viewModelScope.launch { baixar(messenger, baixando, "e$id", { repo.edital(id) }, abrir) }
    fun ata(id: Long, abrir: (ByteArray) -> Unit) = viewModelScope.launch { baixar(messenger, baixando, "a$id", { repo.ata(id) }, abrir) }

    fun carregarRepresentantes(id: Long) = viewModelScope.launch {
        representantes.value = emptyList()
        envio.erro.value = null
        when (val r = repo.representantes(id)) {
            is Outcome.Ok -> representantes.value = r.value
            is Outcome.Err -> envio.erro.value = r.error.message
        }
    }

    fun procuracao(id: Long, representante: Long?, poderes: Poderes, png: ByteArray?) {
        when {
            representante == null -> { envio.erro.value = "Escolha quem vai representar você."; return }
            png == null -> { envio.erro.value = "Assine no quadro para confirmar a procuração."; return }
        }
        viewModelScope.launch {
            envio.rodar({ repo.enviarProcuracao(id, representante!!, poderes, png!!) }) {
                messenger.show("Procuração enviada para ${it.outorgado ?: "o representante"}")
                refresh()
            }
        }
    }

    fun cancelarProcuracao(id: Long, procuracaoId: Long) {
        val atual = _state.value.data ?: return
        _state.value = Loadable(atual.copy(proximas = atual.proximas.map { if (it.id == id) it.copy(minhaProcuracao = null, podeEnviarProcuracao = true) else it }), loading = false)
        messenger.deferred("Procuração cancelada", undo = { refresh() }, commit = { repo.cancelarProcuracao(id, procuracaoId) })
    }
}

/** Plenário virtual: o estado da sala é consultado a cada ~3 s enquanto a tela está aberta. */
@HiltViewModel
class PlenarioViewModel @Inject constructor(saved: SavedStateHandle, private val repo: GovernancaRepository, private val messenger: Messenger) : ViewModel() {
    val id: Long = saved.get<Long>("id") ?: 0L
    private val _state = MutableStateFlow(Loadable<EstadoPlenario>())
    val state: StateFlow<Loadable<EstadoPlenario>> = _state.asStateFlow()
    val votando = MutableStateFlow<Long?>(null)
    val baixando = MutableStateFlow<String?>(null)
    private var poll: Job? = null

    fun refresh() = viewModelScope.launch { _state.carregar { repo.estado(id) } }

    /** a tela liga ao aparecer e desliga ao sair (não consulta em segundo plano) */
    fun acompanhar(ligado: Boolean) {
        poll?.cancel()
        if (!ligado) return
        poll = viewModelScope.launch {
            while (isActive) {
                (repo.estado(id) as? Outcome.Ok)?.let { _state.value = Loadable(it.value, loading = false) }
                delay(INTERVALO_MS)
            }
        }
    }

    fun presenca() = viewModelScope.launch {
        when (val r = repo.presenca(id)) {
            is Outcome.Ok -> { _state.value = Loadable(r.value, loading = false); messenger.show("Presença confirmada") }
            is Outcome.Err -> messenger.show(r.error.message)
        }
    }

    fun votar(itemId: Long, opcao: OpcaoVoto) {
        if (votando.value != null) return
        votando.value = itemId
        viewModelScope.launch {
            when (val r = repo.votar(id, itemId, opcao)) {
                is Outcome.Ok -> {
                    _state.value.data?.let { e -> _state.value = Loadable(e.copy(votos = e.votos + (itemId to r.value)), loading = false) }
                    messenger.show("Voto registrado: ${opcao.label}" + if (r.value.unidades > 1) " · vale por ${r.value.unidades} unidades" else "")
                    (repo.estado(id) as? Outcome.Ok)?.let { _state.value = Loadable(it.value, loading = false) }
                }
                is Outcome.Err -> messenger.show(r.error.message)
            }
            votando.value = null
        }
    }

    fun palavra() = viewModelScope.launch {
        when (val r = repo.palavra(id)) {
            is Outcome.Ok -> {
                _state.value.data?.let { e -> _state.value = Loadable(e.copy(filaPosicao = r.value), loading = false) }
                messenger.show(r.value?.let { "Você está na fila para falar (posição $it)" } ?: "Você saiu da fila")
            }
            is Outcome.Err -> messenger.show(r.error.message)
        }
    }

    fun chat(texto: String, limpar: () -> Unit) {
        if (texto.isBlank()) return
        viewModelScope.launch {
            when (val r = repo.chat(id, texto)) {
                is Outcome.Ok -> {
                    limpar()
                    _state.value.data?.let { e -> if (e.chat.none { it.id == r.value.id }) _state.value = Loadable(e.copy(chat = e.chat + r.value), loading = false) }
                }
                is Outcome.Err -> messenger.show(r.error.message)
            }
        }
    }

    fun comprovante(itemId: Long, abrir: (ByteArray) -> Unit) = viewModelScope.launch { baixar(messenger, baixando, "c$itemId", { repo.comprovante(id, itemId) }, abrir) }

    companion object {
        const val INTERVALO_MS = 3_000L
    }
}
