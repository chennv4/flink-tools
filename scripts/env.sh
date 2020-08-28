# Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0

# Load environment variables from env-local.sh if it exists.i
ROOT_DIR=$(readlink -f $(dirname $0)/..)
export ENV_LOCAL_SCRIPT=$(dirname $0)/env-local.sh
if [[ -f ${ENV_LOCAL_SCRIPT} ]]; then
    source ${ENV_LOCAL_SCRIPT}
fi
export APP_GROUP_ID=${APP_GROUP_ID:-io.pravega}
export APP_ARTIFACT_ID=${APP_ARTIFACT_ID:-flink-tools}
export APP_VERSION=${APP_VERSION:-0.2.0}
export GRADLE_OPTIONS="${GRADLE_OPTIONS:-"-PincludeHadoopS3=false -Pversion=${APP_VERSION}"}"
export FLINK_IMAGE_TAG="1.10.0-2.12-1.2-W2-4-0577915d2"
export NEW_IMAGE_TAG="${FLINK_IMAGE_TAG}-hadoop2.8.3"
export DOCKER_IMAGE_TAR=${ROOT_DIR}/build/flink-${NEW_IMAGE_TAG}.tar
export SDP_INSTALL_PATH=$HOME/nautilus-install/
export SDP_INSTALL_SCRIPT=$HOME/nautilus-install/decks-install-linux-amd64
export CERTS_PATH=$HOME/certs
export DOCKER_REGISTRY=sdpmicro:31001/desdp
