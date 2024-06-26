package co.statu.rule.auth

import co.statu.parsek.model.Result
import co.statu.parsek.model.Result.Companion.encode
import co.statu.parsek.util.TextUtil.convertToSnakeCase

class RegisterInputError(
    private val prefix: String,
    private val suffix: String,
    private val field: String,
    private val statusCode: Int = 422,
    private val statusMessage: String = "",
    private val extras: Map<String, Any?> = mapOf()
) : Throwable(), Result {

    override fun encode(extras: Map<String, Any?>): String {
        val response = mutableMapOf<String, Any?>(
            "result" to "error",
            "error" to getErrorCode()
        )

        response.putAll(this.extras)
        response.putAll(extras)

        return response.encode()
    }

    override fun getStatusCode(): Int = statusCode

    override fun getStatusMessage(): String = statusMessage

    fun getErrorCode() = "$prefix${field.replaceFirstChar(Char::uppercase)}$suffix".convertToSnakeCase().uppercase()
}