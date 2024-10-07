package com.myorg;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class CdkPApp {

	/**
	 * Generate required stack.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		App app = new App();

		String accountID = System.getenv("ACCOUNT_ID");
		String vpcID = System.getenv("VPC_ID");

		if (accountID == null || accountID.trim().length() == 0)
			// for /f "delims=" %i in ('aws sts get-caller-identity --query "Account" --output text') do set ACCOUNT_ID=%i
			throw new Exception(
					"No accountID. Run : for /f \"delims=\" %i in ('aws sts get-caller-identity --query \"Account\" --output text') do set ACCOUNT_ID=%i");
		if (vpcID == null || vpcID.trim().length() == 0)
			// for /f "delims=" %i in ('aws ec2 describe-vpcs --query "Vpcs[?IsDefault==`true`].VpcId" --output text') do set VPC_ID=%i
			throw new Exception(
					"No VPCID. Run : for /f \"delims=\" %i in ('aws ec2 describe-vpcs --query \"Vpcs[?IsDefault==`true`].VpcId\" --output text') do set VPC_ID=%i");

		// env map to hold required attributes
		Map<String, String> envMap = new HashMap<String, String>();
		envMap.put("accountID", accountID);
		envMap.put("region", "us-east-1");
		envMap.put("availabilityZone", "us-east-1c");
		envMap.put("ec2KeyName", "ec2Key");
		envMap.put("VPCID", vpcID);

		new SetupEnv(app, "CdkBuildBoxStack",
				StackProps.builder().env(
						Environment.builder().account(envMap.get("accountID")).region(envMap.get("region")).build())
						.build(),
				envMap);

		app.synth();
	}
}
