#!/bin/bash

# get IP of pods
kubectl get pods -o wide
kubectl get pod kafkabroker1-549b85bb49-sgtz9 -n default --template="{{.status.podIP}}"

# port foward pod to localhost to test
kubectl port-forward pod/orderservice-55c9d559fc-9mx5w 8101:8101 &
curl http://localhost:9101

# port foward service to localhost to test
kubectl port-forward service/orderservicelb 5101:8101 &
curl http://localhost:5101

# get pod container name
kubectl get pod orderservice-58776dc58c-psfgb -o jsonpath='{.spec.containers[*].name}'

# run commands on pod
kubectl exec orderservice-69847c86dc-dh5k4 -c orderservice -- cat /etc/hosts
kubectl exec -it orderservice-7848446467-tmdsb -- /bin/sh

#kubectl config get-contexts
#kubectl config use-context arn:aws:eks:us-east-1:767397700844:cluster/DeploymentCluster2

# tail logs from pod
kubectl logs rds-chart-1728647900-7664fb59f7-7bpf9 --tail=100 -f

# lookup dns name(from inside pods)
nslookup kafkabrokersvc1.default.svc.cluster.local
dig kafkabroker1.default.pod.cluster.local

# get coredns config
kubectl get configmap coredns -n kube-system -o yaml

# list pods/services in all namespaces
kubectl get svc --all-namespaces
kubectl get pods --all-namespaces

# display ingress rules
kubectl describe ingress/alb-ingress-rules

# busybox pod to run cmds
kubectl run -i --tty --rm debug --image=busybox --restart=Never -- sh
/ # nc rdsservice 5432

# rollout restart deployments
kubectl rollout restart deployment orderservic

# scale deployments
kubectl scale deployment my-app --replicas=5
