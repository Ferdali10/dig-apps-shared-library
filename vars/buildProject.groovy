def call(String buildTool, Map config = [:]) {
    stage('🏗 Build Spring Boot + MySQL') {
        script {
            // Vérification des fichiers de config
            def dbConfigFile = 'src/main/resources/application.properties'
            if (!fileExists(dbConfigFile)) {
                error "Fichier de configuration DB introuvable: ${dbConfigFile}"
            }

            switch(buildTool.toLowerCase()) {
                case 'maven':
                    // Construction de la commande Maven
                    def mvnCmd = "./mvnw clean package"
                    
                    // Gestion des environnements (dev/test/prod)
                    if (config.activeProfile) {
                        mvnCmd += " -Dspring.profiles.active=${config.activeProfile}"
                    } else {
                        echo "[INFO] Utilisation de la configuration par défaut (sans profil spécifique)"
                    }
                    
                    // Gestion des tests
                    if (config.skipDbTests) {
                        mvnCmd += " -Dtest=!*RepositoryTest,*ServiceTest" // Exclut les tests DB
                    } else if (config.skipAllTests) {
                        mvnCmd += " -DskipTests"
                    }
                    
                    // Exécution
                    sh mvnCmd
                    
                    // Archive sélective
                    archiveArtifacts artifacts: 'target/*.jar,target/libs/*.jar', fingerprint: true
                    
                    // Vérification du JAR
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
