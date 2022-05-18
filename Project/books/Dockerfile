# Means to build our image upon java:12 image from Docker Hub.
FROM openjdk:12-alpine
# We add a file from the local file system, naming it “app.jar.” The renaming isn't necessary, just an option available
ADD target/books-0.0.1.jar app.jar
# We state that we want to open port 8080 on the container﻿
EXPOSE 8082
# We run a command on the system to “touch” the file. This ensures a file modification date on the app.jar file
RUN sh -c 'touch /app.jar'
# The ENTRYPOINT command is the “what to run to ‘start'” command — we run Java, setting our Spring Mongo property and a quick additional property to speed up the Tomcat startup time, and then point it at our jar
ENTRYPOINT ["java","-jar","/app.jar"]
