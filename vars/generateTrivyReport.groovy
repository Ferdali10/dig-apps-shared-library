def call(Map config = [:]) {
    def imageName = config.imageName ?: error("❌ imageName manquant")

    def htmlTemplateUrl = config.templateUrl ?: 'https://raw.githubusercontent.com/Ferdali10/custom-trivy-template/main/html-advanced.tpl'
    def jsonOutput = config.jsonOutput ?: 'trivy-report.json'
    def htmlOutput = config.htmlOutput ?: 'trivy-report.html'

    echo "🔍 Analyse Trivy de l'image ${imageName}"

    sh """
        # Télécharger le template HTML personnalisé
        curl -sL ${htmlTemplateUrl} -o html-advanced.tpl

        # Mettre à jour la base Trivy
        trivy image --download-db-only

        # Générer le rapport JSON
        trivy image --severity HIGH,CRITICAL \
            --ignore-unfixed \
            --format json \
            -o ${jsonOutput} \
            ${imageName}

        # Générer le rapport HTML enrichi
        trivy image --severity HIGH,CRITICAL \
            --ignore-unfixed \
            --format template \
            --template '@html-advanced.tpl' \
            -o ${htmlOutput} \
            ${imageName}
    """

    archiveArtifacts artifacts: "${jsonOutput},${htmlOutput}", fingerprint: true

    publishHTML([
        allowMissing: false,
        keepAll: true,
        reportDir: '.',
        reportFiles: htmlOutput,
        reportName: '📊 Rapport Trivy Avancé',
        reportTitles: 'Sécurité de l’image Docker'
    ])
}
