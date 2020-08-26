package be.personify.iam.scim.storage;

public enum SearchOperation {

	EQUALS("eq"),
	NOT_EQUALS("ne"),
	CONTAINS("co"),
	STARTS_WITH("sw"),
	ENDS_WITH("ew"),
	PRESENT("pr"),
	GREATER_THEN("gt"),
	GREATER_THEN_OR_EQUAL("ge"),
	LESS_THEN("lt"),
	LESS_THEN_EQUAL("le");
	
	
	
	private String code;
	
	private SearchOperation ( String code ) {
		this.code = code;
	}
	
	
	public static SearchOperation operationFromString(String s) {
		for( SearchOperation o : SearchOperation.values() ) {
			if ( o.code.equalsIgnoreCase(s)) {
				return o;
			}
		}
		return null;
	}
	
}
