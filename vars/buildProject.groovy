def call(String buildTool, Map config = [:]) {
    stage('🏗 Build Spring Boot + MySQL') {
        script {
            // Vérification du fichier de config DB
            def dbConfigFile = 'src/main/resources/application.properties'
            if (!fileExists(dbConfigFile)) {
                error "Fichier de configuration DB introuvable: ${dbConfigFile}"
            }

            switch(buildTool.toLowerCase()) {
                case 'maven':
                    // Construction de la commande Maven
                    def mvnCmd = "./mvnw clean package"

                    // Ajout d'arguments personnalisés (ex: -Pprod, profils, etc)
                    if (config.args) {
                        mvnCmd += " ${config.args}"
                    } else if (config.activeProfile) {
                        mvnCmd += " -Dspring.profiles.active=${config.activeProfile}"
                    } else {
                        echo "[INFO] Utilisation de la configuration par défaut (sans profil spécifique)"
                    }

                    // Gestion des tests
                    if (config.skipDbTests) {
                        mvnCmd += " -Dtest=!*RepositoryTest,*ServiceTest" // Exclut certains tests
                    } else if (config.skipAllTests) {
                        mvnCmd += " -DskipTests"
                    }

                    // S'assurer que ./mvnw est exécutable
                    sh 'chmod +x ./mvnw'

                    // Exécution de la commande
                    sh mvnCmd

                    // Archive des fichiers jar générés
                    archiveArtifacts artifacts: 'target/*.jar,target/libs/*.jar', fingerprint: true

                    // Vérification qu'un JAR a bien été généré
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


