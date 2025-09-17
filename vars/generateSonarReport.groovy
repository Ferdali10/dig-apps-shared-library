def call(Map config = [:]) {
    def projectKey = config.projectKey ?: error("Project key is required")
    def sonarHost = config.sonarHost ?: error("Sonar host URL is required")
    def sonarToken = config.sonarToken ?: error("Sonar token is required")
    def qualityGate = config.qualityGate ?: [status: 'UNKNOWN']
    def projectUrl = config.projectUrl ?: "${sonarHost}/projects"

    echo "📊 Génération du rapport SonarQube pour le projet: ${projectKey}"
    echo "🔗 URLs de debug:"
    echo "   - sonarHost: ${sonarHost}"
    echo "   - projectUrl: ${projectUrl}"
    echo "   - Dashboard URL: http://4.210.176.144:9000/dashboard?id=${projectKey}"

    // Récupération des données SonarQube
    def metrics = getSonarMetrics(sonarHost, projectKey, sonarToken)
    def issues = getSonarIssues(sonarHost, projectKey, sonarToken)
    def qualityGateDetails = getQualityGateStatus(sonarHost, projectKey, sonarToken)

    // Génération du rapport HTML avec des liens absolus et debug
    def htmlContent = """
    <!DOCTYPE html>
    <html lang="fr">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Rapport SonarQube - ${projectKey}</title>
        <style>
            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                margin: 0;
                padding: 20px;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                min-height: 100vh;
            }
            .container {
                max-width: 1200px;
                margin: 0 auto;
                background: white;
                border-radius: 15px;
                box-shadow: 0 20px 40px rgba(0,0,0,0.1);
                overflow: hidden;
            }
            .header {
                background: linear-gradient(135deg, #2c3e50, #3498db);
                color: white;
                padding: 30px;
                text-align: center;
            }
            .header h1 {
                margin: 0;
                font-size: 2.5rem;
                font-weight: 300;
            }
            .header .subtitle {
                margin-top: 10px;
                opacity: 0.9;
                font-size: 1.1rem;
            }
            .content {
                padding: 30px;
            }
            .quality-gate {
                text-align: center;
                margin-bottom: 40px;
                padding: 30px;
                border-radius: 10px;
                box-shadow: 0 5px 15px rgba(0,0,0,0.08);
            }
            .quality-gate.passed {
                background: linear-gradient(135deg, #4CAF50, #45a049);
                color: white;
            }
            .quality-gate.failed {
                background: linear-gradient(135deg, #f44336, #e53935);
                color: white;
            }
            .quality-gate.unknown {
                background: linear-gradient(135deg, #ff9800, #f57c00);
                color: white;
            }
            .quality-gate h2 {
                margin: 0;
                font-size: 2rem;
            }
            .metrics-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                gap: 20px;
                margin-bottom: 40px;
            }
            .metric-card {
                background: white;
                border-radius: 10px;
                padding: 25px;
                text-align: center;
                box-shadow: 0 5px 15px rgba(0,0,0,0.08);
                border-left: 5px solid #3498db;
                transition: transform 0.3s ease;
            }
            .metric-card:hover {
                transform: translateY(-5px);
            }
            .metric-value {
                font-size: 2.5rem;
                font-weight: bold;
                color: #2c3e50;
                margin-bottom: 10px;
            }
            .metric-label {
                color: #7f8c8d;
                font-size: 1.1rem;
                text-transform: uppercase;
                letter-spacing: 1px;
            }
            .issues-section {
                background: #f8f9fa;
                padding: 30px;
                border-radius: 10px;
                margin-bottom: 30px;
            }
            .issues-section h3 {
                color: #2c3e50;
                margin-bottom: 20px;
                font-size: 1.5rem;
            }
            .issue {
                background: white;
                padding: 15px;
                margin-bottom: 10px;
                border-radius: 8px;
                border-left: 4px solid #e74c3c;
                box-shadow: 0 2px 5px rgba(0,0,0,0.05);
            }
            .issue.MAJOR {
                border-left-color: #e67e22;
            }
            .issue.MINOR {
                border-left-color: #f39c12;
            }
            .issue.INFO {
                border-left-color: #3498db;
            }
            .links-section {
                text-align: center;
                padding: 30px;
                background: #f8f9fa;
                margin-top: 30px;
            }
            .btn {
                display: inline-block;
                padding: 15px 30px;
                margin: 10px;
                background: linear-gradient(135deg, #3498db, #2980b9);
                color: white !important;
                text-decoration: none !important;
                border-radius: 25px;
                font-weight: 500;
                transition: all 0.3s ease;
                box-shadow: 0 5px 15px rgba(52, 152, 219, 0.3);
                cursor: pointer;
            }
            .btn:hover {
                transform: translateY(-2px);
                box-shadow: 0 8px 25px rgba(52, 152, 219, 0.4);
                color: white !important;
                text-decoration: none !important;
            }
            .btn:visited {
                color: white !important;
            }
            .debug-info {
                background: #e8f4fd;
                padding: 20px;
                border-radius: 10px;
                margin-bottom: 20px;
                font-family: monospace;
                font-size: 14px;
            }
            .debug-info h4 {
                color: #2c3e50;
                margin-top: 0;
            }
            .timestamp {
                text-align: center;
                color: #7f8c8d;
                margin-top: 20px;
                font-style: italic;
            }
        </style>
        <script>
            function openSonarDashboard() {
                window.open('http://172.201.153.226:9000/dashboard?id=${projectKey}', '_blank');
                return false;
            }
            
            function openSonarProjects() {
                window.open('http://172.201.153.226:9000/projects', '_blank');
                return false;
            }
            
            // Debug : log des URLs au chargement de la page
            console.log('Dashboard URL:', 'http://172.201.153.226:9000/dashboard?id=${projectKey}');
            console.log('Projects URL:', 'http://172.201.153.226:9000/projects');
        </script>
    </head>
    <body>
        ${generateReportContent(projectKey, sonarHost, projectUrl, qualityGate, qualityGateDetails, metrics, issues)}
    </body>
    </html>
    """

    // Écriture et publication du rapport
    def reportFileName = "sonar-report-${projectKey}.html"
    writeFile file: reportFileName, text: htmlContent
    
    // Vérification du contenu du fichier généré
    def fileContent = readFile file: reportFileName
    echo "📝 Taille du fichier généré: ${fileContent.length()} caractères"
    echo "🔍 Les URLs dans le fichier contiennent-elles '172.201.153.226'? ${fileContent.contains('172.201.153.226')}"
    
    publishHTML([
        allowMissing: false,
        alwaysLinkToLastBuild: true,
        keepAll: true,
        reportDir: '.',
        reportFiles: reportFileName,
        reportName: "📊 Rapport SonarQube - ${projectKey}",
        reportTitles: "Rapport de Qualité du Code"
    ])
    
    echo "✅ Rapport SonarQube généré: ${reportFileName}"
}

// Fonctions privées
private def getSonarMetrics(sonarHost, projectKey, sonarToken) {
    try {
        def response = sh(
            script: """curl -s -u ${sonarToken}: '${sonarHost}/api/measures/component?component=${projectKey}&metricKeys=bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density,ncloc,sqale_rating,reliability_rating,security_rating'""",
            returnStdout: true
        ).trim()
        
        def jsonData = readJSON text: response
        def metricsMap = [:]
        
        jsonData.component?.measures?.each { measure ->
            metricsMap[measure.metric] = measure.value
        }
        
        return metricsMap
    } catch(Exception e) {
        echo "⚠️ Erreur récupération métriques: ${e.getMessage()}"
        return [:]
    }
}

private def getSonarIssues(sonarHost, projectKey, sonarToken) {
    try {
        def response = sh(
            script: """curl -s -u ${sonarToken}: '${sonarHost}/api/issues/search?componentKeys=${projectKey}&statuses=OPEN&ps=50'""",
            returnStdout: true
        ).trim()
        
        def jsonData = readJSON text: response
        return jsonData.issues ?: []
    } catch(Exception e) {
        echo "⚠️ Erreur récupération issues: ${e.getMessage()}"
        return []
    }
}

private def getQualityGateStatus(sonarHost, projectKey, sonarToken) {
    try {
        def response = sh(
            script: """curl -s -u ${sonarToken}: '${sonarHost}/api/qualitygates/project_status?projectKey=${projectKey}'""",
            returnStdout: true
        ).trim()
        
        def jsonData = readJSON text: response
        return jsonData.projectStatus ?: [:]
    } catch(Exception e) {
        echo "⚠️ Erreur récupération Quality Gate: ${e.getMessage()}"
        return [:]
    }
}

private def generateReportContent(projectKey, sonarHost, projectUrl, qualityGate, qualityGateDetails, metrics, issues) {
    def currentDate = new Date().format("dd/MM/yyyy HH:mm:ss")
    def qualityGateClass = qualityGate.status == 'OK' ? 'passed' : (qualityGate.status == 'ERROR' ? 'failed' : 'unknown')
    def qualityGateText = qualityGate.status == 'OK' ? '✅ PASSED' : (qualityGate.status == 'ERROR' ? '❌ FAILED' : '⚠️ UNKNOWN')
    
    return """
        <div class="container">
            <div class="header">
                <h1>🔍 Rapport SonarQube</h1>
                <div class="subtitle">Projet: ${projectKey}</div>
            </div>
            
            <div class="content">
                <div class="debug-info">
                    <h4>🛠️ Informations de Debug</h4>
                    <p><strong>Dashboard URL:</strong> http://172.201.153.226:9000/dashboard?id=${projectKey}</p>
                    <p><strong>Projects URL:</strong> http://172.201.153.226:9000/projects</p>
                    <p><strong>Projet:</strong> ${projectKey}</p>
                    <p><strong>Timestamp:</strong> ${currentDate}</p>
                </div>
                
                <div class="quality-gate ${qualityGateClass}">
                    <h2>Quality Gate: ${qualityGateText}</h2>
                    <p>Statut de la qualité du code</p>
                </div>
                
                <div class="metrics-grid">
                    <div class="metric-card">
                        <div class="metric-value">${metrics.bugs ?: '0'}</div>
                        <div class="metric-label">🐛 Bugs</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">${metrics.vulnerabilities ?: '0'}</div>
                        <div class="metric-label">🔒 Vulnérabilités</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">${metrics.code_smells ?: '0'}</div>
                        <div class="metric-label">💨 Code Smells</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">${metrics.coverage ? metrics.coverage + '%' : 'N/A'}</div>
                        <div class="metric-label">📊 Couverture</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">${metrics.ncloc ?: '0'}</div>
                        <div class="metric-label">📄 Lignes de Code</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">${metrics.duplicated_lines_density ? metrics.duplicated_lines_density + '%' : 'N/A'}</div>
                        <div class="metric-label">📋 Duplication</div>
                    </div>
                </div>
                
                ${issues.size() > 0 ? """
                <div class="issues-section">
                    <h3>🚨 Issues Ouvertes (${issues.size()})</h3>
                    ${issues.take(10).collect { issue -> """
                        <div class="issue ${issue.severity}">
                            <strong>[${issue.severity}]</strong> ${issue.message} 
                            <small>(${issue.component}:${issue.line ?: 'N/A'})</small>
                        </div>
                    """ }.join('')}
                    ${issues.size() > 10 ? "<p><em>... et ${issues.size() - 10} autres issues</em></p>" : ""}
                </div>
                """ : """
                <div class="issues-section">
                    <h3>✅ Aucune Issue Ouverte</h3>
                    <p>Félicitations ! Aucun problème de qualité détecté.</p>
                </div>
                """}
                
                <div class="links-section">
                    <h3>🔗 Liens Utiles</h3>
                    
                    <!-- Méthode 1: Lien standard -->
                    <a href="http://172.201.153.226:9000/dashboard?id=${projectKey}" class="btn" target="_blank" rel="noopener noreferrer">
                        📊 Dashboard du Projet (Lien direct)
                    </a>
                    
                    <!-- Méthode 2: Bouton avec JavaScript -->
                    <button type="button" class="btn" onclick="openSonarDashboard()">
                        📊 Dashboard du Projet (JavaScript)
                    </button>
                    
                    <!-- Méthode 3: Lien vers les projets -->
                    <a href="http://172.201.153.226:9000/projects" class="btn" target="_blank" rel="noopener noreferrer">
                        📁 Liste des Projets
                    </a>
                    
                    <!-- Méthode 4: Bouton avec JavaScript pour projets -->
                    <button type="button" class="btn" onclick="openSonarProjects()">
                        📁 Liste des Projets (JavaScript)
                    </button>
                    
                    <!-- URLs copiables -->
                    <div style="margin-top: 20px; padding: 15px; background: white; border-radius: 8px; text-align: left;">
                        <h4>📋 URLs à copier-coller :</h4>
                        <p><strong>Dashboard:</strong> <code style="background: #f4f4f4; padding: 2px 4px;">http://172.201.153.226:9000/dashboard?id=${projectKey}</code></p>
                        <p><strong>Projets:</strong> <code style="background: #f4f4f4; padding: 2px 4px;">http://172.201.153.226:9000/projects</code></p>
                    </div>
                </div>
                
                <div class="timestamp">
                    📅 Rapport généré le ${currentDate}
                </div>
            </div>
        </div>
    """
}
