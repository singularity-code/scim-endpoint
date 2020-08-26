package be.personify.iam.scim.rest;

public class InvalidFilterException extends Exception {
	
	private static final long serialVersionUID = 1960948131227749320L;

	public InvalidFilterException() {
		super();
	}
	
	public InvalidFilterException(String m) {
		super(m);
	}

}
