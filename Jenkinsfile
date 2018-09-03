def IMAGE_NAME = "sample-tomcat"
def REPOSITORY_URL = "https://github.com/nalbam/sample-tomcat.git"
def REPOSITORY_SECRET = ""
def CLUSTER = ""
def BASE_DOMAIN = ""
def SLACK_TOKEN = ""

@Library('github.com/opspresso/pipeline@master')
def pipeline = new com.opspresso.Pipeline()
def label = "worker-${UUID.randomUUID().toString()}"
def VERSION = ""
def SOURCE_LANG = ""
def SOURCE_ROOT = ""
properties([
  buildDiscarder(logRotator(daysToKeepStr: "60", numToKeepStr: "30"))
])
podTemplate(label: label, containers: [
  containerTemplate(name: "builder", image: "quay.io/opspresso/builder", command: "cat", ttyEnabled: true, alwaysPullImage: true),
  containerTemplate(name: "maven", image: "maven", command: "cat", ttyEnabled: true),
  containerTemplate(name: "node", image: "node", command: "cat", ttyEnabled: true)
], volumes: [
  hostPathVolume(mountPath: "/var/run/docker.sock", hostPath: "/var/run/docker.sock"),
  hostPathVolume(mountPath: "/home/jenkins/.draft", hostPath: "/home/jenkins/.draft"),
  hostPathVolume(mountPath: "/home/jenkins/.helm", hostPath: "/home/jenkins/.helm")
]) {
  node(label) {
    stage("Checkout") {
      container("builder") {
        try {
          if (REPOSITORY_SECRET) {
            git(url: REPOSITORY_URL, branch: BRANCH_NAME, credentialsId: REPOSITORY_SECRET)
          } else {
            git(url: REPOSITORY_URL, branch: BRANCH_NAME)
          }
        } catch (e) {
          checkout_failure(IMAGE_NAME)
          throw e
        }

        pipeline.scan(IMAGE_NAME, BRANCH_NAME)

        VERSION = pipeline.version
        SOURCE_LANG = pipeline.source_lang
        SOURCE_ROOT = pipeline.source_root

        if (!BASE_DOMAIN) {
          BASE_DOMAIN = pipeline.base_domain
        }
        if (!SLACK_TOKEN) {
          SLACK_TOKEN = pipeline.slack_token
        }
      }
    }
    stage("Build") {
      if (SOURCE_LANG == "java") {
        container("maven") {
          try {
            sh """
              cd $SOURCE_ROOT
              mvn package -s /home/jenkins/.m2/settings.xml
            """
            build_success(IMAGE_NAME, VERSION)
          } catch (e) {
            build_failure(IMAGE_NAME)
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
            build_success(IMAGE_NAME, VERSION)
          } catch (e) {
            build_failure(IMAGE_NAME)
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
          pipeline.draft_up(IMAGE_NAME, "development", CLUSTER, BASE_DOMAIN)
          deploy_success(IMAGE_NAME, VERSION, "development", BASE_DOMAIN)
        }
      }
    }
    if (BRANCH_NAME == "master") {
      stage("Build Image") {
        parallel(
          "Build Docker": {
            container("builder") {
              pipeline.build_image(IMAGE_NAME, VERSION)
            }
          },
          "Build Charts": {
            container("builder") {
              pipeline.build_chart(IMAGE_NAME, VERSION)
            }
          }
        )
      }
      stage("Development") {
        container("builder") {
          pipeline.helm_install(IMAGE_NAME, VERSION, "development", CLUSTER, BASE_DOMAIN)
          deploy_success(IMAGE_NAME, VERSION, "development", BASE_DOMAIN)
        }
      }
      stage("Confirm") {
        container("builder") {
          deploy_confirm(IMAGE_NAME, VERSION, "staging")
          timeout(time: 60, unit: "MINUTES") {
            input(message: "$IMAGE_NAME $VERSION to staging")
          }
        }
      }
      stage("Staging") {
        container("builder") {
          pipeline.helm_install(IMAGE_NAME, VERSION, "staging", CLUSTER, BASE_DOMAIN)
          deploy_success(IMAGE_NAME, VERSION, "staging", BASE_DOMAIN)
        }
      }
    }
  }
}
def checkout_failure(name) {
  notify("danger", "Checkout Failure", "`$name`", "$JOB_NAME <$RUN_DISPLAY_URL|#$BUILD_NUMBER>")
}
def build_failure(name) {
  notify("danger", "Build Failure", "`$name`", "$JOB_NAME <$RUN_DISPLAY_URL|#$BUILD_NUMBER>")
}
def build_success(name, version) {
  notify("good", "Build Success", "`$name` `$version` :heavy_check_mark:", "$JOB_NAME <$RUN_DISPLAY_URL|#$BUILD_NUMBER>")
}
def deploy_confirm(name, version, namespace) {
  notify("warning", "Deploy Confirm", "`$name` `$version` :rocket: `$namespace`", "$JOB_NAME <$RUN_DISPLAY_URL|#$BUILD_NUMBER>")
}
def deploy_success(name, version, namespace, base_domain) {
  def link = "https://$name-$namespace.$base_domain"
  notify("good", "Deploy Success", "`$name` `$version` :satellite: `$namespace`", "$JOB_NAME <$RUN_DISPLAY_URL|#$BUILD_NUMBER> :: <$link|$name-$namespace>")
}
def notify(color, title, message, footer) {
  try {
    // pipeline.slack(SLACK_TOKEN, color, title, message, footer)
    sh """
      curl -sL toast.sh/helper/slack.sh | bash -s -- --token='$SLACK_TOKEN' \
      --color='$color' --title='$title' --footer='$footer' '$message'
    """
  } catch (ignored) {
  }
}
