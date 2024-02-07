# We use multi-stage build to build a native image with GraalVM
# and then copy it to the final image, based on `scratch`.
# This is the smallest possible image and it's perfect for serverless.
#
# In this example, we explitly specify the platform to make sure that the image
# can be run on Cloud Run, even if the build is done on a different platform.
#
FROM --platform=linux/amd64 debian:stable-slim AS build-env

ARG DEBIAN_FRONTEND=noninteractive
RUN --mount=type=cache,target=/var/cache/apt \
    apt-get update  \
    && apt-get install -y curl gzip build-essential libz-dev zlib1g-dev

# Install Scala CLI
ARG SCALA_CLI_VERSION=1.1.2
RUN \
    curl -sSLf https://github.com/VirtusLab/scala-cli/releases/download/v${SCALA_CLI_VERSION}/scala-cli-x86_64-pc-linux.gz | gunzip -c > /usr/local/bin/scala-cli  \
    && chmod +x /usr/local/bin/scala-cli  \
    && /usr/local/bin/scala-cli version

FROM --platform=linux/amd64 build-env AS build
WORKDIR /src
# Copy local code to the container image
COPY . .
# Build a native binary with Scala CLI
ARG GRAALVM_VERSION=22
RUN \
    --mount=type=cache,target=/root/.cache/coursier \
    /usr/local/bin/scala-cli --power package . \
      --suppress-experimental-feature-warning \
      --server=false \
      --jvm=graalvm-java17 \
      --native-image \
      --graalvm-jvm-id=graalvm-java17:${GRAALVM_VERSION} \
      --graalvm-args="--static" \
      --main-class org.virtuslab.besom.example.main -o app -f

FROM --platform=linux/amd64 gcr.io/distroless/static
# Copy the binary to the production image from the builder stage
COPY --from=build /src/app /bin/app

# Run the web service on container startup.
CMD ["/bin/app"]
