package be.personify.iam.scim;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	

	@Test
	public void testEmptyMap() {	
		Schema userSchema = schemaReader.getSchemaByResourceType(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> map = new HashMap<String, Object>();
		try {
			schemaReader.validate(userSchema, map,true);
		}
		catch ( SchemaException se ) {
			return ;
		}
		fail("No schema exception thrown");
	}
	
	
	
	@Test
	public void testMinimal() {	
		Map<String,Object> map = new HashMap<String, Object>();
		map.put("userName", "username");
		map.put("externalId", "externalId");
		Schema userSchema = schemaReader.getSchemaByResourceType(Constants.RESOURCE_TYPE_USER);
		try {
			schemaReader.validate(userSchema, map, true);
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
		Schema userSchema = schemaReader.getSchemaByResourceType(Constants.RESOURCE_TYPE_USER);
		try {
			schemaReader.validate(userSchema, map, true);
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
		Schema userSchema = schemaReader.getSchemaByResourceType(Constants.RESOURCE_TYPE_USER);
		try {
			schemaReader.validate(userSchema, map, true);
		}
		catch ( SchemaException se ) {
			//se.printStackTrace();
			return;
		}
		fail("No schema exception thrown ");
	}
	
	
	@Test
	public void testMinimalWithInvalidOptionalComplexMultiThree() {	
		
		Schema userSchema = schemaReader.getSchemaByResourceType(Constants.RESOURCE_TYPE_USER);
		
		Map<String,Object> map = new HashMap<String, Object>();
		map.put("userName", "username");
		map.put("externalId", "externalId");
		
		List<Map<String,Object>> mailList = new ArrayList<Map<String,Object>>();
		Map<String,Object> mailMap = new HashMap<String, Object>();
		mailMap.put("test", "test");
		mailList.add(mailMap);
		
		map.put("emails", mailList);
		try {
			schemaReader.validate(userSchema, map, true);
		}
		catch ( SchemaException se ) {
			//se.printStackTrace();
			return;
		}
		fail("No schema exception thrown ");
	}
	
	
	@Test
	public void testMinimalWithValidOptionalComplexMultiOne() {	
		
		Schema userSchema = schemaReader.getSchemaByResourceType(Constants.RESOURCE_TYPE_USER);
		
		Map<String,Object> map = new HashMap<String, Object>();
		map.put("userName", "username");
		map.put("externalId", "externalId");
		
		List<Map<String,Object>> mailList = new ArrayList<Map<String,Object>>();
		Map<String,Object> mailMap = new HashMap<String, Object>();
		mailMap.put("value", "wouter@test.be");
		mailList.add(mailMap);
		
		map.put("emails", mailList);
		try {
			schemaReader.validate(userSchema, map, true);
		}
		catch ( SchemaException se ) {
			fail("schema exception thrown " + se.getMessage());
		}
		
	}
	
	
	
	
	
	
	
	

}
