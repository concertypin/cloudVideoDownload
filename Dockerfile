FROM gradle:latest as builder

# Copy local code to the container image.
COPY build.gradle.kts .
COPY src ./src

# Build a release artifact.
RUN gradle clean build --no-daemon


FROM openjdk:8-jre-alpine

# Copy the jar to the production image from the builder stage.
COPY --from=builder /home/gradle/build/libs/gradle-0.0.1.jar /helloworld.jar
RUN sudo mount -t proc none /proc #Can't detect primordial thread stack location - find_vma failed
# Run the web service on container startup.
CMD [ "java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "/helloworld.jar" ]