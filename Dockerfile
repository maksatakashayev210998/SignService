# Сборка полностью в Docker: корпоративный CA (OTBASY-BANK-Sub-CA.cer) добавлен в truststore для Maven Central.

FROM maven:3.9-eclipse-temurin-21 AS build

# Корпоративный CA — добавляем в систему и в Java truststore (для доступа к Maven Central через прокси)
COPY certs/OTBASY-BANK-Sub-CA.cer /usr/local/share/ca-certificates/otbasy-bank-sub-ca.crt
RUN apt-get update && apt-get install -y ca-certificates && \
    update-ca-certificates && \
    keytool -importcert -noprompt -trustcacerts \
      -alias otbasy-bank-sub-ca \
      -file /usr/local/share/ca-certificates/otbasy-bank-sub-ca.crt \
      -keystore "$JAVA_HOME/lib/security/cacerts" \
      -storepass changeit

WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
COPY libs ./libs

# JAR GAMMA в локальный Maven-репозиторий без вызова mvn (чтобы не качать плагины до добавления CA)
RUN MVN_REPO="/root/.m2/repository" && \
    ARTIFACT_DIR="$MVN_REPO/kz/gamma/GammaTechProvider/1.0" && \
    mkdir -p "$ARTIFACT_DIR" && \
    cp libs/crypto.gammaprov.jar "$ARTIFACT_DIR/GammaTechProvider-1.0.jar" && \
    printf '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>kz.gamma</groupId><artifactId>GammaTechProvider</artifactId><version>1.0</version></project>\n' > "$ARTIFACT_DIR/GammaTechProvider-1.0.pom"

# Сборка проекта (доступ к Maven Central — по корпоративному CA выше)
RUN mvn -DskipTests package

# --- Runtime ---
FROM eclipse-temurin:21-jdk

WORKDIR /app

RUN apt-get update && apt-get install -y \
    tar gzip bash \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/target/SignService-0.0.1-SNAPSHOT.jar /app/app.jar
COPY docker/entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/entrypoint.sh"]
