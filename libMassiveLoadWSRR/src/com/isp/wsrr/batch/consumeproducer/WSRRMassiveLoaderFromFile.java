package com.isp.wsrr.batch.consumeproducer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.net.URLEncoder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.json.JSONArray;

import com.ibm.bancaintesa.connection.Rest;
import com.isp.wsrr.envelopes.WSRREnvelopes;
import com.isp.wsrr.utility.WSRRUtility;

/**
 * 
 * Note rilascio
 * 
 * 
 * aggiunti metodi che ritornano l'errore in formato esteso esempio:
 * getGenericObjectByNameAndPrimaryTypeExtended tolta la cancellazione in caso
 * di errore 241016 aggiunta la gestione ambito e ambito descrizione 16/11/2015
 * aggiunto controllo esistenza file 120117 metto trim sull matricola che mi
 * ritorna dal file myapm 180117 modificate le substring per estrazione dal file
 * matricole
 */

public class WSRRMassiveLoaderFromFile {

	private static FileInputStream fis;
	private static BufferedReader br;
	private static String line = null;

	private static String keyResponseMessage = "responseMessage";
	private static String keycodeMessage = "codeMessage";
	private static String keyErrorMessage = "errorMessage";

	private static final Logger nbplog = LogManager.getLogger(WSRRMassiveLoaderFromFile.class.getName());

	@SuppressWarnings("rawtypes")
	public static void main(String[] args) {

		String logFileName = System.getProperty("LogFileName");

		if (logFileName != null && logFileName.length() != 0)
			updateLogger(logFileName, "caricamentiMassiviISPAppender",
					"com.isp.wsrr.batch.consumeproducer.WSRRMassiveLoaderFromFile");

		nbplog.info(
				"----------------------------------------------------------------------------------------------------------------------");
		nbplog.info("Started ...Batch Massive Loader WSRR SHOST - (BC) V1.0 Luglio 2017");
		nbplog.info("URL " + args[0]);
		nbplog.info("File Name " + args[1]);
		nbplog.info(
				"----------------------------------------------------------------------------------------------------------------------");

		// check Input parameters
		if (args.length == 0) {

			nbplog.info(
					"----------------------------------------------------------------------------------------------------------------------");
			nbplog.info("Error insert : URLBPM(0) FileInput(1)  user(2) password(3) ");
			nbplog.info(
					"----------------------------------------------------------------------------------------------------------------------");
			Runtime.getRuntime().exit(0); // brutale :)

		}

		String action = null;
		String serviceName = "IXPG0%40HS_Servizio_Caricamento_Massivo_SHOST";
		String action_ = args[0] + "/rest/bpm/wle/v1/service/" + serviceName + "?action=start&params=";
		HashMap headerMap = new HashMap();
		headerMap.put("Content-Type", "application/json ;  charset=UTF-8");
		int recNum = 1;
		int ok=0;
		try {

			fis = new FileInputStream(args[1]);
			br = new BufferedReader(new InputStreamReader(fis));
			boolean first = true;
			nbplog.info("Start reading file: " + args[1]);

			while ((line = br.readLine()) != null) {

				try {

					String[] input = line.split(";");

					if (input[0].length() != 0 && input[1].length() != 0 && input[3].length() != 0
							&& input[4].length() != 0 && input[5].length() != 0) {

						nbplog.info("Record # " + recNum + ": " + line);

						String jsonBO = "{\"DatiRichiestiSHOST\":{\"NOME\":\"%NOME%\",\"ACRONIMO\":\"%ACRONIMO%\",\"SSA\":\"%SSA%\",\"DESCRIZIONE\":\"%DESCRIZIONE%\",\"DESCRIZIONE_ESTESA\":\"%DESCRIZIONE_ESTESA%\",\"PGM_SERVIZIO\":\"%PGM_SERVIZIO%\",\"TRANS_SERVIZIO\":\"%TRANS_SERVIZIO%\"}}";

						String ssa = input[2];

						if (ssa.length() == 0)
							ssa = input[1].substring(0, 2);

						jsonBO = jsonBO.replace("%NOME%", input[0]);
						jsonBO = jsonBO.replace("%ACRONIMO%", input[1]);
						jsonBO = jsonBO.replace("%SSA%", ssa);
						jsonBO = jsonBO.replace("%DESCRIZIONE%", input[3]);
						jsonBO = jsonBO.replace("%DESCRIZIONE_ESTESA%", input[3]);
						jsonBO = jsonBO.replace("%PGM_SERVIZIO%", input[4]);
						jsonBO = jsonBO.replace("%TRANS_SERVIZIO%", input[5]);

						jsonBO = URLEncoder.encode(jsonBO, "UTF-8");

						action = action_ + jsonBO + "&createTask=false&parts=all";

						HashMap responseMap = null;

						try {
							responseMap = Rest.doRest("POST", action, "", headerMap, args[2], args[3], true, -1);

							if (responseMap != null) {
								nbplog.info("	Result for Record # " + recNum + "---> " + responseMap.get(keycodeMessage)
										+ " - " + responseMap.get(keyResponseMessage) + " - "
										+ responseMap.get(keyErrorMessage));
								//Creazione Eseguita Correttamente 
								
								if (((String)responseMap.get(keyResponseMessage)).indexOf("Creazione Eseguita Correttamente")!= -1) {
									ok++;
								}
							}
						} catch (Exception ex) {
							nbplog.error("Exception for Record # " + recNum);
							ex.printStackTrace();
						}

					} else {
						nbplog.error("Record # " + recNum + " contains invalid data");
					}
				} catch (Exception ex) {	
					nbplog.error("Record # " + recNum + " RunTimeError : " +ex.toString());
					ex.printStackTrace();
				}
				recNum++;

			}

		} catch (IOException e) {
			nbplog.error("Exception File : " + args[1] + " not exists / not redeable!");
			Runtime.getRuntime().exit(0);
		}
		
		nbplog.info(
				"----------------------------------------------------------------------------------------------------------------------");
		int ko=(recNum-1-ok);
		nbplog.info("Total Record/s : "+(recNum-1)) ;
		nbplog.info("Total Object/s Created : "+ok );
		nbplog.info("Total Record/s in Error (and Not Created) : "+ko);
		nbplog.info(
				"----------------------------------------------------------------------------------------------------------------------");
		nbplog.info("Terminated ...Batch Massive Loader WSRR SHOST - (BC) V1.0 Luglio 2017... CS");
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
