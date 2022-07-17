package be.personify.iam.scim;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.springframework.util.Assert;

import be.personify.iam.scim.storage.util.CouchBaseUtil;
import be.personify.util.SortOrder;



public class CouchUtilTest {
	

	private static final Logger logger = LogManager.getLogger(CouchUtilTest.class);
	
	
	@Test
	public void testOne() {
		String sortBy = "userName";
		String sortOrder = null;
		String s = CouchBaseUtil.getSort(sortBy, sortOrder, SortOrder.ascending);
		logger.info("value returned [{}] ", s);
		Assert.isTrue(s.equals(" ORDER BY t.userName asc"), "has to be equal");
	}
	
	
	@Test
	public void testTwo() {
		String sortBy = "userName";
		String sortOrder = "descending";
		String s = CouchBaseUtil.getSort(sortBy, sortOrder, SortOrder.ascending);
		logger.info("value returned [{}] ", s);
		Assert.isTrue(s.equals(" ORDER BY t.userName desc"), "has to be equal");
	}
	
	
	
	@Test
	public void testThree() {
		String sortBy = "userName,externalId";
		String sortOrder = "descending";
		String s = CouchBaseUtil.getSort(sortBy, sortOrder, SortOrder.ascending);
		logger.info("value returned [{}] ", s);
		Assert.isTrue(s.equals(" ORDER BY t.userName desc, t.externalId asc"), "has to be equal");
	}
	
	

}
