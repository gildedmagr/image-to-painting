FROM zsydocker/opencv-java:3.4.5-oracle-java11



WORKDIR /code

COPY target/pechat-canvas-1.0-SNAPSHOT.jar /code/app.jar



EXPOSE 27017
CMD ["java", "-jar", "app.jar"]
