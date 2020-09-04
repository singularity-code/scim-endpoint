package be.personify.iam.scim.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class LoadTest {
	
	
	private static final Logger logger = LogManager.getLogger(LoadTest.class);
	
	private int finished = 0;
	
	public static void main(String[] args) throws InterruptedException {
		
		String endpoint = args[0];
		String user = args[1];
		String password = args[2];
		int threads = Integer.parseInt(args[3]);
		
		LoadTest test = new LoadTest();
		
		System.out.println("starting load test to " + endpoint + " with " + threads + " threads");
		
		test.load(endpoint, user,password,threads, 1000);
		
		
		
	}

	private void load(String endpoint, String user, String password, int nrOfThreads, int nrOfRequests) throws InterruptedException {

		long mainStart = System.currentTimeMillis();
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(user, password);
		HttpEntity entity = new HttpEntity(headers);
		for ( int i = 0; i < nrOfThreads; i++ ) {
			final int zz = i;
			new Thread() {
				public void run() {
					long start = System.currentTimeMillis();
					for ( int j = 0; j < nrOfRequests; j++) {
						RestTemplate restTemplate = new RestTemplate();
						try {
							ResponseEntity<Object> response  = restTemplate.exchange(endpoint + "/Users/b18fd14b-3d68-4dd7-a22a-b0434c1f755d",HttpMethod.GET,entity, Object.class );
						}
						catch( Exception e ) {
							e.printStackTrace();
							break;
						}
					}
					System.out.println("thread [" + zz + "] " + nrOfRequests + " records processed in " + (System.currentTimeMillis() - start));
					finished++;
				}
			}.start();
		}
		
		while( finished != nrOfThreads) {
			Thread.sleep(100);
		}
		
		long ms = System.currentTimeMillis() - mainStart;
		System.out.println(nrOfRequests * nrOfThreads + " records processed in " + ms);
		System.out.println((nrOfRequests * nrOfThreads) / ( ms/1000) + " per second");
		
	}
	
	
	
	
	
	
	
	

}
