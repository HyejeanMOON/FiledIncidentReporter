package com.example.incidentreporter.incident.api

import com.example.incidentreporter.incident.domain.IncidentNotFoundException
import com.example.incidentreporter.incident.domain.InvalidIncidentException
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

enum class ApiErrorCode {
    INCIDENT_NOT_FOUND,
    INVALID_INCIDENT,
    INVALID_REQUEST,
    PAYLOAD_TOO_LARGE,
    RESOURCE_NOT_FOUND,
    INTERNAL_ERROR,
}

class ResourceNotFoundException : RuntimeException("Resource not found")

@RestControllerAdvice
class IncidentExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = LoggerFactory.getLogger(IncidentExceptionHandler::class.java)

    @ExceptionHandler(IncidentNotFoundException::class)
    fun incidentNotFound(error: IncidentNotFoundException, request: HttpServletRequest): ResponseEntity<ProblemDetail> =
        response(HttpStatus.NOT_FOUND, ApiErrorCode.INCIDENT_NOT_FOUND, error.message ?: "Incident not found", request.requestURI)

    @ExceptionHandler(InvalidIncidentException::class)
    fun invalidIncident(error: InvalidIncidentException, request: HttpServletRequest): ResponseEntity<ProblemDetail> =
        response(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_INCIDENT, error.message ?: "Invalid incident", request.requestURI)

    @ExceptionHandler(ResourceNotFoundException::class)
    fun resourceNotFound(error: ResourceNotFoundException, request: HttpServletRequest): ResponseEntity<ProblemDetail> =
        response(HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND, error.message ?: "Resource not found", request.requestURI)

    @ExceptionHandler(Exception::class)
    fun unexpected(error: Exception, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        log.error("Unexpected API error at {}", request.requestURI, error)
        return response(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.INTERNAL_ERROR, "An unexpected server error occurred", request.requestURI)
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
        val detail = when (code) {
            ApiErrorCode.PAYLOAD_TOO_LARGE -> "The upload exceeds the allowed size"
            ApiErrorCode.RESOURCE_NOT_FOUND -> "Resource not found"
            ApiErrorCode.INTERNAL_ERROR -> "An unexpected server error occurred"
            else -> "The request is invalid"
        }
        val problem = problem(statusCode, code, detail, request.getDescription(false).removePrefix("uri="))
        return super.handleExceptionInternal(ex, problem, headers, statusCode, request)
    }

    private fun response(status: HttpStatus, code: ApiErrorCode, detail: String, path: String): ResponseEntity<ProblemDetail> =
        ResponseEntity.status(status).body(problem(status, code, detail, path))

    private fun problem(status: HttpStatusCode, code: ApiErrorCode, detail: String, path: String): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            setProperty("code", code.name)
            instance = URI.create(path)
        }
}
