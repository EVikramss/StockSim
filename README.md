


Prerequisites: aws cli, nodejs 22.5.1, aws cdk

1) Download the awscdk project & unzip it
2) Run 'aws configure' to setup access keys
3) Run 'for /f "delims=" %i in ('aws sts get-caller-identity --query "Account" --output text') do set ACCOUNT_ID=%i' to set accountID
4) Run 'for /f "delims=" %i in ('aws ec2 describe-vpcs --query "Vpcs[?IsDefault==`true`].VpcId" --output text') do set VPC_ID=%i' to set the default vpc
5) From the awscdk project root directory, run the below commands - 
	a) cdk bootstrap
	b) cdk deploy (enter y when prompted - deployment should complete within 3 minutes) or 
	c) cdk synth - to get the cloudformation template for review and manual creation.
6) Once deployment is complete, login to the ec2 instance (BuildBox) from aws console when instance status shows as checks passed.
7) Navigate to '/home/ec2-user/artifacts/scripts/envSetup' and run 'aws configure' to setup access keys. Then run 'nohup ./setupEnv.sh &'
	Note: In case artifacts folder is not present, follow below steps to set it up (replace aws_account_id with the aws account id value)
		cd /home/ec2-user
		wget https://github.com/EVikramss/StockSim/archive/refs/heads/main.zip
		unzip -qq main.zip
		mv StockSim-main/ artifacts
		sudo chmod -R 777 artifacts
		find /home/ec2-user/artifacts/scripts/deploy/. -type f -name "*.yaml" -exec sed -i 's/{{account_id}}/aws_account_id/g' {} \;

	a) Enter access id and secret id when prompted
	b) Set region to us-east-1
	
	The script sets up the EKS cluster which takes around 20 - 25 minutes.
8) Navigate to '/home/ec2-user/artifacts/scripts/build' and run 'dockerImageBuildScript.sh' which will build the docker image and upload to ECR repo & create fargate profile for each module.
9) Once build is complete, navigate to '/home/ec2-user/artifacts/scripts/deploy' and run 'dockerImageBuildScript.sh'
10) Run 'kubectl describe ingress/alb-ingress-rules' to check load balancer status.
11) Wait for LB to provision in LoadBalancers(EC2) screen. Access http://LBDNS from browser.

Note: 
A) In case of errors during setup 'couldn't get current server API group list: ... no kind "ExecCredential" is registered for version "client.authentication.k8s.io/v1alpha1" ', kill the script (ps -ef|grep setup and use kill cmd) and check that aws version is updated correctly (aws --version)
B) Mounting EFS onto kafka broker nodes might take some time, if still in container creating status for more than 5 min, run below command
	kubectl apply -k "github.com/kubernetes-sigs/aws-efs-csi-driver/deploy/kubernetes/overlays/stable/ecr/?ref=release-2.0"
	
