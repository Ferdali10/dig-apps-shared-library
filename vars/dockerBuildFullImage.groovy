def call(Map config = [:]) {
    stage('🐳 Build/Push Docker') {
        script {
            // Validation
            if (!config.imageName) error "Le paramètre 'imageName' est obligatoire"
            
            // Configuration par défaut
            def dockerfile = config.dockerfilePath ?: 'Dockerfile'
            def registry = config.registry ?: 'https://registry.hub.docker.com'
            def tags = config.tags ?: ["latest", "${env.BUILD_NUMBER}"]
            
            // Build multi-architecture (si activé)
            if (config.multiArch) {
                sh """
                docker buildx build \
                    --platform linux/amd64,linux/arm64 \
                    -t ${config.imageName} \
                    -f ${dockerfile} \
                    . ${config.buildArgs ?: ''} \
                    --push
                """
            } else {
                // Build standard avec cache
                docker.build(config.imageName, "-f ${dockerfile} ${config.buildArgs ?: ''} .")
            }
            
            // Push avec gestion des tags
            docker.withRegistry(registry, config.credentialsId ?: 'docker-hub-creds') {
                tags.each { tag ->
                    docker.image(config.imageName).push(tag)
                    echo "[SUCCESS] Image poussée : ${config.imageName}:${tag}"
                }
            }
            
            // Stockage des métadonnées
            currentBuild.description = "Image: ${config.imageName}:${tags.join(', ')}"
        }
    }
}
