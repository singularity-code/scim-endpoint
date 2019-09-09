package be.mogo.iam.scim.schema;

public enum SchemaAttributeType {
	
	STRING("string"),
	COMPLEX("complex"),
	BOOLEAN("boolean"),
	REFERENCE("reference");
	
	public final String label;
	
	private SchemaAttributeType(String s) {
		this.label = s;
	}
	
	public static SchemaAttributeType fromString(String text) {
        for (SchemaAttributeType b : SchemaAttributeType.values()) {
            if (b.label.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }
	

}
