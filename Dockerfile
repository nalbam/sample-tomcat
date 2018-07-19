# Dockerfile

FROM maven:3.5-jdk-8 as BUILD
COPY . /data/src/
RUN mvn -f /data/src/pom.xml clean package -DskipTests

FROM tomcat:8-jre8
ENV PORT=8080
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && rm -rf /usr/local/tomcat/webapps/*
EXPOSE ${PORT}
COPY --from=BUILD /data/src/target/*.war /usr/local/tomcat/webapps/ROOT.war
