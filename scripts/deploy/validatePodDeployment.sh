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

kubectl logs orderservice-6dfd588fc4-2v5st --tail=100 -f
kubectl logs kafkabroker2-856f97f94b-6jrch --tail=100 -f

kubectl exec -it orderservice-574854d4b-2dwpc -- /bin/sh
kubectl get pod kafkabroker1-549b85bb49-sgtz9 -n default --template="{{.status.podIP}}"
nslookup kafkabrokersvc1.default.svc.cluster.local
dig kafkabroker1.default.pod.cluster.local

kubectl get configmap coredns -n kube-system -o yaml
kubectl get svc --all-namespaces
