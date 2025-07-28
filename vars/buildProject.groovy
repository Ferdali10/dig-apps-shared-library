def call(Map config = [:]) {
    // Paramètre pour désactiver le wrapper stage
    def withStage = config.withStage != false
    
    def buildTool = config.buildTool
    if (!buildTool) {
        error "Paramètre 'buildTool' obligatoire"
    }
    
    def buildLogic = {
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
                
                // Vérification du JAR généré
                script {
                    def jarExists = sh(
                        script: 'ls -la target/*.jar 2>/dev/null || echo "NO_JAR_FOUND"',
                        returnStdout: true
                    ).trim()
                    
                    if (jarExists.contains("NO_JAR_FOUND")) {
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
    
    // Exécuter avec ou sans stage selon le paramètre
    if (withStage) {
        stage('🏗 Build Spring Boot + MySQL') {
            script {
                buildLogic()
            }
        }
    } else {
        buildLogic()
    }
}



