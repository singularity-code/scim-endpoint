package be.personify.iam.scim;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.springframework.util.Assert;

import be.personify.iam.scim.storage.impl.MemoryStorage;
import be.personify.iam.scim.storage.util.MemoryStorageUtil;
import be.personify.util.SearchCriteria;


public class MemoryStorageTest {
	
	private static final MemoryStorage storage = new MemoryStorage();
	
	private static final Logger logger = LogManager.getLogger(MemoryStorageTest.class);
	
	static {
		storage.initialize("Users");
	}
	
	

	@Test
	public void testSubAttributes1() {
		Map<String,Object> m = new HashMap<>();
		
		
		Map<String,Object> v1 = new HashMap<>();
		v1.put("type", "home");
		v1.put("mail", "mail1");
		
		List<Map> list = new ArrayList<>();
		list.add(v1);
		
		m.put("emails", list);
		
				
		Object ss = MemoryStorageUtil.getRecursiveObject(m, "emails.mail");
		if ( ss instanceof List ) {
			if (!((List)ss).contains("mail1")) {
				fail("not equals");
			}
		};
	}
	
	
	@Test
	public void testSubAttributes2() {
		Map<String,Object> m = new HashMap<>();
		
		
		Map<String,Object> v1 = new HashMap<>();
		v1.put("type", "home");
		v1.put("mail", "mail1");
		
		Map<String,Object> v2 = new HashMap<>();
		v1.put("type", "home");
		v1.put("mail", "mail2");
		
		List<Map> list = new ArrayList<>();
		list.add(v1);
		list.add(v2);
		
		m.put("emails", list);
		
				
		Object ss = MemoryStorageUtil.getRecursiveObject(m, "emails.mail");
		if ( ss instanceof List ) {
			if (!((List)ss).contains("mail2")) {
				fail("not equals");
			}
		};
	}
	
	
	

	@Test
	public void testSubAttributes3() {
		Map<String,Object> m = new HashMap<>();
		
		
		Map<String,Object> v1 = new HashMap<>();
		v1.put("familyName", "Simpson");
		
		m.put("name", v1);
		
		if ( !MemoryStorageUtil.getRecursiveObject(m, "name.familyName").equals("Simpson")) {
			fail("not equals");
		};
	}
	
	
	
	@Test
	public void testCreateSearchDelete() {
		Map<String,Object> map = new HashMap<String, Object>();
		map.put("userName", "username");
		map.put("externalId", "externalId");
		
		//create
		try {
			storage.create("atest", map);
		}
		catch( Exception e) {
			e.printStackTrace();
			fail("can not create " +  e.getMessage());
			return;
		}
		
		//test get
		try {
			Assert.notNull(storage.get("atest"), "can not be null");
		}
		catch( Exception e) {
			fail("can not get " +  e.getMessage());
			return;
		}
		
		//test search
		try {
			List<Map> search = storage.search(new SearchCriteria(), 1, 10, null, null);
			long count = storage.count(new SearchCriteria());
			logger.info(" count {} search {}", count, search);
		}
		catch( Exception e) {
			e.printStackTrace();
			fail("can not get " +  e.getMessage());
			return;
		}
		
		//delete
		try {
			Assert.isTrue(storage.delete("atest"), "delete is not truel");
		}
		catch( Exception e) {
			fail("can not delete " +  e.getMessage());
			return;
		}
		
		//test delete
		try {
			Assert.isNull(storage.get("atest"), "must be null");
		}
		catch( Exception e) {
			fail("can not get " +  e.getMessage());
			return;
		}
		
	}
	
	
	
	
	
	
	
	
	
	

}
