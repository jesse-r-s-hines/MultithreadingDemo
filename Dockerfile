FROM ubuntu:20.04

# Prevent apt-get from prompting for geographic aria nd the like
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get upgrade -y
RUN apt-get install -y gnupg curl wget apt-transport-https make
RUN apt-get install -y npm
RUN apt-get install -y openjdk-17-jdk-headless

# Install sbt (https://www.scala-sbt.org/download.html)
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
RUN apt-get update -y
RUN apt-get install -y sbt

# Install dotnet sdk (https://docs.microsoft.com/en-us/dotnet/core/install/linux-ubuntu#2004-)
RUN wget -q https://packages.microsoft.com/config/ubuntu/20.04/packages-microsoft-prod.deb -O packages-microsoft-prod.deb && \
    dpkg -i packages-microsoft-prod.deb
RUN apt-get update -y
RUN apt-get install -y dotnet-sdk-3.1

COPY . /threading-demo
WORKDIR /threading-demo
RUN make publish
WORKDIR /threading-demo/Server

ENV ASPNETCORE_URLS=http://*:80
EXPOSE 443 80

CMD ["/threading-demo/Server/bin/Release/netcoreapp3.1/publish/Server"]
