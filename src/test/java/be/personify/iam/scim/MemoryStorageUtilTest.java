package be.personify.iam.scim;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import be.personify.iam.scim.storage.util.MemoryStorageUtil;
import be.personify.util.SearchCriteria;
import be.personify.util.SearchCriteriaUtil;
import be.personify.util.exception.InvalidFilterException;

public class MemoryStorageUtilTest {
	
	
	private SearchCriteriaUtil searchCriteriaUtil = new SearchCriteriaUtil();
	
	
	@Test
	public void testOne() throws InvalidFilterException {
		String filter = "userName eq \"bjensen\"";
		SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteriaFromSCIMFilter(filter);
		Map<String,Object> object = new HashMap<>();
		object.put("userName", "homer");
		Assert.assertFalse("must not match", MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
		object.put("userName", "bjensen");
		Assert.assertTrue("must match", MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
		object.put("username", "bjensen");
		Assert.assertTrue("must match", MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
		
	}
	
	@Test
	public void testOneCaseInsensitivityLogicalOperator() throws InvalidFilterException {
		String filter = "userName Eq \"bjensen\"";
		SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteriaFromSCIMFilter(filter);
		Map<String,Object> object = new HashMap<>();
		object.put("userName", "homer");
		Assert.assertFalse("must not match", MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
		object.put("userName", "bjensen");
		Assert.assertTrue("must match", MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
		object.put("username", "bjensen");
		Assert.assertTrue("must match", MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
	}
	
	
	@Test
	public void testTwo() {
		String filter = "title pr";
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteriaFromSCIMFilter(filter);
			Map<String,Object> object = new HashMap<>();
			Assert.assertFalse("not must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
			object.put("title", "mister");
			Assert.assertTrue("must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
			
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void testThree() {
		String filter = "title pr and userType eq \"Employee\"";
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteriaFromSCIMFilter(filter);
			
			Map<String,Object> object = new HashMap<>();
			Assert.assertFalse("not must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
			object.put("title", "mister");
			Assert.assertFalse("not must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
			object.put("userType", "external");
			Assert.assertFalse("not must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
			object.put("userType", "Employee");
			Assert.assertTrue("must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
			
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void testFour() {
		String filter = "title pr or userType eq \"Intern\"";
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteriaFromSCIMFilter(filter);
			Map<String,Object> object = new HashMap<>();
			Assert.assertFalse("not must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
			object.put("userType", "external");
			Assert.assertFalse("not must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
			object.put("title", "mister");
			Assert.assertTrue("must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void testFive() {
		String filter = "userType eq \"Employee\" and (emails co \"example.com\" or emails.value co \"example.org\")";
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteriaFromSCIMFilter(filter);
			Map<String,Object> object = new HashMap<>();
			Assert.assertFalse("not must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
			object.put("userType", "Employee");
			Assert.assertFalse("not must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
			Map<String,Object> mailOne = new HashMap<>();
			mailOne.put("value", "test");
			List<Map> mails = new ArrayList<>();
			mails.add(mailOne);
			object.put("emails", mails);
			Assert.assertFalse("not must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
			mailOne = new HashMap<>();
			mailOne.put("value", "example.org");
			mails = new ArrayList<>();
			mails.add(mailOne);
			object.put("emails", mails);
			Assert.assertTrue("must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
			
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void testSix() {
		String filter = "userType eq \"Employee\" and (emails.type eq \"work\")";
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteriaFromSCIMFilter(filter);
			Map<String,Object> object = new HashMap<>();
			Assert.assertFalse("not must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
			object.put("userType", "Employee");
			Assert.assertFalse("not must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
			
			Map<String,Object> mailOne = new HashMap<>();
			mailOne.put("type", "test");
			List<Map> mails = new ArrayList<>();
			mails.add(mailOne);
			object.put("emails", mails);
			Assert.assertFalse("not must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
			mailOne = new HashMap<>();
			mailOne.put("type", "work");
			mails = new ArrayList<>();
			mails.add(mailOne);
			object.put("emails", mails);
			Assert.assertTrue("must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
			
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	
	
	@Test
	public void testSeven() {
		String filter = "emails[type eq \"work\" and value co \"@example.com\"] or ims[type eq \"xmpp\" and value co \"@foo.com\"]";
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteriaFromSCIMFilter(filter);
		
			Map<String,Object> object = new HashMap<>();
			Assert.assertFalse("not must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );

			Map<String,Object> mailOne = new HashMap<>();
			mailOne.put("type", "test");
			List<Map> mails = new ArrayList<>();
			mails.add(mailOne);
			object.put("emails", mails);
			Assert.assertFalse("not must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
			mailOne = new HashMap<>();
			mailOne.put("type", "work");
			mails = new ArrayList<>();
			mails.add(mailOne);
			object.put("emails", mails);
			Assert.assertFalse("not must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
			mailOne.put("value", "test@example.com");
			mails = new ArrayList<>();
			mails.add(mailOne);
			object.put("emails", mails);
			Assert.assertTrue("must match",MemoryStorageUtil.objectMatchesCriteria(criteria, object) );
			
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	
	
	
	

}
