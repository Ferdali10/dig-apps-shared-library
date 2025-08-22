def call(String kubeConfigPath = '~/.kube/config', String manifestDir = 'k8s') {
    stage('🚀 Deploy to Kubernetes') {
        sh """
          export KUBECONFIG=${kubeConfigPath}
          kubectl apply -f ${manifestDir}/
          kubectl rollout status deployment/springfoyer-deployment
        """
    }
}
