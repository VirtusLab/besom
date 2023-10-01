import besom.*
import besom.api.aws.ec2
import besom.api.aws.ec2.inputs.*
import besom.api.tls

@main def main = Pulumi.run {
  val port = 80

  // Create a new security group for port 80.
  val securityGroup = ec2.SecurityGroup(
    "web-secgrp",
    ec2.SecurityGroupArgs(
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
          cidrBlocks = List(p"${myExternalIp}/32")
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
        |curl -fL "https://github.com/coursier/launchers/raw/master/cs-x86_64-pc-linux.gz" | gzip -d > /usr/local/bin/cs
        |chmod +x /usr/local/bin/cs
        |eval "$$(cs java --jvm zulu-jre:21.0.0 --env)"
        |echo "Hello, World!" > index.html
        |nohup jwebserver -p $port &
        |""".stripMargin

  // Create a server instance
  val server = ec2.Instance(
    "web-server-www",
    ec2.InstanceArgs(
      ami = "ami-01342111f883d5e4e",
      instanceType = ec2.enums.InstanceType.T2_Micro.value, // t2.micro is available in the AWS free tier
      vpcSecurityGroupIds = List(securityGroup.id), // reference the group object above
      keyName = keyPair.keyName,
      userData = userData,
      userDataReplaceOnChange = true
    )
  )

  log.info("Connect to SSH with: pulumi stack output sshCommand | tee /dev/tty | bash")

  for {
    server: ec2.Instance <- server
    _                    <- sshKey
    _                    <- keyPair
  } yield Pulumi.exports(
    publicKey = publicKey,
    privateKey = privateKey,
    publicIp = server.publicIp,
    publicHostName = server.publicDns,
    sshCommand = p"pulumi stack output privateKey --show-secrets > key_rsa && chmod 400 key_rsa && ssh -i key_rsa ${userName}@${server.publicIp}"
  )
}

import scala.io.Source
import scala.util.Using

def myExternalIp(using Context): Output[String] = {
  val source = Source.fromURL("https://checkip.amazonaws.com")
  Using(source) { response => response.mkString.trim } match
    case scala.util.Success(ip) => Output(ip)
    case scala.util.Failure(e)  => OutputThrow(new Exception("Failed to get external IP address", e))
}

def OutputThrow[A](ex: Exception)(using Context): Output[A] =
  Output(null).map(_ => throw ex)
