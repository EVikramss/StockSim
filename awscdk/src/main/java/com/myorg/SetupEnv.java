package com.myorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.Size;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.CfnVolumeAttachment;
import software.amazon.awscdk.services.ec2.IKeyPair;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Instance;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceProps;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.KeyPair;
import software.amazon.awscdk.services.ec2.MachineImage;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SecurityGroupProps;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.UserData;
import software.amazon.awscdk.services.ec2.Volume;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.IInstanceEngine;
import software.amazon.awscdk.services.rds.ParameterGroup;
import software.amazon.awscdk.services.rds.ParameterGroupProps;
import software.amazon.awscdk.services.rds.PostgresEngineVersion;
import software.amazon.awscdk.services.rds.PostgresInstanceEngineProps;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.amazon.awscdk.services.ssm.StringParameterProps;
import software.constructs.Construct;

public class SetupEnv extends Stack {

	/**
	 * Create a stack for build box.
	 * 
	 * @param scope
	 * @param id
	 * @param props
	 * @param env
	 * @throws Exception
	 */
	public SetupEnv(final Construct scope, final String id, final StackProps props, Map<String, String> envMap)
			throws Exception {
		super(scope, id, props);

		// get VPC
		IVpc vpcRef = Vpc.fromLookup(this, "VPC", VpcLookupOptions.builder().vpcId(envMap.get("VPCID")).build());

		// create security group
		SecurityGroup securityGroup = new SecurityGroup(this, "BuildBoxSecurityGroup",
				SecurityGroupProps.builder().vpc(vpcRef).allowAllOutbound(true).build());
		securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(22), "Allow SSH access from anywhere");

		// generate ec2key
		IKeyPair ec2KeyPair = KeyPair.Builder.create(this, envMap.get("ec2KeyName"))
				.keyPairName(envMap.get("ec2KeyName")).build();

		List<String> commandList = new ArrayList<String>();
		// commands to run on instance startup - install java, docker, 7zip and add
		// perms.
		UserData userData = UserData.forLinux();

		// Create role for ec2 box
		Role ec2Role = Role.Builder.create(this, "ec2BuildBoxRole").roleName("ec2BuildBoxRole")
				.assumedBy(new ServicePrincipal("ec2.amazonaws.com")).build();
		String buildRepoName = "buildrepo";

		// IBucket bucket = Bucket.fromBucketName(this, "buildBucket", bucketName);
		// create ECR repo to hold docker images
		Repository repository = Repository.Builder.create(this, buildRepoName).repositoryName(buildRepoName)
				.imageScanOnPush(true).build();
		repository.grantRead(ec2Role);
		repository.grantPullPush(ec2Role);

		// commands to download and setup artifacts
		setEC2CommandList(envMap, commandList);

		// grant eks related perms
		ec2Role.addToPolicy(
				PolicyStatement.Builder.create()
						.actions(Arrays.asList("eks:ListFargateProfiles", "eks:CreateFargateProfile",
								"eks:DeleteFargateProfile", "eks:DescribeFargateProfile", "iam:CreateServiceLinkedRole",
								"iam:GetRole", "iam:PassRole", "eks:DescribeCluster", "cloudformation:ListStacks",
								"eks:ListClusters"))
						.resources(Arrays.asList("*")).build());
		userData.addCommands(commandList.toArray(new String[0]));

		// create EC2 instance
		InstanceProps instanceProps = InstanceProps.builder().instanceName("BuildBox")
				.availabilityZone(envMap.get("availabilityZone"))
				.instanceType(InstanceType.of(InstanceClass.T2, InstanceSize.MICRO)).keyPair(ec2KeyPair)
				.userData(userData).machineImage(MachineImage.latestAmazonLinux2()).vpc(vpcRef).role(ec2Role)
				.securityGroup(securityGroup).build();
		Instance buildInstance = new Instance(this, id, instanceProps);

		// Create and attach volume to ec2 instance
		Volume rootVolume = Volume.Builder.create(this, "BuildBoxRootVolume")
				.availabilityZone(envMap.get("availabilityZone")).size(Size.gibibytes(8)).encrypted(false).build();
		CfnVolumeAttachment.Builder.create(this, "RootVolumeAttachment").instanceId(buildInstance.getInstanceId())
				.volumeId(rootVolume.getVolumeId()).device("/dev/sdh").build();

		// create inline policy for fargate
		Map<String, PolicyDocument> fargateInlinePolicies = new HashMap<String, PolicyDocument>();
		fargateInlinePolicies.put("NetworkAndLoggingPolicy",
				PolicyDocument.Builder.create()
						.statements(Arrays.asList(PolicyStatement.Builder.create()
								.actions(Arrays.asList(new String[] { "ec2:CreateNetworkInterface",
										"ec2:DescribeNetworkInterfaces", "ec2:DeleteNetworkInterface",
										"logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents" }))
								.resources(Arrays.asList("*")).effect(Effect.ALLOW).build()))
						.build());

		// create role for fargate
		Role eksFargateRole = Role.Builder.create(this, "eks-fargate-role").roleName("eks-fargate-role")
				.assumedBy(new ServicePrincipal("eks-fargate-pods.amazonaws.com")).description("eks-fargate-role")
				.inlinePolicies(fargateInlinePolicies).build();
		eksFargateRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2ContainerRegistryReadOnly"));
		eksFargateRole
				.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AmazonEKSFargatePodExecutionRolePolicy"));

		// create DB
		PostgresInstanceEngineProps dbProps = PostgresInstanceEngineProps.builder()
				.version(PostgresEngineVersion.VER_16_2).build();
		IInstanceEngine engine = DatabaseInstanceEngine.postgres(dbProps);

		ParameterGroup pg = new ParameterGroup(this, "rdspg", ParameterGroupProps.builder().engine(engine).build());
		pg.addParameter("rds.force_ssl", "0");

		SecurityGroup sg = new SecurityGroup(this, "dbSecurityGroup", SecurityGroupProps.builder().vpc(vpcRef).build());
		// sg.addIngressRule(Peer.anyIpv4(), Port.allTraffic());
		List<SecurityGroup> sgList = new ArrayList<SecurityGroup>();
		sgList.add(sg);

		// username is postgres, password is password
		// set publiclyAccessible as false so that coreDNS(via route 53) can resolve the
		// rds endpoint to private ip (and route via vpc peering as per route table entry) instead of public IP
		DatabaseInstance instance = DatabaseInstance.Builder.create(this, "rdsdb").databaseName("rdsdb").engine(engine)
				.parameterGroup(pg).instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.SMALL))
				.publiclyAccessible(false)
				.credentials(Credentials.fromPassword("postgres", new SecretValue("password"))).vpc(vpcRef)
				.securityGroups(sgList).vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build())
				.preferredBackupWindow("01:00-02:00").build();

		Secret dbCreds = Secret.Builder.create(this, "test/postgres/creds")
				.secretStringValue(SecretValue.unsafePlainText("{\"username\":\"postgres\",\"password\":\"password\","
						+ "\"host\":\"" + instance.getDbInstanceEndpointAddress() + "\"," + "\"port\":\""
						+ instance.getDbInstanceEndpointPort() + "\"," + "\"dbInstanceIdentifier\":\"db\"}"))
				.secretName("test/postgres/creds").build();
	}

	private void setEC2CommandList(Map<String, String> envMap, List<String> commandList) {

		// install java, docker, 7zip & git
		commandList.add("sudo yum install -y java");
		commandList.add("sudo yum install -y docker");
		commandList.add("sudo yum install -y git");
		commandList.add("sudo service docker start");
		commandList.add("sudo usermod -a -G docker ec2-user");
		commandList.add("sudo amazon-linux-extras install -y epel");
		commandList.add("sudo yum install p7zip -y");
		commandList.add("sudo ln -s /usr/bin/7za /usr/bin/7z");

		// install maven
		commandList.add("cd /home/ec2-user");
		commandList
				.add("wget https://archive.apache.org/dist/maven/maven-3/3.9.4/binaries/apache-maven-3.9.4-bin.tar.gz");
		commandList.add("tar -zxf apache-maven-3.9.4-bin.tar.gz");

		// download artifacts & setup folders
		commandList.add("git clone https://github.com/EVikramss/StockSim.git");
		commandList.add("chmod -R 777 StockSim");
		String accountID = envMap.get("accountID");
		commandList.add(
				"find /home/ec2-user/StockSim/scripts/deploy/. -type f -name \"*.yaml\" -exec sed -i 's/{{account_id}}/"
						+ accountID + "/g' {} \\;");
	}
}
