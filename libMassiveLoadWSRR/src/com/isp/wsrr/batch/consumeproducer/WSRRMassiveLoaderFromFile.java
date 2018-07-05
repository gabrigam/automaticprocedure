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



		// check Input parameters
		if (args.length != 10) {

			nbplog.info(
					"----------------------------------------------------------------------------------------------------------------------");
			nbplog.info("Error insert : URLBPM(0) FileInput(1)  operation(2) subtype(3) userbpm(4) password(5) urlWSRR(6) userwsrr(7) password(8) simula(9)");
			nbplog.info(
					"----------------------------------------------------------------------------------------------------------------------");
			Runtime.getRuntime().exit(0); // brutale :)

		}
		
		if (args[2]==null || (!args[2].equals("INSERT") && !args[2].equals("NEWVERSION"))) {
			nbplog.info("Invalid operation use INSERT or NEWVERSION");
			Runtime.getRuntime().exit(0);
		}
		
		if (args[3]==null || (!args[3].equals("BS") && !args[2].equals("MPE"))) {
			nbplog.info("Invalid subtype use BS or MPE");
			Runtime.getRuntime().exit(0);
		}

		nbplog.info(
				"----------------------------------------------------------------------------------------------------------------------");
		nbplog.info("Started ...Batch Massive Loader WSRR SCHOST -  V1.1 July 2018");
		nbplog.info("URL " + args[0]);
		nbplog.info("File Name " + args[1]);
		nbplog.info("Operation " + args[2]);
		nbplog.info("SubType " + args[3]);
		nbplog.info("WSRR URL " + args[6]);
		if (args[9].equals("1")) nbplog.info("Richiesta la SIMULAZIONE NON saranno creati Censimenti!!");
		nbplog.info(
				"----------------------------------------------------------------------------------------------------------------------");
		
		if (args[9].equals("0")) {
		nbplog.info(" :-))  Service Created");
		nbplog.info(" :-((  Service Not Created");
		}
		nbplog.info(
				"----------------------------------------------------------------------------------------------------------------------");		
		//IXPG0CM@HS_Servizio_Caricamento_Massivo_Tipologia_SCHOST
		
		String action = null;
		String serviceName = "IXPG0CM%40HS_Servizio_Caricamento_Massivo_Tipologia_SCHOST";
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
									
					if (input.length ==30) {
					
				    //check doc analisi tecnica e funzionale
						
				    if (input[4].contains("\\")) {
				    	
				    nbplog.error(":(  Exception for Record # " + recNum+ " Analisi Funzionale contiene il campo \\");
				    	
				    }
				    
				    if (input[5].contains(" ")) {
				    	
				    nbplog.error(":(  Exception for Record # " + recNum+ " Analisi Tecnica contiene il campo \\");
				    	
				    }
					
					String json = new Gson().toJson(input);
					
					String jsonBO="{\"operazione\":\"%operazione%\",\"tipoSottoservizio\":\"%sottotipo%\",\"InputDataSCHOST\":";
					
					jsonBO=jsonBO+json+",\"urlwsrr\":\"%urlwsrr%\",\"user\":\"%user%\",\"password\":\"%password%\",\"simula\":\"%simula%\"}";
										
					jsonBO=jsonBO.replaceAll("%operazione%", args[2]);
					
					jsonBO=jsonBO.replaceAll("%sottotipo%", args[3]);
					
					jsonBO=jsonBO.replaceAll("%urlwsrr%", args[6]);
					
					jsonBO=jsonBO.replaceAll("%user%", args[7]);
					
					jsonBO=jsonBO.replaceAll("%password%", args[8]);
					
					jsonBO=jsonBO.replaceAll("%simula%", args[9]);
					
						nbplog.info("Record # " + recNum + ": " + line);

						jsonBO = URLEncoder.encode(jsonBO, "UTF-8");

						action = action_ + jsonBO + "&createTask=false&parts=all";

						HashMap responseMap = null;

						try {
							responseMap = Rest.doRest("POST", action, "", headerMap, args[4], args[5], true, -1);

							if (responseMap != null) {

								String face=":-((";
								
								if (!args[9].equals("0")) face="???";
								
								if (((String)responseMap.get(keyResponseMessage)).indexOf("Creazione Eseguita Correttamente")!= -1) {
								    face=":-))";
									ok++;
								}
								
								if (((String)responseMap.get(keyResponseMessage)).indexOf("Creazione per Nuova Versione Eseguita Correttamente")!= -1) {
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
						nbplog.error(":(  Record # " + recNum + " contains invalid data found : "+input.length+" fields instead 30!");
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
		if (args[9].equals("0")){
		nbplog.info("Total Object/s Created : "+ok );
		nbplog.info("Total Record/s in Error/Existing : "+ko);
		}
		nbplog.info(
				"----------------------------------------------------------------------------------------------------------------------");
		nbplog.info("Terminated ...Batch Massive Loader WSRR SCHOST");
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
