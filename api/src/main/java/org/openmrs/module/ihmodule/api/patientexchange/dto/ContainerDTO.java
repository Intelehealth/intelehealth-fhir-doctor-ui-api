package org.openmrs.module.ihmodule.api.patientexchange.dto;

public class ContainerDTO {
	
	private String imageName;
	
	private int privatePort;
	
	private int publicPort;
	
	private String ipAddress;
	
	private int daemonPort;
	
	public String getImageName() {
		return imageName;
	}
	
	public void setImageName(String imageName) {
		this.imageName = imageName;
	}
	
	public int getPrivatePort() {
		return privatePort;
	}
	
	public void setPrivatePort(int privatePort) {
		this.privatePort = privatePort;
	}
	
	public int getPublicPort() {
		return publicPort;
	}
	
	public void setPublicPort(int publicPort) {
		this.publicPort = publicPort;
	}
	
	public String getIpAddress() {
		return ipAddress;
	}
	
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	
	public int getDaemonPort() {
		return daemonPort;
	}
	
	public void setDaemonPort(int daemonPort) {
		this.daemonPort = daemonPort;
	}
	
	@Override
	public String toString() {
		return "ContainerDTO [imageName=" + imageName + ", privatePort=" + privatePort + ", publicPort=" + publicPort
		        + ", ipAddress=" + ipAddress + ", daemonPort=" + daemonPort + "]";
	}
	
}
