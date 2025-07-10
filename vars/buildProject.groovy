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
                    
                    // Archivage des jars générés - utilise une approche plus robuste
                    script {
                        def jarFiles = sh(
                            script: 'find target -name "*.jar" -type f',
                            returnStdout: true
                        ).trim()
                        
                        if (jarFiles) {
                            echo "JAR files found: ${jarFiles}"
                            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, allowEmptyArchive: true
                        } else {
                            echo "No JAR files found in target directory"
                        }
                    }
                    
                    // Vérification du JAR généré - amélioration du check
                    script {
                        def jarExists = sh(
                            script: 'ls -la target/*.jar 2>/dev/null || echo "NO_JAR_FOUND"',
                            returnStdout: true
                        ).trim()
                        
                        if (jarExists.contains("NO_JAR_FOUND")) {
                            // Liste le contenu du répertoire target pour debugging
                            sh 'echo "=== Contenu du répertoire target ==="'
                            sh 'ls -la target/ || echo "Répertoire target introuvable"'
                            error "Erreur : Aucun JAR généré. Vérifiez les logs Maven."
                        } else {
                            echo "JAR généré avec succès :"
                            echo jarExists
                        }
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



