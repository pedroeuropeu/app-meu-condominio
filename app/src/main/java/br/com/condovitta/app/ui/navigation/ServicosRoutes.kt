package br.com.condovitta.app.ui.navigation

import kotlinx.serialization.Serializable

/* Serviços do morador (antes "Em construção"): chamados, ocorrências, documentos, assembleias, prestação de contas,
   consumo de gás e calendário. Os detalhes usam "id" (lido pelo SavedStateHandle nos ViewModels). */

@Serializable data object ChamadosRoute
@Serializable data object NovoChamadoRoute
@Serializable data class ChamadoRoute(val id: Long)
@Serializable data object OcorrenciasRoute
@Serializable data object NovaOcorrenciaRoute
@Serializable data object DocumentosRoute
@Serializable data class DocumentoRoute(val id: Long)
@Serializable data object AssembleiasRoute
@Serializable data class PlenarioRoute(val id: Long)
@Serializable data object PrestacaoRoute
@Serializable data object GasMoradorRoute
@Serializable data object CalendarioRoute
