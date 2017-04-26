

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.json.JSONArray;
import org.json.JSONObject;


import com.isp.wsrr.envelopes.WSRREnvelopes;
import com.isp.wsrr.utility.WSRRUtility;

public class AggiornaValoreProprieta {

	private static FileInputStream fis;
	private static BufferedReader br;
	private static String line = null;
	// private static StringTokenizer st = null;
	// private static ArrayList<String> list;

	protected static Logger log = LogManager.getLogger(AggiornaValoreProprieta.class.getName());

	public static void main(String[] args) throws Exception {
		
		String logFileName = System.getProperty("LogFileName");

		if (logFileName != null && logFileName.length() != 0)

			updateLogger(logFileName, "aggiornaProprietaAppender",
					"AggiornaValoreProprieta");

		log.info(
				"----------------------------------------------------------------------------------------------------------------------");
		log.info("Aggiornamento proprieta' specifica per gli oggetti della tipologia scelta dell'utente V1.0");		
		log.info(
				"----------------------------------------------------------------------------------------------------------------------");

		log.info("");

		// check Input parameters
		if (args.length == 0) {


			System.out.println(
					"----------------------------------------------------------------------------------------------------------------------");
			System.out.println(
					"Errore : fornire i seguenti parametri: (0) Indirizzo WSRR (1) Tipo Oggetto (2) Proprietà (3) Valore (4) Utente (5) Password ");

			System.out.println(
					"----------------------------------------------------------------------------------------------------------------------");
			Runtime.getRuntime().exit(0);

		}

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

			cdb.setUser(args[4]);
			cdb.setPassword(args[5]);
		}
		HashMap bsrURIMap=new HashMap();
		JSONArray soapEpAll=new JSONArray();
		JSONArray soapEpNew=new JSONArray();
		
		soapEpAll=wsrrutility.getAllObjectsSpecifiedByPrimaryType(args[1], cdb.getUrl(),cdb.getUser(), cdb.getPassword());
		soapEpNew=wsrrutility.getObjectPropertiesDataFromGeneralQuery("@primaryType=%27"+args[1]+"%27%20and%20"+args[2], "&p1=bsrURI", cdb.getUrl(), cdb.getUser(),cdb.getPassword());
		
		JSONArray jsae = null;
		JSONObject jso = null;
		String bsrURICurrent = null;
		
		int i = soapEpNew.length();
		int j = 0;
		int upd=0;
		int upderr=0;
		log.info("Trovati : "+soapEpAll.length()+" censimenti di tipo : "+args[1]);
		log.info("Trovati : "+soapEpNew.length() +" censimenti di tipo : "+args[1]+" con la proprietà : "+args[2]+" DEFINITA");
		log.info("Per i : "+(soapEpAll.length()-soapEpNew.length())+ " censimenti verra' eseguito l'aggiornamento della proprieta' : "+ args[2]+ "con il valore: "+args[3]);
		log.info("");
		while (i > j) {
				jsae = (JSONArray) soapEpNew.getJSONArray(j);
				jso = (JSONObject) jsae.getJSONObject(0);
				bsrURICurrent = (String) jso.get("value");
				bsrURIMap.put(bsrURICurrent, bsrURICurrent);
				j++;
		}
		i = soapEpAll.length();
		j = 0;
		while (i > j) {
				jsae = (JSONArray) soapEpAll.getJSONArray(j);
				jso = (JSONObject) jsae.getJSONObject(0);
				bsrURICurrent = (String) jso.get("value");
				
				if (bsrURIMap.get(bsrURICurrent) == null) {
					
					if (wsrrutility.updateSinglePropertyJSONFormat(bsrURICurrent, args[2], args[3], cdb.getUrl(), cdb.getUser(), cdb.getPassword())) {
						log.info("Per l'oggetto con chiave : "+bsrURICurrent + " è stata aggiornata la proprieta' con il valore richiesto");
						upd++;
					}else {
						log.info("Per l'oggetto con chiave : "+bsrURICurrent + " è stato riscontrato un errore in fase di aggiornamento della proprietà - CONTROLLARE il System.out per il dettaglio sull' Errore");
						upderr++;
					}
						
				}
				
				j++;
		}
		log.info("---------------------------------------------------------------------------------------------------------------------------------------");
		log.info("Totale censimenti di tipo : "+args[1]+" : " +i);
		log.info("Trovati : "+soapEpNew.length() +" censimenti di tipo : "+args[1]+" con la proprietà : "+args[2]+" DEFINITA");
		if (upd >0) log.info("Aggiornata con successo la proprieta' per : "+upd+ " censimenti");
		log.info("Riscontrati " +upderr+ " errori in fase di aggiornamento della proprietà");
		log.info("---------------------------------------------------------------------------------------------------------------------------------------");
		log.info("CS");
		Runtime.getRuntime().exit(0);
		
	}

	static void updateLogger(String file_name, String appender_name, String package_name) {
		LoggerContext context = (LoggerContext) LogManager.getContext(false);
		Configuration configuration = context.getConfiguration();
		Layout<? extends Serializable> old_layout = configuration.getAppender(appender_name).getLayout();

		// delete old appender/logger
		configuration.getAppender(appender_name).stop();
		configuration.removeLogger(package_name);

		// create new appender/logger
		LoggerConfig loggerConfig = new LoggerConfig(package_name, Level.INFO, false);
		FileAppender appender = FileAppender.createAppender(file_name, "false", "false", appender_name, "true", "true",
				"true", "8192", old_layout, null, "false", "", configuration);

		appender.start();
		loggerConfig.addAppender(appender, Level.INFO, null);
		configuration.addLogger(package_name, loggerConfig);

		context.updateLoggers();
	}
}