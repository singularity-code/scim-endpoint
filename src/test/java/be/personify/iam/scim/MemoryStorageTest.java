package be.personify.iam.scim;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import be.personify.iam.scim.storage.impl.MemoryStorage;

public class MemoryStorageTest {
	
	private static final MemoryStorage storage = new MemoryStorage();
	
	

	@Test
	public void testSubAttributes1() {
		Map<String,Object> m = new HashMap<>();
		
		
		Map<String,Object> v1 = new HashMap<>();
		v1.put("type", "home");
		v1.put("mail", "mail1");
		
		List<Map> list = new ArrayList<>();
		list.add(v1);
		
		m.put("emails", list);
		
				
		Object ss = storage.getRecursiveObject(m, "emails.mail");
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
		
				
		Object ss = storage.getRecursiveObject(m, "emails.mail");
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
		
		if ( !storage.getRecursiveObject(m, "name.familyName").equals("Simpson")) {
			fail("not equals");
		};
	}
	
	
	
	
	
	
	
	
	
	

}
