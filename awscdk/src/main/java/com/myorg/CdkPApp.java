package com.myorg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

		String accountID = readAccountID();
		String vpcID = readVPCID();

		if (accountID == null || accountID.trim().length() == 0)
			// for /f "delims=" %i in ('aws sts get-caller-identity --query "Account"
			// --output text') do set ACCOUNT_ID=%i
			throw new Exception(
					"No accountID. Set manually in code. Run : for /f \"delims=\" %i in ('aws sts get-caller-identity --query \"Account\" --output text') do set ACCOUNT_ID=%i");
		if (vpcID == null || vpcID.trim().length() == 0)
			// for /f "delims=" %i in ('aws ec2 describe-vpcs --query
			// "Vpcs[?IsDefault==`true`].VpcId" --output text') do set VPC_ID=%i
			throw new Exception(
					"No VPCID. Set manually in code. Run : for /f \"delims=\" %i in ('aws ec2 describe-vpcs --query \"Vpcs[?IsDefault==`true`].VpcId\" --output text') do set VPC_ID=%i");

		// env map to hold required attributes
		Map<String, String> envMap = new HashMap<String, String>();
		envMap.put("accountID", accountID);
		envMap.put("region", "us-east-1");
		envMap.put("availabilityZone", "us-east-1c");
		envMap.put("ec2KeyName", "ec2Key");
		envMap.put("VPCID", vpcID);

		/*
		 * new CdkPStack(app, "CdkPStack", StackProps.builder()
		 * .env(Environment.builder().account(env.get("accountID")).region(env.get(
		 * "region")).build()).build(), env);
		 */

		new SetupEnv(app, "CdkBuildBoxStack",
				StackProps.builder().env(
						Environment.builder().account(envMap.get("accountID")).region(envMap.get("region")).build())
						.build(),
				envMap);

		app.synth();
	}

	private static String readAccountID() throws Exception {

		String account_id = null;
		Process process = Runtime.getRuntime().exec("aws sts get-caller-identity --query \"Account\" --output text");

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			if ((line = reader.readLine()) != null) {
				account_id = line.trim();
			}
		}
		
		int exitStatus = process.waitFor();

		return account_id;
	}
	
	private static String readVPCID() throws Exception {

		String vpc_id = null;
		Process process = Runtime.getRuntime().exec("aws ec2 describe-vpcs --query \"Vpcs[?IsDefault==`true`].VpcId\" --output text");

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			if ((line = reader.readLine()) != null) {
				vpc_id = line.trim();
			}
		}
		
		int exitStatus = process.waitFor();

		return vpc_id;
	}
}
