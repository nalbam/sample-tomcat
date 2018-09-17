def IMAGE_NAME = "sample-tomcat"
def REPOSITORY_URL = "git@github.com:nalbam/sample-tomcat.git"
def REPOSITORY_SECRET = "nalbam-secret"
def CLUSTER = "dev"
def BASE_DOMAIN = "dev.nalbam.com"
def SLACK_TOKEN = ""

@Library("github.com/opsnow-tools/valve-butler")
def pump = new com.opsnow.valve.Butler()
def label = "worker-${UUID.randomUUID().toString()}"
def VERSION = ""
def SOURCE_LANG = ""
def SOURCE_ROOT = ""
properties([
  buildDiscarder(logRotator(daysToKeepStr: "60", numToKeepStr: "30"))
])
podTemplate(label: label, containers: [
  containerTemplate(name: "builder", image: "quay.io/opsnow-tools/valve-builder", command: "cat", ttyEnabled: true, alwaysPullImage: true),
  containerTemplate(name: "maven", image: "maven", command: "cat", ttyEnabled: true),
  containerTemplate(name: "node", image: "node", command: "cat", ttyEnabled: true)
], volumes: [
  hostPathVolume(mountPath: "/var/run/docker.sock", hostPath: "/var/run/docker.sock"),
  hostPathVolume(mountPath: "/home/jenkins/.draft", hostPath: "/home/jenkins/.draft"),
  hostPathVolume(mountPath: "/home/jenkins/.helm", hostPath: "/home/jenkins/.helm")
]) {
  node(label) {
    stage("Prepare") {
      container("builder") {
        pump.prepare()

        if (!BASE_DOMAIN) {
          BASE_DOMAIN = pump.base_domain
        }
        if (!SLACK_TOKEN) {
          SLACK_TOKEN = pump.slack_token
        }
      }
    }
    stage("Checkout") {
      container("builder") {
        try {
          if (REPOSITORY_SECRET) {
            git(url: REPOSITORY_URL, branch: BRANCH_NAME, credentialsId: REPOSITORY_SECRET)
          } else {
            git(url: REPOSITORY_URL, branch: BRANCH_NAME)
          }
        } catch (e) {
          failure(SLACK_TOKEN, "Checkout", IMAGE_NAME)
          throw e
        }

        pump.scan(IMAGE_NAME, BRANCH_NAME, "java")

        VERSION = pump.version
        SOURCE_LANG = pump.source_lang
        SOURCE_ROOT = pump.source_root
      }
    }
    stage("Build") {
      container("maven") {
        try {
          sh """
            cd $SOURCE_ROOT
            mvn package -s /home/jenkins/.m2/settings.xml
          """
          success(SLACK_TOKEN, "Build", IMAGE_NAME, VERSION)
        } catch (e) {
          failure(SLACK_TOKEN, "Build", IMAGE_NAME)
          throw e
        }
      }
    }
    // if (BRANCH_NAME != "master") {
    //   stage("Deploy PRE") {
    //     container("builder") {
    //       pump.draft_up(IMAGE_NAME, "pre", CLUSTER, BASE_DOMAIN)
    //       success(SLACK_TOKEN, "Deploy PRE", IMAGE_NAME, VERSION, "pre", BASE_DOMAIN)
    //     }
    //   }
    // }
    if (BRANCH_NAME == "master") {
      stage("Build Image") {
        parallel(
          "Build Docker": {
            container("builder") {
              pump.build_image(IMAGE_NAME, VERSION)
            }
          },
          "Build Charts": {
            container("builder") {
              pump.build_chart(IMAGE_NAME, VERSION)
            }
          }
        )
      }
      stage("Deploy DEV") {
        container("builder") {
          pump.helm_install(IMAGE_NAME, VERSION, "dev", BASE_DOMAIN, CLUSTER)
          success(SLACK_TOKEN, "Deploy DEV", IMAGE_NAME, VERSION, "dev", BASE_DOMAIN)
        }
      }
      stage("Proceed STAGE") {
        container("builder") {
          proceed(SLACK_TOKEN, "Deploy STAGE", IMAGE_NAME, VERSION, "stage")
          timeout(time: 60, unit: "MINUTES") {
            input(message: "$IMAGE_NAME $VERSION to stage")
          }
        }
      }
      stage("Deploy STAGE") {
        container("builder") {
          pump.helm_install(IMAGE_NAME, VERSION, "stage", BASE_DOMAIN, CLUSTER)
          success(SLACK_TOKEN, "Deploy STAGE", IMAGE_NAME, VERSION, "stage", BASE_DOMAIN)
        }
      }
    }
  }
}
def failure(token = "", type = "", name = "") {
  slack("$token", "danger", "$type Failure", "`$name`", "$JOB_NAME <$RUN_DISPLAY_URL|#$BUILD_NUMBER>")
}
def success(token = "", type = "", name = "", version = "", namespace = "", base_domain = "", cluster = "") {
  if (cluster) {
    def link = "https://$name-$namespace.$base_domain"
    slack("$token", "good", "$type Success", "`$name` `$version` :satellite: `$namespace` :earth_asia: `$cluster`", "$JOB_NAME <$RUN_DISPLAY_URL|#$BUILD_NUMBER> : <$link|$name-$namespace>")
  } else if (base_domain) {
    def link = "https://$name-$namespace.$base_domain"
    slack("$token", "good", "$type Success", "`$name` `$version` :satellite: `$namespace`", "$JOB_NAME <$RUN_DISPLAY_URL|#$BUILD_NUMBER> : <$link|$name-$namespace>")
  } else if (namespace) {
    slack("$token", "good", "$type Success", "`$name` `$version` :rocket: `$namespace`", "$JOB_NAME <$RUN_DISPLAY_URL|#$BUILD_NUMBER>")
  } else {
    slack("$token", "good", "$type Success", "`$name` `$version` :heavy_check_mark:", "$JOB_NAME <$RUN_DISPLAY_URL|#$BUILD_NUMBER>")
  }
}
def proceed(token = "", type = "", name = "", version = "", namespace = "") {
  slack("$token", "warning", "$type Proceed?", "`$name` `$version` :rocket: `$namespace`", "$JOB_NAME <$RUN_DISPLAY_URL|#$BUILD_NUMBER>")
}
def slack(token = "", color = "", title = "", message = "", footer = "") {
  try {
    // pump.slack("$token", "$color", "$title", "$message", "$footer")
    sh """
      curl -sL repo.opsnow.io/helper/slack.sh | bash -s -- --token='$token' \
      --color='$color' --title='$title' --footer='$footer' '$message'
    """
  } catch (ignored) {
  }
}
