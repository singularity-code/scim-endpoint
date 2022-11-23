package be.personify.iam.scim.authentication;

import java.util.List;

public class Consumer {
	
	private String clientid;
	private String secret;
	private List<String> roles;
	private String tenant;
	
	public Consumer( String clientid, String secret) {
		this.clientid = clientid;
		this.secret = secret;
	}
	
	
	public Consumer( String clientid, String secret, List<String> roles, String tenant) {
		this.clientid = clientid;
		this.secret = secret;
		this.roles = roles;
		this.tenant = tenant;
	}
	
	
	public String getClientid() {
		return clientid;
	}
	public void setClientid(String clientid) {
		this.clientid = clientid;
	}
	public String getSecret() {
		return secret;
	}
	public void setSecret(String secret) {
		this.secret = secret;
	}
	public List<String> getRoles() {
		return roles;
	}
	public void setRoles(List<String> roles) {
		this.roles = roles;
	}
	public String getTenant() {
		return tenant;
	}
	public void setTenant(String tenant) {
		this.tenant = tenant;
	}
	
	
	

}
