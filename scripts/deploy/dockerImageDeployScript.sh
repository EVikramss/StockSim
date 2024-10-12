#!/bin/bash

account_id=$(aws sts get-caller-identity --query 'Account' --output text)

# register cluster name
aws eks update-kubeconfig --name DeploymentCluster
#eksctl get cluster

sed -i "s/{{account_id}}/${account_id}/g" deploy.yaml
kubectl apply -f deploy.yaml

#show pods
kubectl get pod
kubectl get svc

kubectl describe ingress/alb-ingress-rules

# list fargate profiles
#aws eks list-fargate-profiles --cluster-name DeploymentCluster