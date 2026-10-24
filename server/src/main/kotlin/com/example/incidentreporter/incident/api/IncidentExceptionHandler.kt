package com.example.incidentreporter.incident.api

import com.example.incidentreporter.incident.domain.IncidentNotFoundException
import com.example.incidentreporter.incident.domain.InvalidIncidentException
import com.example.incidentreporter.incident.domain.PhotoNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.net.URI

enum class ApiErrorCode(val defaultDetail: String) {
    INCIDENT_NOT_FOUND("Incident not found"),
    INVALID_INCIDENT("Invalid incident"),
    INVALID_REQUEST("The request is invalid"),
    PAYLOAD_TOO_LARGE("The upload exceeds the allowed size"),
    RESOURCE_NOT_FOUND("Resource not found"),
    INTERNAL_ERROR("An unexpected server error occurred"),
}

@RestControllerAdvice
class IncidentExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = LoggerFactory.getLogger(IncidentExceptionHandler::class.java)

    @ExceptionHandler(IncidentNotFoundException::class)
    fun incidentNotFound(error: IncidentNotFoundException, request: HttpServletRequest): ResponseEntity<ProblemDetail> =
        response(HttpStatus.NOT_FOUND, ApiErrorCode.INCIDENT_NOT_FOUND, error, request)

    @ExceptionHandler(InvalidIncidentException::class)
    fun invalidIncident(error: InvalidIncidentException, request: HttpServletRequest): ResponseEntity<ProblemDetail> =
        response(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_INCIDENT, error, request)

    @ExceptionHandler(PhotoNotFoundException::class)
    fun photoNotFound(error: PhotoNotFoundException, request: HttpServletRequest): ResponseEntity<ProblemDetail> =
        response(HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND, error, request)

    @ExceptionHandler(Exception::class)
    fun unexpected(error: Exception, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        log.error("Unexpected API error at {}", request.requestURI, error)
        val code = ApiErrorCode.INTERNAL_ERROR
        return ResponseEntity.internalServerError().body(problem(HttpStatus.INTERNAL_SERVER_ERROR, code, code.defaultDetail, request.requestURI))
    }

    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val code = when (ex) {
            is MaxUploadSizeExceededException -> ApiErrorCode.PAYLOAD_TOO_LARGE
            is NoResourceFoundException -> ApiErrorCode.RESOURCE_NOT_FOUND
            else -> if (statusCode.is4xxClientError) ApiErrorCode.INVALID_REQUEST else ApiErrorCode.INTERNAL_ERROR
        }
        if (statusCode.is5xxServerError) log.error("Spring MVC error", ex)
        val problem = problem(statusCode, code, code.defaultDetail, request.getDescription(false).removePrefix("uri="))
        return super.handleExceptionInternal(ex, problem, headers, statusCode, request)
    }

    /** Builds a response whose detail is the domain exception's own message. */
    private fun response(
        status: HttpStatus,
        code: ApiErrorCode,
        error: RuntimeException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        ResponseEntity.status(status).body(problem(status, code, error.message ?: code.defaultDetail, request.requestURI))

    private fun problem(status: HttpStatusCode, code: ApiErrorCode, detail: String, path: String): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            setProperty("code", code.name)
            instance = URI.create(path)
        }
}
