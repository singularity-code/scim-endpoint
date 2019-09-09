package be.mogo.iam.scim.schema;

import java.util.List;

public class Schema {
	
	private String id;
	private String name;
	private String description;
	private List<SchemaAttribute> attributes;
	private SchemaMeta meta;
	
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
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public List<SchemaAttribute> getAttributes() {
		return attributes;
	}
	
	public void setAttributes(List<SchemaAttribute> attributes) {
		this.attributes = attributes;
	}
	
	public SchemaMeta getMeta() {
		return meta;
	}
	
	public void setMeta(SchemaMeta meta) {
		this.meta = meta;
	}
	
	
	
}
