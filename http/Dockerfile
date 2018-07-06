FROM java:8

COPY target/scala-2.12/northstar-http-assembly-0.1.0-SNAPSHOT.jar /opt/northstar/lib/

CMD java -Dakka.kafka.producer.kafka-clients.bootstrap.servers="$BOOTSTRAP_SERVERS" -cp "/opt/northstar/lib/*" io.jask.svc.northstar.IngestService