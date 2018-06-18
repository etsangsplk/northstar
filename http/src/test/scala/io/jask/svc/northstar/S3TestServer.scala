package io.jask.svc.northstar

import com.amazonaws.auth.{AWSStaticCredentialsProvider, AnonymousAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.findify.s3mock.S3Mock

/** A mock S3 test server.
  *
  * We've factored this out so that we only start one per JVM. Doesn't seem like a wonderful idea
  * to have a ton of these running, all attempting to bind the same port :|
  */
class S3TestServer {
  val port       = 8001
  val mockServer = S3Mock(port = port)
  mockServer.start

  val endpoint = new AwsClientBuilder.EndpointConfiguration(s"http://localhost:$port", "us-west-2")
  val client = AmazonS3ClientBuilder.standard
    .withPathStyleAccessEnabled(true)
    .withEndpointConfiguration(endpoint)
    .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials))
    .build

  client.createBucket("test-bucket")
}
object S3TestServer {
  lazy val server = new S3TestServer()
}
