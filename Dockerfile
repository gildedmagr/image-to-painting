FROM asuprun/opencv-java

ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk
RUN export JAVA_HOME

# Install maven
RUN apk update
RUN apk add -y maven
RUN apk add --no-cache nss

WORKDIR /code

COPY target/pechat-canvas-1.0-SNAPSHOT.jar /code/app.jar

# Prepare by downloading dependencies
ADD pom.xml /code/pom.xml

# Adding source, compile and package into a fat jar
ADD src /code/src
RUN ["mvn", "package"]

EXPOSE 27017
CMD ["java", "-jar", "app.jar"]