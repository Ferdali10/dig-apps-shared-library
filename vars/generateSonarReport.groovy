def call(Map config = [:]) {
    def projectKey = config.projectKey ?: error("Project key is required")
    def sonarHost = config.sonarHost ?: error("Sonar host URL is required")
    def qualityGate = config.qualityGate ?: [status: 'UNKNOWN']

    // Récupération des données SonarQube
    def metrics = getSonarMetrics(sonarHost, projectKey)
    def issues = getSonarIssues(sonarHost, projectKey)

    // Génération du rapport HTML
    def htmlContent = """
    <!DOCTYPE html>
    <html>
    <head>
        <title>Rapport Qualité - ${projectKey}</title>
        <style>
            /* Styles CSS ici */
        </style>
    </head>
    <body>
        ${generateReportContent(projectKey, sonarHost, qualityGate, metrics, issues)}
    </body>
    </html>
    """

    // Écriture et publication du rapport
    writeFile file: "sonar-report-${projectKey}.html", text: htmlContent
    publishHTML([
        reportDir: '.',
        reportFiles: "sonar-report-${projectKey}.html",
        reportName: "Rapport SonarQube - ${projectKey}",
        keepAll: true
    ])
}

// Fonctions privées
private def getSonarMetrics(sonarHost, projectKey) {
    try {
        def response = httpRequest url: "${sonarHost}/api/measures/component?component=${projectKey}&metricKeys=bugs,vulnerabilities,code_smells,coverage",
                                  authentication: 'SonarQubeServer'
        return readJSON(text: response.content)
    } catch(e) {
        echo "Erreur récupération métriques: ${e.getMessage()}"
        return [:]
    }
}

private def getSonarIssues(sonarHost, projectKey) {
    try {
        def response = httpRequest url: "${sonarHost}/api/issues/search?componentKeys=${projectKey}&statuses=OPEN",
                                  authentication: 'SonarQubeServer'
        return readJSON(text: response.content).issues
    } catch(e) {
        echo "Erreur récupération issues: ${e.getMessage()}"
        return []
    }
}

private def generateReportContent(projectKey, sonarHost, qualityGate, metrics, issues) {
    // Implémentez la génération du contenu HTML ici
    return """
        <h1>Rapport Qualité</h1>
        <!-- Contenu dynamique -->
    """
}
