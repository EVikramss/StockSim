#!/bin/bash
# run only once cluster is setup - get vpc id of cluster

account_id=$(aws sts get-caller-identity --query 'Account' --output text)
cluster_vpc_id=$(aws ec2 describe-vpcs --query "Vpcs[?Tags[?Key=='eksctl.cluster.k8s.io/v1alpha1/cluster-name' && Value=='DeploymentCluster']].VpcId" --output text)

if [ -z "$cluster_vpc_id" ]; then
    echo "Unable to fetch cluster_vpc_id. Rerun setupLBControllerInCluster.sh after cluster_vpc_id is available."
else
	# create IAM policy	
	curl -O https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.7.2/docs/install/iam_policy.json

	aws iam create-policy \
	--policy-name AWSLoadBalancerControllerIAMPolicy \
	--policy-document file://iam_policy.json
	
	# create service account in cluster
	eksctl utils associate-iam-oidc-provider --region=us-east-1 --cluster=DeploymentCluster --approve
	
	eksctl create iamserviceaccount \
	--cluster=DeploymentCluster \
	--namespace=kube-system \
	--name=aws-load-balancer-controller \
	--role-name AmazonEKSLoadBalancerControllerRole \
	--attach-policy-arn=arn:aws:iam::"$account_id":policy/AWSLoadBalancerControllerIAMPolicy \
	--override-existing-serviceaccounts \
	--approve
	
	
	# install helm
	curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
	chmod 700 get_helm.sh
	./get_helm.sh
	helm repo add eks https://aws.github.io/eks-charts
	helm repo update eks
	
	# install load balancer - specify vpc id from cluster config
	helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
	-n kube-system \
	--set clusterName=DeploymentCluster \
	--set serviceAccount.create=false \
	--set serviceAccount.name=aws-load-balancer-controller \
	--set region=us-east-1 \
	--set vpcId="$cluster_vpc_id"
	
	
	#helm search repo eks/aws-load-balancer-controller --versions
	
	#
	kubectl get deployment -n kube-system aws-load-balancer-controller
	
	echo "kubectl get deployment -n kube-system aws-load-balancer-controller"
	
	while true; do
		output=$(kubectl get deployment -n kube-system aws-load-balancer-controller|grep aws-load-balancer-controller|awk '{print $2}')
		
		if [ "$output" = "2/2" ]; then
			./configLBControllerInCluster.sh
		break
		fi

		# Wait for 10 seconds before the next iteration
		sleep 10
	done
fi
