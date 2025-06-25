#!/bin/bash

if [ "$(id -u)" -ne 0 ]; then
  echo "This script must be run as root. Please use 'sudo'." >&2
  exit 1
fi

echo "Updating package lists..."
apt-get update

echo "Installing required packages..."
apt-get install -y --no-install-recommends \
    build-essential \
    openjdk-21-jdk \
    git \
