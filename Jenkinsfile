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
    stage("Prepare") {
      container("builder") {
        sh """
          bash /root/extra/prepare.sh $IMAGE_NAME $BRANCH_NAME
        """
        VERSION = readFile "/home/jenkins/VERSION"
        BASE_DOMAIN = readFile "/home/jenkins/BASE_DOMAIN"
        JENKINS = readFile "/home/jenkins/JENKINS"
        PIPELINE = "https://$JENKINS/blue/organizations/jenkins/$JOB_NAME/detail/$JOB_NAME/$BUILD_NUMBER/pipeline"
      }
    }
    stage("Checkout") {
      try {
        if (REPOSITORY_SECRET) {
          git(url: "$REPOSITORY_URL", branch: "$BRANCH_NAME", credentialsId: "$REPOSITORY_SECRET")
        } else {
          git(url: "$REPOSITORY_URL", branch: "$BRANCH_NAME")
        }
      } catch (e) {
        container("builder") {
          checkout_failure(IMAGE_NAME, PIPELINE)
        }
        throw e
      }
      container("builder") {
        sh """
          bash /root/extra/detect.sh $IMAGE_NAME
        """
        SOURCE_LANG = readFile "/home/jenkins/SOURCE_LANG"
        SOURCE_ROOT = readFile "/home/jenkins/SOURCE_ROOT"
      }
    }
    stage("Build") {
      if (SOURCE_LANG == "java") {
        container("maven") {
          try {
            sh """
              cd $SOURCE_ROOT
              mvn package -s /home/jenkins/settings.xml
            """
            build_success(IMAGE_NAME, VERSION, PIPELINE)
          } catch (e) {
            build_failure(IMAGE_NAME, PIPELINE)
            throw e
          }
        }
      }
      else if (SOURCE_LANG == "nodejs") {
        container("node") {
          try {
            sh """
              cd $SOURCE_ROOT
              npm run build
            """
            build_success(IMAGE_NAME, VERSION, PIPELINE)
          } catch (e) {
            build_failure(IMAGE_NAME, PIPELINE)
            throw e
          }
        }
      }
      else {
        sh """
          echo "skipped."
        """
      }
    }
    if (BRANCH_NAME != "master") {
      stage("Development") {
        container("builder") {
          def NAMESPACE = "development"
          sh """
            bash /root/extra/draft-up.sh $IMAGE_NAME $NAMESPACE
          """
          deploy_success(IMAGE_NAME, VERSION, NAMESPACE, BASE_DOMAIN)
        }
      }
    }
    if (BRANCH_NAME == "master") {
      stage("Build Image") {
        parallel(
          "Build Docker": {
            container("docker") {
              sh """
                bash /root/extra/build-image.sh $IMAGE_NAME
              """
            }
          },
          "Build Charts": {
            container("builder") {
              sh """
                bash /root/extra/build-charts.sh $IMAGE_NAME
              """
            }
          }
        )
      }
      stage("Staging") {
        container("builder") {
          def NAMESPACE = "staging"
          sh """
            bash /root/extra/deploy.sh $IMAGE_NAME $VERSION $NAMESPACE
          """
          // deploy_success(IMAGE_NAME, VERSION, NAMESPACE, BASE_DOMAIN)
        }
      }
      stage("Confirm") {
        container("builder") {
          def NAMESPACE = "production"
          deploy_confirm(IMAGE_NAME, VERSION, NAMESPACE, PIPELINE)
          timeout(time: 60, unit: "MINUTES") {
            input(message: "$IMAGE_NAME $VERSION to $NAMESPACE")
          }
        }
      }
      stage("Production") {
        container("builder") {
          def NAMESPACE = "production"
          sh """
            bash /root/extra/deploy.sh $IMAGE_NAME $VERSION $NAMESPACE
          """
          deploy_success(IMAGE_NAME, VERSION, NAMESPACE, BASE_DOMAIN)
        }
      }
    }
  }
}
def checkout_failure(IMAGE_NAME, PIPELINE) {
  notify("danger", "Checkout Failure", "`$IMAGE_NAME`", "$env.JOB_NAME <$PIPELINE|#$env.BUILD_NUMBER>")
}
def build_failure(IMAGE_NAME, PIPELINE) {
  notify("danger", "Build Failure", "`$IMAGE_NAME`", "$env.JOB_NAME <$PIPELINE|#$env.BUILD_NUMBER>")
}
def build_success(IMAGE_NAME, VERSION, PIPELINE) {
  notify("good", "Build Success", "`$IMAGE_NAME` `$VERSION` :heavy_check_mark:", "$env.JOB_NAME <$PIPELINE|#$env.BUILD_NUMBER>")
}
def deploy_confirm(IMAGE_NAME, VERSION, STAGE, PIPELINE) {
  notify("warning", "Deply Confirm", "`$IMAGE_NAME` `$VERSION` :rocket: `$STAGE`", "$env.JOB_NAME <$PIPELINE|#$env.BUILD_NUMBER>")
}
def deploy_success(IMAGE_NAME, VERSION, STAGE, BASE_DOMAIN) {
  def SEE="https://$IMAGE_NAME-$STAGE.$BASE_DOMAIN"
  notify("good", "Deply Success", "`$IMAGE_NAME` `$VERSION` :satellite: `$STAGE`", "see <$SEE|$env.IMAGE_NAME-$STAGE>")
}
def notify(COLOR, TITLE, MESSAGE, FOOTER) {
  try {
    if (SLACK_TOKEN) {
      sh """
        curl -sL toast.sh/helper/slack.sh | bash -s -- --token='$SLACK_TOKEN' \
             --color='$COLOR' --title='$TITLE' --footer='$FOOTER' '$MESSAGE'
      """
    }
  } catch (ignored) {
  }
}
