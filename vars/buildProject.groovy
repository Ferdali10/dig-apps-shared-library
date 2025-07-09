def call(Map config = [:]) {
    // Extrait le buildTool
    def buildTool = config.buildTool
    if (!buildTool) {
        error "Paramètre 'buildTool' obligatoire"
    }

    stage('🏗 Build Spring Boot + MySQL') {
        script {
            // vérifications
            switch(buildTool.toLowerCase()) {
                case 'maven':
                    def mvnCmd = "./mvnw clean package"

                    if (config.args) {
                        mvnCmd += " ${config.args}"
                    } else if (config.activeProfile) {
                        mvnCmd += " -Dspring.profiles.active=${config.activeProfile}"
                    } else {
                        echo "[INFO] Utilisation de la configuration par défaut (sans profil spécifique)"
                    }

                    if (config.skipDbTests) {
                        mvnCmd += " -Dtest=!*RepositoryTest,*ServiceTest"
                    } else if (config.skipAllTests) {
                        mvnCmd += " -DskipTests"
                    }

                    // Assure les droits d'exécution sur mvnw
                    sh 'chmod +x ./mvnw'

                    // Lancement de la commande Maven
                    sh mvnCmd

                    // Archivage des jars générés
                    archiveArtifacts artifacts: 'target/*.jar,target/libs/*.jar', fingerprint: true

                    // Vérification du JAR généré
                    if (!fileExists('target/*.jar')) {
                        error "Erreur : Aucun JAR généré. Vérifiez les logs Maven."
                    }
                    break

                case 'npm':
                    sh "npm install && npm run build ${config.args ?: ''}"
                    archiveArtifacts artifacts: 'dist/**/*', fingerprint: true
                    break

                default:
                    error "Outil de build non supporté : ${buildTool}"
            }
        }
    }
}



