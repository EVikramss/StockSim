#!/bin/bash
# start only once setup is complete and controller pods are up and running

kubectl apply -f ingress-class.yaml

kubectl describe ingressclass/ingres-class