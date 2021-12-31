# Base for both builder and app images
FROM ubuntu:20.04 AS base

# Prevent apt-get from prompting for geographic area and the like
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get upgrade -y
RUN apt-get install -y apt-transport-https openjdk-17-jdk-headless


FROM base AS builder

RUN apt-get install -y npm
RUN apt-get install -y curl wget make

# Install sbt (https://www.scala-sbt.org/download.html)
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
RUN apt-get update -y
RUN apt-get install -y sbt
# Run sbt once so it already has the sbt launcher downloaded
RUN cd /root && sbt --version

# Install dotnet sdk (https://docs.microsoft.com/en-us/dotnet/core/install/linux-ubuntu#2004-)
RUN wget -q https://packages.microsoft.com/config/ubuntu/20.04/packages-microsoft-prod.deb -O packages-microsoft-prod.deb && \
    dpkg -i packages-microsoft-prod.deb
RUN apt-get update -y
RUN apt-get install -y dotnet-sdk-6.0

COPY . /multithreading-demo
WORKDIR /multithreading-demo

RUN make publish RID="linux-x64"


FROM base as app

COPY --from=builder /multithreading-demo/Server ./multithreading-demo
WORKDIR /multithreading-demo

EXPOSE 80 443

CMD ["/multithreading-demo/bin/Release/publish/Server"]
