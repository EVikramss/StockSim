#!/bin/bash

BASE_DIR="/home/ec2-user/deploymentFolder/modules"
account_id=$(aws sts get-caller-identity --query 'Account' --output text)

# Loop over each subdirectory in the base directory
for dir in "$BASE_DIR"/*/; do
    # Get the name of the immediate parent folder
    parent_folder=$(basename "$dir")
	cd $dir

    # Build the Docker image with the parent folder name as the tag
    docker build -t "$parent_folder" -f Dockerfile .
	
	aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin "$account_id".dkr.ecr.us-east-1.amazonaws.com
	docker tag "$parent_folder" "$account_id".dkr.ecr.us-east-1.amazonaws.com/buildrepo:"$parent_folder"
	docker push "$account_id".dkr.ecr.us-east-1.amazonaws.com/buildrepo:"$parent_folder"
	
	aws eks create-fargate-profile --cluster-name DeploymentCluster --fargate-profile-name "$parent_folder" \
    --pod-execution-role-arn arn:aws:iam::"$account_id":role/eks-fargate-role \
    --selectors namespace="$parent_folder",labels={app="$parent_folder"}

done

# list fargate profiles
aws eks list-fargate-profiles --cluster-name DeploymentCluster