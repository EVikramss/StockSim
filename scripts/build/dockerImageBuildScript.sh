#!/bin/bash

BASE_DIR="/home/ec2-user/StockSim/modules"
account_id=$(aws sts get-caller-identity --query 'Account' --output text)
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin "$account_id".dkr.ecr.us-east-1.amazonaws.com

# Loop over each subdirectory in the base directory
for dir in "$BASE_DIR"/*/; do
    # Get the name of the immediate parent folder
    parent_folder=$(basename "$dir")
	cd $dir
	
	# clean target folder and run install
	export PATH=/home/ec2-user/apache-maven-3.9.4/bin:$PATH
	export MAVEN_OPTS="-Xmx512m"
	mvn clean
	mvn install
	
	# proceed further only if war file generated
	if [ -f target/"$parent_folder".war ]; then
		cp target/"$parent_folder".war .
		
		# Build the Docker image with the parent folder name as the tag
		docker build -t "$parent_folder" -f Dockerfile .
		
		if [ $? -eq 0 ]; then
			# if no issues with docker build push to ecr
			docker tag "$parent_folder" "$account_id".dkr.ecr.us-east-1.amazonaws.com/buildrepo:"$parent_folder"
			docker push "$account_id".dkr.ecr.us-east-1.amazonaws.com/buildrepo:"$parent_folder"
			
			existingProfile=$(aws eks describe-fargate-profile --cluster-name DeploymentCluster --fargate-profile-name "$parent_folder" --query "fargateProfile.fargateProfileName" --output text)
			if [ "$existingProfile" = "$parent_folder" ]; then
				echo 'profile exists'
			else
				aws eks create-fargate-profile --cluster-name DeploymentCluster --fargate-profile-name "$parent_folder" \
				--pod-execution-role-arn arn:aws:iam::"$account_id":role/eks-fargate-role \
				--selectors namespace=default,labels={app="$parent_folder"}
			fi
		else
			echo "Docker build failed."
			exit 1
		fi
	fi
done

# list fargate profiles
aws eks list-fargate-profiles --cluster-name DeploymentCluster