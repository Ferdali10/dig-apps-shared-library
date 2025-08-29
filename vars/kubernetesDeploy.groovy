def call(Map params = [:]) {
    String kubeConfigPath = params.kubeConfigPath ?: '/var/lib/jenkins/.kube/config'
    String manifestDir = params.manifestDir ?: 'k8s'
    
    script {
        if (!fileExists(manifestDir)) {
            error "❌ Dossier des manifests Kubernetes introuvable: ${manifestDir}"
        }
        
        withEnv(["KUBECONFIG=${kubeConfigPath}"]) {
            sh """
                echo "📂 Déploiement depuis: ${manifestDir}"
                kubectl apply -f ${manifestDir}/
                kubectl rollout status deployment/springfoyer-deployment --timeout=120s
            """
        }
    }
}

