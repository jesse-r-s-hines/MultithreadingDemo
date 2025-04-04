# Base for both builder and app images
FROM ubuntu:24.04 AS base

# Prevent apt-get from prompting for geographic area and the like
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get upgrade -y
RUN apt-get install -y apt-transport-https libicu-dev


FROM base AS builder

RUN apt-get install -y npm
RUN apt-get install -y curl wget make sqlite3 openjdk-17-jdk-headless

# Install sbt (https://www.scala-sbt.org/download.html)
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
RUN apt-get update -y
RUN apt-get install -y sbt
# Run sbt once so it already has the sbt launcher downloaded
RUN cd /root && sbt --version

# Install dotnet sdk (https://learn.microsoft.com/en-us/dotnet/core/install/linux-ubuntu-2204)
RUN apt-get install -y dotnet-sdk-8.0
# dotnet-ef isn't installed by default anymore (and we have to use `dotnet-ef` not `dotnet ef`)
ENV PATH=$PATH:/root/.dotnet/tools
RUN dotnet tool install --global dotnet-ef --version=8.0.14

COPY . /multithreading-demo
WORKDIR /multithreading-demo

RUN make publish RID="linux-x64"


FROM base AS app

COPY --from=builder /multithreading-demo/Server/bin/Release/publish ./multithreading-demo
WORKDIR /multithreading-demo

EXPOSE 80 443

CMD ["/multithreading-demo/Server"]
