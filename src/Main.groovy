import groovy.json.JsonSlurper

class Main {
    def private static jsonSlurper = new JsonSlurper()
    def private static params = [
            tenantId      : '<enter_tenant_id>',
            subscriptionId: '<enter_subscription_id>',
            clientId      : '<app_service_principal_id>',
            clientSecret  : '<app_service_principal_secret>',
            loginUrl      : 'https://login.microsoftonline.com/',
            resource      : 'https://management.azure.com/'
    ]

    def private static acrName = '<azure_container_registry_name>'
    def private static acrPassword = '<password for the name above>'

    static void main(String[] args) {
        String tokenType, accessToken
        (tokenType, accessToken) = authenticate()
        listResourceGroups(tokenType, accessToken)
        listACRs(tokenType, accessToken)
        def repositories = listACRRepositories()
        def tags = new ArrayList<String>()
        for (repositoryName in repositories) {
            tags.addAll(listACRRepositoryTags(repositoryName.toString()))
        }

        println('All tags')
        println(tags)
    }

    private static List<String> listACRRepositoryTags(String repositoryName) {
        def req = (HttpURLConnection) new URL("https://${acrName}.azurecr.io/acr/v1/${repositoryName}/_tags").openConnection()
        def basicCred = "${acrName}:${acrPassword}".bytes.encodeBase64().toString()
        req.setRequestProperty('Authorization', "Basic ${basicCred}")
        def result = jsonSlurper.parseText(req.inputStream.text)
        println(result.toString())
        return result.tags.collect { it.name }
    }

    private static List<String> listACRRepositories() {
        def req = (HttpURLConnection) new URL("https://${acrName}.azurecr.io/acr/v1/_catalog").openConnection()
        def basicCred = "${acrName}:${acrPassword}".bytes.encodeBase64().toString()
        req.setRequestProperty('Authorization', "Basic ${basicCred}")
        def result = jsonSlurper.parseText(req.inputStream.text)
        println(result.repositories.value)
        return result.repositories.value
    }

    /**
     * GET https://management.azure.com/subscriptions/{subscriptionId}/providers/Microsoft.ContainerRegistry/registries?api-version=2019-05-01
     */
    private static void listACRs(String tokenType, String accessToken) {
        def req = (HttpURLConnection) new URL("${params.resource}subscriptions/${params.subscriptionId}/providers/Microsoft.ContainerRegistry/registries?api-version=2019-05-01").openConnection()
        req.setRequestProperty('Authorization', "${tokenType} ${accessToken}")
        def result = jsonSlurper.parseText(req.inputStream.text)
        println(result.value.collect { it.name })
    }

    private static void listResourceGroups(String tokenType, String accessToken) {
        def req = (HttpURLConnection) new URL("${params.resource}subscriptions/${params.subscriptionId}/resourcegroups?api-version=2020-06-01").openConnection()
        req.setRequestProperty('Authorization', "${tokenType} ${accessToken}")
        def result = jsonSlurper.parseText(req.inputStream.text)
        println(result.value.collect { it.name })
    }

    private static List authenticate() {
        def reqBody = ['grant_type=client_credentials',
                       "client_id=${params.clientId}",
                       "client_secret=${params.clientSecret}",
                       "resource=${URLEncoder.encode(params.resource, 'UTF-8')}"].join('&')

        def req = (HttpURLConnection) new URL("${params.loginUrl}${params.tenantId}/oauth2/token").openConnection()
        req.setRequestProperty('Content-Type', 'application/x-www-form-urlencoded')
        req.setRequestProperty('Accept', 'application/json; charset=UTF-8')
        req.with {
            requestMethod = 'POST'
            doOutput = true
            outputStream.withWriter { writer ->
                writer << reqBody
            }
        }

        def result = jsonSlurper.parseText(req.inputStream.text)
        return [result.token_type, result.access_token]
    }
}
