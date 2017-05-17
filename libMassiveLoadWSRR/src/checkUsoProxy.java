

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.isp.wsrr.utility.WSRRUtility;

public class checkUsoProxy {

	public static void main(String[] args) throws Exception {
		WSRRUtility wsrrutility = new WSRRUtility();


		ConnectionDataBeanSingleton cdb = null;

		try {
			cdb = ConnectionDataBeanSingleton.setData(); // fake è stata

		} catch (Exception e1) {
			e1.printStackTrace();
			System.exit(0);
		}

		cdb.setUrl(args[0]);

		if (args[0] != null && args[0].contains("https")) {

			cdb.setUser(args[2]);
			cdb.setPassword(args[3]);
		}
		
		int totaleSOAProxy=0;
		int totaleRESTProxy=0;
		int totaleCALLABLEProxy=0;
		
		JSONArray allNAMES=new JSONArray();
		JSONArray rootEndpoint=new JSONArray();
		JSONArray rootToSOAPProxy=new JSONArray();
		JSONArray rootToRESTProxyy=new JSONArray();
		JSONArray rootToCALLABLEProxy=new JSONArray();

		JSONArray allSOPEN=new JSONArray();

		JSONArray jsae = null;
		JSONArray jsae1 = null;
		JSONArray jsae2 = null;
		JSONArray jsae3 = null;
		JSONArray jsae4 = null;
		JSONObject jso = null;
		JSONObject jso1 = null;
		JSONObject jso2 = null;
		JSONObject jso3 = null;
		JSONObject jso3_ = null;
		JSONObject jso4 = null;
		JSONObject jso5 = null;
		JSONObject jso6 = null;
		JSONObject jso7 = null;
		String value = null;


		String rootbsrURI=null;
		String rootName=null;
		String endpointbsrURI=null;
		String endpointName=null;
		String proxybsrURI=null;
		String proxyName=null;
		String proxyPrimaryType=null;
		String v1=null;
		String v2=null;
		String v3=null;

		String proxySOAP=null;
		String proxyREST=null;
		String proxyCALLABLE=null;
		String endpoint=null;



		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println(dateFormat.format(date));
		System.out.println("Controlli Proxies\n");
		System.out.println("Tipo Servizio richiesto : "+args[1]+"\n");
		System.out.println("--------------------------------------------------------------------------------------------------------------------");
		System.out.println("Inizio elaborazione...");
		
		String baseQuery="/Metadata/JSON/PropertyQuery?query=/WSRR/GenericObject[@primaryType='http://www.ibm.com/xmlns/prod/serviceregistry/profile/v6r3/GovernanceEnablementModel%23%TYPE%ServiceVersion']";
		
		baseQuery=baseQuery.replaceAll("%TYPE%", args[1]);
		
		allNAMES=wsrrutility.getObjectPropertiesDataFromGeneralQueryExtended(baseQuery,"&p1=name&p2=bsrURI", cdb.getUrl(), cdb.getUser(),cdb.getPassword());
		
		int i = allNAMES.length();
		int j = 0;
		
		System.out.println("Trovati : "+i + " Servizi da Analizzare..\n");
		
		while (i > j) {
			jsae = (JSONArray) allNAMES.getJSONArray(j);
			jso = (JSONObject) jsae.getJSONObject(0);
			jso1 = (JSONObject) jsae.getJSONObject(1);

			v1=(String) jso.get("name");
			v2=(String) jso1.get("name");

			if (v1.equals("name")) rootName=(String) jso.get("value");
			if (v1.equals("bsrURI")) rootbsrURI=(String) jso.get("value");

			if (v2.equals("name")) rootName=(String) jso1.get("value");
			if (v2.equals("bsrURI")) rootbsrURI=(String) jso1.get("value");

			endpoint="/Metadata/JSON/PropertyQuery?query=/WSRR/GenericObject[@bsrURI=%27%BSRURI%%27]/gep63_provides%28.%29/gep63_availableEndpoints%28.%29";

			endpoint=endpoint.replaceAll("%BSRURI%", rootbsrURI);

			rootEndpoint=wsrrutility.getObjectPropertiesDataFromGeneralQueryExtended(endpoint, "&p1=name&p2=bsrURI&p3=primaryType", cdb.getUrl(), cdb.getUser(),cdb.getPassword());

			int ii = rootEndpoint.length();
			int jj = 0;
			while (ii > jj) {
				jsae1 = (JSONArray) rootEndpoint.getJSONArray(jj);
				jso2 = (JSONObject) jsae1.getJSONObject(0);
				jso3 = (JSONObject) jsae1.getJSONObject(1);
				jso3_ = (JSONObject) jsae1.getJSONObject(2);

				v1=(String) jso2.get("name");
				v2=(String) jso3.get("name");
				v3=(String) jso3_.get("name");

				if (v1.equals("name")) endpointName=(String) jso2.get("value");
				if (v1.equals("bsrURI")) endpointbsrURI=(String) jso2.get("value");
				if (v1.equals("primaryType")) proxyPrimaryType=(String) jso2.get("value");

				if (v2.equals("name")) endpointName=(String) jso3.get("value");
				if (v2.equals("bsrURI")) endpointbsrURI=(String) jso3.get("value");
				if (v2.equals("primaryType")) proxyPrimaryType=(String) jso3.get("value");
				
				if (v3.equals("name")) endpointName=(String) jso3_.get("value");
				if (v3.equals("bsrURI")) endpointbsrURI=(String) jso3_.get("value");
				if (v3.equals("primaryType")) proxyPrimaryType=(String) jso3_.get("value");

				//Proxies
				
				proxySOAP="/Metadata/JSON/PropertyQuery?query=/WSRR/GenericObject/gep63_provides%28.%29/gep63_availableEndpoints%28.%29[@bsrURI=%27%BSRURI%%27]/sm63_SOAPProxy%28.%29";
				proxyREST="/Metadata/JSON/PropertyQuery?query=/WSRR/GenericObject/gep63_provides%28.%29/gep63_availableEndpoints%28.%29[@bsrURI=%27%BSRURI%%27]/sm63_RESTProxy%28.%29";
				proxyCALLABLE="/Metadata/JSON/PropertyQuery?query=/WSRR/GenericObject/gep63_provides%28.%29/gep63_availableEndpoints%28.%29[@bsrURI=%27%BSRURI%%27]/sm63_CALLABLEProxy%28.%29";

				
				proxySOAP=proxySOAP.replaceAll("%BSRURI%", endpointbsrURI);

				rootToSOAPProxy=wsrrutility.getObjectPropertiesDataFromGeneralQueryExtended(proxySOAP, "&p1=name&p2=bsrURI", cdb.getUrl(), cdb.getUser(),cdb.getPassword());

				int iii = rootToSOAPProxy.length();
				int jjj = 0;
				
				while (iii > jjj) {
					jsae2 = (JSONArray) rootToSOAPProxy.getJSONArray(jjj);
					jso4 = (JSONObject) jsae2.getJSONObject(0);
					jso5 = (JSONObject) jsae2.getJSONObject(1);

					v1=(String) jso4.get("name");
					v2=(String) jso5.get("name");

					if (v1.equals("name")) proxyName=(String) jso4.get("value");
					if (v1.equals("bsrURI")) proxybsrURI=(String) jso4.get("value");

					if (v2.equals("name")) proxyName=(String) jso5.get("value");
					if (v2.equals("bsrURI")) proxybsrURI=(String) jso5.get("value");

					System.out.println("Servizio : " + rootName +" ["+rootbsrURI+"] - Endpoint : "+endpointName +" ["+endpointbsrURI + "] PrimaryType : ["+proxyPrimaryType+"] - SoapProxy : "+proxyName+" ["+proxybsrURI+"]");
					
					totaleSOAProxy++;
					
					jjj++;
				}
				
				proxyREST=proxyREST.replaceAll("%BSRURI%", endpointbsrURI);

				rootToRESTProxyy=wsrrutility.getObjectPropertiesDataFromGeneralQueryExtended(proxyREST, "&p1=name&p2=bsrURI", cdb.getUrl(), cdb.getUser(),cdb.getPassword());

				int iiii = rootToRESTProxyy.length();
				int jjjj = 0;

				while (iiii > jjjj) {
					jsae3 = (JSONArray) rootToRESTProxyy.getJSONArray(jjjj);
					jso4 = (JSONObject) jsae3.getJSONObject(0);
					jso5 = (JSONObject) jsae3.getJSONObject(1);

					v1=(String) jso4.get("name");
					v2=(String) jso5.get("name");

					if (v1.equals("name")) proxyName=(String) jso4.get("value");
					if (v1.equals("bsrURI")) proxybsrURI=(String) jso4.get("value");

					if (v2.equals("name")) proxyName=(String) jso5.get("value");
					if (v2.equals("bsrURI")) proxybsrURI=(String) jso5.get("value");
					
					System.out.println("Servizio : " + rootName +" ["+rootbsrURI+"] - Endpoint : "+endpointName +" ["+endpointbsrURI + "] PrimaryType : ["+proxyPrimaryType+"] - RestProxy : "+proxyName+" ["+proxybsrURI+"]");
					
					totaleRESTProxy++;
					
					jjjj++;
				}
				
				proxyCALLABLE=proxyCALLABLE.replaceAll("%BSRURI%", endpointbsrURI);

				rootToCALLABLEProxy=wsrrutility.getObjectPropertiesDataFromGeneralQueryExtended(proxyCALLABLE, "&p1=name&p2=bsrURI", cdb.getUrl(), cdb.getUser(),cdb.getPassword());

				int iiiii = rootToCALLABLEProxy.length();
				int jjjjj = 0;

				while (iiiii > jjjjj) {
					jsae4 = (JSONArray) rootToCALLABLEProxy.getJSONArray(jjjjj);
					jso5 = (JSONObject) jsae4.getJSONObject(0);
					jso6 = (JSONObject) jsae4.getJSONObject(1);

					v1=(String) jso4.get("name");
					v2=(String) jso5.get("name");

					if (v1.equals("name")) proxyName=(String) jso4.get("value");
					if (v1.equals("bsrURI")) proxybsrURI=(String) jso4.get("value");

					if (v2.equals("name")) proxyName=(String) jso5.get("value");
					if (v2.equals("bsrURI")) proxybsrURI=(String) jso5.get("value");
					
					System.out.println("Servizio : " + rootName +" ["+rootbsrURI+"] - Endpoint : "+endpointName +" ["+endpointbsrURI + "] PrimaryType : ["+proxyPrimaryType+"] - CallableProxy : "+proxyName+" ["+proxybsrURI+"]");
					
					totaleCALLABLEProxy++;
					
					jjjjj++;
				}

				jj++;
			}

			j++;
		}

	    date = new Date();
		System.out.println(dateFormat.format(date));
		System.out.println("Fine Elaborazione.\n");
		System.out.println("Totali:\n");
		System.out.println("SOAP -Proxy : "+totaleSOAProxy);
		System.out.println("REST - Proxy : "+totaleRESTProxy);
		System.out.println("CALLABLE - Proxy : "+totaleCALLABLEProxy);
		System.out.println("--------------------------------------------------------------------------------------------------------------------");

		Runtime.getRuntime().exit(0);
	}

}