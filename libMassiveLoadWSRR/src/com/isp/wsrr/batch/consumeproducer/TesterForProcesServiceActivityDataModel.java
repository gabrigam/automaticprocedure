package com.isp.wsrr.batch.consumeproducer;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import com.ibm.bancaintesa.connection.Rest;

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

public class TesterForProcesServiceActivityDataModel {


	private static String keyResponseMessage = "responseMessage";
	private static String keycodeMessage = "codeMessage";
	private static String keyErrorMessage = "errorMessage";

	private static final Logger nbplog = LogManager.getLogger(TesterForProcesServiceActivityDataModel.class.getName());

	@SuppressWarnings("rawtypes")
	public static void main(String[] args) throws UnsupportedEncodingException {

		String logFileName = System.getProperty("LogFileName");

		if (logFileName != null && logFileName.length() != 0)
			updateLogger(logFileName, "caricamentiMassiviISPAppender",
					"com.isp.wsrr.batch.consumeproducer.WSRRMassiveLoaderFromFile");


		// check Input parameters
		if (args.length == 0) {

			Runtime.getRuntime().exit(0); // brutale :)
		}

		String action = null;
		String serviceName = "IXPG0CM%40HS_DatiModello_Processo_Servzio_ExtActivity";
		String action_ = args[0] + "/rest/bpm/wle/v1/service/" + serviceName + "?action=start&params=";
		HashMap headerMap = new HashMap();
		headerMap.put("Content-Type", "application/json ;  charset=UTF-8");
		int recNum = 1;
		int ok = 0;

			String jsonBO = "{\"tipoFunzione\":\"P\",\"bpdID\":\"%BPDID%\",\"serviceID\":\"%SERVICEID%\",\"externalActivityID\":\"%EXTERNALACTIVITYID%\",\"snapshotID\":\"%SNAPSHOTID%\",\"branchID\":\"%BRANCHID%\",\"processAppID\":\"%PROCESSAPPID%\"}";

			jsonBO = jsonBO.replace("%BPDID%", "25.c21e4ccc-deab-40be-b133-d44136f8d5ef");
			//jsonBO = jsonBO.replace("%BPDID%", "");
			jsonBO = jsonBO.replace("%SERVICEID%", "");
			jsonBO = jsonBO.replace("%EXTERNALACTIVITYID%", "");
			jsonBO = jsonBO.replace("%SNAPSHOTID%", "2064.a958282f-02ba-435d-a74a-a621e72dacb8");
			//jsonBO = jsonBO.replace("%SNAPSHOTID%", "");
			jsonBO = jsonBO.replace("%BRANCHID%", "");
			jsonBO = jsonBO.replace("%PROCESSAPPID%", "");

			jsonBO = URLEncoder.encode(jsonBO, "UTF-8");

			action = action_ + jsonBO + "&createTask=false&parts=all";

			HashMap responseMap = null;

			try {
				responseMap = Rest.doRest("POST", action, "", headerMap, args[1], args[2], true, -1);

				if (responseMap != null) {
					nbplog.info("	Result for Record # " + recNum + "---> " + responseMap.get(keycodeMessage) + " - "
							+ responseMap.get(keyResponseMessage) + " - " + responseMap.get(keyErrorMessage));
					// Creazione Eseguita Correttamente

					if (((String) responseMap.get(keyResponseMessage))
							.indexOf("Creazione Eseguita Correttamente") != -1) {
						ok++;
					}
				}
			} catch (Exception ex) {
				nbplog.error("Exception for Record # " + recNum);
				ex.printStackTrace();
			}

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
