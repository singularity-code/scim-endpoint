package be.personify.iam.scim.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

public class LoadTest {
	
	
	private static final ObjectMapper objectMapper = new ObjectMapper();
	
	private static int getFinished = 0;
	private static int searchFinished = 0;
	private static int createFinished = 0;
	private static int deleteFinished = 0;
	
	private static int requests;
	
	private static final DecimalFormat format = new DecimalFormat("#.##");
	
	
	
	public static void main(String[] args) throws Exception {
		
		String endpoint = args[0];
		String user = args[1];
		String password = args[2];
		int threads = Integer.parseInt(args[3]);
		requests = Integer.parseInt(args[4]);
		
		LoadTest test = new LoadTest();
		
		System.out.println("starting load test to " + endpoint + " with " + threads + " threads and " + requests + " requests....");
		
		
		
		Map<Integer,List<Map<String,Object>>> threadMap = test.loadTestCreate(endpoint, user, password, threads, requests);
		
		while ( createFinished != threads ) {
			System.out.println("create finished " + createFinished);
			Thread.sleep(1000);
		}
		
		
		test.loadTestGet(endpoint, user, password, threadMap);
		
		
		
		while ( getFinished != threads ) {
			System.out.println("get finished " + getFinished);
			Thread.sleep(1000);
		}
		
		test.loadTestSearch(endpoint, user, password, threadMap);
		
		while ( searchFinished != threads ) {
			System.out.println("search finished " + getFinished);
			Thread.sleep(1000);
		}
		
		test.loadTestDelete(endpoint, user, password, threadMap);
		
		
		
		while ( deleteFinished != threads ) {
			System.out.println("deleteFinished finished " + deleteFinished);
			Thread.sleep(1000);
		}
		
		
		
		
	}

	
	
	
	
	
	
	
	
	private Map<Integer,List<Map<String,Object>>> loadTestCreate(String endpoint, String user, String password, int nrOfThreads, int nrOfRequests) throws Exception {

		String body = new String(readFileAsBytes(new FileInputStream(new File("src/test/resources/load_test_user_create.json"))));
		Map<String,Object> userObject = objectMapper.readValue(body, Map.class);
		
		
		Map<Integer,List<Map<String,Object>>> threadMap = new HashMap<>();
		for ( int i=0; i < nrOfThreads; i++) {
			List<Map<String,Object>> users = new ArrayList<>();
			for (int j=0; j < nrOfRequests; j++) {
				Map<String,Object> tt = new HashMap<>(userObject);
				tt.put(Constants.ID, UUID.randomUUID().toString());
				String identifier = "joske" + i + j; 
				tt.put("userName", identifier );
				tt.put("externalId", identifier);
				users.add(tt);
			}
			threadMap.put(i, users);
		}
		
		long mainStart = System.currentTimeMillis();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBasicAuth(user, password);
		
		for ( int i = 0; i < threadMap.size(); i++ ) {
			final int zz = i;
			final List<Map<String,Object>> userList = threadMap.get(i); 
			new Thread() {
				public void run() {
					long start = System.currentTimeMillis();
					for ( int j = 0; j < userList.size(); j++) {
						RestTemplate restTemplate = new RestTemplate();
						try {
							HttpEntity entity = new HttpEntity(userList.get(j),headers);
							ResponseEntity<Object> response  = restTemplate.exchange(endpoint + "/Users" ,HttpMethod.POST,entity, Object.class );
						}
						catch( Exception e ) {
							e.printStackTrace();
							break;
						}
					}
					System.out.println("thread [" + zz + "] " + nrOfRequests + " records processed in " + (System.currentTimeMillis() - start));
					createFinished++;
				}
			}.start();
		}
		
		while( createFinished != nrOfThreads) {
			Thread.sleep(100);
		}
		
		long ms = System.currentTimeMillis() - mainStart;
		System.out.println(nrOfRequests * nrOfThreads + " records processed in " + ms + " ms");
		double dd = ms/1000d;
		double dc = nrOfRequests * nrOfThreads;
		
		
		System.out.println("--------------- loadTestCreate() --- " +  format.format(dc / dd) + " req/sec");
		return threadMap;
		
	}
	
	
	
	
	
	
	
	private void loadTestGet(String endpoint, String user, String password, Map<Integer,List<Map<String,Object>>> threadMap) throws Exception {


		long mainStart = System.currentTimeMillis();
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(user, password);
		HttpEntity entity = new HttpEntity(headers);
		for ( int i = 0; i < threadMap.size(); i++ ) {
			final int zz = i;
			final List<Map<String,Object>> userList = threadMap.get(i); 
			new Thread() {
				public void run() {
					long start = System.currentTimeMillis();
					for ( int j = 0; j < userList.size(); j++) {
						RestTemplate restTemplate = new RestTemplate();
						try {
							ResponseEntity<Object> response  = restTemplate.exchange(endpoint + "/Users/" + userList.get(j).get(Constants.ID) ,HttpMethod.GET,entity, Object.class );
						}
						catch( Exception e ) {
							e.printStackTrace();
							break;
						}
					}
					System.out.println("thread [" + zz + "] " + userList.size() + " records processed in " + (System.currentTimeMillis() - start));
					getFinished++;
				}
			}.start();
		}
		
		while( getFinished != threadMap.size()) {
			Thread.sleep(100);
		}
		
		long ms = System.currentTimeMillis() - mainStart;
		System.out.println(requests * threadMap.size() + " records processed in " + ms + " ms");
		if ( ms < 1000) {
			System.out.println("loadTestGet() " + (requests * threadMap.size()) + " per second");
		}
		else {
			double dd = ms/1000d;
			double dc = requests * threadMap.size();
			System.out.println("--------------- loadTestGet()    --- " +  format.format(dc / dd) + " req/sec");
		}
		
	}
	
	
	
	
	private void loadTestSearch(String endpoint, String user, String password, Map<Integer,List<Map<String,Object>>> threadMap) throws Exception {


		long mainStart = System.currentTimeMillis();
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(user, password);
		HttpEntity entity = new HttpEntity(headers);
		
		String sortBy = "&sortBy=externalId&sortOrder=ascending";
		for ( int i = 0; i < threadMap.size(); i++ ) {
			final int zz = i;
			final List<Map<String,Object>> userList = threadMap.get(i); 
			new Thread() {
				public void run() {
					long start = System.currentTimeMillis();
					for ( int j = 0; j < userList.size(); j++) {
						RestTemplate restTemplate = new RestTemplate();
						try {
							String identifier = "joske" + zz + j;
							String filter = "userName eq " + identifier + " and externalId ne homer and externalId pr";
							String encodedFilter = URLEncoder.encode(filter);
							
							ResponseEntity<Map> response  = restTemplate.exchange(endpoint + "/Users?filter=" + encodedFilter + sortBy ,HttpMethod.GET,entity, Map.class );
							//System.out.println(response.getBody());
							if ( (int)response.getBody().get("totalResults") != 1) {
								throw new Exception("no result found");
							}
						}
						catch( Exception e ) {
							e.printStackTrace();
							break;
						}
					}
					System.out.println("thread [" + zz + "] " + userList.size() + " records processed in " + (System.currentTimeMillis() - start));
					searchFinished++;
				}
			}.start();
		}
		
		while( searchFinished != threadMap.size()) {
			Thread.sleep(100);
		}
		
		long ms = System.currentTimeMillis() - mainStart;
		System.out.println(requests * threadMap.size() + " records processed in " + ms + " ms");
		if ( ms < 1000) {
			System.out.println("loadTestSearch() " + (requests * threadMap.size()) + " per second");
		}
		else {
			double dd = ms/1000d;
			double dc = requests * threadMap.size();
			System.out.println("--------------- loadTestSearch()    --- " +  format.format(dc / dd) + " req/sec");
		}
		
	}
	
	
	
	
	
	
	private void loadTestDelete(String endpoint, String user, String password, Map<Integer,List<Map<String,Object>>> threadMap) throws Exception {

		String body = new String(readFileAsBytes(new FileInputStream(new File("src/test/resources/load_test_user_create.json"))));
		Map<String,Object> userObject = objectMapper.readValue(body, Map.class);
		
		
		
		long mainStart = System.currentTimeMillis();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBasicAuth(user, password);
		
		
		for ( int i = 0; i < threadMap.size(); i++ ) {
			final int zz = i;
			final List<Map<String,Object>> userList = threadMap.get(i); 
			new Thread() {
				public void run() {
					long start = System.currentTimeMillis();
					for ( int j = 0; j < userList.size(); j++) {
						RestTemplate restTemplate = new RestTemplate();
						try {
							HttpEntity entity = new HttpEntity(headers);
							ResponseEntity<Object> response  = restTemplate.exchange(endpoint + "/Users/" + userList.get(j).get(Constants.ID) ,HttpMethod.DELETE,entity, Object.class );
						}
						catch( Exception e ) {
							e.printStackTrace();
							break;
						}
					}
					System.out.println("thread [" + zz + "] " + userList.size() + " records processed in " + (System.currentTimeMillis() - start));
					deleteFinished++;
				}
			}.start();
		}
		
		while( deleteFinished != threadMap.size()) {
			Thread.sleep(100);
		}
		
		long ms = System.currentTimeMillis() - mainStart;
		System.out.println(requests * threadMap.size() + " records processed in " + ms + " ms");
		if ( ms < 1000) {
			System.out.println("loadTestDelete() " + (requests * threadMap.size()) + " per second");
		}
		else {
			double dd = ms/1000d;
			double dc = requests * threadMap.size();
			System.out.println("--------------- loadTestDelete() --- " +  format.format(dc / dd) + " req/sec");
		}
		
	}
	

	
	
	
	public static byte[] readFileAsBytes(InputStream inputStream) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
	    int nbytes = 0;
	    byte[] buffer = new byte[1024*4];

	    try {
	        while ((nbytes = inputStream.read(buffer)) != -1) {
	            out.write(buffer, 0, nbytes);
	        }
	        return out.toByteArray();
	    } 
	    finally {
	        if (inputStream != null) { 
	        	inputStream.close();
	        }
	        if (out != null) {
	            out.close();
	        }
	    }    
	}
	
	
	
	
	

}
