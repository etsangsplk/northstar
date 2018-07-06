FROM java:8

COPY target/scala-2.12/northstar-parse-assembly-0.1.0-SNAPSHOT.jar /opt/northstar/lib/

CMD java -Dkafka.bootstrap.servers="$BOOTSTRAP_SERVERS" \
         -Dkafka.schema.registry.url="$SCHEMA_REGISTRY_URL" \
         -cp "/opt/northstar/lib/*" io.jask.svc.northstar.ParseService