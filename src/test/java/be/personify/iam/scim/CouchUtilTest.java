package be.personify.iam.scim;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.util.Assert;

import org.junit.Test;

import be.personify.iam.scim.storage.SortOrder;
import be.personify.iam.scim.storage.util.CouchBaseUtil;



public class CouchUtilTest {
	

	private static final Logger logger = LogManager.getLogger(CouchUtilTest.class);
	
	
	@Test
	public void testOne() {
		String sortBy = "userName";
		String sortOrder = null;
		try {
			String s = CouchBaseUtil.getSort(sortBy, sortOrder, SortOrder.ascending);
			logger.info("value returned [{}] ", s);
			Assert.isTrue(s.equals(" ORDER BY userName asc"), "has to be equal");
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void testTwo() {
		String sortBy = "userName";
		String sortOrder = "descending";
		try {
			String s = CouchBaseUtil.getSort(sortBy, sortOrder, SortOrder.ascending);
			logger.info("value returned [{}] ", s);
			Assert.isTrue(s.equals(" ORDER BY userName desc"), "has to be equal");
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	
	
	@Test
	public void testThree() {
		String sortBy = "userName,externalId";
		String sortOrder = "descending";
		try {
			String s = CouchBaseUtil.getSort(sortBy, sortOrder, SortOrder.ascending);
			logger.info("value returned [{}] ", s);
			Assert.isTrue(s.equals(" ORDER BY userName desc, externalId asc"), "has to be equal");
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	

}
