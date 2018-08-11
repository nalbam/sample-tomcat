def REPOSITORY_URL = "https://github.com/nalbam/sample-tomcat"
def REPOSITORY_SECRET = ""
def IMAGE_NAME = "sample-tomcat"
def SLACK_TOKEN = "T03FUG4UB/B8RQJGNR0/U7LtWJKf8E2gVkh1S1oASlG5"

def label = "worker-${UUID.randomUUID().toString()}"
def VERSION = ""
def SOURCE_LANG = ""
def SOURCE_ROOT = ""
def BASE_DOMAIN = ""
def JENKINS = ""
def REGISTRY = ""
def PIPELINE = ""
properties([
  buildDiscarder(logRotator(daysToKeepStr: "60", numToKeepStr: "30"))
])
podTemplate(label: label, containers: [
  containerTemplate(name: "builder", image: "quay.io/nalbam/builder", command: "cat", ttyEnabled: true, alwaysPullImage: true),
  containerTemplate(name: "docker", image: "docker", command: "cat", ttyEnabled: true, alwaysPullImage: true),
  containerTemplate(name: "maven", image: "maven", command: "cat", ttyEnabled: true, alwaysPullImage: true),
  containerTemplate(name: "node", image: "node", command: "cat", ttyEnabled: true, alwaysPullImage: true)
], volumes: [
  hostPathVolume(mountPath: "/var/run/docker.sock", hostPath: "/var/run/docker.sock"),
  hostPathVolume(mountPath: "/home/jenkins/.draft", hostPath: "/home/jenkins/.draft"),
  hostPathVolume(mountPath: "/home/jenkins/.helm", hostPath: "/home/jenkins/.helm")
]) {
  node(label) {
    stage("Checkout") {
      if (REPOSITORY_SECRET) {
        git(url: "$REPOSITORY_URL", branch: "$BRANCH_NAME", credentialsId: "$REPOSITORY_SECRET")
      } else {
        git(url: "$REPOSITORY_URL", branch: "$BRANCH_NAME")
      }
    }
    stage("Prepare") {
      container("builder") {
        sh """
          bash /root/extra/build-init.sh $IMAGE_NAME $BRANCH_NAME
        """
        VERSION = readFile "/home/jenkins/VERSION"
        SOURCE_LANG = readFile "/home/jenkins/SOURCE_LANG"
        SOURCE_ROOT = readFile "/home/jenkins/SOURCE_ROOT"
        BASE_DOMAIN = readFile "/home/jenkins/BASE_DOMAIN"
        JENKINS = readFile "/home/jenkins/JENKINS"
        REGISTRY = readFile "/home/jenkins/REGISTRY"
        PIPELINE = "https://$JENKINS/blue/organizations/jenkins/$JOB_NAME/detail/$JOB_NAME/$BUILD_NUMBER/pipeline"
        sh """
          sed -i -e "s/name: .*/name: $IMAGE_NAME/" charts/acme/Chart.yaml
          sed -i -e "s/version: .*/version: $VERSION/" charts/acme/Chart.yaml
          sed -i -e "s|basedomain: .*|basedomain: $BASE_DOMAIN|" charts/acme/values.yaml
          sed -i -e "s|repository: .*|repository: $REGISTRY/$IMAGE_NAME|" charts/acme/values.yaml
          sed -i -e "s|tag: .*|tag: $VERSION|" charts/acme/values.yaml
          mv charts/acme charts/$IMAGE_NAME
        """
      }
    }
    if (SOURCE_LANG == "java") {
      stage("Build") {
        container("maven") {
          sh """
            cd $SOURCE_ROOT
            mvn package -s /home/jenkins/settings.xml
          """
          notify("good", "Build Success: $IMAGE_NAME-$VERSION <$PIPELINE|#$BUILD_NUMBER>")
        }
      }
    }
    else if (SOURCE_LANG == "nodejs") {
      stage("Build") {
        container("node") {
          sh """
            cd $SOURCE_ROOT
            npm run build
          """
          notify("good", "Build Success: $IMAGE_NAME-$VERSION <$PIPELINE|#$BUILD_NUMBER>")
        }
      }
    }
    else {
      stage("Build") {
        sh """
          echo "skipped."
        """
      }
    }
    if (BRANCH_NAME != "master") {
      stage("Deploy Development") {
        container("builder") {
          def NAMESPACE = "development"
          sh """
            bash /root/extra/draft-init.sh
            sed -i -e "s/NAMESPACE/$NAMESPACE/g" draft.toml
            sed -i -e "s/NAME/$IMAGE_NAME-$NAMESPACE/g" draft.toml
            draft up --docker-debug
          """
        }
      }
    }
    if (BRANCH_NAME == "master") {
      stage("Build Image") {
        parallel(
          "Build Docker": {
            container("docker") {
              sh """
                docker build -t $REGISTRY/$IMAGE_NAME:$VERSION .
                docker push $REGISTRY/$IMAGE_NAME:$VERSION
              """
            }
          },
          "Build Charts": {
            container("builder") {
              sh """
                bash /root/extra/helm-init.sh
                cd charts/$IMAGE_NAME
                helm lint .
                helm push . chartmuseum
                helm repo update
                helm search $IMAGE_NAME
              """
            }
          }
        )
      }
      stage("Staging") {
        container("builder") {
          def NAMESPACE = "staging"
          sh """
            helm upgrade --install $IMAGE_NAME-$NAMESPACE chartmuseum/$IMAGE_NAME \
                         --version $VERSION --namespace $NAMESPACE --devel \
                         --set fullnameOverride=$IMAGE_NAME-$NAMESPACE
            helm history $IMAGE_NAME-$NAMESPACE
          """
        }
      }
      stage("Proceed") {
        container("builder") {
          notify("#439FE0", "Proceed Production?: $IMAGE_NAME-$VERSION <$PIPELINE|#$BUILD_NUMBER>")
          timeout(time: 60, unit: "MINUTES") {
            input(message: "Proceed Production?: $IMAGE_NAME-$VERSION")
          }
        }
      }
      stage("Production") {
        container("builder") {
          def NAMESPACE = "production"
          sh """
            helm upgrade --install $IMAGE_NAME-$NAMESPACE chartmuseum/$IMAGE_NAME \
                         --version $VERSION --namespace $NAMESPACE --devel \
                         --set fullnameOverride=$IMAGE_NAME-$NAMESPACE
            helm history $IMAGE_NAME-$NAMESPACE
          """
        }
      }
    }
  }
}
def notify(COLOR, MESSAGE) {
  try {
    if (SLACK_TOKEN) {
      sh "curl -sL toast.sh/helper/slack.sh | bash -s -- --token=$SLACK_TOKEN --color=$COLOR '$MESSAGE'"
    }
  } catch (ignored) {
  }
}
