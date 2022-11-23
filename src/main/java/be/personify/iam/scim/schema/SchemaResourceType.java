package be.personify.iam.scim.schema;

import java.util.List;

public class SchemaResourceType {
	
	
	private List<String> schemas;
	private String id;
	private String name;
	private String endpoint;
	private String description;
	private String schema;
	private List<SchemaExtension> schemaExtensions;
	private SchemaMeta meta;
	
	private Schema schemaObject;
	
	
	public List<String> getSchemas() {
		return schemas;
	}
	public void setSchemas(List<String> schemas) {
		this.schemas = schemas;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEndpoint() {
		return endpoint;
	}
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getSchema() {
		return schema;
	}
	public void setSchema(String schema) {
		this.schema = schema;
	}
	public List<SchemaExtension> getSchemaExtensions() {
		return schemaExtensions;
	}
	public void setSchemaExtensions(List<SchemaExtension> schemaExtensions) {
		this.schemaExtensions = schemaExtensions;
	}
	public SchemaMeta getMeta() {
		return meta;
	}
	public void setMeta(SchemaMeta meta) {
		this.meta = meta;
	}
	
	public Schema getSchemaObject() {
		return schemaObject;
	}
	public void setSchemaObject(Schema schemaObject) {
		this.schemaObject = schemaObject;
	}
	
	
	
	
	
	
	
	
	
	

}
