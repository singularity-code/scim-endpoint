/*
*     Copyright 2019-2022 Wouter Van der Beken @ https://personify.be
*
* Generated software by personify.be

* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*          http://www.apache.org/licenses/LICENSE-2.0
*
 * Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package be.personify.iam.scim.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import be.personify.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

public class LoadTest {

	private static final String EXCLUDED_ATTRIBUTES_GROUPS = "excludedAttributes=groups";

	private static final ObjectMapper objectMapper = new ObjectMapper();
	
	private int maxIdsSearch = 1000;

	private static int createFinished = 0;
	private static int createGroupsFinished = 0;
	private static int getFinished = 0;
	private static int searchFinished = 0;
	private static int findIdsFinished = 0;
	private static int deleteFinished = 0;

	private static int requests;

	private static final DecimalFormat format = new DecimalFormat("#.##");
	
	private static final String OPERATION_CREATE = "CREATE";
	private static final String OPERATION_GET = "GET";
	private static final String OPERATION_SEARCH = "SEARCH";
	private static final String OPERATION_FIND_IDS = "FIND_IDS";
	private static final String OPERATION_DELETE = "DELETE";

	public static void main(String[] args) throws Exception {

		String endpoint = args[0];
		String user = args[1];
		String password = args[2];
		int threads = Integer.parseInt(args[3]);
		requests = Integer.parseInt(args[4]);
		
		List<String> operations = new ArrayList<>();
		if ( args.length == 6 ) {
			operations = Arrays.asList(args[5].split(StringUtils.ESCAPED_PIPE)); 
			System.out.println("operations :" + operations);
		}
		else {
			operations.add(OPERATION_CREATE);
			operations.add(OPERATION_GET);
			operations.add(OPERATION_SEARCH);
			operations.add(OPERATION_FIND_IDS);
			operations.add(OPERATION_DELETE);
			System.out.println("operations :" + operations);
		}

		LoadTest test = new LoadTest();

		System.out.println("starting load test to " + endpoint + " with " + threads + " threads and " + requests + " requests....");
		
		String body = new String( readFileAsBytes(new FileInputStream(new File("src/test/resources/load_test_user_create.json"))));
		Map<String, Object> userObject = objectMapper.readValue(body, Map.class);
		
		body = new String( readFileAsBytes(new FileInputStream(new File("src/test/resources/load_test_group_create.json"))));
		Map<String, Object> groupObject = objectMapper.readValue(body, Map.class);

		Map<Integer, List<Map<String, Object>>> threadMap = getThreadMap(threads, requests, userObject);

		//CREATE
		if ( operations.contains(OPERATION_CREATE)) {
			test.loadTestCreate(endpoint, user, password, threadMap,requests, threads);

			while (createFinished != threads) {
				System.out.println("create finished " + createFinished);
				Thread.sleep(1000);
			}
			
			test.loadTestCreateGroups(endpoint, user, password, threadMap,requests, threads, groupObject);

			while (createGroupsFinished != threads) {
				System.out.println("create finished " + createGroupsFinished);
				Thread.sleep(1000);
			}
		}

		//GET WITHOUT GROUPS
		if ( operations.contains(OPERATION_GET)) {
			test.loadTestGet(endpoint, user, password, threadMap, true);

			while (getFinished != threads) {
				System.out.println("get finished " + getFinished);
				Thread.sleep(1000);
			}
		}
		getFinished = 0;
		
		//GET WITH GROUPS
		if ( operations.contains(OPERATION_GET)) {
			test.loadTestGet(endpoint, user, password, threadMap,false );

			while (getFinished != threads) {
				System.out.println("get finished " + getFinished);
				Thread.sleep(1000);
			}
		}
		
		if ( operations.contains(OPERATION_SEARCH)) {
			test.loadTestSearch(endpoint, user, password, threadMap);

			while (searchFinished != threads) {
				System.out.println("search finished " + searchFinished);
				Thread.sleep(1000);
			}
		}
		
		if ( operations.contains(OPERATION_FIND_IDS)) {
			test.loadTestFindAllIds(endpoint, user, password, threadMap);
		}

		if ( operations.contains(OPERATION_DELETE)) {
			test.loadTestDelete(endpoint, user, password, threadMap, "User");

			while (deleteFinished != threads) {
				System.out.println("deleteFinished finished for type User " + deleteFinished);
				Thread.sleep(1000);
			}
			
			deleteFinished = 0;
			
			test.loadTestDelete(endpoint, user, password, threadMap, "Group");

			while (deleteFinished != threads) {
				System.out.println("deleteFinished finished for type Group" + deleteFinished);
				Thread.sleep(1000);
			}
			
		}
	}
	
	
	
	private static Map<Integer, List<Map<String, Object>>> getThreadMap(int nrOfThreads, int nrOfRequests,	Map<String, Object> userObject) {
		Map<Integer, List<Map<String, Object>>> threadMap = new HashMap<>();
		for (int i = 0; i < nrOfThreads; i++) {
			List<Map<String, Object>> users = new ArrayList<>();
			Map<String, Object> tt = null;
			for (int j = 0; j < nrOfRequests; j++) {
				tt = new HashMap<>(userObject);
				tt.put(Constants.ID, UUID.randomUUID().toString());
				String identifier = "joske" + i + j;
				tt.put("userName", identifier);
				tt.put("externalId", identifier);
				users.add(tt);
			}
			threadMap.put(i, users);
		}
		return threadMap;
	}

	
	
	private Map<Integer, List<Map<String, Object>>> loadTestCreate(String endpoint, String user, String password, final Map<Integer,List<Map<String, Object>>> threadMap, int nrOfRequests, int nrOfThreads) throws Exception {


		long mainStart = System.currentTimeMillis();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBasicAuth(user, password);
		
		Object lock = new Object();

		for (int i = 0; i < threadMap.size(); i++) {
			final int zz = i;
			final List<Map<String, Object>> userList = threadMap.get(i);
			new Thread() {
				public void run() {
					long start = System.currentTimeMillis();
					for (int j = 0; j < userList.size(); j++) {
						RestTemplate restTemplate = new RestTemplate();
						HttpEntity entity = null;
						ResponseEntity<Object> response = null;
						try {
							entity = new HttpEntity(userList.get(j), headers);
							response = restTemplate.exchange(endpoint + "/Users", HttpMethod.POST, entity, Object.class);
						}
						catch (Exception e) {
							e.printStackTrace();
							continue;
						}
					}
					//System.out.println("thread [" + zz + "] " + nrOfRequests + " records processed in "	+ (System.currentTimeMillis() - start));
					synchronized (lock) {
						createFinished++;
					}
				}
			}.start();
		}

		while (createFinished != nrOfThreads) {
			Thread.sleep(100);
		}

		long ms = System.currentTimeMillis() - mainStart;
		double dd = ms / 1000d;
		double dc = nrOfRequests * nrOfThreads;

		System.out.println("--------------- loadTestCreate() --- " + format.format(dc / dd) + " req/sec");
		return threadMap;
	}

	
	
	
	private Map<Integer, List<Map<String, Object>>> loadTestCreateGroups(String endpoint, String user, String password, final Map<Integer,List<Map<String, Object>>> threadMap, int nrOfRequests, int nrOfThreads, Map<String, Object> groupObject) throws Exception {


		long mainStart = System.currentTimeMillis();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBasicAuth(user, password);
		
		Object lock = new Object();

		for (int i = 0; i < threadMap.size(); i++) {
			final int zz = i;
			final List<Map<String, Object>> userList = threadMap.get(i);
			new Thread() {
				public void run() {
					long start = System.currentTimeMillis();
					for (int j = 0; j < userList.size(); j++) {
						RestTemplate restTemplate = new RestTemplate();
						HttpEntity entity = null;
						ResponseEntity<Object> response = null;
						try {
							Map<String,Object> group = new HashMap(groupObject);
							group.put("id", "organisation" + zz + j);
							group.put("displayName", "organisation" + zz + j);
							entity = new HttpEntity(group, headers);
							response = restTemplate.exchange(endpoint + "/Groups", HttpMethod.POST, entity, Object.class);
						}
						catch (Exception e) {
							e.printStackTrace();
							continue;
						}
					}
					//System.out.println("thread [" + zz + "] " + nrOfRequests + " records processed in "	+ (System.currentTimeMillis() - start));
					synchronized (lock) {
						createGroupsFinished++;
					}
				}
			}.start();
		}

		while (createGroupsFinished != nrOfThreads) {
			Thread.sleep(100);
		}

		long ms = System.currentTimeMillis() - mainStart;
		double dd = ms / 1000d;
		double dc = nrOfRequests * nrOfThreads;

		System.out.println("--------------- loadTestCreateGroups() --- " + format.format(dc / dd) + " req/sec");
		return threadMap;
	}


	
	
	

	private void loadTestGet(String endpoint, String user, String password,	Map<Integer, List<Map<String, Object>>> threadMap, boolean excludeGroups) throws Exception {

		long mainStart = System.currentTimeMillis();
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(user, password);
		HttpEntity entity = new HttpEntity(headers);
		
		Object lock = new Object();
		 
		for (int i = 0; i < threadMap.size(); i++) {
			final int zz = i;
			final List<Map<String, Object>> userList = threadMap.get(i);
			new Thread() {
				public void run() {
					long start = System.currentTimeMillis();
					for (int j = 0; j < userList.size(); j++) {
						RestTemplate restTemplate = new RestTemplate();
						ResponseEntity<Object> response = null;
						try {
							String uri = endpoint + "/Users/" + userList.get(j).get(Constants.ID);
							if ( excludeGroups ) {
								uri = uri + StringUtils.QUESTION_MARK + EXCLUDED_ATTRIBUTES_GROUPS;
							}
							response = restTemplate.exchange(uri, HttpMethod.GET, entity, Object.class);
						}
						catch (Exception e) {
							e.printStackTrace();
							continue;
						}
					}
					//System.out.println("thread [" + zz + "] " + userList.size() + " records processed in " + (System.currentTimeMillis() - start));
					synchronized (lock) {
						getFinished++;
					}
				}
			}.start();
		}

		while (getFinished != threadMap.size()) {
			Thread.sleep(100);
		}

		long ms = System.currentTimeMillis() - mainStart;
		if (ms < 1000) {
			System.out.println("loadTestGet() excludeGroups [" + excludeGroups + "] - " + (requests * threadMap.size()) + " per second");
		} else {
			double dd = ms / 1000d;
			double dc = requests * threadMap.size();
			System.out.println("--------------- loadTestGet()  excludeGroups [" + excludeGroups + "] --- " + format.format(dc / dd) + " req/sec");
		}
	}

	
	
	private void loadTestSearch(String endpoint, String user, String password,	Map<Integer, List<Map<String, Object>>> threadMap) throws Exception {

		long mainStart = System.currentTimeMillis();
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(user, password);
		HttpEntity entity = new HttpEntity(headers);
		
		Object lock = new Object();

		String sortBy = "&sortBy=externalId&sortOrder=ascending";
		for (int i = 0; i < threadMap.size(); i++) {
			final int zz = i;
			final List<Map<String, Object>> userList = threadMap.get(i);
			new Thread() {
				public void run() {
					long start = System.currentTimeMillis();
					for (int j = 0; j < userList.size(); j++) {
						RestTemplate restTemplate = new RestTemplate();
						try {
							String identifier = "joske" + zz + j;
							String filter = "userName eq " + identifier + " and externalId pr";
							String encodedFilter = URLEncoder.encode(filter);

							ResponseEntity<Map> response = restTemplate.exchange(
									endpoint + "/Users?filter=" + encodedFilter + sortBy, HttpMethod.GET, entity,
									Map.class);
							// System.out.println(response.getBody());
							int results = (int) response.getBody().get("totalResults");
							if (results != 1) {
								System.out.println("number of results " + results);
								throw new Exception("no result found");
							}
						}
						catch (Exception e) {
							e.printStackTrace();
							continue;
						}
					}
					//System.out.println("thread [" + zz + "] " + userList.size() + " records processed in "	+ (System.currentTimeMillis() - start));
					
					synchronized (lock) {
						searchFinished++;
					}
				}
			}.start();
		}

		while (searchFinished != threadMap.size()) {
			Thread.sleep(100);
		}

		long ms = System.currentTimeMillis() - mainStart;
		if (ms < 1000) {
			System.out.println("loadTestSearch() " + (requests * threadMap.size()) + " per second");
		} else {
			double dd = ms / 1000d;
			double dc = requests * threadMap.size();
			System.out.println("--------------- loadTestSearch()    --- " + format.format(dc / dd) + " req/sec");
		}
	}

	private void loadTestFindAllIds(String endpoint, String user, String password, Map<Integer, List<Map<String, Object>>> threadMap) throws Exception {

		long mainStart = System.currentTimeMillis();
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(user, password);
		HttpEntity entity = new HttpEntity(headers);

		long start = System.currentTimeMillis();
		RestTemplate restTemplate = new RestTemplate();
		try {
			ResponseEntity<Map> response = restTemplate.exchange(endpoint + "/Users?attributes=id", HttpMethod.GET,	entity, Map.class);
			// System.out.println(response.getBody());
			int findIdsFinished = (int) response.getBody().get("totalResults");
			if ( findIdsFinished > maxIdsSearch) {
				findIdsFinished = maxIdsSearch;
			}
			long count = 5;
			long time = 0;
			for (int i = 0; i < count; i++) {
				long cstart = System.currentTimeMillis();
				response = restTemplate.exchange(endpoint + "/Users?attributes=id&startIndex=1&count=" + findIdsFinished, HttpMethod.GET, entity, Map.class);
				time = time + (System.currentTimeMillis() - cstart);
				// System.out.println(response.getBody());
			}
			System.out.println("--------------- loadTestAllIds() average   --- " + (time / count) + " ms");

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadTestDelete(String endpoint, String user, String password, Map<Integer, List<Map<String, Object>>> threadMap, String type) throws Exception {

		long mainStart = System.currentTimeMillis();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBasicAuth(user, password);
		
		HttpEntity entity = new HttpEntity(headers);
		RestTemplate restTemplateOne = new RestTemplate();
		ResponseEntity<Map> response = restTemplateOne.exchange(endpoint + "/" + type + "s?attributes=id", HttpMethod.GET,	entity, Map.class);
		// System.out.println(response.getBody());
		int findIdsFinished = (int) response.getBody().get("totalResults");
		response = restTemplateOne.exchange(endpoint + "/" + type + "s?attributes=id&startIndex=1&count=" + findIdsFinished, HttpMethod.GET, entity, Map.class);
		List<Map> resources = (List)response.getBody().get("Resources");
		
		//System.out.println("got total ids " + resources.size());

		Object lock = new Object();
		
		for (int i = 0; i < threadMap.size(); i++) {
			new Thread() {
				public void run() {
					String id = null;
					while ( resources.size() > 0) {
						long start = System.currentTimeMillis();
						synchronized (lock) {
							if ( resources.size() > 0 ) {
								id = (String)((Map)resources.get(0)).get("id");
								resources.remove(0);
							}
							else {
								continue;
							}
						}
						
						RestTemplate restTemplate = new RestTemplate();
						try {
							ResponseEntity<Object> response = restTemplate.exchange(endpoint + "/" + type + "s/" + id, HttpMethod.DELETE, entity,Object.class);
						}
						catch (Exception e) {
							e.printStackTrace();
						}
						
					}
					synchronized (lock) {
						deleteFinished++;
					}
				}
			}.start();
		}

		while (deleteFinished != threadMap.size()) {
			//System.out.println("deleteF " + deleteFinished + " - threadm " + threadMap.size() );
			Thread.sleep(100);
		}

		long ms = System.currentTimeMillis() - mainStart;
		//System.out.println(requests * threadMap.size() + " records for type " + type + " processed in " + ms + " ms");
		if (ms < 1000) {
			System.out.println("loadTestDelete() " + (requests * threadMap.size()) + " per second");
		}
		else {
			double dd = ms / 1000d;
			double dc = requests * threadMap.size();
			System.out.println("--------------- loadTestDelete() --- type [" + type +  "] " + format.format(dc / dd) + " req/sec");
		}
	}

	public static byte[] readFileAsBytes(InputStream inputStream) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int nbytes = 0;
		byte[] buffer = new byte[1024 * 4];

		try {
			while ((nbytes = inputStream.read(buffer)) != -1) {
				out.write(buffer, 0, nbytes);
			}
			return out.toByteArray();
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
			if (out != null) {
				out.close();
			}
		}
	}
}
