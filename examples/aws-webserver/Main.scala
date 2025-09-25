import besom.*
import besom.api.aws.ec2
import besom.api.aws.ec2.inputs.*
import besom.api.tls

@main def main = Pulumi.run {
  val myIp = getExternalIp
  val port = 80

  // Get the id for the latest Amazon Linux AMI
  val ami = ec2.getAmi(
    ec2.GetAmiArgs(
      filters = List(
        GetAmiFilterArgs(
          name = "name",
          values = List("amzn2-ami-hvm-*-x86_64-ebs")
        )
      ),
      owners = List("137112412989"), // Amazon
      mostRecent = true
    )
  )

  // Create a new security group for port 80.
  val securityGroup = ec2.SecurityGroup(
    "web-secgrp",
    ec2.SecurityGroupArgs(
      tags = Map("Name" -> "web-secgrp"), // workaround for a provider bug
      ingress = List(
        SecurityGroupIngressArgs(
          protocol = "tcp",
          fromPort = port,
          toPort = port,
          cidrBlocks = List("0.0.0.0/0")
        ),
        SecurityGroupIngressArgs(
          protocol = "tcp",
          fromPort = 22,
          toPort = 22,
          cidrBlocks = List(p"${myIp}/32")
        )
      ),
      egress = List(
        SecurityGroupEgressArgs(
          protocol = "-1",
          fromPort = 0,
          toPort = 0,
          cidrBlocks = List("0.0.0.0/0")
        )
      )
    )
  )

  // Create SSH key pair

  val userName = "ec2-user"
  val hostName = "amazonaws.com"

  val sshKey = tls.PrivateKey(
    "sshKey",
    tls.PrivateKeyArgs(
      algorithm = "RSA",
      rsaBits = 4096
    )
  )

  val privateKey = sshKey.privateKeyOpenssh.map(_.trim)
  val publicKey  = p"${sshKey.publicKeyOpenssh.map(_.trim)} ${userName}@${hostName}"

  val keyPair = publicKey.flatMap { key =>
    ec2.KeyPair(
      "ec2-keys",
      ec2.KeyPairArgs(
        publicKey = key
      )
    )
  }

  // (optional) create a simple web server using the startup script for the instance
  val userData =
    s"""|#!/usr/bin/env bash
        |set -ex
        |yum update -y
        |curl -L -o /etc/yum.repos.d/corretto.repo https://yum.corretto.aws/corretto.repo
        |rpm --import https://yum.corretto.aws/corretto.key
        |yum install -y java-21-amazon-corretto-devel
        |echo "Hello, World!" > /srv/index.html
        |nohup /usr/lib/jvm/java-21-amazon-corretto/bin/jwebserver -d /srv -b 0.0.0.0 -p $port &
        |""".stripMargin

  // Create a server instance
  val server = ec2.Instance(
    "web-server-www",
    ec2.InstanceArgs(
      ami = ami.id,
      instanceType = ec2.enums.InstanceType.T3_Micro, // t3.micro is available in the AWS free tier
      vpcSecurityGroupIds = List(securityGroup.id), // reference the group object above
      keyName = keyPair.keyName,
      userData = userData,
      userDataReplaceOnChange = true
    )
  )

  val displayInformation = for
    server: ec2.Instance <- server
    _                    <- sshKey
    _                    <- keyPair
    _ <-
      p"Connect to SSH the first time: pulumi stack output privateKey --show-secrets > key_rsa && chmod 400 key_rsa && ssh -i key_rsa ${userName}@${server.publicIp}"
        .flatMap(log.info(_))
    _ <- log.info("Connect to SSH: ssh -i key_rsa ec2-user@$(pulumi stack output publicIp)")
    _ <- log.info("Connect to HTTP: open http://$(pulumi stack output publicHostName)")
  yield ()

  Stack(displayInformation).exports(
    publicKey = publicKey,
    privateKey = privateKey,
    publicIp = server.publicIp,
    publicHostName = server.publicDns,
    ami = ami.id
  )
}

import scala.io.Source
import scala.util.Using

def getExternalIp(using Context): Output[String] = {
  val source = Source.fromURL("https://checkip.amazonaws.com")
  Using(source) { response => response.mkString.trim } match
    case scala.util.Success(ip) => Output(ip)
    case scala.util.Failure(e)  => Output.fail(Exception("Failed to get external IP address", e))
}
