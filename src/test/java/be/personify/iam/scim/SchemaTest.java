package be.personify.iam.scim;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import be.personify.iam.scim.init.Application;
import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaException;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.util.Constants;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class SchemaTest {
	
	@Autowired
	private SchemaReader schemaReader;
	
	private static final Logger logger = LogManager.getLogger(SchemaTest.class);
	

	@Test
	public void testEmptyMap() {	
		Schema userSchema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> map = new HashMap<String, Object>();
		try {
			schemaReader.validate(schemaReader.getResourceTypeByName(userSchema.getName()), map,true, "GET");
		}
		catch ( SchemaException se ) {
			logger.info("exception {}", se.getMessage());
			Assert.assertTrue(se.getMessage().equals("schemas does not contain main schema urn:ietf:params:scim:schemas:core:2.0:User"));
			return ;
		}
		fail("No schema exception thrown");
	}
	
	
	@Test
	public void testEmptyMapWithSchema() {	
		Schema userSchema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> map = new HashMap<String, Object>();
		addSchema(map);
		try {
			schemaReader.validate(schemaReader.getResourceTypeByName(userSchema.getName()), map,true, "GET");
		}
		catch ( SchemaException se ) {
			logger.info("exception {}", se.getMessage());
			Assert.assertTrue(se.getMessage().contains("is required"));
			return ;
		}
		fail("No schema exception thrown");
	}


	private void addSchema(Map<String, Object> map) {
		List<String> schemas = new ArrayList<>();
		schemas.add("urn:ietf:params:scim:schemas:core:2.0:User");
		map.put("schemas", schemas);
	}
	
	
	
	
	
	@Test
	public void testMinimal() {	
		Map<String,Object> map = new HashMap<String, Object>();
		addSchema(map);
		
		map.put("userName", "username");
		map.put("externalId", "externalId");
		Schema userSchema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		try {
			schemaReader.validate(schemaReader.getResourceTypeByName(userSchema.getName()), map, true,"GET");
		}
		catch ( SchemaException se ) {
			fail("No schema exception thrown " + se.getMessage());
		}
	}
	
	
	@Test
	public void testMinimalWithInvalidOptionalComplexMultiOne() {	
		Map<String,Object> map = new HashMap<String, Object>();
		map.put("userName", "username");
		map.put("externalId", "externalId");
		map.put("emails", "another email");
		Schema userSchema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		try {
			schemaReader.validate(schemaReader.getResourceTypeByName(userSchema.getName()), map, true, "GET");
		}
		catch ( SchemaException se ) {
			//se.printStackTrace();
			return;
		}
		fail("No schema exception thrown ");
	}
	
	
	@Test
	public void testMinimalWithInvalidOptionalComplexMultiTwo() {	
		Map<String,Object> map = new HashMap<String, Object>();
		map.put("userName", "username");
		map.put("externalId", "externalId");
		String[] emails = new String[] {"another email"};
		map.put("emails", emails);
		Schema userSchema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		try {
			schemaReader.validate(schemaReader.getResourceTypeByName(userSchema.getName()), map, true, "GET");
		}
		catch ( SchemaException se ) {
			//se.printStackTrace();
			return;
		}
		fail("No schema exception thrown ");
	}
	
	
	@Test
	public void testMinimalWithInvalidOptionalComplexMultiThree() {	
		
		Schema userSchema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		
		Map<String,Object> map = new HashMap<String, Object>();
		map.put("userName", "username");
		map.put("externalId", "externalId");
		
		List<Map<String,Object>> mailList = new ArrayList<Map<String,Object>>();
		Map<String,Object> mailMap = new HashMap<String, Object>();
		mailMap.put("test", "test");
		mailList.add(mailMap);
		
		map.put("emails", mailList);
		try {
			schemaReader.validate(schemaReader.getResourceTypeByName(userSchema.getName()), map, true, "GET");
		}
		catch ( SchemaException se ) {
			//se.printStackTrace();
			return;
		}
		fail("No schema exception thrown ");
	}
	
	
	@Test
	public void testMinimalWithValidOptionalComplexMultiOne() {	
		
		Schema userSchema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		
		Map<String,Object> map = new HashMap<String, Object>();
		addSchema(map);
		
		
		map.put("userName", "username");
		map.put("externalId", "externalId");
		
		List<Map<String,Object>> mailList = new ArrayList<Map<String,Object>>();
		Map<String,Object> mailMap = new HashMap<String, Object>();
		mailMap.put("value", "wouter@test.be");
		mailList.add(mailMap);
		
		map.put("emails", mailList);
		try {
			schemaReader.validate(schemaReader.getResourceTypeByName(userSchema.getName()), map, true, "GET");
		}
		catch ( SchemaException se ) {
			fail("schema exception thrown " + se.getMessage());
		}
		
	}
	
	
	@Test
	public void testInvalidExtension() {	
		Map<String,Object> map = new HashMap<String, Object>();
		addSchema(map);
		((List)map.get("schemas")).add("invalid:extension");
		
		map.put("userName", "username");
		map.put("externalId", "externalId");
		Schema userSchema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		try {
			schemaReader.validate(schemaReader.getResourceTypeByName(userSchema.getName()), map, true,"GET");
		}
		catch ( SchemaException se ) {
			Assert.assertTrue("error message has to contain 'invalid extension'" , se.getMessage().contains("invalid extension"));
			return;
		}
		fail("No schema exception thrown ");
	}
	
	
	
	
	
	@Test
	public void testMinimalWithValidOptionalComplexMultiOneWithInvalidAttribute() {	
		
		Schema userSchema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		
		Map<String,Object> map = new HashMap<String, Object>();
		addSchema(map);
		
		
		map.put("userName", "username");
		map.put("externalId", "externalId");
		map.put("invalidAttributeName", "invalidValue");
		
		List<Map<String,Object>> mailList = new ArrayList<Map<String,Object>>();
		Map<String,Object> mailMap = new HashMap<String, Object>();
		mailMap.put("value", "wouter@test.be");
		mailList.add(mailMap);
		
		map.put("emails", mailList);
		try {
			schemaReader.validate(schemaReader.getResourceTypeByName(userSchema.getName()), map, true, "GET");
		}
		catch ( SchemaException se ) {
			Assert.assertTrue("error message has to contain 'is not defined in schemas'" , se.getMessage().contains("is not defined in schemas"));
			return;
		}
		fail("exception has to be thrown because invalid attribute");
		
	}
	
	
	
	
	@Test
	public void testEnterpriseUserWithCorrectSchema() {	
		
		Schema userSchema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		
		Map<String,Object> map = new HashMap<String, Object>();
		addSchema(map);
		((List)map.get("schemas")).add("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User");
		
		
		map.put("userName", "username");
		map.put("externalId", "externalId");
		map.put("employeeNumber", "1025001");
		
		List<Map<String,Object>> mailList = new ArrayList<Map<String,Object>>();
		Map<String,Object> mailMap = new HashMap<String, Object>();
		mailMap.put("value", "wouter@test.be");
		mailList.add(mailMap);
		
		map.put("emails", mailList);
		try {
			schemaReader.validate(schemaReader.getResourceTypeByName(userSchema.getName()), map, true, "GET");
		}
		catch ( SchemaException se ) {
			fail("exception should not be thrown because valid attributes/schema");
		}
		
		
	}
	
	
	
	@Test
	public void testEnterpriseUserWithoutCorrectSchema() {	
		
		Schema userSchema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		
		Map<String,Object> map = new HashMap<String, Object>();
		addSchema(map);
		
		map.put("userName", "username");
		map.put("externalId", "externalId");
		map.put("employeeNumber", "1025001");
		
		List<Map<String,Object>> mailList = new ArrayList<Map<String,Object>>();
		Map<String,Object> mailMap = new HashMap<String, Object>();
		mailMap.put("value", "wouter@test.be");
		mailList.add(mailMap);
		
		map.put("emails", mailList);
		try {
			schemaReader.validate(schemaReader.getResourceTypeByName(userSchema.getName()), map, true, "GET");
		}
		catch ( SchemaException se ) {
			return;
		}
		
		fail("exception should be thrown because valid attributes/schema");
		
		
	}
	
	
	
	
	
	
	
	

}
