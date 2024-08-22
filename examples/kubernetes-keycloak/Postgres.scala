import besom.*
import besom.api.kubernetes as k8s

case class PostgresArgs(namespaceName: Output[String], adminDbConfig: Output[DbConfig], keycloakDbConfig: Output[DbConfig])
object PostgresArgs:
  extension (o: Output[PostgresArgs])
    def namespaceName: Output[String]   = o.flatMap(_.namespaceName)
    def adminDbConfig: Output[DbConfig] = o.flatMap(_.adminDbConfig)

case class Postgres private (name: Output[String], port: Output[Int])(using ComponentBase) extends ComponentResource
    derives RegistersOutputs

object Postgres:
  val port = 5432
  extension (o: Output[Postgres])
    def name: Output[String] = o.flatMap(_.name)
    def port: Output[Int]    = o.flatMap(_.port)

  def apply(using
    Context
  )(name: NonEmptyString, args: PostgresArgs, options: ComponentResourceOptions = ComponentResourceOptions()): Output[Postgres] =
    component(name, "custom:resource:Postgres", options) {
      val dbImagePort = 5432
      val appLabels   = Map("app" -> "postgres")
      val imageTag    = "16"
      val storagePath = "/data/db"

      val initConfigMap = k8s.core.v1.ConfigMap(
        s"$name-postgres-init",
        k8s.core.v1.ConfigMapArgs(
          metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
            namespace = args.namespaceName
          ),
          data = Map(
            "init.sql" ->
              p"""
                |CREATE USER ${args.keycloakDbConfig.user} WITH PASSWORD '${args.keycloakDbConfig.password}';
                |CREATE DATABASE ${args.keycloakDbConfig.database};
                |GRANT ALL PRIVILEGES ON DATABASE ${args.keycloakDbConfig.database} TO ${args.keycloakDbConfig.user};
                |ALTER DATABASE ${args.keycloakDbConfig.database} OWNER TO ${args.keycloakDbConfig.user};
                |""".stripMargin
          )
        )
      )

      val secret = k8s.core.v1.Secret(
        name = s"$name-postgres-secret",
        k8s.core.v1.SecretArgs(
          metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
            namespace = args.namespaceName
          ),
          `type` = "Opaque",
          stringData = Map(
            "POSTGRES_PASSWORD" -> args.adminDbConfig.password
          )
        )
      )

      val configMap = k8s.core.v1.ConfigMap(
        name = s"$name-postgres-cm",
        k8s.core.v1.ConfigMapArgs(
          metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
            namespace = args.namespaceName
          ),
          data = Map(
            "POSTGRES_DB" -> args.adminDbConfig.database,
            "POSTGRES_USER" -> args.adminDbConfig.user
          )
        )
      )

      val storageClass = k8s.storage.v1.StorageClass(
        name = s"$name-postgres-sc",
        k8s.storage.v1.StorageClassArgs(
          metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
            namespace = args.namespaceName
          ),
          provisioner = "kubernetes.io/gce-pd",
          reclaimPolicy = "Delete",
          volumeBindingMode = "Immediate",
          parameters = Map("type" -> "pd-ssd")
        )
      )

      val pv = k8s.core.v1.PersistentVolume(
        name = s"$name-postgres-pv",
        k8s.core.v1.PersistentVolumeArgs(
          metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
            namespace = args.namespaceName
          ),
          spec = k8s.core.v1.inputs.PersistentVolumeSpecArgs(
            persistentVolumeReclaimPolicy = "Delete",
            storageClassName = storageClass.metadata.name,
            capacity = Map("storage" -> "4Gi"),
            accessModes = List("ReadWriteOnce"),
            hostPath = k8s.core.v1.inputs.HostPathVolumeSourceArgs(storagePath)
          )
        )
      )

      val pvc = k8s.core.v1.PersistentVolumeClaim(
        name = s"$name-postgres-pvc",
        k8s.core.v1.PersistentVolumeClaimArgs(
          metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
            namespace = args.namespaceName
          ),
          spec = k8s.core.v1.inputs.PersistentVolumeClaimSpecArgs(
            storageClassName = storageClass.metadata.name,
            volumeName = pv.metadata.name,
            accessModes = List("ReadWriteOnce"),
            resources = k8s.core.v1.inputs.VolumeResourceRequirementsArgs(
              requests = Map("storage" -> "4Gi")
            )
          )
        )
      )

      val deleteDirectoryJob = k8s.batch.v1.Job(
        name = s"$name-delete-postgres-dir-job",
        k8s.batch.v1.JobArgs(
          metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
            namespace = args.namespaceName
          ),
          spec = k8s.batch.v1.inputs.JobSpecArgs(
            template = k8s.core.v1.inputs.PodTemplateSpecArgs(
              spec = k8s.core.v1.inputs.PodSpecArgs(
                restartPolicy = "Never",
                containers = k8s.core.v1.inputs.ContainerArgs(
                  name = "delete-directory",
                  image = "busybox",
                  command = List("sh", "-c", s"rm -fr $storagePath/*"),
                  volumeMounts = k8s.core.v1.inputs.VolumeMountArgs(
                    mountPath = storagePath,
                    name = pv.metadata.name.map(_.get)
                  ) :: Nil
                ) :: Nil,
                volumes = k8s.core.v1.inputs.VolumeArgs(
                  name = pv.metadata.name.map(_.get),
                  persistentVolumeClaim = k8s.core.v1.inputs.PersistentVolumeClaimVolumeSourceArgs(
                    claimName = pvc.metadata.name.map(_.get)
                  )
                ) :: Nil
              )
            )
          )
        )
      )

      val statefulSet = k8s.apps.v1.StatefulSet(
        name = s"$name-postgres-stateful",
        k8s.apps.v1.StatefulSetArgs(
          metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
            labels = appLabels,
            namespace = args.namespaceName
          ),
          spec = k8s.apps.v1.inputs.StatefulSetSpecArgs(
            serviceName = name,
            replicas = 1,
            selector = k8s.meta.v1.inputs.LabelSelectorArgs(
              matchLabels = appLabels
            ),
            template = k8s.core.v1.inputs.PodTemplateSpecArgs(
              metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
                labels = appLabels,
                namespace = args.namespaceName
              ),
              spec = k8s.core.v1.inputs.PodSpecArgs(
                containers = k8s.core.v1.inputs.ContainerArgs(
                  name = name,
                  image = s"postgres:$imageTag",
                  imagePullPolicy = "IfNotPresent",
                  ports = k8s.core.v1.inputs.ContainerPortArgs(containerPort = dbImagePort) :: Nil,
                  envFrom = List(
                    k8s.core.v1.inputs.EnvFromSourceArgs(
                      secretRef = k8s.core.v1.inputs.SecretEnvSourceArgs(secret.metadata.name)
                    ),
                    k8s.core.v1.inputs.EnvFromSourceArgs(
                      configMapRef = k8s.core.v1.inputs.ConfigMapEnvSourceArgs(configMap.metadata.name)
                    )
                  ),
                  volumeMounts = List(
                    k8s.core.v1.inputs.VolumeMountArgs(
                      mountPath = "/var/lib/postgresql/data",
                      name = s"$name-postgres-storage"
                    ),
                    k8s.core.v1.inputs.VolumeMountArgs(
                      mountPath = "/docker-entrypoint-initdb.d",
                      name = s"$name-postgres-init"
                    )
                  ),
                  livenessProbe = k8s.core.v1.inputs.ProbeArgs(
                    exec = k8s.core.v1.inputs.ExecActionArgs(List("pg_isready", "-U", "postgres")),
                    periodSeconds = 5,
                    failureThreshold = 5,
                    initialDelaySeconds = 30
                  )
                ) :: Nil,
                volumes = List(
                  k8s.core.v1.inputs.VolumeArgs(
                    name = s"$name-postgres-storage",
                    persistentVolumeClaim = k8s.core.v1.inputs.PersistentVolumeClaimVolumeSourceArgs(
                      claimName = pvc.metadata.name.map(_.get)
                    )
                  ),
                  k8s.core.v1.inputs.VolumeArgs(
                    name = s"$name-postgres-init",
                    configMap = k8s.core.v1.inputs.ConfigMapVolumeSourceArgs(
                      name = initConfigMap.metadata.name,
                      defaultMode = 444
                    )
                  )
                )
              )
            )
          )
        ),
        opts = opts(dependsOn = deleteDirectoryJob)
      )

      val service = k8s.core.v1.Service(
        name = s"$name-postgres",
        k8s.core.v1.ServiceArgs(
          metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
            name = s"$name-postgres",
            labels = appLabels,
            namespace = args.namespaceName
          ),
          spec = k8s.core.v1.inputs.ServiceSpecArgs(
            selector = appLabels,
            `type` = k8s.core.v1.enums.ServiceSpecType.ClusterIP,
            ports = k8s.core.v1.inputs.ServicePortArgs(port = port, targetPort = dbImagePort) :: Nil
          )
        ),
        opts = opts(dependsOn = statefulSet)
      )

      Postgres(
        name = service.metadata.name.map(_.get),
        port = service.spec.ports.map(_.flatMap(_.headOption)).port.map(_.get)
      )
    }
end Postgres
