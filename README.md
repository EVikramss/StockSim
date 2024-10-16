


Prerequisites: aws cli, nodejs 22.5.1, aws cdk

1) Download the awscdk project & unzip it
   
2) Run 'aws configure' to setup access keys
   
3) From the awscdk project root directory, run the below commands -
   
	a) cdk bootstrap

	b) cdk deploy (enter y when prompted - deployment should complete within 10 minutes) or

	c) cdk synth - to get the cloudformation template for review and manual creation.

    This step creates a t2.micro ec2     instance with code artifacts and a     postgres db.

4) Once deployment is complete, login to the ec2 instance (BuildBox) from aws console when instance status shows as checks passed.
   
5) Navigate to '/home/ec2-user/artifacts/scripts' and run 'aws configure' to setup access keys. Then run 'nohup ./createEnvBuildAndDeploy.sh &'
	
	The script sets up the EKS cluster, kafka pods and DB connections. It also builds and deploys the application along with internet facing load balancer. The entire process takes around 50 minutes.

6) Run 'kubectl describe ingress/alb-ingress-rules' to check load balancer status.
   
7) Wait for LB to provision in LoadBalancers(EC2) screen. Access http://LBDNS from browser.
	
