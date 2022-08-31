package be.personify.iam.scim;


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
	public void testNothing() {
		
	}
	
	
//	@Test
//	public void testPatchRole() {
//		Schema schema = schemaReader.getSchemaByResourceType(Constants.RESOURCE_TYPE_USER);
//		Map<String,Object> entity = new HashMap<String,Object>();
//		entity.put("roles", new ArrayList());
//		
//		Map<String,Object> role = new HashMap<>();
//		role.put("value", "role1");
//		
//		patchUtils.patchEntity(entity, PatchOperation.add, "roles", role, schema );
//		
//		logger.info("patched entity {}", entity );
//		
//		Assert.isTrue( ((List)entity.get("roles")).size() == 1 , "has to have one role");
//	}
//	
//	
//	@Test
//	public void testPatchRoleList() {
//		Schema schema = schemaReader.getSchemaByResourceType(Constants.RESOURCE_TYPE_USER);
//		Map<String,Object> entity = new HashMap<String,Object>();
//		entity.put("roles", new ArrayList());
//		
//		Map<String,Object> role_one = new HashMap<>();
//		role_one.put("value", "role1");
//		
//		Map<String,Object> role_two = new HashMap<>();
//		role_two.put("value", "role2");
//		
//		List roleList = new ArrayList<>();
//		roleList.add(role_one);
//		roleList.add(role_two);
//		
//		patchUtils.patchEntity(entity, PatchOperation.add, "roles", roleList, schema );
//		
//		logger.info("entity {}", entity);
//		Assert.isTrue( ((List)entity.get("roles")).size() == 2 , "has to have two roles");
//	}
//	
//	
//	
//	
//	@Test
//	public void testGetPathTwo() {
//		Schema schema = schemaReader.getSchemaByResourceType(Constants.RESOURCE_TYPE_USER);
//		Map<String,Object> entity = new HashMap<String,Object>();
//		entity.put("roles", new ArrayList());
//		
//		patchUtils.patchEntity(entity, PatchOperation.add, "roles[primary eq \"True\"].value", "role1", schema );
//		Assert.isTrue( ((List)entity.get("roles")).size() == 1 , "has to have one role");
//		
//	}
	
	
	

}
