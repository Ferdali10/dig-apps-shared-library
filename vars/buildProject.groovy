def call(Map config = [:]) {
    def buildTool = config.buildTool ?: 'maven'

    stage('🏗 Build Spring Boot + MySQL') {
        script {
            def dbConfigFile = 'src/main/resources/application.properties'
            if (!fileExists(dbConfigFile)) {
                error "Fichier de configuration DB introuvable: ${dbConfigFile}"
            }

            switch(buildTool.toLowerCase()) {
                case 'maven':
                    def mvnCmd = "./mvnw clean package"

                    if (config.activeProfile) {
                        mvnCmd += " -Dspring.profiles.active=${config.activeProfile}"
                    } else {
                        echo "[INFO] Utilisation de la configuration par défaut (sans profil spécifique)"
                    }

                    if (config.skipDbTests) {
                        mvnCmd += " -Dtest=!*RepositoryTest,*ServiceTest"
                    } else if (config.skipAllTests) {
                        mvnCmd += " -DskipTests"
                    }

                    if (config.args) {
                        mvnCmd += " ${config.args}"
                    }

                    sh mvnCmd

                    archiveArtifacts artifacts: 'target/*.jar,target/libs/*.jar', fingerprint: true

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

