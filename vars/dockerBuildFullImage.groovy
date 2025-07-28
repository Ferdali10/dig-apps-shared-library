def call(Map config = [:]) {
    // Paramètre pour désactiver le wrapper stage
    def withStage = config.withStage != false
    
    if (!config.imageName) error "Le paramètre 'imageName' est obligatoire"
    
    def dockerfile = config.dockerfilePath ?: 'Dockerfile'
    def registry = config.registry ?: 'https://registry.hub.docker.com'
    def tags = config.tags ?: ["latest", "${env.BUILD_NUMBER}"]
    
    def dockerLogic = {
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
            docker.build(config.imageName, "-f ${dockerfile} ${config.buildArgs ?: ''} .")
        }
        
        docker.withRegistry(registry, config.credentialsId ?: 'docker-hub-creds') {
            tags.each { tag ->
                docker.image(config.imageName).push(tag)
                echo "[SUCCESS] Image poussée : ${config.imageName}:${tag}"
            }
        }
        
        currentBuild.description = "Image: ${config.imageName}:${tags.join(', ')}"
    }
    
    // Exécuter avec ou sans stage selon le paramètre
    if (withStage) {
        stage('🐳 Build/Push Docker') {
            script {
                dockerLogic()
            }
        }
    } else {
        dockerLogic()
    }
}


