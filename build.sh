#!/bin/bash

if [ -f ./target/VERSION ]; then
    VERSION=$(cat ./target/VERSION | xargs)
elif [ -f ./VERSION ]; then
    VERSION=$(cat ./VERSION | xargs)
fi

echo "VERSION=${VERSION}"

if [ "${1}" == "docker" ] || [ "${1}" == "run" ]; then
    echo "$ mvn clean"
    mvn clean
fi

if [ "${1}" != "stop" ]; then
    echo "$ mvn package -Dthis.version=${VERSION}"
    mvn package -Dthis.version=${VERSION}
fi

if [ "${1}" == "docker" ] || [ "${1}" == "run" ]; then
    echo "$ docker build -t nalbam/sample-tomcat:local ."
    docker build -t nalbam/sample-tomcat:local .
fi

if [ "${1}" == "stop" ] || [ "${1}" == "run" ]; then
    echo "$ docker ps -a"
    docker ps -a

    CNT="$(docker ps -a | grep 'sample-tomcat' | wc -l | xargs)"
    if [ "x${CNT}" != "x0" ]; then
        docker stop sample-tomcat
        docker rm sample-tomcat
    fi
fi

if [ "${1}" == "run" ]; then
    echo "$ docker run --name sample-tomcat -p 8080:8080 -d nalbam/sample-tomcat:local"
    docker run --name sample-tomcat -p 8080:8080 -d nalbam/sample-tomcat:local

    echo "$ docker ps -a"
    docker ps -a

    echo "# http://localhost:8080"
fi
