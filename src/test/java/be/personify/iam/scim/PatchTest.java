package be.personify.iam.scim;


import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import be.personify.iam.scim.init.Application;
import be.personify.iam.scim.rest.PatchUtils;
import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.util.Constants;
import be.personify.util.scim.PatchOperation;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class PatchTest {
	
	@Autowired
	private SchemaReader schemaReader;
	
	
	@Autowired
	private PatchUtils patchUtils;
	

	private static final Logger logger = LogManager.getLogger(PatchTest.class);

	
	
	@Test
	public void testPatchRolesSingleRoleRolesNotPresent() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> entity = new HashMap<String,Object>();
		
		Map<String,Object> role = new HashMap<>();
		role.put("value", "role1");
		
		patchUtils.patchEntity(entity, PatchOperation.add, "roles", role, schema );
		
		logger.info("patched entity {}", entity );
		
		Assert.isTrue( ((List)entity.get("roles")).size() == 1 , "has to have one role");
	}
	
	
	@Test
	public void testPatchBoolean() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> entity = new HashMap<String,Object>();
		
		entity.put("active", "true");
		
		patchUtils.patchEntity(entity, PatchOperation.replace, "active", Boolean.FALSE, schema );
		
		logger.info("patched entity {}", entity );
		
		Assert.isTrue( entity.get("active").equals(Boolean.FALSE) , "has to have one role");
	}
	
	
	@Test
	public void testPatchRolesSingleRoleRolesPresentButEmpty() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> entity = new HashMap<String,Object>();
		entity.put("roles", new ArrayList());
		
		Map<String,Object> role = new HashMap<>();
		role.put("value", "role1");
		
		patchUtils.patchEntity(entity, PatchOperation.add, "roles", role, schema );
		
		logger.info("patched entity {}", entity );
		
		Assert.isTrue( ((List)entity.get("roles")).size() == 1 , "has to have one role");
	}
	
	
	
	@Test
	public void testPatchRoleList() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> entity = new HashMap<String,Object>();
		entity.put("roles", new ArrayList());
		
		Map<String,Object> role_one = new HashMap<>();
		role_one.put("value", "role1");
		
		Map<String,Object> role_two = new HashMap<>();
		role_two.put("value", "role2");
		
		List roleList = new ArrayList<>();
		roleList.add(role_one);
		roleList.add(role_two);
		
		patchUtils.patchEntity(entity, PatchOperation.add, "roles", roleList, schema );
		
		logger.info("entity {}", entity);
		Assert.isTrue( ((List)entity.get("roles")).size() == 2 , "has to have two roles");
	}
	
	
	
	
	@Test
	public void testPatchRoleListUnique() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> entity = new HashMap<String,Object>();
		entity.put("roles", new ArrayList());
		
		Map<String,Object> role_one = new HashMap<>();
		role_one.put("value", "role1");
		
		Map<String,Object> role_two = new HashMap<>();
		role_two.put("value", "role2");
		
		Map<String,Object> role_twocopy = new HashMap<>();
		role_twocopy.put("value", "role2");
		
		List roleList = new ArrayList<>();
		roleList.add(role_one);
		roleList.add(role_two);
		
		
		patchUtils.patchEntity(entity, PatchOperation.add, "roles", roleList, schema );
		
		logger.info("entity {}", entity);
		Assert.isTrue( ((List)entity.get("roles")).size() == 2 , "has to have two roles");
		
		roleList = new ArrayList<>();
		roleList.add(role_twocopy);
		
		patchUtils.patchEntity(entity, PatchOperation.add, "roles", roleList, schema );
		
		Assert.isTrue( ((List)entity.get("roles")).size() == 2 , "has to have two roles");
		
	}
	
	
	
	
	
	@Test
	public void testPatchRoleListWithRolesPresent() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> entity = new HashMap<String,Object>();
		List currentRoleList = new ArrayList<>();
		Map<String,Object> role_zero = new HashMap<>();
		role_zero.put("value", "role0");
		currentRoleList.add(role_zero);
		entity.put("roles", currentRoleList);
		
		Map<String,Object> role_one = new HashMap<>();
		role_one.put("value", "role1");
		
		Map<String,Object> role_two = new HashMap<>();
		role_two.put("value", "role2");
		
		List roleList = new ArrayList<>();
		roleList.add(role_one);
		roleList.add(role_two);
		
		patchUtils.patchEntity(entity, PatchOperation.add, "roles", roleList, schema );
		
		logger.info("entity {}", entity);
		Assert.isTrue( ((List)entity.get("roles")).size() == 3 , "has to have 3 roles");
	}
	
	
	
	@Test
	public void testPatchAddString() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> entity = new HashMap<String,Object>();
		
		patchUtils.patchEntity(entity, PatchOperation.add, "title", "mister", schema );
		
		logger.info("entity {}", entity);
		Assert.isTrue( ((String)entity.get("title")).equals("mister") , "has to be mister");
	}
	
	
	@Test
	public void testPatchAddStringExistingReplace() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> entity = new HashMap<String,Object>();
		entity.put("title", "misses");
		
		patchUtils.patchEntity(entity, PatchOperation.replace, "title", "mister", schema );
		
		logger.info("entity {}", entity);
		Assert.isTrue( ((String)entity.get("title")).equals("mister") , "has to be mister");
	}
	
	
	@Test
	public void testPatchString() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_ENTERPRISEUSER);
		Map<String,Object> entity = new HashMap<String,Object>();
		entity.put("department", "dep one");
		
		patchUtils.patchEntity(entity, PatchOperation.replace, "department", "dep two", schema );
		
		logger.info("entity {}", entity);
		Assert.isTrue( ((String)entity.get("department")).equals("dep two") , "has to be dep two");
	}
	
	
	
	@Test
	public void testPatchNameSubAttribute() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> entity = new HashMap<String,Object>();
		Map<String,Object> name = new HashMap<String,Object>();
		name.put("familyName", "Simpson");
		entity.put("name", name);
		
		patchUtils.patchEntity(entity, PatchOperation.replace, "name.familyName", "Samson", schema );
		
		logger.info("entity {}", entity);
		Assert.isTrue( ((Map)entity.get("name")).get("familyName").equals("Samson") , "has to be Samson");
	}
	
	 
	@Test
	public void testPatchNameSubAttributeWithNull() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> entity = new HashMap<String,Object>();
		
		try {
			patchUtils.patchEntity(entity, PatchOperation.replace, "name.familyName", "Samson", schema );
		}
		catch(Exception e ) {
			logger.info("it's ok");
		}
		
		
	}
	
	
	@Test
	public void testPatchNameSubAttributeWithEmptyName() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> entity = new HashMap<String,Object>();
		Map<String,Object> name = new HashMap<String,Object>();
		entity.put("name", name);
		
		patchUtils.patchEntity(entity, PatchOperation.replace, "name.familyName", "Samson", schema );
		Assert.isTrue( entity.get("name") != null , "has to be Samson");
		
		logger.info("entity {}", entity);
		Assert.isTrue( ((Map)entity.get("name")).get("familyName").equals("Samson") , "has to be Samson");
	}
	
	
	
	
	
	@Test
	public void testPatchEnterpriseWithUrn() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> entity = new HashMap<String,Object>();
		Map<String,Object> enterprise = new HashMap<String,Object>();
		enterprise.put("department", "dep one");
		entity.put("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User", enterprise);
		
		
		patchUtils.patchEntity(entity, PatchOperation.replace, "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:department", "dep two", schema );
		
		logger.info("entity {}", entity);
		
		Map<String,Object> ent = (Map)entity.get("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User");
		
		if ( ent != null ) {
			Assert.isTrue( ((String)ent.get("department")).equals("dep two") , "has to be dep two");
		}
		else {
			fail("custom enterprise objectmap not found");
		}
	}
	
	
	
	@Test
	public void testPatchAddWithUrn() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> entity = new HashMap<String,Object>();

		patchUtils.patchEntity(entity, PatchOperation.add, "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:department", "dep two", schema );
		
		logger.info("entity {}", entity);
		Assert.isTrue( ((String)entity.get("department")).equals("dep two") , "has to be dep two");
	}
	
	
	
	
	
	//for azure?
	@Test
	public void testGetPathTwo() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> entity = new HashMap<String,Object>();
		entity.put("roles", new ArrayList());
		
		patchUtils.patchEntity(entity, PatchOperation.add, "roles[primary eq \"True\"].value", "role1", schema );
		
		logger.info("entity {}", entity);
		
		Assert.isTrue( ((List)entity.get("roles")).size() == 1 , "has to have one role");
		
	}
	
	
	//for azure?
	@Test
	public void testGetPathThree() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> entity = new HashMap<String,Object>();
		entity.put("roles", new ArrayList());
		
		patchUtils.patchEntity(entity, PatchOperation.add, "phoneNumbers[type eq \"mobile\"].value", "090-1234-5678", schema );
		
		logger.info("entity {}", entity);
			
		Assert.isTrue( ((List)entity.get("phoneNumbers")).size() == 1 , "has to have one phoneNumber");
		
	}
	
	
	
	
	//for azure?
	@Test
	public void testGetPathFour() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> entity = new HashMap<String,Object>();
		
		
		Map<String,String> existingPhone  = new HashMap<>();
		existingPhone.put("type", "fixed");
		existingPhone.put("value", "011141144");
		List phoneNumbers = new ArrayList<>();
		phoneNumbers.add(existingPhone);
		entity.put("phoneNumbers", phoneNumbers);
		
		patchUtils.patchEntity(entity, PatchOperation.add, "phoneNumbers[type eq \"mobile\"].value", "090-1234-5678", schema );
			
		logger.info("entity {}", entity);
				
		Assert.isTrue( ((List)entity.get("phoneNumbers")).size() == 2 , "has to have two phoneNumbers");
			
	}
	
	
	
	//for azure?
	@Test
	public void testGetPathFive() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_USER);
		Map<String,Object> entity = new HashMap<String,Object>();
			
			
		Map<String,String> existingPhone  = new HashMap<>();
		existingPhone.put("type", "mobile");
		existingPhone.put("value", "tobereplaced");
		List phoneNumbers = new ArrayList<>();
		phoneNumbers.add(existingPhone);
		entity.put("phoneNumbers", phoneNumbers);
			
		patchUtils.patchEntity(entity, PatchOperation.replace, "phoneNumbers[type eq \"mobile\"].value", "090-1234-5678", schema );
			
		logger.info("entity {}", entity);
		
		List p = (List)entity.get("phoneNumbers");
					
		Assert.isTrue( p.size() == 1 , "has to have one phoneNumber");
		
		Map m = (Map)p.get(0);
		Assert.isTrue( m.get("value").equals("090-1234-5678") , "phoneNumber has to be overwritten to 090-1234-5678");
		
				
	}
	
	
	
	
	@Test
	public void testPatchGroupOne() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_GROUP);
		Map<String,Object> entity = new HashMap<String,Object>();
		
		String value = "222222222";
		
		patchUtils.patchEntity(entity, PatchOperation.add, "members", value, schema );
		
		logger.info("entity {}", entity);
		
		List members = (List)entity.get("members");
		if ( members != null ) {
			Assert.isTrue( members.size() == 1 , "has to be one length");
			Map firstEntry = (Map)members.get(0);
			Assert.isTrue( firstEntry.get("value").equals(value) , "has to be value " + value);
		}
		else {
			fail("no members found");
		}
		
		
	}
	
	
	
	
	@Test
	public void testPatchGroupTwo() {
		Schema schema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_GROUP);
		Map<String,Object> entity = new HashMap<String,Object>();
		
		String value = "222222222";
		Map m = new HashMap<>();
		m.put("value", value);
		
		List members = new ArrayList<>();
		members.add(m);
		entity.put("members", members);
		
		patchUtils.patchEntity(entity, PatchOperation.add, "members", value, schema );
		
		logger.info("entity {}", entity);
		
		members = (List)entity.get("members");
		if ( members != null ) {
			Assert.isTrue( members.size() == 1 , "has to be one length and is " + members.size());
			Map firstEntry = (Map)members.get(0);
			Assert.isTrue( firstEntry.get("value").equals(value) , "has to be value " + value);
		}
		else {
			fail("no members found");
		}
		
	}
	
	
	
	
	
	

}
