package co.statu.rule.auth.route.profile

import co.statu.parsek.PluginEventManager
import co.statu.parsek.annotation.Endpoint
import co.statu.parsek.model.Path
import co.statu.parsek.model.Result
import co.statu.parsek.model.RouteType
import co.statu.parsek.model.Successful
import co.statu.rule.auth.AuthFieldManager
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.auth.api.LoggedInApi
import co.statu.rule.auth.event.AuthEventListener
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.database.DatabaseManager
import io.vertx.ext.web.RoutingContext
import io.vertx.json.schema.SchemaParser

@Endpoint
class GetProfileAPI(
    private val authPlugin: AuthPlugin
) : LoggedInApi() {

    private val databaseManager by lazy {
        authPlugin.pluginBeanContext.getBean(DatabaseManager::class.java)
    }

    private val authProvider by lazy {
        authPlugin.pluginBeanContext.getBean(AuthProvider::class.java)
    }

    private val authFieldManager by lazy {
        authPlugin.pluginBeanContext.getBean(AuthFieldManager::class.java)
    }

    override val paths = listOf(Path("/profile", RouteType.GET))

    override fun getValidationHandler(schemaParser: SchemaParser) = null

    override suspend fun handle(context: RoutingContext): Result {
        val userId = authProvider.getUserIdFromRoutingContext(context)

        val jdbcPool = databaseManager.getConnectionPool()

        val user = userDao.getById(userId, jdbcPool)!!

        val response = mutableMapOf<String, Any?>()

        response["email"] = user.email

        val registerFields = authFieldManager.getRegisterFields()

        user.additionalFields.forEach { additionalField ->
            val registerField = registerFields.find { it.field == additionalField.key }

            if (registerField != null && registerField.hiddenToUI == false) {
                response[additionalField.key] = additionalField.value
            }
        }

        val authEventHandlers = PluginEventManager.getEventListeners<AuthEventListener>()

        authEventHandlers.forEach { it.onGetProfile(user, response) }

        return Successful(response)
    }
}