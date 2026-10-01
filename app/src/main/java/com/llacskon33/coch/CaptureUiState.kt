package com.llacskon33.coch

enum class CaptureStatus(val wireValue: String) {
    IDLE("idle"),
    PERMISSION_PENDING("permission_pending"),
    STARTING("starting"),
    RUNNING("running"),
    STOPPED("stopped"),
    ERROR("error");

    companion object {
        fun fromWireValue(value: String?): CaptureStatus =
            entries.firstOrNull { it.wireValue == value } ?: IDLE
    }
}

data class CaptureUiState(
    val status: CaptureStatus = CaptureStatus.IDLE,
    val message: String = "La captura está detenida."
) {
    fun permissionPending() = copy(
        status = CaptureStatus.PERMISSION_PENDING,
        message = "Esperando el permiso de captura de Android."
    )

    fun permissionDenied() = copy(
        status = CaptureStatus.IDLE,
        message = "Permiso denegado o cancelado. Puedes volver a intentarlo."
    )

    fun starting() = copy(
        status = CaptureStatus.STARTING,
        message = "Iniciando la sesión de captura…"
    )

    fun running() = copy(
        status = CaptureStatus.RUNNING,
        message = "Captura activa. Las sugerencias son solo ejemplos fijos."
    )

    fun stopped() = copy(
        status = CaptureStatus.STOPPED,
        message = "La captura está detenida."
    )

    fun failed(reason: String) = copy(
        status = CaptureStatus.ERROR,
        message = "No se pudo iniciar la captura: $reason"
    )
}
