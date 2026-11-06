package br.com.condovitta.app.ui

import br.com.condovitta.app.domain.Destino

/** O que os dois modos recebem da raiz: o contador do sino e a tela pedida por uma notificação. */
data class ModoExtras(val naoLidas: Long = 0, val destino: Destino? = null, val consumirDestino: () -> Unit = {})
