#!/bin/bash

#get IP of pods
#kubectl get pods -o wide

kubectl port-forward pod/orderservice-55c9d559fc-9mx5w 8101:8101 &
curl http://localhost:9101

kubectl port-forward service/orderservicelb 5101:8101 &
curl http://localhost:5101

#kubectl get pod orderservice-58776dc58c-psfgb -o jsonpath='{.spec.containers[*].name}'
kubectl exec orderservice-58776dc58c-psfgb -c orderservice -- cat /etc/hosts

#kubectl config get-contexts
#kubectl config use-context arn:aws:eks:us-east-1:767397700844:cluster/DeploymentCluster2

