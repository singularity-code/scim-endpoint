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
	
	
	private static final String FILTER_ONE = "userName eq \"bjensen\"";
	private static final String FILTER_TWO = "title pr";
	private static final String FILTER_THREE = "title pr and userType eq \"Employee\"";
	private static final String FILTER_FOUR = "title pr or userType eq \"Intern\"";
	private static final String FILTER_FIVE = "userType eq \"Employee\" and (emails co \"example.com\" or emails.value co \"example.org\")";
	private static final String FILTER_FIVE_BIS = "(emails co \"example.com\" or emails.value co \"example.org\") and userType eq \"Employee\"";
	private static final String FILTER_FIVE_TRIS = "(emails co \"example.com\" or emails.value co \"example.org\") or userType eq \"Employee\"";
	private static final String FILTER_SIX = "userType eq \"Employee\" and (emails.type eq \"work\")";
	private static final String FILTER_SEVEN = "emails[type eq \"work\" and value co \"@example.com\"] or ims[type eq \"xmpp\" and value co \"@foo.com\"]";
	private static final String FILTER_EIGHT = "(id eq \"df7af4be-6851-423d-a41e-e32dc3e5a17e\") or (id eq \"01f91eaf-b2fe-4781-8c21-f0644b8db62d\") or (id eq \"7d8cbb88-4c75-4dd5-94e1-12b48632fbdc\")";
	private static final String FILTER_EIGHT_BIS = "((id eq \"df7af4be-6851-423d-a41e-e32dc3e5a17e\") or (id eq \"01f91eaf-b2fe-4781-8c21-f0644b8db62d\") or (id eq \"7d8cbb88-4c75-4dd5-94e1-12b48632fbdc\"))";
	private static final String FILTER_EIGHT_TRIS = "( (id eq \"df7af4be-6851-423d-a41e-e32dc3e5a17e\") or (id eq \"01f91eaf-b2fe-4781-8c21-f0644b8db62d\") or (id eq \"7d8cbb88-4c75-4dd5-94e1-12b48632fbdc\") )";


	private static final Logger logger = LogManager.getLogger(FilterTest.class);
	
	private SearchCriteriaUtil searchCriteriaUtil = new SearchCriteriaUtil();

	
	
	@Test
	public void testOne() {
		String filter = FILTER_ONE;
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria", criteria.size() == 1 );
			Assert.assertTrue("has to be equals",criteria.getCriteria().get(0).getSearchOperation().equals(SearchOperation.EQUALS));
			Assert.assertTrue("has to be userName",criteria.getCriteria().get(0).getKey().equals("userName"));
			Assert.assertTrue("has to be userName",criteria.getCriteria().get(0).getValue().equals("bjensen"));
		}
		catch( Exception e ) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testTwo() {
		String filter = FILTER_TWO;
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria", criteria.size() == 1 );
			Assert.assertTrue("has to be equals",criteria.getCriteria().get(0).getSearchOperation().equals(SearchOperation.PRESENT));
			Assert.assertTrue("has to be userName",criteria.getCriteria().get(0).getKey().equals("title"));
		}
		catch( Exception e ) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	
	@Test
	public void testThree() {
		String filter = FILTER_THREE;
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria", criteria.size() == 2 );
			Assert.assertTrue("logical operator of the criteria", criteria.getOperator() == LogicalOperator.AND );
		}
		catch( Exception e ) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	
	@Test
	public void testFour() {
		String filter = FILTER_FOUR;
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria", criteria.size() == 2 );
			Assert.assertTrue("logical operator of the criteria", criteria.getOperator() == LogicalOperator.OR);
		}
		catch( Exception e ) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	
	@Test
	public void testFive() {
		String filter = FILTER_FIVE;
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria", criteria.size() == 2 );
			Assert.assertTrue("logical operator of the criteria", criteria.getOperator() == LogicalOperator.AND);
			logger.info("crit {}", criteria);
		}
		catch( Exception e ) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testFiveBis() {
		String filter = FILTER_FIVE_BIS;
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria", criteria.size() == 2 );
			Assert.assertTrue("logical operator of the criteria", criteria.getOperator() == LogicalOperator.AND);
			logger.info("crit {}", criteria);
		}
		catch( Exception e ) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testFiveTris() {
		String filter = FILTER_FIVE_TRIS;
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria", criteria.size() == 2 );
			Assert.assertTrue("logical operator of the criteria", criteria.getOperator() == LogicalOperator.OR);
			logger.info("crit {}", criteria);
		}
		catch( Exception e ) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	
	@Test
	public void testSix() {
		String filter = FILTER_SIX;
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria", criteria.size() == 2 );
			Assert.assertTrue("logical operator of the criteria", criteria.getOperator() == LogicalOperator.AND);
			logger.info("crit {}", criteria);
		}
		catch( Exception e ) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	
	
	@Test
	public void testSeven() {
		String filter = FILTER_SEVEN;
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria should be 2 and is " + criteria.size(), criteria.size() == 2 );
			Assert.assertTrue("logical operator of the criteria", criteria.getOperator() == LogicalOperator.OR);
			logger.info("crit {}", criteria);
		}
		catch( Exception e ) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	
	
	@Test
	public void testEight() {
		String filter = FILTER_EIGHT;
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria should be 3 and is " + criteria.size(), criteria.size() == 3 );
			Assert.assertTrue("logical operator of the criteria", criteria.getOperator() == LogicalOperator.OR);
			logger.info("crit {}", criteria);
		}
		catch( Exception e ) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	
	
	@Test
	public void testNiner() {
		String filter = FILTER_EIGHT_BIS;
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria should be 3 and is " + criteria.size(), criteria.size() == 3 );
			Assert.assertTrue("logical operator of the criteria", criteria.getOperator() == LogicalOperator.OR);
			logger.info("crit {}", criteria);
		}
		catch( Exception e ) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	
	@Test
	public void testTen() {
		String filter = FILTER_EIGHT_TRIS;
		try {
			SearchCriteria criteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Assert.assertTrue("size of the criteria should be 3 and is " + criteria.size(), criteria.size() == 3 );
			Assert.assertTrue("logical operator of the criteria", criteria.getOperator() == LogicalOperator.OR);
			logger.info("crit {}", criteria);
		}
		catch( Exception e ) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	

}
