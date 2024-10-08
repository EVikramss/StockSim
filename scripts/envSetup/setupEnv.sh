#!/bin/bash
aws configure
account_id=$(aws sts get-caller-identity --query 'Account' --output text)

# if version diff
current_version=$(aws --version 2>&1)
expected_version="aws-cli/2.17.61 Python/3.12.6 Linux/5.10.225-213.878.amzn2.x86_64 exe/x86_64.amzn.2"
if [ "$current_version" != "$expected_version" ]; then
	curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
	unzip -qq awscliv2.zip
	sudo ./aws/install --bin-dir /usr/bin --install-dir /usr/bin/aws-cli --update
	rm -rf aws
	rm -rf awscliv2.zip
else
    echo "AWS CLI already latest."
fi

# install kubectl
if command -v kubectl &> /dev/null
then
    echo "kubectl is installed."
else
    curl -O https://s3.us-west-2.amazonaws.com/amazon-eks/1.30.2/2024-07-12/bin/linux/amd64/kubectl
	chmod +x ./kubectl
	mkdir -p $HOME/bin && cp ./kubectl $HOME/bin/kubectl && export PATH=$PATH:$HOME/bin
	rm -rf kubectl
fi

# install eksctl
if command -v eksctl &> /dev/null
then
    echo "eksctl is installed."
else
    curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
	sudo mv /tmp/eksctl /usr/local/bin
fi

# create cluster if it doesnt exist
if eksctl get cluster --name DeploymentCluster &> /dev/null
then
    echo "DeploymentCluster already exists"
else
    eksctl create cluster --name DeploymentCluster --fargate --with-oidc --nodes-max 5 --enable-ssm --node-private-networking --managed --asg-access --external-dns-access --full-ecr-access --appmesh-access --alb-ingress-access
	
	# install application load balancer and start pods
	./setupLBControllerInCluster.sh
	
	# setup kafka
	# sleep 10
	# ./setupKafka.sh
fi