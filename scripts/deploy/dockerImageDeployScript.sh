#!/bin/bash

BASE_DIR="/home/ec2-user/deploymentFolder/"
account_id=$(aws sts get-caller-identity --query 'Account' --output text)

# register cluster name
aws eks update-kubeconfig --name DeploymentCluster
#eksctl get cluster

kubectl apply -f deploy.yaml

#show pods
kubectl get pod
kubectl get svc

kubectl describe ingress/alb-ingress-rules

# list fargate profiles
#aws eks list-fargate-profiles --cluster-name DeploymentCluster