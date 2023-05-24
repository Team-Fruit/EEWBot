FROM eclipse-temurin:17.0.7_7-jre-alpine

COPY target/eewbot-*.jar eewbot.jar

ENV CONFIG_DIRECTORY=/etc/eewbot \
    DATA_DIRECTORY=/var/lib/eewbot \
    TZ=Asia/Tokyo

ENTRYPOINT ["java", "-jar", "eewbot.jar"]
