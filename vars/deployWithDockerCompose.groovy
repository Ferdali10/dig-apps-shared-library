def call(Map config = [:]) {
    // Paramètre pour désactiver le wrapper stage
    def withStage = config.withStage != false
    
    // Validation des paramètres obligatoires
    if (!config.imageName) {
        error "❌ Le paramètre 'imageName' est obligatoire"
    }
    
    // Configuration par défaut
    def composeFile = config.composeFile ?: 'docker-compose.yml'
    def environment = config.environment ?: 'production'
    def serviceName = config.serviceName ?: 'springfoyer'
    def networkName = config.networkName ?: 'backend'
    def healthCheck = config.healthCheck != false
    def timeout = config.timeout ?: 300 // 5 minutes par défaut
    
    def deployLogic = {
        echo "🚀 Déploiement avec Docker Compose"
        echo "📋 Configuration:"
        echo "   - Image: ${config.imageName}"
        echo "   - Compose file: ${composeFile}"
        echo "   - Service: ${serviceName}"
        echo "   - Environment: ${environment}"
        echo "   - Network: ${networkName}"
        
        // Créer le fichier docker-compose.yml dynamiquement si nécessaire
        if (config.generateComposeFile) {
            def dbPassword = config.dbPassword ?: '123'
            def dbName = config.dbName ?: 'springfoyer'
            def appPort = config.appPort ?: '8080'
            def dbPort = config.dbPort ?: '3306'
            
            def composeContent = """
version: '3.8'
services:
  mysql:
    image: mysql:8
    container_name: mysql-db-${environment}
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${dbPassword}
      MYSQL_DATABASE: ${dbName}
    ports:
      - "${dbPort}:3306"
    networks:
      - ${networkName}
    volumes:
      - mysql_data_${environment}:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      timeout: 20s
      retries: 10

  ${serviceName}:
    image: ${config.imageName}
    container_name: ${serviceName}-${environment}
    restart: always
    depends_on:
      mysql:
        condition: service_healthy
    ports:
      - "${appPort}:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql-db-${environment}:3306/${dbName}
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${dbPassword}
      SPRING_PROFILES_ACTIVE: ${environment}
    networks:
      - ${networkName}
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 40s

networks:
  ${networkName}:
    driver: bridge

volumes:
  mysql_data_${environment}:
""".trim()
            
            writeFile file: composeFile, text: composeContent
            echo "✅ Fichier ${composeFile} généré"
        }
        
        // Arrêter les services existants si demandé
        if (config.stopExisting != false) {
            sh """
                echo "🛑 Arrêt des services existants..."
                docker-compose -f ${composeFile} down --remove-orphans || true
            """
        }
        
        // Nettoyer les images obsolètes si demandé
        if (config.cleanup) {
            sh """
                echo "🧹 Nettoyage des images obsolètes..."
                docker image prune -f || true
                docker volume prune -f || true
            """
        }
        
        // Pull de la dernière image
        if (config.pullLatest != false) {
            sh """
                echo "📥 Pull de la dernière image..."
                docker pull ${config.imageName} || echo "Warning: Could not pull image"
            """
        }
        
        // Déploiement avec Docker Compose
        sh """
            echo "🚀 Démarrage des services..."
            docker-compose -f ${composeFile} up -d
        """
        
        // Health check si activé
        if (healthCheck) {
            timeout(time: timeout, unit: 'SECONDS') {
                script {
                    echo "🔍 Vérification de la santé des services..."
                    
                    // Attendre que les services soient en cours d'exécution
                    sh """
                        echo "⏳ Attente du démarrage des conteneurs..."
                        sleep 30
                    """
                    
                    // Vérifier le statut des services
                    def maxRetries = config.maxRetries ?: 10
                    def retryInterval = config.retryInterval ?: 15
                    
                    for (int i = 1; i <= maxRetries; i++) {
                        try {
                            echo "🔄 Tentative ${i}/${maxRetries} - Vérification des services..."
                            
                            def mysqlStatus = sh(
                                script: "docker-compose -f ${composeFile} ps mysql | grep -E '(healthy|Up)'",
                                returnStatus: true
                            )
                            
                            def appStatus = sh(
                                script: "docker-compose -f ${composeFile} ps ${serviceName} | grep -E '(healthy|Up)'",
                                returnStatus: true
                            )
                            
                            if (mysqlStatus == 0 && appStatus == 0) {
                                echo "✅ Tous les services sont opérationnels!"
                                
                                // Test de connectivité applicative
                                if (config.appHealthUrl) {
                                    sh """
                                        echo "🌐 Test de connectivité sur ${config.appHealthUrl}..."
                                        curl -f ${config.appHealthUrl} || exit 1
                                    """
                                }
                                
                                // Afficher les logs récents si demandé
                                if (config.showLogs) {
                                    sh """
                                        echo "📋 Logs récents des services:"
                                        docker-compose -f ${composeFile} logs --tail=20
                                    """
                                }
                                
                                currentBuild.description = "✅ Déployé: ${config.imageName} (${environment})"
                                return
                            }
                            
                        } catch (Exception e) {
                            echo "❌ Échec de la vérification: ${e.message}"
                        }
                        
                        if (i < maxRetries) {
                            echo "⏳ Attente de ${retryInterval}s avant nouvelle tentative..."
                            sleep retryInterval
                        }
                    }
                    
                    // Si on arrive ici, le déploiement a échoué
                    sh """
                        echo "❌ Échec du health check - Affichage des logs:"
                        docker-compose -f ${composeFile} logs --tail=50
                        echo "📊 État des conteneurs:"
                        docker-compose -f ${composeFile} ps
                    """
                    
                    error "❌ Le déploiement a échoué - Les services ne sont pas opérationnels après ${maxRetries} tentatives"
                }
            }
        }
        
        // Afficher les informations de déploiement
        sh """
            echo "📊 État final des services:"
            docker-compose -f ${composeFile} ps
            echo ""
            echo "🌐 Services accessibles:"
            echo "   - Application: http://localhost:${config.appPort ?: '8080'}"
            echo "   - MySQL: localhost:${config.dbPort ?: '3306'}"
        """
        
        // Archiver le fichier compose pour référence
        if (config.archiveCompose != false) {
            archiveArtifacts artifacts: composeFile, fingerprint: true, allowEmptyArchive: true
        }
    }
    
    // Exécuter avec ou sans stage selon le paramètre
    if (withStage) {
        stage('🚀 Déploiement Docker Compose') {
            script {
                deployLogic()
            }
        }
    } else {
        deployLogic()
    }
}
