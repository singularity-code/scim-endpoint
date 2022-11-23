package be.personify.iam.scim.schema;

public class SchemaExtension {
	
	private String schema;
	private boolean required;
	
	private Schema schemaObject;
	
	public String getSchema() {
		return schema;
	}
	
	public void setSchema(String schema) {
		this.schema = schema;
	}
	
	public boolean isRequired() {
		return required;
	}
	
	public void setRequired(boolean required) {
		this.required = required;
	}
	
	public Schema getSchemaObject() {
		return schemaObject;
	}
	public void setSchemaObject(Schema schemaObject) {
		this.schemaObject = schemaObject;
	}
	
	
	
	

}
