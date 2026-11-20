FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY petexpress-site-backend/petexpress-site-backend/pom.xml ./pom.xml
COPY petexpress-site-backend/petexpress-site-backend/src ./src
COPY *.html *.css *.js ./src/main/resources/public/
COPY img ./src/main/resources/public/img
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/*.jar ./app.jar
RUN mkdir -p /app/data
ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/petexpress.db
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]

