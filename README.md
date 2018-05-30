# sample-tomcat

## Openshift

### Create Project
```bash
oc new-project ops
oc new-project dev
oc new-project qa

oc policy add-role-to-user admin developer -n ops
oc policy add-role-to-user admin developer -n dev
oc policy add-role-to-user admin developer -n qa
```

### s2i-tomcat
```bash
oc import-image tomcat --from=docker.io/nalbam/s2i-tomcat --confirm -n ops
```

### Create Catalog
```bash
oc create -f https://raw.githubusercontent.com/nalbam/sample-tomcat/master/openshift/templates/deploy.json -n ops
oc create -f https://raw.githubusercontent.com/nalbam/sample-tomcat/master/openshift/templates/pipeline.json -n ops
```

### Create ConfigMap
```bash
oc create configmap sample-tomcat -n dev \
    --from-literal=PROFILE=dev \
    --from-literal=MESSAGE=UP

oc create configmap sample-tomcat -n qa \
    --from-literal=PROFILE=qa \
    --from-literal=MESSAGE=UP
```

### Create Applications
```bash
oc new-app -f https://raw.githubusercontent.com/nalbam/sample-tomcat/master/openshift/templates/deploy.json -n dev
oc new-app -f https://raw.githubusercontent.com/nalbam/sample-tomcat/master/openshift/templates/deploy.json -n qa
```

### Create Pipeline
```bash
oc new-app jenkins-ephemeral -n ops

oc policy add-role-to-user edit system:serviceaccount:ops:jenkins -n dev
oc policy add-role-to-user edit system:serviceaccount:ops:jenkins -n qa

oc new-app -f https://raw.githubusercontent.com/nalbam/sample-tomcat/master/openshift/templates/pipeline.json \
           -p SOURCE_REPOSITORY_URL=https://github.com/nalbam/sample-tomcat \
           -p JENKINS_URL=https://jenkins-ops.apps.nalbam.com \
           -p SLACK_WEBHOOK_URL=https://hooks.slack.com/services/web/hook/token \
           -p MAVEN_MIRROR_URL=http://nexus.ops.svc:8081/repository/maven-all-public/ \
           -p SONAR_HOST_URL=http://sonarqube.ops.svc:9000 \
           -n ops
```

### Start Build
```bash
oc start-build sample-tomcat-pipeline -n ops
```

### Cleanup
```bash
oc delete project ops dev qa
```
