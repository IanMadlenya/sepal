#!/usr/bin/env bash


REGION=$1
AV_ZONE=$2
ENV=$3
CONTEXT_DIR=${5:-"../.."}
VERSION=${4:-latest}
INVENTORY_FILE=${6:-"ec2.py"}
PRIVATE_KEY=${7:-"~/.ssh/sepal/$REGION.pem"}

export ANSIBLE_HOST_KEY_CHECKING=False
export ANSIBLE_CONFIG=${CONTEXT_DIR}/ansible.cfg

#to make the provisioning script works locally. Create a symlink from ProjectRoot to /opt/sepal

INVENTORY_FILE_PATH="$CONTEXT_DIR"/sepal/inventory/"$INVENTORY_FILE_NAME"

echo "using inventory file $INVENTORY_FILE_PATH"
echo "using private key $PRIVATE_KEY"

ansible-playbook ${CONTEXT_DIR}/sepal/setup.yml \
    -i ${INVENTORY_FILE_PATH} \
    --private-key=${PRIVATE_KEY}  \
    --extra-vars "region=$REGION availability_zone=$AV_ZONE deploy_environment=$ENV"

${INVENTORY_FILE_PATH} --refresh-cache > /dev/null

ansible-playbook ${CONTEXT_DIR}/sepal/sepal.yml \
    -i ${INVENTORY_FILE_PATH} \
    --private-key=${PRIVATE_KEY} \
    --extra-vars " region=$REGION availability_zone=$AV_ZONE local_php=false local_sepal=false deploy_environment=$ENV version=$VERSION use_custom_host=false secret_vars_file=~/.sepal/secret.yml"