package br.com.condovitta.app.feature.servicos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.condovitta.app.core.common.TimeProvider
import br.com.condovitta.app.domain.Anexo
import br.com.condovitta.app.domain.ArquivoEnvio
import br.com.condovitta.app.domain.AtendimentoRepository
import br.com.condovitta.app.domain.ChamadoDetalhe
import br.com.condovitta.app.domain.MeusChamados
import br.com.condovitta.app.domain.NovaOcorrencia
import br.com.condovitta.app.domain.NovoChamado
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.PainelOcorrencias
import br.com.condovitta.app.domain.UnidadeOpcao
import br.com.condovitta.app.feature.portaria.Loadable
import br.com.condovitta.app.ui.Messenger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado de um envio (formulário): salvando, erro no lugar e um contador que sobe quando deu certo. */
class Envio {
    val salvando = MutableStateFlow(false)
    val erro = MutableStateFlow<String?>(null)
    val ok = MutableStateFlow(0)

    suspend fun <T> rodar(block: suspend () -> Outcome<T>, sucesso: (T) -> Unit = {}): Boolean {
        if (salvando.value) return false
        salvando.value = true
        erro.value = null
        val r = block()
        salvando.value = false
        return when (r) {
            is Outcome.Ok -> { sucesso(r.value); ok.value++; true }
            is Outcome.Err -> { erro.value = r.error.message; false }
        }
    }
}

/* ------------------------------------------------------------------ chamados */

@HiltViewModel
class ChamadosViewModel @Inject constructor(private val repo: AtendimentoRepository) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<MeusChamados>())
    val state: StateFlow<Loadable<MeusChamados>> = _state.asStateFlow()
    fun refresh() = viewModelScope.launch { _state.carregar { repo.chamados() } }
}

@HiltViewModel
class NovoChamadoViewModel @Inject constructor(private val repo: AtendimentoRepository, private val messenger: Messenger) : ViewModel() {
    val envio = Envio()
    val fotos = MutableStateFlow<List<ArquivoEnvio>>(emptyList())
    /** id do chamado criado: a tela abre o detalhe */
    val criado = MutableStateFlow<Long?>(null)

    fun adicionar(novas: List<ArquivoEnvio>) { fotos.value = (fotos.value + novas).take(5) }
    fun remover(f: ArquivoEnvio) { fotos.value = fotos.value - f }

    fun abrir(form: NovoChamado) {
        if (form.titulo.trim().length < 4) { envio.erro.value = "Dê um título curto que explique o problema."; return }
        viewModelScope.launch {
            envio.rodar({ repo.abrirChamado(form, fotos.value) }) {
                messenger.show("Chamado aberto · a administração foi avisada")
                criado.value = it.chamado.id
            }
        }
    }
}

@HiltViewModel
class ChamadoViewModel @Inject constructor(saved: SavedStateHandle, private val repo: AtendimentoRepository, private val messenger: Messenger) : ViewModel() {
    val id: Long = saved.get<Long>("id") ?: 0L
    private val _state = MutableStateFlow(Loadable<ChamadoDetalhe>())
    val state: StateFlow<Loadable<ChamadoDetalhe>> = _state.asStateFlow()
    val envio = Envio()
    val fotos = MutableStateFlow<List<ArquivoEnvio>>(emptyList())
    val baixando = MutableStateFlow<String?>(null)
    val fechou = MutableStateFlow(false)

    fun refresh() = viewModelScope.launch { _state.carregar { repo.chamado(id) } }

    fun adicionar(novas: List<ArquivoEnvio>) { fotos.value = (fotos.value + novas).take(5) }
    fun remover(f: ArquivoEnvio) { fotos.value = fotos.value - f }

    fun responder(texto: String, limpar: () -> Unit) {
        if (texto.isBlank() && fotos.value.isEmpty()) return
        viewModelScope.launch {
            envio.rodar({ repo.responder(id, texto, fotos.value) }) {
                _state.value = Loadable(it, loading = false)
                fotos.value = emptyList()
                limpar()
            }
        }
    }

    fun avaliar(nota: Int, comentario: String) = viewModelScope.launch {
        when (val r = repo.avaliar(id, nota, comentario)) {
            is Outcome.Ok -> { messenger.show("Obrigado pela avaliação!"); refresh() }
            is Outcome.Err -> messenger.show(r.error.message)
        }
    }

    fun reabrir() = viewModelScope.launch {
        when (val r = repo.reabrir(id)) {
            is Outcome.Ok -> { _state.value = Loadable(r.value, loading = false); messenger.show("Chamado reaberto") }
            is Outcome.Err -> messenger.show(r.error.message)
        }
    }

    /** cancelar sem "tem certeza?": volta para a lista e dá para desfazer pela barra */
    fun cancelar() {
        fechou.value = true
        messenger.deferred("Chamado cancelado", undo = { fechou.value = false }, commit = { repo.cancelarChamado(id) })
    }

    fun abrirAnexo(a: Anexo, abrir: (ByteArray) -> Unit) = viewModelScope.launch {
        baixar(messenger, baixando, "a${a.id}", { repo.anexoChamado(id, a.id) }, abrir)
    }
}

/* ------------------------------------------------------------------ ocorrências */

@HiltViewModel
class OcorrenciasViewModel @Inject constructor(private val repo: AtendimentoRepository, private val messenger: Messenger, private val time: TimeProvider) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<PainelOcorrencias>())
    val state: StateFlow<Loadable<PainelOcorrencias>> = _state.asStateFlow()
    val envio = Envio()
    val fotos = MutableStateFlow<List<ArquivoEnvio>>(emptyList())
    val baixando = MutableStateFlow<String?>(null)
    fun hoje() = time.today()

    fun refresh() = viewModelScope.launch { _state.carregar { repo.ocorrencias() } }

    fun adicionar(novas: List<ArquivoEnvio>) { fotos.value = (fotos.value + novas).take(5) }
    fun remover(f: ArquivoEnvio) { fotos.value = fotos.value - f }

    fun defesa(id: Long, texto: String) {
        if (texto.trim().length < 10) { envio.erro.value = "Conte a sua versão com um pouco mais de detalhe."; return }
        viewModelScope.launch {
            envio.rodar({ repo.defesa(id, texto, fotos.value) }) {
                _state.value = Loadable(it, loading = false)
                fotos.value = emptyList()
                messenger.show("Defesa enviada · a administração vai analisar")
            }
        }
    }

    fun abrirProva(ocorrenciaId: Long, a: Anexo, abrir: (ByteArray) -> Unit) = viewModelScope.launch {
        baixar(messenger, baixando, "a${a.id}", { repo.anexoOcorrencia(ocorrenciaId, a.id) }, abrir)
    }
}

@HiltViewModel
class NovaOcorrenciaViewModel @Inject constructor(private val repo: AtendimentoRepository, private val messenger: Messenger, private val time: TimeProvider) : ViewModel() {
    val unidades = MutableStateFlow<List<UnidadeOpcao>>(emptyList())
    val envio = Envio()
    val fotos = MutableStateFlow<List<ArquivoEnvio>>(emptyList())
    fun agora() = time.now()

    init {
        viewModelScope.launch { (repo.unidades() as? Outcome.Ok)?.let { unidades.value = it.value } }
    }

    fun adicionar(novas: List<ArquivoEnvio>) { fotos.value = (fotos.value + novas).take(5) }
    fun remover(f: ArquivoEnvio) { fotos.value = fotos.value - f }

    fun registrar(form: NovaOcorrencia, declaracao: Boolean) {
        when {
            !form.geral && form.unidade.isNullOrBlank() -> { envio.erro.value = "Escolha a unidade envolvida."; return }
            form.descricao.trim().length < 10 -> { envio.erro.value = "Descreva o que aconteceu com um pouco mais de detalhe."; return }
            form.ocorridaEm.isAfter(time.now()) -> { envio.erro.value = "A data não pode ser no futuro."; return }
            !declaracao -> { envio.erro.value = "Confirme que as informações são verdadeiras."; return }
        }
        viewModelScope.launch {
            envio.rodar({ repo.registrarOcorrencia(form, fotos.value) }) { messenger.show("Ocorrência registrada · a administração vai analisar") }
        }
    }
}
