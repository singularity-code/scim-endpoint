package be.personify.iam.scim.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
	
	private static int finished = 0;
	private static int  createFinished = 0;
	private static int deleteFinished = 0;
	
	private static int requests;
	
	
	
	public static void main(String[] args) throws Exception {
		
		String endpoint = args[0];
		String user = args[1];
		String password = args[2];
		int threads = Integer.parseInt(args[3]);
		requests = Integer.parseInt(args[4]);
		
		LoadTest test = new LoadTest();
		
		System.out.println("starting load test to " + endpoint + " with " + threads + " threads");
		
		Map<Integer,List<Map<String,Object>>> threadMap = test.loadTestCreate(endpoint, user, password, threads, requests);
		
		while ( createFinished != threads ) {
			System.out.println("create finished " + createFinished);
			Thread.sleep(1000);
		}
		
		test.loadTestDelete(endpoint, user, password, threadMap);
		
		while ( deleteFinished != threads ) {
			System.out.println("deleteFinished finished " + deleteFinished);
			Thread.sleep(1000);
		}
		
		test.loadTestGet(endpoint, user, password, threads, requests);
		
	}

	
	
	
	private void loadTestGet(String endpoint, String user, String password, int nrOfThreads, int nrOfRequests) throws Exception {

		String body = new String(readFileAsBytes(new FileInputStream(new File("src/test/resources/load_test_user_create.json"))));
		Map<String,Object> userObject = objectMapper.readValue(body, Map.class);
		
		deleteUser(endpoint,user,password, userObject);
		
		userObject = createUser(endpoint,user,password, userObject);
		final Object id = userObject.get(Constants.ID);
		System.out.println("user created with id " + id);
		
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
							ResponseEntity<Object> response  = restTemplate.exchange(endpoint + "/Users/" + id ,HttpMethod.GET,entity, Object.class );
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
		System.out.println(nrOfRequests * nrOfThreads + " records processed in " + ms + " ms");
		
		
		
		System.out.println("loadTestGet() " + (nrOfRequests * nrOfThreads) / ( ms/1000) + " per second");
		
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
				tt.put("userName", "joske" + i + j);
				tt.put("externalId", "joske" + i + j);
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
		System.out.println("loadTestCreate() " + (nrOfRequests * nrOfThreads) / ( ms/1000) + " per second");
		return threadMap;
		
	}
	
	
	
	
	
	
	private void loadTestDelete(String endpoint, String user, String password, Map<Integer,List<Map<String,Object>>> threadMap) throws Exception {

		String body = new String(readFileAsBytes(new FileInputStream(new File("src/test/resources/load_test_user_create.json"))));
		Map<String,Object> userObject = objectMapper.readValue(body, Map.class);
		
		
		
		long mainStart = System.currentTimeMillis();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBasicAuth(user, password);
		
		int nrOfRequests = 0;
		
		for ( int i = 0; i < threadMap.size(); i++ ) {
			final int zz = i;
			final List<Map<String,Object>> userList = threadMap.get(i); 
			nrOfRequests = nrOfRequests + userList.size();
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
			System.out.println("loadTestDelete() " + (requests * threadMap.size()) / ( ms/1000) + " per second");
		}
		
	}
	
	

	private Map<String, Object> createUser( String endpoint, String user, String password, Map<String,Object> userObject ) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(user, password);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity entity = new HttpEntity(userObject,headers);
		RestTemplate restTemplate = new RestTemplate();
		try {
			ResponseEntity<Map> response  = restTemplate.exchange(endpoint + "/Users",HttpMethod.POST,entity, Map.class );
			return response.getBody();
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	
	private Object deleteUser( String endpoint, String user, String password, Map<String,Object> userObject ) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(user, password);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity entity = new HttpEntity(headers);
		RestTemplate restTemplate = new RestTemplate();
		try {
			ResponseEntity<Object> response  = restTemplate.exchange(endpoint + "/Users/" + userObject.get(Constants.ID), HttpMethod.DELETE, entity, Object.class );
			return response.getBody();
		}
		catch( Exception e ) {
			//e.printStackTrace();
		}
		return null;
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
