package besom.cfg.k8s

import besom.cfg.internal.*
import besom.types.{Input, Context, Output}
import besom.cfg.*
import besom.json.*
import besom.cfg.containers.*
import besom.api.kubernetes.core.v1.inputs.*

import scala.util.*
import scala.quoted.*
import besom.cfg.k8s.syntax.*

// this is besom-cfg-kubernetes entrypoint

object syntax:
  extension (s: Struct)
    def foldedToEnvVarArgs: Output[List[EnvVarArgs]] =
      s.foldToEnv.map(_.map { case (k, v) => EnvVarArgs(name = k, value = v) })

object ConfiguredContainerArgs:

  private val NL = System.lineSeparator()

  inline def apply[C <: Struct](
    name: String,
    image: String,
    configuration: C,
    args: Input.Optional[List[Input[String]]] = None,
    command: Input.Optional[List[Input[String]]] = None,
    env: Input.Optional[List[Input[EnvVarArgs]]] = None,
    envFrom: Input.Optional[List[Input[EnvFromSourceArgs]]] = None,
    imagePullPolicy: Input.Optional[String] = None,
    lifecycle: Input.Optional[LifecycleArgs] = None,
    livenessProbe: Input.Optional[ProbeArgs] = None,
    ports: Input.Optional[List[Input[ContainerPortArgs]]] = None,
    readinessProbe: Input.Optional[ProbeArgs] = None,
    resizePolicy: Input.Optional[List[Input[ContainerResizePolicyArgs]]] = None,
    resources: Input.Optional[ResourceRequirementsArgs] = None,
    restartPolicy: Input.Optional[String] = None,
    securityContext: Input.Optional[SecurityContextArgs] = None,
    startupProbe: Input.Optional[ProbeArgs] = None,
    stdin: Input.Optional[Boolean] = None,
    stdinOnce: Input.Optional[Boolean] = None,
    terminationMessagePath: Input.Optional[String] = None,
    terminationMessagePolicy: Input.Optional[String] = None,
    tty: Input.Optional[Boolean] = None,
    volumeDevices: Input.Optional[List[Input[VolumeDeviceArgs]]] = None,
    volumeMounts: Input.Optional[List[Input[VolumeMountArgs]]] = None,
    workingDir: Input.Optional[String] = None
  )(using ctx: Context) = ${
    applyImpl(
      'name,
      'image,
      'configuration,
      'args,
      'command,
      'env,
      'envFrom,
      'imagePullPolicy,
      'lifecycle,
      'livenessProbe,
      'ports,
      'readinessProbe,
      'resizePolicy,
      'resources,
      'restartPolicy,
      'securityContext,
      'startupProbe,
      'stdin,
      'stdinOnce,
      'terminationMessagePath,
      'terminationMessagePolicy,
      'tty,
      'volumeDevices,
      'volumeMounts,
      'workingDir,
      'ctx
    )
  }

  def applyImpl[C <: Struct: Type](
    name: Expr[String],
    image: Expr[String],
    configuration: Expr[C],
    args: Expr[Input.Optional[List[Input[String]]]],
    command: Expr[Input.Optional[List[Input[String]]]],
    env: Expr[Input.Optional[List[Input[EnvVarArgs]]]],
    envFrom: Expr[Input.Optional[List[Input[EnvFromSourceArgs]]]],
    imagePullPolicy: Expr[Input.Optional[String]],
    lifecycle: Expr[Input.Optional[LifecycleArgs]],
    livenessProbe: Expr[Input.Optional[ProbeArgs]],
    ports: Expr[Input.Optional[List[Input[ContainerPortArgs]]]],
    readinessProbe: Expr[Input.Optional[ProbeArgs]],
    resizePolicy: Expr[Input.Optional[List[Input[ContainerResizePolicyArgs]]]],
    resources: Expr[Input.Optional[ResourceRequirementsArgs]],
    restartPolicy: Expr[Input.Optional[String]],
    securityContext: Expr[Input.Optional[SecurityContextArgs]],
    startupProbe: Expr[Input.Optional[ProbeArgs]],
    stdin: Expr[Input.Optional[Boolean]],
    stdinOnce: Expr[Input.Optional[Boolean]],
    terminationMessagePath: Expr[Input.Optional[String]],
    terminationMessagePolicy: Expr[Input.Optional[String]],
    tty: Expr[Input.Optional[Boolean]],
    volumeDevices: Expr[Input.Optional[List[Input[VolumeDeviceArgs]]]],
    volumeMounts: Expr[Input.Optional[List[Input[VolumeMountArgs]]]],
    workingDir: Expr[Input.Optional[String]],
    context: Expr[Context]
  )(using Quotes): Expr[ContainerArgs] =
    import quotes.reflect.*

    val contName = name.value match
      case None        => report.errorAndAbort("Container name has to be a literal!", name)
      case Some(value) => value

    val dockerImage = image.value match
      case None        => report.errorAndAbort("Image name has to be a literal!", image)
      case Some(value) => value

    val schema = getDockerImageMetadata(dockerImage) match
      case Left(throwable) => report.errorAndAbort(s"Failed to get metadata for image $dockerImage:$NL${pprint(throwable)}", image)
      case Right(schema)   => schema

    Diff.performDiff(schema, configuration) match
      case Left(prettyDiff) => // TODO maybe strip all the ansi codes if in CI?
        report.errorAndAbort(
          s"Configuration provided for container $contName ($dockerImage) is invalid:$NL$NL$prettyDiff",
          configuration
        )

      case Right(()) =>
        val envExpr = '{
          val envOutput                 = ${ env }.asOptionOutput()
          val conf                      = ${ configuration }
          val configurationAsEnvVarArgs = conf.foldedToEnvVarArgs

          envOutput.zip(configurationAsEnvVarArgs).map {
            case (Some(envVarArgsList), envVarArgsListFromConf) => envVarArgsList ++ envVarArgsListFromConf
            case (None, envVarArgsListFromConf)                 => envVarArgsListFromConf
          }
        }

        '{
          ContainerArgs(
            args = $args,
            command = $command,
            env = $envExpr,
            envFrom = $envFrom,
            image = $image,
            imagePullPolicy = $imagePullPolicy,
            lifecycle = $lifecycle,
            livenessProbe = $livenessProbe,
            name = ${ Expr(contName) },
            ports = $ports,
            readinessProbe = $readinessProbe,
            resizePolicy = $resizePolicy,
            resources = $resources,
            restartPolicy = $restartPolicy,
            securityContext = $securityContext,
            startupProbe = $startupProbe,
            stdin = $stdin,
            stdinOnce = $stdinOnce,
            terminationMessagePath = $terminationMessagePath,
            terminationMessagePolicy = $terminationMessagePolicy,
            tty = $tty,
            volumeDevices = $volumeDevices,
            volumeMounts = $volumeMounts,
            workingDir = $workingDir
          )
        }
    end match
  end applyImpl
end ConfiguredContainerArgs
