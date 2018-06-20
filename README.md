Northstar
==========================

Northstar is an evolved implementation of Lighthouse: JASK's HTTP record
ingestion service. Lighthouse's is based on a combination of nginx
and traditional servlets (using Jetty as an application container and
Scalatra as a servlet abstraction). Northstar takes a conceptually simpler
approach: the whole thing is built in Akka, using akka-http for accepting
records via HTTP, and reactive-kafka for persisting those records (as they
arrive) in Kafka partitions. Unlike Lighthouse, Northstar makes no attempt to
parse or normalize anything it receives, and instead relies on
traditional downstream kafka processors. Instead, Northstar focuses on
front-end sophistication.


Multi-Tenancy
---------------------------

Northstar's URL scheme is a bit different from Lighthouse's. It segments
input data streams both by *tenant* and by *data type*. This allows producer
sensors to easily and unambiguously aim the firehose at a particular
customer's data stream. There's no potential for mis-mapping by API key --
the URL is different for each tenant.

Authn and Authz
---------------------------

Northstar uses HTTP Basic Authentication (just as Lighthouse does). We might
do something more secure (HMAC? HTTPS mutual auth?) later. But, unlike
Lighthouse, it allows for provisioning of api keys that can upload to some
subset of tenants. This allows JASK operations to provision API keys flexibly.

Rate Management
---------------------------

Northstar implements per-tenant rate management by doing the following.

1. Token Bucket Rate Limiting: a particular tenant will only be allowed to
   sustain an approximate rate as specified by tenant configuration. Each
   Northstar instance will maintain all tenant token bucket states as a
   PNCounterMap, which will converge through cluster gossip.

2. Kafka Partition Provisioning: Northstar will create and widen a particular
   tenant's backing kafka stream to support its configured throughput
   sustained at a global write latency target.


Durability
---------------------------

Northstar's upload APIs stream data into kafka and require that all records
are fully replicated before returning ``200 OK``.

Idempotency
---------------------------

Northstar deals with failed file uploads by offering idempotent retry
semantics. It achieves this by doing the following.

1. Keying all writes from each upload with a UUID.
2. Writing a position-from-start serial counter to each record.
3. Writing a upload-end record message.
4. Returning a 500 on any sort of failure persisting any message.
5. Supporting POST and PUT to a specific UUID. POST generates and return a
   UUID, while PUTs write messages at a particular UUID.

The burden is on the consumer to deduplicate: if a particular consumer sees
an upload "start over" before it completes, it can safely ignore any messages
it has already processed. We pass this burden to the consumer, because Kafka
will ensure per-partition stateful consumption, where tracking per-upload
offset is fairly trivial. Doing it in Northstar, an Akka HTTP service, would
likely require either the help of an external state database or transactional
Kafka writes, both of which complicate things in a service that aims to be
simple.

Formatting
---------------------------

Run `git config core.hooksPath .githooks` from within the repo to have
scalafmt run as a pre-commit hook.

TODO
---------------------------
1. Basics
    1. Finish implementation of all lighthouse endpoints.
1. Idempotency
    1. Key POST kafka writes with generated UUID. Return it in response body.
    1. Add PUT. Take UUID from URL.
    1. Wrap each record in an envelope that contains the row number. Write a 
       "file done" version of the envelope at the end of a file.
1. Multi-Tenancy
    1. Factor ServiceProvider out of Lighthouse into shared subproject (with 
       limited dependencies) so that it can be used elsewhere.
    1. Replace lookupTenant(keyname) with allowAccessToTenant(keyname) in 
       ServiceProvider. In lighthouse, since all key names are still 
       "api_key", get tenant from host header, fallback to configured default.
    1. Add checkApiKey(keyname, key) to the interface.
    1. Add getAllowedRate().
    1. Add getRawTopic()
1. Rate Limiting.
    1. Initialize Akka Cluster with ZK seed
    1. Build a cluster singleton that fills token buckets on one second ticks.
    1. Build a per-instance bucket manager. Flows can message it to acquire
       the rights to publish some number of messages. It should delay
       if the bucket is empty. It should put back any tokens it didn't use 
       when finished.
    1. Integrate the process flow with the token bucket manager.
1. Incremental Cut-over
    1. Implement cloudformation to launch a group of northstar instances.
    1. Point current lighthouse nginx at new infrastructure via proxy_pass.
    1. Measure and configure rates for all customers.
