FROM ubuntu:22.04

ARG HOME=/root

ARG ANTLR_VERSION=4.13.2

# 0. Set up mirrors and install basic tools
RUN sed -i s@/archive.ubuntu.com/@/mirrors.tuna.tsinghua.edu.cn/@g /etc/apt/sources.list
RUN sed -i s@/security.ubuntu.com/@/mirrors.tuna.tsinghua.edu.cn/@g /etc/apt/sources.list
ARG DEBIAN_FRONTEND=noninteractive
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    build-essential bsdmainutils openjdk-21-jdk \
    git curl zip
RUN rm -rf /var/lib/apt/lists/*

# Ready to go
WORKDIR /mnt