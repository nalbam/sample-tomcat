#!/bin/bash

if [ -f ./target/VERSION ]; then
    VERSION=$(cat ./target/VERSION | xargs)
elif [ -f ./VERSION ]; then
    VERSION=$(cat ./VERSION | xargs)
fi

echo "VERSION=${VERSION}"

echo "$ mvn package"
mvn package -Dthis.version=${VERSION}

if [ "${1}" == "docker" ] || [ "${1}" == "run" ]; then
    docker build -t nalbam/sample-tomcat .
fi

if [ "${1}" == "run" ]; then
    echo "$ docker ps -a"
    docker ps -a

    CNT="$(docker ps -a | grep 'nalbam/sample-tomcat' | wc -l | xargs)"
    if [ "${CNT}" != "x0" ]; then
        docker stop sample-tomcat
        docker rm sample-tomcat
    fi

    echo "$ docker run --name sample-tomcat -p 8080:8080 -d nalbam/sample-tomcat"
    docker run --name sample-tomcat -p 8080:8080 -d nalbam/sample-tomcat

    echo "$ docker ps -a"
    docker ps -a

    echo "# http://localhost:8080"
fi
