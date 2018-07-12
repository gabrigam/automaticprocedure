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

import com.google.gson.Gson;
import com.ibm.bancaintesa.connection.Rest;
import com.isp.wsrr.envelopes.WSRREnvelopes;
//import com.isp.wsrr.utility.WSRRUtility; tolto

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

public class WSRRMassiveUpdaterFromFile {

	private static FileInputStream fis;
	private static BufferedReader br;
	private static String line = null;

	private static String keyResponseMessage = "responseMessage";
	private static String keycodeMessage = "codeMessage";
	private static String keyErrorMessage = "errorMessage";

	private static final Logger nbplog = LogManager.getLogger(WSRRMassiveUpdaterFromFile.class.getName());

	@SuppressWarnings("rawtypes")
	public static void main(String[] args) {

		String logFileName = System.getProperty("LogFileName");

		if (logFileName != null && logFileName.length() != 0)
			updateLogger(logFileName, "caricamentiMassiviISPAppender",
					"com.isp.wsrr.batch.consumeproducer.WSRRMassiveUpdaterFromFile");



		// check Input parameters
		if (args.length != 7) {

			nbplog.info(
					"----------------------------------------------------------------------------------------------------------------------");
			nbplog.info("Error insert : URLBPM(0) FileInput(1)  userbpm(2) password(3) urlWSRR(4) userwsrr(5) password(6)");
			nbplog.info(
					"----------------------------------------------------------------------------------------------------------------------");
			Runtime.getRuntime().exit(0); // brutale :)

		}
		

		nbplog.info(
				"----------------------------------------------------------------------------------------------------------------------");
		nbplog.info("Started ...Batch Massive Updater WSRR SCHOST -  V1.0 July 2018");
		nbplog.info("URL " + args[0]);
		nbplog.info("File Name " + args[1]);
		nbplog.info("WSRR URL " + args[4]);
		nbplog.info(
				"----------------------------------------------------------------------------------------------------------------------");
		

		nbplog.info(" :-))  Service Updated");
		nbplog.info(" :-((  Service Not Updated");

		nbplog.info(
				"----------------------------------------------------------------------------------------------------------------------");		
		//IXPG0CM@HS_Servizio_Caricamento_Massivo_Tipologia_SCHOST
		
		String action = null;
		String serviceName = "IXPG0CM%40HS_Servizio_Caricamento_Massivo_Tipologia_SCHOST_UPD";
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

					//line=line+"MIGR";
					
					String[] input = line.split("@#@");
									
					if (input.length ==16) {

					
					String json = new Gson().toJson(input);
					
					String jsonBO="{\"InputDataSCHOST\":";
					
					jsonBO=jsonBO+json+",\"urlwsrr\":\"%urlwsrr%\"}";
															
					jsonBO=jsonBO.replaceAll("%urlwsrr%", args[4]);
					
					
						nbplog.info("Record # " + recNum + ": " + line);

						jsonBO = URLEncoder.encode(jsonBO, "UTF-8");

						action = action_ + jsonBO + "&createTask=false&parts=all";

						HashMap responseMap = null;

						try {
							responseMap = Rest.doRest("POST", action, "", headerMap, args[2], args[3], true, -1);

							if (responseMap != null) {

								String face=":-((";
								
								if (((String)responseMap.get(keyResponseMessage)).indexOf("eseguita Correttamente!")!= -1) {
								    face=":-))";
									ok++;
								}
																
								nbplog.info(face+"   Result for Record # " + recNum + "---> " + responseMap.get(keycodeMessage)
										+ " - " + responseMap.get(keyResponseMessage) + " - "
										+ responseMap.get(keyErrorMessage));
								//Creazione Eseguita Correttamente 
								

							}
						} catch (Exception ex) {
							nbplog.error(":(  Exception for Record # " + recNum);
							nbplog.error(ex.getMessage());
							ex.printStackTrace();
						}

					} else {
						nbplog.error(":(  Record # " + recNum + " contains invalid data found : "+input.length+" fields instead 16!");
					}
				} catch (Exception ex) {	
					nbplog.error(":(  Record # " + recNum + " RunTimeError : " +ex.toString());
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
		
		nbplog.info("Total Object/s Updated : "+ok );
		nbplog.info("Total Record/s in Error/Not Existing : "+ko);

		nbplog.info(
				"----------------------------------------------------------------------------------------------------------------------");
		nbplog.info("Terminated ...Batch Massive Updater WSRR SCHOST");
		nbplog.info("CS");
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
