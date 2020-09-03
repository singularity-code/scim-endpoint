package be.personify.iam.scim.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoadTest {
	
	
	private static final Logger logger = LogManager.getLogger(LoadTest.class);
	
	public static void main(String[] args) {
		
		
		
		String endpoint = args[0];
		String user = args[1];
		String password = args[2];
		int threads = Integer.parseInt(args[3]);
		
		
		LoadTest test = new LoadTest();
		
		System.out.println("starting load test to " + endpoint + " with " + threads + " threads");
		
		test.load(endpoint, user,password,threads);
		
		
		
	}

	private void load(String endpoint, String user, String password, int nrOfThreads) {

		
	}
	
	
	
	
	
	

}
