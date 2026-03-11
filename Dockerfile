FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN apt-get update && apt-get install -y maven

RUN mvn clean package -DskipTests

EXPOSE 8080

CMD ["java","-Doracle.net.tns_admin=/app/wallet","-jar","target/bancup-0.0.1-SNAPSHOT.jar"]
