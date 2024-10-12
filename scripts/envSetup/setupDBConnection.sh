#!/bin/bash

# References : https://dev.to/bensooraj/accessing-amazon-rds-from-aws-eks-2pc3#lets-build-the-bridge

account_id=$(aws sts get-caller-identity --query 'Account' --output text)
cluster_vpc_id=$(aws ec2 describe-vpcs --query "Vpcs[?Tags[?Key=='eksctl.cluster.k8s.io/v1alpha1/cluster-name' && Value=='DeploymentCluster']].VpcId" --output text)
cluster_vpc_cider_block=$(aws ec2 describe-vpcs --query "Vpcs[?Tags[?Key=='eksctl.cluster.k8s.io/v1alpha1/cluster-name' && Value=='DeploymentCluster']].CidrBlock" --output text)
cluster_vpc_subnets=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$cluster_vpc_id" --query "Subnets[].SubnetId" --output text)
OIDC_PROVIDER=$(aws eks describe-cluster --name DeploymentCluster --query "cluster.identity.oidc.issuer" --output text | sed -e "s/^https:\/\///")

if [ -z "$cluster_vpc_id" ]; then
    echo "Unable to fetch cluster_vpc_id. Rerun setupKafka.sh after cluster_vpc_id is available."
else
	# assume only 1 rds instance
	rds_vpc_id=$(aws rds describe-db-instances --query "DBInstances[0].DBSubnetGroup.VpcId" --output text)
	rdc_vpc_cider_block=$(aws ec2 describe-vpcs --vpc-ids $rds_vpc_id --query "Vpcs[0].CidrBlock" --output text)
	rdc_vpc_security_group_id=$(aws rds describe-db-instances --query "DBInstances[0].VpcSecurityGroups[0].VpcSecurityGroupId" --output text)
	rdc_endpoint=$(aws rds describe-db-instances --query "DBInstances[0].Endpoint.Address" --output text)
	rdc_port=$(aws rds describe-db-instances --query "DBInstances[0].Endpoint.Port" --output text)

	if [ -z "$rds_vpc_id" ]; then
		vpcPeeringConnectionId=$(aws ec2 create-vpc-peering-connection --vpc-id $cluster_vpc_id --peer-vpc-id $rds_vpc_id --query "VpcPeeringConnection.VpcPeeringConnectionId" --output text)

		while true; do
			output=$(aws ec2 describe-vpc-peering-connections --vpc-peering-connection-ids $vpcPeeringConnectionId --query "VpcPeeringConnections[0].Status.Code" --output text)
		
			if [ "$output" = "pending-acceptance" ]; then
				echo 'provide acceptance'
			break
			fi

			echo "Waiting for peering connection ..."
			sleep 10
		done

		aws ec2 accept-vpc-peering-connection --vpc-peering-connection-id $vpcPeeringConnectionId

		while true; do
			output=$(aws ec2 describe-vpc-peering-connections --vpc-peering-connection-ids $vpcPeeringConnectionId --query "VpcPeeringConnections[0].Status.Code" --output text)
		
			if [ "$output" = "active" ]; then
				echo 'peering connection active'
			break
			fi

			echo "Waiting for peering connection activation ..."
			sleep 10
		done
		
		rdc_route_table_id=$(aws ec2 describe-route-tables --filters "Name=vpc-id,Values=$rds_vpc_id" --query "RouteTables[0].RouteTableId" --output text)
		cluster_route_table_id=$(aws ec2 describe-route-tables --filters "Name=vpc-id,Values=$cluster_vpc_id" --query "RouteTables[0].RouteTableId" --output text)

		if [ -z "$rdc_route_table_id" ] && [ -z "$cluster_route_table_id" ]; then
			aws ec2 create-route --route-table-id $rdc_route_table_id --destination-cidr-block $cluster_vpc_cider_block --vpc-peering-connection-id $vpcPeeringConnectionId
			aws ec2 create-route --route-table-id $cluster_route_table_id --destination-cidr-block $rdc_vpc_cider_block --vpc-peering-connection-id $vpcPeeringConnectionId
			
			# add security group 
			aws ec2 authorize-security-group-ingress --group-id ${rdc_vpc_security_group_id} --protocol tcp --port $rdc_port --cidr $cluster_vpc_cider_block
			
			if [ -z "$rdc_endpoint" ]; then
			
echo "
apiVersion: v1
kind: Service
metadata:
  labels:
    app: rdsservice
  name: rdsservice
spec:
  externalName: $rdc_endpoint
  selector:
    app: rdsservice
  type: ExternalName
status:
  loadBalancer: {}
" | kubectl apply -f -

			else
				echo "unable to fetch rds endpoint"
			fi
		else
			echo "Unable to fetch routeID for VPC's."
		fi

	fi
fi