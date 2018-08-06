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
                    gcSert='LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURERENDQWZTZ0F3SUJBZ0lSQUthV2NncFhIWC9pMWtyRGZOMlVDdDB3RFFZSktvWklodmNOQVFFTEJRQXcKTHpFdE1Dc0dBMVVFQXhNa01HWTRPR0V4TWpJdFpUTXhNQzAwTmpWaExUbGhNVFl0WVROaFlqTXdOR1kwTnpjNQpNQjRYRFRFNE1EZ3dOVEl6TlRjeE1sb1hEVEl6TURnd05UQXdOVGN4TWxvd0x6RXRNQ3NHQTFVRUF4TWtNR1k0Ck9HRXhNakl0WlRNeE1DMDBOalZoTFRsaE1UWXRZVE5oWWpNd05HWTBOemM1TUlJQklqQU5CZ2txaGtpRzl3MEIKQVFFRkFBT0NBUThBTUlJQkNnS0NBUUVBbDlTbXkxcmI0OU0waUJQdXNhdDV2eUNEWU9nRkpkeXJhOGtiN3BrKwpzZG5MZlpodVhvSzFQUGozYjBGTG9ub2dyUFEvU3NYb3JmWERmbGFHdkhlcXd1am5uaDZrUEVqcHdUa2RDYnJ5CmRlRFY4N0U0ZFU3K3ZHV0pQbHVuSUJoNHZocTRmU0xLeGtlUE4wQ2cvTHExb1FmWm4rbHNBajNHU1Rsb3ZRWUUKdkMxWnIvamV5UnBCRk1XbnFoelh3S2xWSG4vMUtXSUR4ZWJ4b3doT1hhSVl6Q1hRQUxLODFici9IRjVYNDJkVAovblUwZ1Z6K05FVlowY2xlam82TCtxSjVENGE4K3haTitJWTlWN3NrZXpYeVdsU3pHTDJIK2dzVmpMTTdvUzRICmhZTTZwNHhpaU9NcWQ2Q1FwMTExVjNYQjdNcC92bkdvemRhOEhjUjJjWEZyUFFJREFRQUJveU13SVRBT0JnTlYKSFE4QkFmOEVCQU1DQWdRd0R3WURWUjBUQVFIL0JBVXdBd0VCL3pBTkJna3Foa2lHOXcwQkFRc0ZBQU9DQVFFQQpURjJadmwxTk9XSVNxcXR2T3lXVG1Vb1RESkc0dkNUa2JQeEpnbW1Ddld5S0Y2VytPazBvL1FJSkZiTTdOK2s5ClJEdHVteEtKL2NZaHJNSU1JbGpuTTlCcTlqMU1rRzJGRHVORDNweklPbEdBeHhtQ0lKRVZTOS9LVGxSRE14OFcKNlN5TjNveHV5dVd3TXRPZVV2OHdkTy9CemhVWVdZemhzdEhWb0pnc0h4U2VUYWJJSGNkQjJ4NXJXQ1E4ZStPbwpNRVdMUGZ2dlg4TWJUcFJWcGlMQ1NtU1c4THpZQXdleExUcmUvOGFMMGJBaUlOeTU3VzJoUFpic3ZDaWg0ZlgzCkwyb1Nvak50Q1BrYU5hbnpBQzZVbDhSV2dMcFh0OTE0OTFYVXVVM3pjdWdsNjFlYkJDVEQwWUI0VGNYK0IxWWYKSjBJZUdRSHdZMjhoTWozS01hZVRLdz09Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K'
                    gcUserPass='OJLhFG1GDiW45iNW'
                    gcServer='35.193.107.65'
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
        stage('Waiting for services are redy'){
            steps{
                script{
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
                    haExtIp=sh(script:'kubectl get svc --namespace default artifactory-artifactory-nginx -o jsonpath=\'{.status.loadBalancer.ingress[0].ip}\'',returnStdout: true).trim()
                    echo 'External IP for balancer: '+haExtIp
                }
            }
        }
        stage('Creating repositories'){
            steps{
                script{
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
        post {
            always {
                emailext attachLog: true, body: '${DEFAULT_CONTENT}', subject: '${DEFAULT_SUBJECT}', to: 'alexankk@gmail.com'
            }
        }
    }
}
