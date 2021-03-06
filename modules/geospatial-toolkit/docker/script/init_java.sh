#!/bin/bash
set -e

echo
echo "***********************"
echo "*** Installing Java ***"
echo "***********************"
export SDKMAN_DIR=/usr/local/lib/sdkman
curl -s get.sdkman.io | bash
source "$SDKMAN_DIR/bin/sdkman-init.sh"
yes | sdk install java 8u161-oracle
sdk install groovy

echo 'source "$SDKMAN_DIR/bin/sdkman-init.sh"' >> /etc/profile