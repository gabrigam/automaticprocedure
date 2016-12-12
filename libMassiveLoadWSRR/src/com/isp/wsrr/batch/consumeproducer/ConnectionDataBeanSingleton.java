package com.isp.wsrr.batch.consumeproducer;

import com.isp.wsrr.batch.consumeproducer.exception.LIBWSRRTException;


/**
 * 
 * The BPM connection credential are passed inside an object of type : ConnectionDataBean <p>
 * that contains this member variables:
 * <p>
 * BPM server name
 * <p>
 * port
 * <p>
 * user
 * <p>
 * password
 *<p>
 *this information are present in a property file passed to the jvm by the option:
 *<p><h4>-Dbpm.connection.data</h4>
 *<p>
 *example:
 *<p>
 *-Dbpm.connection.data=c:\bpmconnection.property
 *
 * @author Primeur
 * @version 1.0
 * 
 *  */

public class ConnectionDataBeanSingleton {

	private static ConnectionDataBeanSingleton instance=null; 

	private  String url;
	private  String user;
	private  String password;
	private  String endpointTrace;
	private  int timout;
	private  String lastInvocationTs;
    private  boolean trace;
	

     //rimane sololo scheletro ma i dati di connessione sono passati mediante args[]
    
	private ConnectionDataBeanSingleton() throws LIBWSRRTException {

		String url=System.getProperty("LIBLKPWSRRURL");
		String user=System.getProperty("LIBLKPWSRRUSER");
		String password=System.getProperty("LIBLKPWSRRPASSWORD");
		
		String errorurl="";
		String erroruser="";
		String errorpassword="";
		boolean error=false;
		
		StringBuffer sb=new StringBuffer();

		if (url == null ) {		   
			errorurl=" NO value found for LIBLKPWSRRURL environment variable ";
			error=false;
			
		} else {

			if (url.contains("https://")) {
				if (user == null ) {		   
					erroruser=" NO value found for LIBLKPWSRRUSER environment variable ";
					error=true;
				}
				if (password== null ) {		   
					errorpassword=" NO value found for LIBLKPWSRRPASSWORD environment variable ";
					error=false;
				}
			}
		}


	}

	/**
	 * Return the instance with BPM connection data
	 *
	 *@throws Exception ( "Error in reading data from the property -Dbpm.connection.data )
	 *  */
	public static synchronized ConnectionDataBeanSingleton setData() throws Exception {

		if (instance == null) {
			instance = new ConnectionDataBeanSingleton();
		}
		return instance;	
	}

	public  String getUrl() {
		return url;
	}
	protected  void setUrl(String url) {
		this.url = url;
	}

	public  String getUser() {
		return user;
	}
	protected void setUser(String user) {
		this.user = user;
	}
	public String getPassword() {
		return password;
	}
	protected   void setPassword(String password) {
		this.password = password;
	}
	public String getEndpointTrace() {
		return endpointTrace;
	}

	protected void setEndpointTrace(String endpointTrace) {
		this.endpointTrace = endpointTrace;
	}

	public boolean getTrace() {
		return trace;
	}

	protected void setTrace(boolean trace) {
		this.trace = trace;
	}

	public String getLastInvocationTs() {
		return lastInvocationTs;
	}

	protected void setLastInvocationTs(String lastInvocationTs) {
		this.lastInvocationTs = lastInvocationTs;
	}
	
	public int getTimout() {
		return timout;
	}

	public void setTimout(int timout) {
		this.timout = timout;
	}


}