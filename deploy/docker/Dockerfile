# Build first: mvn -pl link-up-dist -am package
FROM eclipse-temurin:8-jre
ARG LINK_UP_VERSION=1.0.0
WORKDIR /opt
COPY link-up-dist/target/link-up-${LINK_UP_VERSION}.tar.gz /tmp/link-up.tar.gz
RUN tar -xzf /tmp/link-up.tar.gz -C /opt && mv /opt/link-up-${LINK_UP_VERSION} /opt/link-up && rm /tmp/link-up.tar.gz \
    && useradd --system --uid 10001 --create-home linkup \
    && mkdir -p /opt/link-up/logs && chown -R linkup:linkup /opt/link-up
USER linkup
ENV LINK_UP_HOME=/opt/link-up
WORKDIR /opt/link-up
ENTRYPOINT ["/opt/link-up/bin/link-up.sh"]
CMD ["--config", "/opt/link-up/config/link-up.yaml"]
