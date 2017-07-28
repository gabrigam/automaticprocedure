package com.ibm.bancaintesa.connection;

import java.beans.MethodDescriptor;
import java.beans.ParameterDescriptor;
import java.beans.SimpleBeanInfo;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

public class RestBeanInfo extends SimpleBeanInfo {

	@SuppressWarnings("rawtypes")
	private Class beanClass = Rest.class;

	@SuppressWarnings("unchecked")
	@Override
	public MethodDescriptor[] getMethodDescriptors() {
		try {
			Method method = beanClass.getMethod("doRest", String.class, String.class, String.class, HashMap.class, String.class, String.class,boolean.class);
			
			Method methodDoRestAlias = beanClass.getMethod("doRest", String.class, String.class, String.class, HashMap.class, String.class,boolean.class);
		
			
			if (method == null) {
				System.out.println("Unable to find method.");
				return null;
			}
			// (String command, String urlString, String content, HashMap headerMap, String userid, String password)
			ArrayList<ParameterDescriptor> al = new ArrayList<ParameterDescriptor>();
			
			ArrayList<ParameterDescriptor> alDoRestAlias = new ArrayList<ParameterDescriptor>();
			
			ParameterDescriptor param = new ParameterDescriptor();
			param.setShortDescription("Command");
			param.setDisplayName("Command");
			al.add(param);
			alDoRestAlias.add(param);
			
			
			param = new ParameterDescriptor();
			param.setShortDescription("URL");
			param.setDisplayName("URL");
			al.add(param);
			alDoRestAlias.add(param);
			
			param = new ParameterDescriptor();
			param.setShortDescription("Content");
			param.setDisplayName("Content");
			al.add(param);
			alDoRestAlias.add(param);
			
			param = new ParameterDescriptor();
			param.setShortDescription("Header Map");
			param.setDisplayName("Header Map");
			al.add(param);
			alDoRestAlias.add(param);
			
			param = new ParameterDescriptor();
			param.setShortDescription("Userid");
			param.setDisplayName("Userid");
			al.add(param);
			
			param = new ParameterDescriptor();
			param.setShortDescription("Password");
			param.setDisplayName("Password");
			al.add(param);
			
			param = new ParameterDescriptor();
			param.setShortDescription("aliasAuthName");
			param.setDisplayName("aliasAuthName");
			alDoRestAlias.add(param);
			
			param = new ParameterDescriptor();
			param.setShortDescription("debugMode");
			param.setDisplayName("debugMode");
			al.add(param);
			alDoRestAlias.add(param);
			
			


			MethodDescriptor methodDescriptor = new MethodDescriptor(method,  al.toArray(new ParameterDescriptor[0]));
			MethodDescriptor methodDescriptorDoRestAlias = new MethodDescriptor(methodDoRestAlias,  alDoRestAlias.toArray(new ParameterDescriptor[0]));
			
			
			return new MethodDescriptor[] { methodDescriptor,methodDescriptorDoRestAlias };
		} catch (Exception e) {
			e.printStackTrace();
		}

		return super.getMethodDescriptors();
	} // End of getMethodDescriptors
} // End of class
// End of file