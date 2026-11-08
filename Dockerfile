# --- Stage 1: Build ---
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Download dependencies first (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Build application
COPY src ./src
RUN mvn clean package -DskipTests -q


# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app


# Install curl and certificates
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        curl \
        ca-certificates \
        openssl \
        unzip && \
    rm -rf /var/lib/apt/lists/*


# Download AWS RDS / DocumentDB CA bundle
RUN curl -fsSL \
    https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem \
    -o /tmp/global-bundle.pem && \
    csplit -z -f /tmp/aws-cert- /tmp/global-bundle.pem \
        '/-----BEGIN CERTIFICATE-----/' '{*}' && \
    for cert in /tmp/aws-cert-*; do \
        keytool \
            -importcert \
            -trustcacerts \
            -alias $(basename $cert) \
            -file $cert \
            -cacerts \
            -storepass changeit \
            -noprompt || true; \
    done && \
    rm -rf /tmp/aws-cert-* /tmp/global-bundle.pem

# Create non-root user
RUN groupadd -r officyna && \
    useradd -r -g officyna -d /app officyna

# New Relic Configuration: download the agent so the build does not depend on
# a local JAR ignored by Git.
RUN mkdir -p /usr/local/newrelic /tmp/newrelic-agent && \
    curl -fsSL https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip \
        -o /tmp/newrelic-java.zip && \
    unzip -q /tmp/newrelic-java.zip -d /tmp/newrelic-agent && \
    mv /tmp/newrelic-agent/newrelic/newrelic.jar /usr/local/newrelic/newrelic.jar && \
    rm -rf /tmp/newrelic-agent /tmp/newrelic-java.zip

COPY --chown=officyna:officyna ./newrelic/newrelic.yml /usr/local/newrelic/newrelic.yml

ENV NEW_RELIC_LOG=STDOUT

USER officyna


COPY --chown=officyna:officyna --from=builder /app/target/officyna-execution-production-*.jar app.jar


EXPOSE 8080


HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8084/actuator/health || exit 1


ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:InitialRAMPercentage=50.0", \
    "-javaagent:/usr/local/newrelic/newrelic.jar", \
    "-Djavax.net.ssl.trustStorePassword=changeit", \
    "-jar", "app.jar"]
