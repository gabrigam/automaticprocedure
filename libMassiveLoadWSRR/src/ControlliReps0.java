

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.isp.wsrr.utility.WSRRUtility;

public class ControlliReps0 {

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

			cdb.setUser(args[1]);
			cdb.setPassword(args[2]);
		}

		int Reps0ScopenSI=0;
		int Reps0ScopenMIGR=0;
		int Reps0ScopenNEW=0;
		int Reps0SopenSI=0;
		int Reps0SopenMIGR=0;
		int Reps0SopenNEW=0;
		
		JSONArray allSCOPEN=new JSONArray();
		JSONArray allSOPEN=new JSONArray();
		
		allSCOPEN=wsrrutility.getObjectPropertiesDataFromGeneralQuery("@primaryType='http://www.ibm.com/xmlns/prod/serviceregistry/profile/v6r3/GovernanceEnablementModel%23SCOPENServiceVersion'", "&p1=gep63_SCOPEN_REPS0", cdb.getUrl(), cdb.getUser(),cdb.getPassword());
		
		JSONArray jsae = null;
		JSONObject jso = null;
		String value = null;
		
		int i = allSCOPEN.length();
		int j = 0;
		while (i > j) {
				jsae = (JSONArray) allSCOPEN.getJSONArray(j);
				jso = (JSONObject) jsae.getJSONObject(0);
				
				
				if (!jso.isNull("value")) {			
				 value = (String) jso.get("value");
				 if (value != null && value.equals("SI")) Reps0ScopenSI++;
				 if (value != null && value.equals("MIGR")) Reps0ScopenMIGR++;
				 if (value != null && value.equals("NEW")) Reps0ScopenNEW++;
				}

			j++;
		}
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println(dateFormat.format(date));
		System.out.println("Controlli per REPS0");
		System.out.println("--------------------------------------------------------------------------------------------------------------------");
		System.out.println("SCOPEN Totali : "+allSCOPEN.length());
		System.out.println("SCOPEN_REPS0 =SI : "+Reps0ScopenSI);
		System.out.println("SCOPEN_REPS0 =MIGR : "+Reps0ScopenMIGR);
		System.out.println("SCOPEN_REPS0 =NEW : "+Reps0ScopenNEW);
		int Reps0ScopenOTHERS=allSCOPEN.length() - Reps0ScopenSI - Reps0ScopenMIGR - Reps0ScopenNEW;
		System.out.println("SCOPEN_REPS0 =ALTRI : "+Reps0ScopenOTHERS);
		System.out.println("--------------------------------------------------------------------------------------------------------------------");
		
		allSOPEN=wsrrutility.getObjectPropertiesDataFromGeneralQuery("@primaryType='http://www.ibm.com/xmlns/prod/serviceregistry/profile/v6r3/GovernanceEnablementModel%23SOPENServiceVersion'", "&p1=gep63_SOPEN_REPS0", cdb.getUrl(), cdb.getUser(),cdb.getPassword());

		
		i = allSOPEN.length();
		j = 0;
		while (i > j) {
				jsae = (JSONArray) allSOPEN.getJSONArray(j);
				jso = (JSONObject) jsae.getJSONObject(0);
				
				if (!jso.isNull("value")) {
				 value = (String) jso.get("value");
				 if (value != null && value.equals("SI")) Reps0SopenSI++;
				 if (value != null && value.equals("MIGR")) Reps0SopenMIGR++;
				 if (value != null && value.equals("NEW")) Reps0SopenNEW++;
				}
				
			j++;
		}

		System.out.println("SOPEN Totali : "+allSOPEN.length());
		System.out.println("SOPEN_REPS0 =SI : "+Reps0SopenSI);
		System.out.println("SOPEN_REPS0 =MIGR : "+Reps0SopenMIGR);
		System.out.println("SOPEN_REPS0 =NEW : "+Reps0SopenNEW);
		int Reps0SopenOTHERS=allSOPEN.length() - Reps0SopenSI - Reps0SopenMIGR - Reps0SopenNEW;
		System.out.println("SOPEN_REPS0 =ALTRI : "+Reps0SopenOTHERS);
		System.out.println("--------------------------------------------------------------------------------------------------------------------");
		date = new Date();
		System.out.println(dateFormat.format(date));
		
		Runtime.getRuntime().exit(0);
		
	}
		
}