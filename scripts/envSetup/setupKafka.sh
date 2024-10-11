#!/bin/bash

account_id=$(aws sts get-caller-identity --query 'Account' --output text)
cluster_vpc_id=$(aws ec2 describe-vpcs --query "Vpcs[?Tags[?Key=='eksctl.cluster.k8s.io/v1alpha1/cluster-name' && Value=='DeploymentCluster']].VpcId" --output text)
cluster_vpc_cider_block=$(aws ec2 describe-vpcs --query "Vpcs[?Tags[?Key=='eksctl.cluster.k8s.io/v1alpha1/cluster-name' && Value=='DeploymentCluster']].CidrBlock" --output text)
cluster_vpc_subnets=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$cluster_vpc_id" --query "Subnets[].SubnetId" --output text)
no_of_kafka_brokers=2
kafka_uuid="a4RhP4phQs-ZlZX1sTk-jg"

if [ -z "$cluster_vpc_id" ]; then
    echo "Unable to fetch cluster_vpc_id. Rerun setupKafka.sh after cluster_vpc_id is available."
else
	eksctl create iamserviceaccount \
    --name efs-csi-controller-sa \
    --namespace kube-system \
    --cluster DeploymentCluster \
    --role-name csidriverrole \
    --role-only \
    --attach-policy-arn arn:aws:iam::aws:policy/service-role/AmazonEFSCSIDriverPolicy \
    --approve
	
	TRUST_POLICY=$(aws iam get-role --role-name csidriverrole --query 'Role.AssumeRolePolicyDocument' | \
    sed -e 's/efs-csi-controller-sa/efs-csi-*/' -e 's/StringEquals/StringLike/')
	
	aws iam update-assume-role-policy --role-name csidriverrole --policy-document "$TRUST_POLICY"

	helm repo add aws-efs-csi-driver https://kubernetes-sigs.github.io/aws-efs-csi-driver/
	helm repo update aws-efs-csi-driver
	#helm upgrade --install aws-efs-csi-driver --namespace kube-system aws-efs-csi-driver/aws-efs-csi-driver --set controller.serviceAccount.create=false --set controller.serviceAccount.name=csidriverrole --set deleteAccessPointRootDir=true
	#helm uninstall aws-efs-csi-driver --namespace kube-system

	kubectl apply -k "github.com/kubernetes-sigs/aws-efs-csi-driver/deploy/kubernetes/overlays/stable/ecr/?ref=release-2.0"
	#kubectl delete -k "github.com/kubernetes-sigs/aws-efs-csi-driver/deploy/kubernetes/overlays/stable/ecr/?ref=release-2.0"
	
	while true; do
		output=$(kubectl get pods -n kube-system -l app=efs-csi-node|grep efs-csi-node|head -n 1|awk '{print $2}')
		
		if [ "$output" = "3/3" ]; then
			echo 'csi pods running'
		break
		fi

		echo "Waiting for efs csi pods ..."
		sleep 10
	done

	# create efs file system
	WOF_EFS_FS_ID=$(aws efs create-file-system --creation-token kafkaFS --performance-mode generalPurpose --throughput-mode bursting --tags Key=Name,Value=kafkaFS --output text  --query "FileSystemId")
	echo "WOF_EFS_FS_ID"
	echo "$WOF_EFS_FS_ID"
	
	# wait for file system to be in available state
	while true; do
		output=$(aws efs describe-file-systems --file-system-id $WOF_EFS_FS_ID --query "FileSystems[0].LifeCycleState" --output text)
		
		if [ "$output" = "available" ]; then
			echo 'efs available'
		break
		fi

		echo "Waiting for efs ..."
		sleep 10
	done
	
	# add security group and add ingress rule (required for in vpc access ?)
	WOF_EFS_SG_ID=$(aws ec2 create-security-group --description kafkaFS --group-name kafkaFS --vpc-id $cluster_vpc_id --query 'GroupId' --output text)
	echo "WOF_EFS_SG_ID"
	echo "$WOF_EFS_SG_ID"
  
	aws ec2 authorize-security-group-ingress --group-id $WOF_EFS_SG_ID --protocol tcp --port 2049 --cidr $cluster_vpc_cider_block
		
	# add mount points on all subnets in VPC
	for subnet_id in $cluster_vpc_subnets; do (aws efs create-mount-target --file-system-id $WOF_EFS_FS_ID --subnet-id $subnet_id --security-group $WOF_EFS_SG_ID); done
		
	for i in $(seq 1 $no_of_kafka_brokers); do
		echo "Creating kafka profile, access point, PVC $i"
		
		aws eks create-fargate-profile --cluster-name DeploymentCluster --fargate-profile-name kafkaBroker$i --pod-execution-role-arn arn:aws:iam::"$account_id":role/eks-fargate-role --selectors namespace=default,labels={app=kafkaBroker$i}
		
		# create access point
		WOF_EFS_AP=$(aws efs create-access-point --file-system-id $WOF_EFS_FS_ID --posix-user Uid=1000,Gid=1000 --root-directory "Path=/kafkaDir$i,CreationInfo={OwnerUid=1000,OwnerGid=1000,Permissions=777}" --query 'AccessPointId' --output text)
		
		echo "WOF_EFS_AP"
		echo "$WOF_EFS_AP"
	
# create pcv inline
echo "
apiVersion: storage.k8s.io/v1
kind: CSIDriver
metadata:
  name: efs.csi.aws.com
spec:
  attachRequired: false
  podInfoOnMount: true
---
kind: StorageClass
apiVersion: storage.k8s.io/v1
metadata:
  name: efs-sc
provisioner: efs.csi.aws.com
---
apiVersion: v1
kind: PersistentVolume
metadata:
  name: kafkaefspv$i
spec:
  capacity:
    storage: 8Gi
  volumeMode: Filesystem
  accessModes:
    - ReadWriteMany
  persistentVolumeReclaimPolicy: Retain
  storageClassName: efs-sc
  csi:
    driver: efs.csi.aws.com
    volumeHandle: $WOF_EFS_FS_ID::$WOF_EFS_AP
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: kafkaefspvc$i
spec:
  accessModes:
    - ReadWriteMany
  storageClassName: efs-sc
  resources:
    requests:
      storage: 8Gi   
" | kubectl apply -f -

	while true; do
		output=$(kubectl get pv |grep kafkaefspv$i|awk '{print $5}')
		
		if [ "$output" = "Bound" ]; then
			echo 'persistent volumes bound'
		break
		fi
	
		echo "Waiting for persistent volume ..."
		sleep 10
	done
	
	while true; do
		output=$(aws eks describe-fargate-profile --cluster-name DeploymentCluster --fargate-profile-name kafkaBroker$i --query "fargateProfile.status" --output text)
		
		if [ "$output" = "ACTIVE" ]; then
			echo 'profile active'
		break
		fi
	
		echo "Waiting for fargate profile ..."
		sleep 10
	done

echo "
apiVersion: v1
kind: Service
metadata:
  name: kafkabrokersvc$i
spec:
  selector:
    app: kafkaBroker$i
  ports:
    - protocol: TCP
      name: port-9092
      port: 9092
      targetPort: 9092
    - protocol: TCP
      name: port-9093
      port: 9093
      targetPort: 9093
  type: ClusterIP
" | kubectl apply -f -

sleep 10

echo "apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafkabroker$i
  labels:
    app: kafkaBroker$i
    svccategory: kafka
spec:
  replicas: 1
  selector:
    matchLabels:
     app: kafkaBroker$i
  template:
    metadata:
      labels:
        app: kafkaBroker$i
    spec:
      containers:
        - name: kafkabroker$i
          image: public.ecr.aws/bitnami/kafka:3.7.0-debian-12-r0
          resources:
            requests:
              memory: '4Gi'
              cpu: '2'
            limits:
              memory: '4Gi'
              cpu: '2'
          imagePullPolicy: Always
          volumeMounts:
          - name: efs-storage
            mountPath: /bitnami/kafka
          ports:
          - containerPort: 9092
          - containerPort: 9093
          env:
          - name: KAFKA_CFG_NODE_ID
            value: '$i'
          - name: KAFKA_CFG_PROCESS_ROLES
            value: 'controller,broker'
          - name: KAFKA_CFG_CONTROLLER_QUORUM_VOTERS
            value: '1@kafkabrokersvc1.default.svc.cluster.local:9093,2@kafkabrokersvc2.default.svc.cluster.local:9093'
          - name: KAFKA_CFG_LISTENERS
            value: 'PLAINTEXT://:9092,CONTROLLER://:9093'
          - name: KAFKA_CFG_ADVERTISED_LISTENERS
            value: 'PLAINTEXT://kafkabrokersvc$i.default.svc.cluster.local:9092'
          - name: KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP
            value: 'CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT'
          - name: KAFKA_CFG_CONTROLLER_LISTENER_NAMES
            value: 'CONTROLLER'
          - name: KAFKA_CFG_INTER_BROKER_LISTENER_NAME
            value: 'PLAINTEXT'
          - name: KAFKA_KRAFT_CLUSTER_ID
            value: '$kafka_uuid'
          - name: KAFKA_TLS_CLIENT_AUTH
            value: 'none'
          - name: KAFKA_CFG_SASL_MECHANISM_CONTROLLER_PROTOCOL
            value: 'PLAIN'
          - name: ALLOW_PLAINTEXT_LISTENER
            value: 'yes'
      volumes:
      - name: efs-storage
        persistentVolumeClaim:
          claimName: kafkaefspvc$i
" | kubectl apply -f -

sleep 10
done

# setup headless service for brokers (clusterIP = None)
#echo "
#apiVersion: v1
#kind: Service
#metadata:
#  name: kafkaheadlesssvc
#spec:
#  clusterIP: None
#  selector:
#    svccategory: kafka
#  ports:
#    - protocol: TCP
#      name: port-9092
#      port: 9092
#      targetPort: 9092
#    - protocol: TCP
#      name: port-9093
#      port: 9093
#      targetPort: 9093
#"  | kubectl apply -f -

#dig kafkaheadlesssvc.default.svc.cluster.local

sleep 60
# Pods are getting stuck in efs mount failed. Reapplying here seems to solve the issue
kubectl apply -k "github.com/kubernetes-sigs/aws-efs-csi-driver/deploy/kubernetes/overlays/stable/ecr/?ref=release-2.0"

fi