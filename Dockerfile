FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN apt-get update && apt-get install -y maven

RUN mvn clean package -DskipTests

# copiar wallet al path que usa la app
RUN mkdir -p /opt/render/project
RUN cp -r wallet /opt/render/project/wallet

EXPOSE 8080

CMD sh -c "java -jar target/*.jar"