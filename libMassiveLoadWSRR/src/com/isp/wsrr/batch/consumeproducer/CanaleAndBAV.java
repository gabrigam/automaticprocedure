package com.isp.wsrr.batch.consumeproducer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
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

import com.isp.wsrr.envelopes.WSRREnvelopes;
import com.isp.wsrr.utility.WSRRUtility;

/**
 * 
 * Note rilascio
 * 
 * 05062018 prima versione
 * 
 */

public class CanaleAndBAV {

	private static FileInputStream fis;
	private static BufferedReader br;
	private static String line = null;

	private static final Logger nbplog = LogManager.getLogger(CanaleAndBAV.class.getName());

	public static void main(String[] args) {

		String logFileName = System.getProperty("LogFileName");
		if (logFileName != null && logFileName.length() != 0)
			updateLogger(logFileName, "creazioneBAVCanaleAppender",
					"com.isp.wsrr.batch.consumeproducer.CanaleAndBAV");

		nbplog.info(
				"----------------------------------------------------------------------------------------------------------------------");
		nbplog.info("Batch CanaleAndBAV V1.0 Giugno  2018");
		nbplog.info(
				"----------------------------------------------------------------------------------------------------------------------");
		System.out.println("");

		// check Input parameters
		if (args.length == 0) {

			System.out.println(
					"----------------------------------------------------------------------------------------------------------------------");
			System.out
					.println("Errore : fornire i seguenti parametri : WSRRURL(0) nomedelfileCanali(1)  user(2) password(3)");
			System.out.println("user password solo se https");
			System.out.println("Per il log (log4j2) specificare il nome del file mediante -DLogFileName=LogFileName");
			System.out.println(
					"Se il nome del file non viene specificato il log verrà creato nella cartella corrente di lancio con il nome creazioneBAVCanale.log");
			System.out.println("Nota;");
			System.out.println(
					"Anche se viene specificato il nome del file nella cartella di lancio viene sempre creato il file vuoto creazioneBAVCanale.log ");
			System.out.println(
					"----------------------------------------------------------------------------------------------------------------------");
			Runtime.getRuntime().exit(0); // brutale :)

		}

		ConnectionDataBeanSingleton cdb = null;

		try {
			cdb = ConnectionDataBeanSingleton.setData(); // fake è stata

			// mantenuta la
			// struttura ma il
			// bean viene
			// inizializzato
			// direttamente

		} catch (Exception e1) {
			e1.printStackTrace();
			System.exit(0);
		}

		cdb.setUrl(args[0]);

		if (args[0] != null && args[0].contains("https")) {

			cdb.setUser(args[2]);
			cdb.setPassword(args[3]);
		}

		cdb.setUrl(args[0]);

		if (args[0] != null && args[0].contains("https")) {

			cdb.setUser(args[2]);
			cdb.setPassword(args[3]);
		}

		nbplog.info(
				"-----------------------------------------------------------------------------------------------------------------------");
		nbplog.info("URL " + cdb.getUrl());
		nbplog.info("File Canali " + args[1]);
		if (logFileName != null && logFileName.length() != 0)
			nbplog.info("Log file name " + logFileName);
		else
			nbplog.info("Log file name creazioneBAVCanale.log ");
		nbplog.info(
				"----------------------------------------------------------------------------------------------------------------------");

		// load file in memory
		HashMap matricoleHash = new HashMap();
		int errors = 0;

		CanaleAndBAV canaleAndBAV = new CanaleAndBAV();

		try {

			fis = new FileInputStream(args[1]);
			br = new BufferedReader(new InputStreamReader(fis));
			int recNum = 1;
			String nomeCanale = null;
			String descrizioneCanale = null;
			String url = cdb.getUrl();
			String user = cdb.getUser();
			String password = cdb.getPassword();

			boolean first = true;

			nbplog.info("Start reading file: " + args[1]);

			while ((line = br.readLine()) != null) {

				nomeCanale = line.substring(0, 2);
				descrizioneCanale = line.substring(2, 31);
				
				if(nomeCanale==null || nomeCanale.contains(" ")) {
					nbplog.error(recNum + " Canale name  : " + nomeCanale + " is Not valid Record "+line);
				}
				else 
				canaleAndBAV.createBAVAndCanale("CC_"+nomeCanale, descrizioneCanale, url, user, password, recNum, line);
				
				recNum++;
			}			

		} catch (IOException e) {
			nbplog.error("Exception File : " + args[1] + " not exist / not redeable!");
			Runtime.getRuntime().exit(0);
		}
		nbplog.error("Procedure CanaleAndBAV terminated.. CS");
	}

	private void createBAVAndCanale(String name, String description, String url, String user, String password,
			int recNum, String record) {

		WSRREnvelopes wsrrenvelopes = new WSRREnvelopes();
		WSRRUtility wsrrutility = new WSRRUtility();
		boolean result = true;
		String xmlcanaleBAVEnvelope = null;
		String bsrURIApplicationVersionCanale = null;

		String  bsrURIBAV = wsrrutility.getGenericObjectByNameAndPrimaryTypeExtended(name,
				"http://www.ibm.com/xmlns/prod/serviceregistry/profile/v6r3/GovernanceEnablementModel%23ApplicationVersion",
				url, user, password);

		if (bsrURIBAV == null) {

			xmlcanaleBAVEnvelope = wsrrenvelopes.createApplicationVersionXMLDataCanale(name, null, description);
			bsrURIApplicationVersionCanale = wsrrutility.createWSRRGenericObject(xmlcanaleBAVEnvelope, "POST", url,
					user, password);

			if (bsrURIApplicationVersionCanale == null) {
				nbplog.error(recNum + " Creation of object Application Version Canale  : " + name + " failed - Record "+record);
			} else {
				nbplog.info(recNum + " - Created Application Version Canale Object : " + name + " "
						+ bsrURIApplicationVersionCanale+" - Record "+record);
			}

		} else {

			if (bsrURIBAV.contains(">>**ERROR**>>")) {

				nbplog.error(recNum + " Error checking existance of business a  : " + name + " failed - Record "+record);
			} else
				nbplog.error(recNum + " Application Version Canale Object  : " + name + " - "+bsrURIBAV +" Already exist - NO creation needed - Record Value "+record);

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
