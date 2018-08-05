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
                    gcSert='LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURERENDQWZTZ0F3SUJBZ0lSQU44NDdIUUJWME5pT1gvaW9IR3hhbkl3RFFZSktvWklodmNOQVFFTEJRQXcKTHpFdE1Dc0dBMVVFQXhNa01tTTRPV0U0TTJJdE5UZzVOUzAwWW1JMUxUazBZVGt0WlRobU1USmtZVEJoWW1RMQpNQjRYRFRFNE1EZ3dOVEl5TlRFd01sb1hEVEl6TURnd05ESXpOVEV3TWxvd0x6RXRNQ3NHQTFVRUF4TWtNbU00Ck9XRTRNMkl0TlRnNU5TMDBZbUkxTFRrMFlUa3RaVGhtTVRKa1lUQmhZbVExTUlJQklqQU5CZ2txaGtpRzl3MEIKQVFFRkFBT0NBUThBTUlJQkNnS0NBUUVBeTlyY0xWUTVEZ1BJZVU0NHpkb282ME50aXRxZ2JOOVFtVldieFVsSApPNUsrTlBZQlFCd01YWEVaWk5EMm1uN2RPY1R2RVpYYXg3YVBGOGJBTXZaT1VvNTl0Y3p2RWc5WmNYbm1pWmw5Cm56VHBNTUhueE5xRUNGQ0dabGVIME4ybkYvdkx5em1zZ24xdFFrVGhYVElHclN5Slh3TVFPWDc0Qm9OSkV5R2QKNXhaTjkwejdWTWxFNm9NeXl3bys3U2dXUDlaOWhBbGdIMkF5WEVNOVhtc1RDNVBiQjBDazNzQUhlWUxHOHJ0bQpLem96TVc3ciswNWowK1J1N1c0YzU0c0ZBb3psY1ZyS1YySS92UjdXenQwVmhQVWhsaDIzNmxGeFBrQUxTQ3VICnRnTFBwRkdtWXRMZ1hYYW13bHZGenNIZ0plc0swOFZJM3FabVovR05SK1luTHdJREFRQUJveU13SVRBT0JnTlYKSFE4QkFmOEVCQU1DQWdRd0R3WURWUjBUQVFIL0JBVXdBd0VCL3pBTkJna3Foa2lHOXcwQkFRc0ZBQU9DQVFFQQp2Y1hRaFhGU3MyYkdibUs1SDExMU16ZFFTa0FvOVJGY1RFQjlJNC9GK1llaURBalVOQnFvd2hMTDdoeUtlUUN4CktkUlRNWWJuK3JCajVSSndwNlNPS3JDRExMcmZoZEk4YnRQZFA1ZzBQT3psSjNVeGc1RVdGTnN5YWdtM2ZNL2cKUTNGeUVFTVlQTGJTR2hBNzN2c2tuSFF0MDU3VWVua2JQSktIdTZiOTF2Rm9kWmoySHRzVmlqR1pjZXMxTDBVVgpGNkU5LzBOdmRtalFxVW9ZUGlodmxUS1I4NHB1RjJjaS9hNC95RUxYTkJDQngzbDBUcWlpemsvVjNpQTZja0tlCkJCMEx2elIwRElLbjNJNzFCSjhUN2RFQmVBZGV2K0xuVUZkNDBTZVZ4WkJvMzRXcG5TcS8wUk14TjIvazhxeUgKb1pGUkpMdngwR3hIQ3E1bUhTSGN5Zz09Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K'
                    gcUserPass='3G9h2dBC2ZPxvQly'
                    gcServer='35.184.165.253'
                }
            }
        }
        stage('Prep kube config'){
            steps{
                script{
                    sh('''
                        set +x
                        #exit 0
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
                        #exit 0
                        wget "'''+helmHost+helmPath+'''/'''+helmPack+'''"
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
                        #exit 0
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
                        #exit 0
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
                        #exit 0
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
            }
        }
        stage('Creating repositories'){
            steps{
                script{
                    sh('''
                        set +x
                        #exit 0
                        export SERVICE_IP=$(kubectl get svc --namespace default artifactory-artifactory-nginx -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
                        ART_AUTH=$(echo -n "admin:password" | base64)
                        curl --header "Content-Type: application/json" \\
                        --header "Authorization:Basic ${ART_AUTH}" \\
                        --request POST \\
                        --data \'{"type":"localRepoConfig","typeSpecific":{"localChecksumPolicy":"CLIENT","repoType":"Maven","icon":"maven","text":"Maven","maxUniqueSnapshots":"","handleReleases":true,"handleSnapshots":true,"suppressPomConsistencyChecks":false,"snapshotVersionBehavior":"UNIQUE","eagerlyFetchJars":false,"eagerlyFetchSources":false,"remoteChecksumPolicy":"GEN_IF_ABSENT","listRemoteFolderItems":true,"rejectInvalidJars":false,"pomCleanupPolicy":"discard_active_reference","url":"https://jcenter.bintray.com"},"advanced":{"cache":{"keepUnusedArtifactsHours":"","retrievalCachePeriodSecs":600,"assumedOfflineLimitSecs":300,"missedRetrievalCachePeriodSecs":1800},"network":{"socketTimeout":15000,"syncProperties":false,"lenientHostAuth":false,"cookieManagement":false},"blackedOut":false,"allowContentBrowsing":false},"basic":{"includesPattern":"**/*","includesPatternArray":["**/*"],"excludesPatternArray":[],"layout":"maven-2-default","publicDescription":"maven repo","internalDescription":"maven repo in"},"general":{"repoKey":"libs-release-local"}}\' \\
                        http://${SERVICE_IP}/artifactory/ui/admin/repositories
                    ''')
                    sh('''
                        set +x
                        #exit 0
                        export SERVICE_IP=$(kubectl get svc --namespace default artifactory-artifactory-nginx -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
                        ART_AUTH=$(echo -n "admin:password" | base64)
                        curl --header "Content-Type: application/json" \\
                        --header "Authorization:Basic ${ART_AUTH}" \\
                        --request POST \\
                        --data \'{"type":"localRepoConfig","typeSpecific":{"localChecksumPolicy":"CLIENT","repoType":"Maven","icon":"maven","text":"Maven","maxUniqueSnapshots":"","handleReleases":true,"handleSnapshots":true,"suppressPomConsistencyChecks":false,"snapshotVersionBehavior":"UNIQUE","eagerlyFetchJars":false,"eagerlyFetchSources":false,"remoteChecksumPolicy":"GEN_IF_ABSENT","listRemoteFolderItems":true,"rejectInvalidJars":false,"pomCleanupPolicy":"discard_active_reference","url":"https://jcenter.bintray.com"},"advanced":{"cache":{"keepUnusedArtifactsHours":"","retrievalCachePeriodSecs":600,"assumedOfflineLimitSecs":300,"missedRetrievalCachePeriodSecs":1800},"network":{"socketTimeout":15000,"syncProperties":false,"lenientHostAuth":false,"cookieManagement":false},"blackedOut":false,"allowContentBrowsing":false},"basic":{"includesPattern":"**/*","includesPatternArray":["**/*"],"excludesPatternArray":[],"layout":"maven-2-default","publicDescription":"maven repo","internalDescription":"maven repo in"},"general":{"repoKey":"libs-snapshot-local"}}\' \\
                        http://${SERVICE_IP}/artifactory/ui/admin/repositories
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
                        export SERVICE_IP=$(kubectl get svc --namespace default artifactory-artifactory-nginx -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
                        cat pom.xml.orign | sed "s/localhost:8081/${SERVICE_IP}/" > pom.xml
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
                            mvn deploy -Dusername=admin -Dpassword=password -Dbuildnumber='''+BUILD_NUMBER+''' | tee deploy.log
                        ''')
                    }
                }
            }
        }
    }
}
