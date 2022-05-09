package be.personify.iam.scim;


import org.junit.Test;

import be.personify.iam.scim.util.SearchCriteriaUtil;
import be.personify.util.LogicalOperator;
import be.personify.util.SearchCriteria;
import be.personify.util.SearchOperation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

public class FilterTest {
	
	private static final Logger logger = LogManager.getLogger(FilterTest.class);
	
	private SearchCriteriaUtil searchCriteriaUtil = new SearchCriteriaUtil();
	
//	
//	        filter=userName eq "bjensen"
//
//			filter=name.familyName co "O'Malley"
//
//			filter=userName sw "J"
//
//			filter=urn:ietf:params:scim:schemas:core:2.0:User:userName sw "J"
//
//			filter=title pr
//
//			filter=meta.lastModified gt "2011-05-13T04:42:34Z"
//
//			filter=meta.lastModified ge "2011-05-13T04:42:34Z"
//
//			filter=meta.lastModified lt "2011-05-13T04:42:34Z"
//
//			filter=meta.lastModified le "2011-05-13T04:42:34Z"
//
//			filter=title pr and userType eq "Employee"
//
//			filter=title pr or userType eq "Intern"
//
//			filter=schemas eq "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
//
//			filter=userType eq "Employee" and (emails co "example.com" or
//			  emails.value co "example.org")
//
//			filter=userType ne "Employee" and not (emails co "example.com" or
//			  emails.value co "example.org")
//
//			filter=userType eq "Employee" and (emails.type eq "work")
//
//			filter=userType eq "Employee" and emails[type eq "work" and
//			  value co "@example.com"]
//
//			filter=emails[type eq "work" and value co "@example.com"] or
//			  ims[type eq "xmpp" and value co "@foo.com"]

	
	
	@Test
	public void testOne() {
		String filter = "userName eq \"bjensen\"";
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria", criteria.size() == 1 );
			Assert.assertTrue("has to be equals",criteria.getCriteria().get(0).getSearchOperation().equals(SearchOperation.EQUALS));
			Assert.assertTrue("has to be userName",criteria.getCriteria().get(0).getKey().equals("userName"));
			Assert.assertTrue("has to be userName",criteria.getCriteria().get(0).getValue().equals("bjensen"));
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testTwo() {
		String filter = "title pr";
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria", criteria.size() == 1 );
			Assert.assertTrue("has to be equals",criteria.getCriteria().get(0).getSearchOperation().equals(SearchOperation.PRESENT));
			Assert.assertTrue("has to be userName",criteria.getCriteria().get(0).getKey().equals("title"));
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void testThree() {
		String filter = "title pr and userType eq \"Employee\"";
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria", criteria.size() == 2 );
			Assert.assertTrue("logical operator of the criteria", criteria.getOperator() == LogicalOperator.AND );
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void testFour() {
		String filter = "title pr or userType eq \"Intern\"";
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria", criteria.size() == 2 );
			Assert.assertTrue("logical operator of the criteria", criteria.getOperator() == LogicalOperator.OR);
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void testFive() {
		String filter = "userType eq \"Employee\" and (emails co \"example.com\" or emails.value co \"example.org\")";
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria", criteria.size() == 2 );
			Assert.assertTrue("logical operator of the criteria", criteria.getOperator() == LogicalOperator.AND);
			logger.info("crit {}", criteria);
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testFiveBis() {
		String filter = "(emails co \"example.com\" or emails.value co \"example.org\") and userType eq \"Employee\"";
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria", criteria.size() == 2 );
			Assert.assertTrue("logical operator of the criteria", criteria.getOperator() == LogicalOperator.AND);
			logger.info("crit {}", criteria);
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testFiveTris() {
		String filter = "(emails co \"example.com\" or emails.value co \"example.org\") or userType eq \"Employee\"";
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria", criteria.size() == 2 );
			Assert.assertTrue("logical operator of the criteria", criteria.getOperator() == LogicalOperator.OR);
			logger.info("crit {}", criteria);
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void testSix() {
		String filter = "userType eq \"Employee\" and (emails.type eq \"work\")";
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria", criteria.size() == 2 );
			Assert.assertTrue("logical operator of the criteria", criteria.getOperator() == LogicalOperator.AND);
			logger.info("crit {}", criteria);
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	
	
	@Test
	public void testSeven() {
		String filter = "emails[type eq \"work\" and value co \"@example.com\"] or ims[type eq \"xmpp\" and value co \"@foo.com\"]";
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria should be 2 and is " + criteria.size(), criteria.size() == 2 );
			Assert.assertTrue("logical operator of the criteria", criteria.getOperator() == LogicalOperator.OR);
			logger.info("crit {}", criteria);
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	
	
	@Test
	public void testEight() {
		String filter = "(id eq \"df7af4be-6851-423d-a41e-e32dc3e5a17e\") or (id eq \"01f91eaf-b2fe-4781-8c21-f0644b8db62d\") or (id eq \"7d8cbb88-4c75-4dd5-94e1-12b48632fbdc\")";
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria should be 3 and is " + criteria.size(), criteria.size() == 3 );
			Assert.assertTrue("logical operator of the criteria", criteria.getOperator() == LogicalOperator.OR);
			logger.info("crit {}", criteria);
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	
	
	

}
