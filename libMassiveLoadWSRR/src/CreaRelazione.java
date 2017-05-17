

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.Serializable;

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

import com.isp.wsrr.utility.WSRRUtility;

public class CreaRelazione {


	protected static Logger log = LogManager.getLogger(CreaRelazione.class.getName());

	public static void main(String[] args) throws Exception {


		String logFileName = System.getProperty("LogFileName");

		if (logFileName != null && logFileName.length() != 0)

			updateLogger(logFileName, "creaRelazioneAppender",
					"CreaRelazione");
		log.info(
				"----------------------------------------------------------------------------------------------------------------------");
		log.info("Crea la relazione richiesta per gli oggetti con il tipo specificato dell 'utente V1.0");
		log.info("Tipologia dei  censimenti da estrarre : "+args[1]);
		log.info("Relazione da creare : "+args[2]);
		log.info(
				"----------------------------------------------------------------------------------------------------------------------");

		log.info("");

		// check Input parameters
		if (args.length == 0) {

			System.out.println(
					"----------------------------------------------------------------------------------------------------------------------");
			System.out.println(
					"Errore : fornire i seguenti parametri: (0) Indirizzo WSRR (1) Tipo Oggetto (2) Relazione (3) Utente (4) Password ");

			System.out.println(
					"----------------------------------------------------------------------------------------------------------------------");
			Runtime.getRuntime().exit(0);

		}

		WSRRUtility wsrrutility = new WSRRUtility();


		ConnectionDataBeanSingleton cdb = null;

		try {
			cdb = ConnectionDataBeanSingleton.setData(); 

		} catch (Exception e1) {
			e1.printStackTrace();
			System.exit(0);
		}

		cdb.setUrl(args[0]);

		if (args[0] != null && args[0].contains("https")) {

			cdb.setUser(args[3]);
			cdb.setPassword(args[4]);
		}

		JSONArray soapEpAll=new JSONArray();

		soapEpAll=wsrrutility.getAllObjectsSpecifiedByPrimaryType(args[1], cdb.getUrl(),cdb.getUser(), cdb.getPassword());

		JSONArray jsae = null;
		JSONObject jso = null;
		String bsrURICurrent = null;

		int i = soapEpAll.length();
		int j = 0;
		int rel=0;
		int nrel=0;
		String graph=null;
		log.info("Trovati : "+i +" censimenti di tipo : "+args[1]);
		log.info("Tutti gli oggettiverranno analizzati per eseguire la creazione della relazione: "+args[2]);
		while (i > j) {
			jsae = (JSONArray) soapEpAll.getJSONArray(j);
			jso = (JSONObject) jsae.getJSONObject(0);
			bsrURICurrent = (String) jso.get("value");
			graph=wsrrutility.getDataFromGraphQuery("@bsrURI='"+bsrURICurrent+"'", cdb.getUrl(), cdb.getUser(), cdb.getPassword());
			if (graph != null) {					
				if (graph.indexOf(args[2], 0) == -1) {						
						if(wsrrutility.updateEmptyRelationShip(bsrURICurrent, args[2], cdb.getUrl(), cdb.getUser(), cdb.getPassword())) {
							log.info("Oggetto con chiave : "+bsrURICurrent + " NON CONTIENE la relazione richiesta - La relazione verra' Creata");
						} else log.error("Errore durante l'aggiornamento della relazione per l'oggetto con chiave : "+bsrURICurrent);					
					nrel++;						
				}
				else {
					log.info("Oggetto con chiave : "+bsrURICurrent + " CONTIENE gia' la relazione Richiesta");
					rel++;
				}
			} 
			j++;
		}
		log.info("---------------------------------------------------------------------------------------------------------------------------------------");
		log.info("Totale censimenti di tipo : "+args[1]+" : " +i);
		log.info("Trovati : "+ rel +" censimenti CONTENENTI la relazione richiesta.");
		if (nrel !=0)
		log.info("Aggiornati : "+ nrel +" censimenti che NON CONTENEVANO la relazione richiesta");
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