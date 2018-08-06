pipeline{
    agent any
    stages{
        stage('Init'){
            steps{
                script{
                    deleteDir()
                    helmHost='https://storage.googleapis.com/'
                    helmPath='kubernetes-helm'
                    helmPack='helm-v2.10.0-rc.2-linux-amd64.tar.gz'
                    gcSert='LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURERENDQWZTZ0F3SUJBZ0lSQUxoRnJWOEl5UGROc25ESi9yMTQwTzR3RFFZSktvWklodmNOQVFFTEJRQXcKTHpFdE1Dc0dBMVVFQXhNa1lqZGhZbVEzTUdNdFpHSTFNQzAwTlRRd0xXRmpPR1l0TldVMk5UQXlaakptWkRSagpNQjRYRFRFNE1EZ3dOakF3TVRRek1Gb1hEVEl6TURnd05UQXhNVFF6TUZvd0x6RXRNQ3NHQTFVRUF4TWtZamRoClltUTNNR010WkdJMU1DMDBOVFF3TFdGak9HWXROV1UyTlRBeVpqSm1aRFJqTUlJQklqQU5CZ2txaGtpRzl3MEIKQVFFRkFBT0NBUThBTUlJQkNnS0NBUUVBck11QU1jVGZrd2VBU2JyOVZITm1yc3FkeWdCMU1GZTEycmIwS3J5RwpTemRnL1VlN29jZ3pFMmlEV2t0NWFkQ2xmcmFBbHhzdXRhb3hRSDhzbVpDNEZqTEpENjJwNTZXWXFodFRGaGZhClUwT2F2RXlMRmd0ejNBbUdKaXFUb3pUTzJ1V1htMm51RW5qS2EwNml5TjVMd3Y2d05pUi9sUGFDa0t4SnRkK3cKMzZCbHhrRmFWOWI0VXJkYzR4SHo3bkU5WGJkRDZiZlJmMTJyMDF4UmhET3dGSi9uYk1CMHJHZTJTNENmcFBKeQpBNlNnTFRjZVpPekNXbjlFQWpNQmF6bVZtVWpFYm0wbEpjTmhZZExYOVRrSHY4UGZ5eU9kU0FRUEhzazU4RW1pCmxlR3NxVGFJQklPeUN3ZXVNWVRVTUx3eTdxR3BJM1Z1Qk80c2g5ZXlsZmdLUXdJREFRQUJveU13SVRBT0JnTlYKSFE4QkFmOEVCQU1DQWdRd0R3WURWUjBUQVFIL0JBVXdBd0VCL3pBTkJna3Foa2lHOXcwQkFRc0ZBQU9DQVFFQQpXdmxTLzQ5WnQ3dm1VSlFNenZseTM5dm1ZQmZWZXpwZFVMbTNNS2xYeWxraTQ4cXVpS0w3anE0QUNYdSt5b3kvCnJEeTRsQnhDbjZEb010bEdqcGtJakFrd2ZMS2R5M0pHT3RGTXR5Z3V4cDlGVUFGWitITUNvR1RBQVQrYUt1Ny8KTmN6bS9QVHN2eW4yWWErODRWd2xFbTR2WitxL1ZaY29GUW9FZ1JNQmRNd3V4OWxmam5qMG1nVXQvVDd2dkNvYgpFWU1rTHVJUWZiV1RPMWlPS1o2QzVRTjRwYmJ4ditNK3h1dTJMakpZS3k3UVhrRlBQNnRpbkcrR1d0V1pZaEVKCkZKMFM2bjhRcTR2T3UxRmZNSVZyYUxQUld4NnhzMnVvRmM3ZHlReVZmM3FUMHlPRnJSWU5yTVRvQzlXalFKcm8KZU5zS2FzUWFDdnpudktSNDZ5bnYvdz09Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K'
                    gcUserPass='ibvXdPtPdnBQjGva'
                    gcServer='35.232.164.58'
                    artUser='admin'
                    artPass='password'
                    haExtIp=''
                }
            }
        }
        stage('Prep kube config'){
            steps{
                script{
                    sh('''
                        set +x
                        mkdir -p ~/.kube
                        cat <<EOF > ~/.kube/config
apiVersion: v1
clusters:
- cluster:
    certificate-authority-data: '''+gcSert+'''
    server: https://'''+gcServer+'''
  name: gke_nifty-field-211113_us-central1-a_cluster-1
contexts:
- context:
    cluster: gke_nifty-field-211113_us-central1-a_cluster-1
    user: gke_nifty-field-211113_us-central1-a_cluster-1
  name: gke_nifty-field-211113_us-central1-a_cluster-1
current-context: gke_nifty-field-211113_us-central1-a_cluster-1
kind: Config
preferences: {}
users:
- name: gke_nifty-field-211113_us-central1-a_cluster-1
  user:
    password: '''+gcUserPass+'''
    username: admin

current-context: "gke_nifty-field-211113_us-central1-a_cluster-1"
EOF
                        chmod 600 ~/.kube/config
                    ''')
                }
            }
        }
        stage('Check helm is setup'){
            steps{
                script{
                    sh('''
                        set +x
                        wget -q "'''+helmHost+helmPath+'''/'''+helmPack+'''"
                        tar -xzf '''+helmPack+'''
                    ''')
                }
            }
        }
        stage('Prepare cluster'){
            steps{
                script{
                    sh('''
                        set +x
                        cd linux-amd64
                        ./helm init
                        kubectl create serviceaccount --namespace kube-system tiller || echo -n
                        kubectl create clusterrolebinding tiller-cluster-rule --clusterrole=cluster-admin --serviceaccount=kube-system:tiller || echo -n
                        kubectl patch deploy --namespace kube-system tiller-deploy -p '{"spec":{"template":{"spec":{"serviceAccount":"tiller"}}}}' || echo -n
                        ./helm init --upgrade
                        sleep 10
                    ''')
                }
            }
        }
        stage('Deploy artifactory'){
            steps{
                script{
                    artDeployed=(sh(script:'./helm ls --all artifactory | grep artifactory | sed "s/.*\\(DEPLOYED\\).*/\\1/"',returnStdout: true).trim()=='DEPLOYED')
                    if (!artDeployed){
                        sh('''
                            set +x
                            cd linux-amd64
                            ./helm install --name artifactory \\
                           --set artifactory.image.repository=docker.bintray.io/jfrog/artifactory-oss \\
                           --set artifactory.resources.requests.cpu="500m" \\
                           --set artifactory.resources.limits.cpu="2" \\
                           --set artifactory.resources.requests.memory="1Gi" \\
                           --set artifactory.resources.limits.memory="2Gi" \\
                           --set artifactory.javaOpts.xms="1g" \\
                           --set artifactory.javaOpts.xmx="2g" \\
                           --set nginx.resources.requests.cpu="100m" \\
                           --set nginx.resources.limits.cpu="250m" \\
                           --set nginx.resources.requests.memory="250Mi" \\
                           --set nginx.resources.limits.memory="500Mi" \\
                           stable/artifactory
                        ''')
                    }
                }
            }
        }
        stage('Waiting for services are redy'){
            steps{
                script{
                    if (!artDeployed){
                        sh('''
                            set +x
                            echo "Waiting for services are redy"
                            ti=90
                            while [ $ti -gt 0 ]; do
                                sleep 10
                                ART_STATE=$(kubectl get po | grep -v nginx | grep artifactory-artifactory | sed "s/.*\\(Running\\).*/\\1/")
                                if [ "X${ART_STATE}" = "XRunning" ]; then ti=0; fi
                                ti=$(($ti-1))
                                if [ $ti -eq 0 ]; then 
                                    echo "Timeout starting Artifactory service in cluster"
                                    exit 1
                                fi
                            done
                            ti=90
                            while [ $ti -gt 0 ]; do
                                sleep 10
                                NGX_STATE=$(kubectl get po | grep nginx | sed "s/.*\\(Running\\).*/\\1/")
                                if [ "X${NGX_STATE}" = "XRunning" ]; then ti=0; fi
                                ti=$(($ti-1))
                                if [ $ti -eq 0 ]; then 
                                    echo "Timeout starting Artifactory-Nginx service in cluster"
                                    exit 1
                                fi
                            done
                        ''')
                    }
                    haExtIp=sh(script:'kubectl get svc --namespace default artifactory-artifactory-nginx -o jsonpath=\'{.status.loadBalancer.ingress[0].ip}\'',returnStdout: true).trim()
                    echo 'External IP for balancer: '+haExtIp
                }
            }
        }
        stage('Creating repositories'){
            steps{
                script{
                    if (!artDeployed){
                        sh('''
                            set +x
                            ART_AUTH=$(echo -n "'''+artUser+''':'''+artPass+'''" | base64)
                            curl -s --header "Content-Type: application/json" \\
                            --header "Authorization:Basic ${ART_AUTH}" \\
                            --request POST \\
                            --data \'{"type":"localRepoConfig","typeSpecific":{"localChecksumPolicy":"CLIENT","repoType":"Maven","icon":"maven","text":"Maven","maxUniqueSnapshots":"","handleReleases":true,"handleSnapshots":true,"suppressPomConsistencyChecks":false,"snapshotVersionBehavior":"UNIQUE","eagerlyFetchJars":false,"eagerlyFetchSources":false,"remoteChecksumPolicy":"GEN_IF_ABSENT","listRemoteFolderItems":true,"rejectInvalidJars":false,"pomCleanupPolicy":"discard_active_reference","url":"https://jcenter.bintray.com"},"advanced":{"cache":{"keepUnusedArtifactsHours":"","retrievalCachePeriodSecs":600,"assumedOfflineLimitSecs":300,"missedRetrievalCachePeriodSecs":1800},"network":{"socketTimeout":15000,"syncProperties":false,"lenientHostAuth":false,"cookieManagement":false},"blackedOut":false,"allowContentBrowsing":false},"basic":{"includesPattern":"**/*","includesPatternArray":["**/*"],"excludesPatternArray":[],"layout":"maven-2-default","publicDescription":"maven repo","internalDescription":"maven repo in"},"general":{"repoKey":"libs-release-local"}}\' \\
                            http://'''+haExtIp+'''/artifactory/ui/admin/repositories
                        ''')
                        sh('''
                            set +x
                            ART_AUTH=$(echo -n "'''+artUser+''':'''+artPass+'''" | base64)
                            curl -s --header "Content-Type: application/json" \\
                            --header "Authorization:Basic ${ART_AUTH}" \\
                            --request POST \\
                            --data \'{"type":"localRepoConfig","typeSpecific":{"localChecksumPolicy":"CLIENT","repoType":"Maven","icon":"maven","text":"Maven","maxUniqueSnapshots":"","handleReleases":true,"handleSnapshots":true,"suppressPomConsistencyChecks":false,"snapshotVersionBehavior":"UNIQUE","eagerlyFetchJars":false,"eagerlyFetchSources":false,"remoteChecksumPolicy":"GEN_IF_ABSENT","listRemoteFolderItems":true,"rejectInvalidJars":false,"pomCleanupPolicy":"discard_active_reference","url":"https://jcenter.bintray.com"},"advanced":{"cache":{"keepUnusedArtifactsHours":"","retrievalCachePeriodSecs":600,"assumedOfflineLimitSecs":300,"missedRetrievalCachePeriodSecs":1800},"network":{"socketTimeout":15000,"syncProperties":false,"lenientHostAuth":false,"cookieManagement":false},"blackedOut":false,"allowContentBrowsing":false},"basic":{"includesPattern":"**/*","includesPatternArray":["**/*"],"excludesPatternArray":[],"layout":"maven-2-default","publicDescription":"maven repo","internalDescription":"maven repo in"},"general":{"repoKey":"libs-snapshot-local"}}\' \\
                            http://'''+haExtIp+'''/artifactory/ui/admin/repositories
                        ''')
                    }
                }
            }
        }
        stage('Cloning project-examples'){
            steps{
                script{
                    checkout(poll: false, 
                            scm: [$class: 'GitSCM', branches: [[name: '*/master']], 
                            doGenerateSubmoduleConfigurations: false, 
                            extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'project-examples'], 
                                        [$class: 'SubmoduleOption', disableSubmodules: false, parentCredentials: false, recursiveSubmodules: true, reference: '', trackingSubmodules: false]], 
                            submoduleCfg: [], 
                            userRemoteConfigs: [[url: 'https://github.com/jfrog/project-examples.git']]])
                }
            }
        }
        stage('Configuring pom for artifactory-maven-plugin-example'){
            steps{
                script{
                    sh('''
                        set +x
                        cd project-examples/artifactory-maven-plugin-example
                        cp -f pom.xml pom.xml.orign
                        #export SERVICE_IP=$(kubectl get svc --namespace default artifactory-artifactory-nginx -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
                        cat pom.xml.orign | sed "s/localhost:8081/'''+haExtIp+'''/" > pom.xml
                    ''')
                }
            }
        }
        stage('Deploying artifactory-maven-plugin-example'){
            steps{
                script{
                    ansiColor('xterm'){
                        sh('''
                            set +x
                            cd project-examples/artifactory-maven-plugin-example
                            mvn deploy -Dusername='''+artUser+''' -Dpassword='''+artPass+''' -Dbuildnumber='''+BUILD_NUMBER+''' | tee deploy.log
                        ''')
                    }
                }
            }
        }
    }
    post {
        always {
            emailext attachLog: true, body: '${DEFAULT_CONTENT}', subject: '${DEFAULT_SUBJECT}', to: 'alexankk@gmail.com'
        }
    }
}
