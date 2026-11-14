package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.ContenidoRevision

// HU 53: contrato para probar la revisión sin red.
interface RevisionRepository {

    suspend fun porRevisar(): Resultado<List<ContenidoRevision>>

    suspend fun aprobar(id: Long): Resultado<Unit>
}
