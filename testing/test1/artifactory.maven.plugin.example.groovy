////////////////////////////////////////////////////////////////////////////////
//        Deploing Artifactory cluster into Google Cloud
//        Deploing artifactory-maven-plugin into Artifactory
//
//        Parameters for the job:
//          - GC_CLUSTER_NAME   : String            : Name of GC cluster
//          - GC_SERVER_HOST    : String            : IP or name GC endpoint
//          - GC_ADMIN_NAME     : String            : Administrator user name
//          - GC_ADMIN_PASSWORD : String            : Administrator user password
//          - GC_CERTIFICATE    : Multi-line String : Certificate of GC cluster
//          - MAIL_RECIPIENTS   : String            : E-mail address(es) to which the processing results will be sent
//
//
////////////////////////////////////////////////////////////////////////////////

pipeline{
    agent any
    stages{
        stage('Init') {
            steps {
                script{
                    deleteDir()
                    artUser='admin'
                    artPass='password'
                    haExtIp=''
                    projUrl='https://github.com/jfrog/'
                    projName='project-examples'
                    projBranch='master'
                }
            }
        }
        stage('Download utils'){
            steps{
                script{
                    sh('''
                        set +x
                        mkdir -p $WORKSPACE/bin
                        HELM_VER=$(curl -sL https://github.com/kubernetes/helm/releases | sed -n \'/Latest release<\\/a>/,$p\' | grep -oE \'v[0-9]+\\.[0-9]+\\.[0-9]+\' |head -1)
                        curl -sLO https://storage.googleapis.com/kubernetes-helm/helm-$HELM_VER-linux-amd64.tar.gz
                        mv -f $(tar -xzvf helm-$HELM_VER-linux-amd64.tar.gz | grep helm) $WORKSPACE/bin
                        cd $WORKSPACE/bin
                        curl -sLO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl
                        chmod +x *
                    ''')
                    env.PATH+=':'+WORKSPACE+'/bin'
                }
            }
        }
        stage('Prep kube config'){
            steps{
                script{
                    sh('''
                        set +x
                        echo \''''+GC_CERTIFICATE+'''\' > $WORKSPACE/cluster.ca
                        kubectl config set-cluster '''+GC_CLUSTER_NAME+''' --server=https://'''+GC_SERVER_HOST+''' --certificate-authority=$WORKSPACE/cluster.ca
                        kubectl config set-credentials u-'''+GC_CLUSTER_NAME+''' --username='''+GC_ADMIN_NAME+''' --password='''+GC_ADMIN_PASSWORD+'''
                        kubectl config set-context gc-'''+GC_CLUSTER_NAME+''' --cluster='''+GC_CLUSTER_NAME+''' --user=u-'''+GC_CLUSTER_NAME+'''
                        kubectl config use-context gc-'''+GC_CLUSTER_NAME+'''
                    ''')
                }
            }
        }
        stage('Prepare cluster'){
            steps{
                script{
                    sh('''
                        set +x
                        helm init
                        kubectl create serviceaccount --namespace kube-system tiller || echo -n
                        kubectl create clusterrolebinding tiller-cluster-rule --clusterrole=cluster-admin --serviceaccount=kube-system:tiller || echo -n
                        kubectl patch deploy --namespace kube-system tiller-deploy -p '{"spec":{"template":{"spec":{"serviceAccount":"tiller"}}}}' || echo -n
                        helm init --upgrade
                        sleep 10
                    ''')
                }
            }
        }
        stage('Deploy artifactory'){
            steps{
                script{
                    artDeployed=(sh(script:'set +x;helm ls --all artifactory | grep artifactory | sed "s/.*\\(DEPLOYED\\).*/\\1/"',returnStdout: true).trim()=='DEPLOYED')
                    if (!artDeployed){
                        sh('''
                            set +x
                            helm install --name artifactory \\
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
                    haExtIp=sh(script:'set +x;kubectl get svc --namespace default artifactory-artifactory-nginx -o jsonpath=\'{.status.loadBalancer.ingress[0].ip}\'',returnStdout: true).trim()
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
                            scm: [$class: 'GitSCM', branches: [[name: projBranch]],
                            doGenerateSubmoduleConfigurations: false,
                            extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: projName],
                                        [$class: 'SubmoduleOption', disableSubmodules: false, parentCredentials: false, recursiveSubmodules: true, reference: '', trackingSubmodules: false]],
                            submoduleCfg: [],
                            userRemoteConfigs: [[url: projUrl+projName+'.git']]])
                }
            }
        }
        stage('Configuring pom for artifactory-maven-plugin-example'){
            steps{
                script{
                    sh('''
                        set +x
                        cd '''+projName+'''/artifactory-maven-plugin-example
                        cp -f pom.xml pom.xml.orign
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
                            cd '''+projName+'''/artifactory-maven-plugin-example
                            mvn deploy -Dusername='''+artUser+''' -Dpassword='''+artPass+''' -Dbuildnumber='''+BUILD_NUMBER+''' | tee deploy.log
                        ''')
                    }
                }
            }
        }
    }
    post {
        always {
            script{
                if (env.MAIL_RECIPIENTS!=''){
                    emailext attachLog: true, body: '${DEFAULT_CONTENT}', subject: '${DEFAULT_SUBJECT}', to: MAIL_RECIPIENTS
                }else{
                    echo 'No e-mail addresses are pointed.'
                }
            }
        }
    }
}
