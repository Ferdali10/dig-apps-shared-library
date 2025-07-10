def call(Map config = [:]) {
    if (!config.repoUrl && !config.containsKey('branch')) {
        error "Paramètres manquants : repoUrl ou branch doivent être spécifiés"
    }
    
    if (config.repoUrl?.contains('github.com') && !config.credentialsId) {
        echo "[WARNING] Aucun credential spécifié pour un dépôt GitHub - risque d'échec si privé"
    }
    
    // Nouveau paramètre pour spécifier le répertoire de destination
    def targetDir = config.directory ?: '.'
    
    echo """[DEBUG] Configuration du clone :
    - URL: ${config.repoUrl ?: 'Défaut (projectSpring)'}
    - Branche: ${config.branch ?: 'master'}
    - Credentials: ${config.credentialsId ?: 'Aucun'}
    - Répertoire: ${targetDir}
    - Shallow Clone: ${config.shallowClone != false}"""
    
    stage('🔁 Clone du dépôt') {
        retry(3) {
            timeout(time: config.timeout ?: 10, unit: 'MINUTES') {
                script {
                    try {
                        // Si un répertoire spécifique est demandé, on utilise dir()
                        if (targetDir != '.') {
                            dir(targetDir) {
                                checkout([
                                    $class: 'GitSCM',
                                    branches: [[name: "*/${config.branch ?: 'master'}"]],
                                    extensions: [
                                        [$class: 'CloneOption',
                                         depth: config.shallowClone == false ? 0 : (config.depth ?: 1),
                                         shallow: config.shallowClone != false,
                                         timeout: config.timeout ?: 10
                                        ],
                                        [$class: 'CleanBeforeCheckout'],
                                        [$class: 'CleanCheckout'],
                                        [$class: 'SubmoduleOption',
                                         disableSubmodules: false,
                                         parentCredentials: true
                                        ]
                                    ],
                                    userRemoteConfigs: [
                                        [
                                            url: config.repoUrl ?: "https://github.com/Ferdali10/projectSpring.git",
                                            credentialsId: config.credentialsId ?: '',
                                            refspec: '+refs/heads/*:refs/remotes/origin/*'
                                        ]
                                    ]
                                ])
                            }
                        } else {
                            // Clone dans le workspace courant
                            checkout([
                                $class: 'GitSCM',
                                branches: [[name: "*/${config.branch ?: 'master'}"]],
                                extensions: [
                                    [$class: 'CloneOption',
                                     depth: config.shallowClone == false ? 0 : (config.depth ?: 1),
                                     shallow: config.shallowClone != false,
                                     timeout: config.timeout ?: 10
                                    ],
                                    [$class: 'CleanBeforeCheckout'],
                                    [$class: 'CleanCheckout'],
                                    [$class: 'SubmoduleOption',
                                     disableSubmodules: false,
                                     parentCredentials: true
                                    ]
                                ],
                                userRemoteConfigs: [
                                    [
                                        url: config.repoUrl ?: "https://github.com/Ferdali10/projectSpring.git",
                                        credentialsId: config.credentialsId ?: '',
                                        refspec: '+refs/heads/*:refs/remotes/origin/*'
                                    ]
                                ]
                            ])
                        }
                    } catch (Exception e) {
                        error "Échec du clone après 3 tentatives : ${e.message}\n" +
                              "Vérifiez :\n" +
                              "1. L'URL et les droits d'accès\n" +
                              "2. L'existence de la branche '${config.branch ?: 'master'}'\n" +
                              "3. La configuration des credentials dans Jenkins"
                    }
                }
            }
        }
    }
}

