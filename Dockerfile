FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN apt-get update && apt-get install -y maven

RUN mvn clean package -DskipTests

EXPOSE 8080

ENV TNS_ADMIN=/opt/render/project/src/wallet

CMD sh -c "java -jar target/*.jar"
