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
 * 
 * aggiunti metodi che ritornano l'errore in formato esteso esempio:
 * getGenericObjectByNameAndPrimaryTypeExtended tolta la cancellazione in caso
 * di errore
 * 241016 aggiunta la gestione ambito e ambito descrizione
 * 16/11/2015 aggiunto controllo esistenza file
 * 120117 metto trim sull matricola che mi ritorna dal file myapm
 * 180117 modificate le substring per estrazione dal file matricole
 */

public class SSAAcronimoBusinessApplicationAmbito extends SLAConsumerAndProvider {

	private static FileInputStream fis;
	private static BufferedReader br;
	private static String line = null;

	private static final Logger nbplog = LogManager.getLogger(SSAAcronimoBusinessApplicationAmbito.class.getName());

	public static void main(String[] args) {

		String logFileName = System.getProperty("LogFileName");

		if (logFileName != null && logFileName.length() != 0)
			updateLogger(logFileName, "caricamentiSSAISPAppender",
					"com.isp.wsrr.batch.consumeproducer.SSAAcronimoBusinessApplicationAmbito");

		nbplog.info(
				"----------------------------------------------------------------------------------------------------------------------");
		nbplog.info("Batch SSAAcronimoBusinessApplication V1.6 Gennaio  2017");
		nbplog.info("aggiunti ambito e ambito descrizione per oggetti");
		nbplog.info("migliorata gestione file non trovato o non leggibile (1.5)");
		nbplog.info("120117 inserito trim matricole, check matricole non presenti od omesse , check matricole contenenti spazi");
		nbplog.info("180117 modificate le substring per estrazione dal file matricole");
		nbplog.info("03-02-2018 aggiunti i campi di AV per la regola ODM");
		nbplog.info(
				"----------------------------------------------------------------------------------------------------------------------");
		System.out.println("");

		// check Input parameters
		if (args.length == 0) {

			System.out.println(
					"----------------------------------------------------------------------------------------------------------------------");
			System.out.println(
					"Errore : fornire due parametri : WSRRURL(0) nomedelfileMYPAM(1)  nomedelfilematricole(2) user(3) password(4)");
			System.out.println("user password solo se https");
			System.out.println("Per il log (log4j2) specificare il nome del file mediante -DLogFileName=LogFileName");
			System.out.println(
					"Se il nome del file non viene specificato il log verrà creato nella cartella corrente di lancio con il nome caricamentiNBP.log");
			System.out.println("Nota;");
			System.out.println(
					"Anche se viene specificato il nome del file nella cartella di lancio viene sempre creato il file vuoto caricamentiNBP.log ");
			System.out.println(
					"----------------------------------------------------------------------------------------------------------------------");
			Runtime.getRuntime().exit(0); // brutale :)

		}

		ConnectionDataBeanSingleton cdb = null;
		SSAAcronimoBusinessApplicationAmbito ssatoacronimoassociation = new SSAAcronimoBusinessApplicationAmbito();

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

			cdb.setUser(args[3]);
			cdb.setPassword(args[4]);
		}

		cdb.setUrl(args[0]);

		if (args[0] != null && args[0].contains("https")) {

			cdb.setUser(args[3]);
			cdb.setPassword(args[4]);
		}

		nbplog.info(
				"-----------------------------------------------------------------------------------------------------------------------");
		nbplog.info("URL " + cdb.getUrl());
		nbplog.info("File MYAPM " + args[1]);
		nbplog.info("File Matricole " + args[2]);
		if (logFileName != null && logFileName.length() != 0)
			nbplog.info("Log file name " + logFileName);
		else
			nbplog.info("Log file name caricamentiNBP.log ");
		nbplog.info(
				"----------------------------------------------------------------------------------------------------------------------");

		// load file in memory
		HashMap matricoleHash = new HashMap();

		String matricola = null;
		String surname = null;
		String name = null;

		int errors = 0;

		try {

			fis = new FileInputStream(args[2]);
			br = new BufferedReader(new InputStreamReader(fis));
			int recNum = 1;

			boolean first = true;

			nbplog.info("Start reading file: " + args[2]);

			while ((line = br.readLine()) != null) {

				if (!first) {

					boolean inError = false;

					try {
						matricola = line.substring(0, 6);
					} catch (Exception ex1) {
						inError = true;
						errors++;
						nbplog.error("Error on getting matricola field for record  " + line);
					}

					try {
						surname = line.substring(6, 30); //180117 before 31
					} catch (Exception ex2) {
						inError = true;
						errors++;
						nbplog.error("Error on getting surname field for record  " + line);
					}

					try {
						name = line.substring(30, 46); //180117 before 31 47

					} catch (Exception ex3) {
						nbplog.error("Error  on getting name field for record " + line);
						inError = true;
						errors++;
					}

					if (!inError)
						matricoleHash.put(matricola.trim(), surname.trim() + " " + name.trim());

				} else
					first = false;

			}

		} catch (IOException e) {
			nbplog.error("Exception File : " + args[2]+ " not exist / not redeable!");
			Runtime.getRuntime().exit(0);

		}

		nbplog.info("Terminate  reading file: " + args[2] + " found :" + matricoleHash.size()
				+ " valid records (no first record) - found (" + errors + ") error/s");

		StringBuffer recordsInError = new StringBuffer();

		try {

			fis = new FileInputStream(args[1]);
			br = new BufferedReader(new InputStreamReader(fis));
			int recNum = 1;

			while ((line = br.readLine()) != null) {

				String[] paramArray = line.split("\\;");
				nbplog.info("Record - " + " (" + recNum + ") >> " + line);

				if (!ssatoacronimoassociation.createSSAToAcronimoAssociation(cdb, paramArray[3], paramArray[4],
						paramArray[6], paramArray[7], paramArray[0],paramArray[1],paramArray, recNum, matricoleHash))
					recordsInError.append(recNum).append(",");

				recNum++;
			}

		} catch (IOException e) {
			//e.printStackTrace();
			nbplog.error("Exception File : " + args[1]+ " not exist / not redeable !");
			Runtime.getRuntime().exit(0);
		}
		
		if (recordsInError.length() != 0) {
			nbplog.error("Found  errors in input recors see log for more details...");
			nbplog.error("Records with errors :" + recordsInError.toString());
		}

		nbplog.info("Batch SSAAcronimoBusinessApplication Finish .. CS");
	}

	private boolean createSSAToAcronimoAssociation(ConnectionDataBeanSingleton cdb, String ssa, String ssaDescr,
			String acronimo, String acronimoDescr,String ambito,String ambito_descr, String[] param, int recNum, HashMap matricoleHash) {

		if (recNum==4) {
			int g=0;
		}
		
		boolean result = false;
		boolean checkObject = false;
		boolean checkObjectAcronimo = false;
		boolean checkObjectSSA = false;

		String checkAcronimo = null;
		String checkSSA = null;

		WSRREnvelopes wsrrenvelopes = new WSRREnvelopes();
		WSRRUtility wsrrutility = new WSRRUtility();

		boolean stepError = false;
		
		String xmlOEnvelope = null;
		String bsrURIssa = null;
		String bsrURIacronimo = null;
		String bsrURIBusinessApplication = null;
		String bsrURIApplicationVersion = null;

		boolean ssaNew = false;
		boolean acronimoNew = false;

		String ssaDescrAtt = null;
		String acronimoDescrAtt = null;

		String url = cdb.getUrl();
		String user = cdb.getUser();
		String password = cdb.getPassword();

		try {

			bsrURIssa = wsrrutility.getGenericObjectByNameAndPrimaryTypeExtended(ssa,
					"http://www.ibm.com/xmlns/prod/serviceregistry/v6r3/ALEModel%23Organization", url, user, password);

			bsrURIacronimo = wsrrutility.getGenericObjectByNameAndPrimaryTypeExtended(acronimo,
					"http://www.ibm.com/xmlns/prod/serviceregistry/v6r3/ALEModel%23Organization", url, user, password);

			checkObjectAcronimo = true;
			checkObjectSSA = true;

			if (bsrURIssa != null && bsrURIssa.contains(">>**ERROR**>>"))
				checkObjectSSA = false;
			if (bsrURIacronimo != null && bsrURIacronimo.contains(">>**ERROR**>>"))
				checkObjectAcronimo = false;

			if (checkObjectAcronimo & checkObjectSSA) {

				if (bsrURIacronimo == null) {
					acronimoNew = true;

					String resp_funz_nom_new = "";
					String resp_funz_nom_current = "";
					try {
						if (param[37].contains(" ")){
							nbplog.error(recNum + " - "
									+ param[37] + " contains blank character/s");
						}
						resp_funz_nom_current = param[37].trim();
						if (param[37].trim() != null && param[37].trim().length() >= 2) {

							resp_funz_nom_new = (String) matricoleHash.get(param[37].trim().substring(1, param[37].trim().length()));

							if (resp_funz_nom_new == null) {

								nbplog.info(recNum + " matricola - " + param[37].trim().substring(1, param[37].trim().length())
										+ " no match set RESP_FUNZIONALE_NOMINATIVO = blank");
								resp_funz_nom_new = "";
							}

						}
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.info(recNum + " - RESP_FUNZIONALE_NOMINATIVO not defined in input file assumed blank");
						resp_funz_nom_current = "";
					}

					String resp_tecn_nom_new = "";
					String resp_tecn_nom_current = "";
					String resp_uff_matricola="";
					String resp_serv_matricola="";
					String resp_attiv_matricola="";
					try {
						if (param[38].contains(" ")){
							nbplog.error(recNum + " - "
									+ param[38] + " contains blank character/s");
						}
						resp_tecn_nom_current = param[38].trim();
						if (param[38].trim() != null && param[38].trim().length() >= 2) {

							resp_tecn_nom_new = (String) matricoleHash.get(param[38].trim().substring(1, param[38].trim().length()));

							if (resp_tecn_nom_new == null) {

								nbplog.info(recNum + " matricola - " + param[38].trim().substring(1, param[38].trim().length())
										+ " no match set RESP_TECNICO_NOMINATIVO = blank");
								resp_tecn_nom_new = "";
							}

						}
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.info(recNum + " - RESP_TECNICO_NOMINATIVO not defined in input file assumed blank");
						resp_tecn_nom_current = "";
					}

					try {
						resp_uff_matricola=param[22].trim();
						if (param[22].contains(" ")){
							nbplog.error(recNum + " - "
									+ param[22] + " contains blank character/s");
						}
					}catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.info(recNum + " - ale63_RESP_UFFICIO_MATRICOLA not defined in input file assumed blank");
						resp_uff_matricola= "";
					}
					
					try {
						resp_serv_matricola=param[26].trim();
						if (param[26].contains(" ")){
							nbplog.error(recNum + " - "
									+ param[26] + " contains blank character/s");
						}
					}catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.info(recNum + " - ale63_RESP_SERVIZIO_MATRICOLA not defined in input file assumed blank");
						resp_serv_matricola= "";
					}
					
					try {
						resp_attiv_matricola=param[18].trim();
						if (param[18].contains(" ")){
							nbplog.error(recNum + " - "
									+ param[18] + " contains blank character/s");
						}
					}catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.info(recNum + " - ale63_RESP_ATTIVITA_MATRICOLA not defined in input file assumed blank");
						resp_attiv_matricola= "";
					}
					
					// xmlOEnvelope =
					// wsrrenvelopes.createOrganizationXMLDataExtended(acronimo,
					// acronimoDescr, param[22],
					// param[2], resp_funz_nom_new, param[27], param[38],
					// param[19],
					// param[18], resp_tecn_nom_new,
					// param[37], param[26], param[23], null, "ACRONIMO");

					//xmlOEnvelope = wsrrenvelopes.createOrganizationXMLDataExtendedAmbito(acronimo, acronimoDescr, param[22].trim(),
					//		param[2], resp_funz_nom_new, param[27], resp_tecn_nom_current, param[19], param[18].trim(),
					//		resp_tecn_nom_new, resp_funz_nom_current, param[26].trim(), param[23], null, "ACRONIMO",ambito,ambito_descr);
					
					xmlOEnvelope = wsrrenvelopes.createOrganizationXMLDataExtendedAmbito(acronimo, acronimoDescr, resp_uff_matricola,
							param[2], resp_funz_nom_new, param[27], resp_tecn_nom_current, param[19], resp_attiv_matricola,
							resp_tecn_nom_new, resp_funz_nom_current, resp_serv_matricola, param[23], null, "ACRONIMO",ambito,ambito_descr);

					bsrURIacronimo = wsrrutility.createWSRRGenericObject(xmlOEnvelope, "POST", url, user, password);

					if (bsrURIacronimo == null) {
						stepError = true;
						nbplog.error(recNum + " - " + "Error : on creating Acronimo : " + acronimo);
					} else
						nbplog.info(recNum + " - " + "Created Organization for acronimo = " + acronimo + " bsrURI "
								+ bsrURIacronimo);

				} else {

					nbplog.info(recNum + " - " + "Found organization (Acronimo) = " + acronimo + " bsrURI "
							+ bsrURIacronimo);

					JSONArray dett = null;

					dett = wsrrutility.getPropertiesByURI(bsrURIacronimo,
							"&p1=description&p2=ale63_RESP_UFFICIO_MATRICOLA&p3=ale63_CODICE_SISTEMA_APPLICATIVO&p4=ale63_RESP_FUNZIONALE_NOMINATIVO&p5=ale63_RESP_TECNICO_MATRICOLA&p6=ale63_RESP_ATTIVITA_NOMINATIVO&p7=ale63_RESP_ATTIVITA_MATRICOLA&p8=ale63_RESP_TECNICO_NOMINATIVO&p9=ale63_RESP_FUNZIONALE_MATRICOLA&p10=ale63_RESP_UFFICIO_NOMINATIVO&p11=ale63_RESP_SERVIZIO_MATRICOLA&p12=ale63_RESP_SERVIZIO_NOMINATIVO&p13=ale63_AMBITO&p14=ale63_DESC_AMBITO",
							url, user, password);
										
					acronimoDescrAtt = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
							"description");
					
					String ambito_curr=WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
							"ale63_AMBITO");
							
				    String ambito_descr_curr=WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
							"ale63_DESC_AMBITO");
					
					String resp_uff_matr = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
							"ale63_RESP_UFFICIO_MATRICOLA");

					String codice_sist_appl = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
							"ale63_CODICE_SISTEMA_APPLICATIVO");

					String resp_funz_nom = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
							"ale63_RESP_FUNZIONALE_NOMINATIVO");

					String resp_serv_nom = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
							"ale63_RESP_SERVIZIO_NOMINATIVO");

					String resp_tecn_matr = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
							"ale63_RESP_TECNICO_MATRICOLA");

					String resp_att_nom = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
							"ale63_RESP_ATTIVITA_NOMINATIVO");

					String resp_att_matr = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
							"ale63_RESP_ATTIVITA_MATRICOLA");

					String resp_tecn_nom = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
							"ale63_RESP_TECNICO_NOMINATIVO");

					String resp_funz_matr = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
							"ale63_RESP_FUNZIONALE_MATRICOLA");

					String resp_serv_matr = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
							"ale63_RESP_SERVIZIO_MATRICOLA");

					String resp_uff_nom = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
							"ale63_RESP_UFFICIO_NOMINATIVO");

					String fromFile = "";

					// [22] ale63_RESP_UFFICIO_MATRICOLA

					try {
						if (param[22].contains(" ")){
							nbplog.error(recNum + " - "
									+ param[22] + " contains blank character/s");
						}
						fromFile = param[22].trim();

						if (!resp_uff_matr.equals(fromFile)) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIacronimo, "ale63_RESP_UFFICIO_MATRICOLA",
									fromFile, url, user, password)) {
								nbplog.error(+recNum + " - "
										+ "Error : on updating ale63_RESP_UFFICIO_MATRICOLA for object type Organization : "
										+ acronimo + " " + bsrURIacronimo);
							} else
								nbplog.info(recNum + " - " + "Acronimo : " + bsrURIacronimo
										+ " changed field - ale63_RESP_UFFICIO_MATRICOLA - value before = " + resp_uff_matr
										+ " new value = " + fromFile);
						}
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.error(+recNum + " - "
								+ "No update for : ale63_RESP_UFFICIO_MATRICOLA for object type Organization : "
								+ acronimo + " " + bsrURIacronimo+" because column 22 is not present or empty (columns start from 0)");
					}


					// [2] ale63_CODICE_SISTEMA_APPLICATIVO

					try {
						fromFile = param[2];
						if (!codice_sist_appl.equals(fromFile)) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIacronimo,
									"ale63_CODICE_SISTEMA_APPLICATIVO", fromFile, url, user, password)) {
								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_CODICE_SISTEMA_APPLICATIVO for object type Organization : "
										+ acronimo + " " + bsrURIacronimo);
							} else
								nbplog.info(recNum + " - " + "Acronimo : " + bsrURIacronimo
										+ " changed field - ale63_CODICE_SISTEMA_APPLICATIVO - value before = "
										+ codice_sist_appl + " new value = " + fromFile);
						}
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.error(+recNum + " - "
								+ "No update for : ale63_CODICE_SISTEMA_APPLICATIVO for object type Organization : "
								+ acronimo + " " + bsrURIacronimo+" because column 2 is not present or empty (columns start from 0)");
					}


					// ale63_RESP_FUNZIONALE_NOMINATIVO [ricalcolato]

					String resp_funz_nom_new = "";
					//System.out.println("ZZZZZ "+param[37]);
					try {
						if (param[37].contains(" ")){
							nbplog.error(recNum + " - "
									+ param[37] + " contains blank character/s");
						}
						if (param[37].trim() != null && param[37].trim().length() >= 2) {

							resp_funz_nom_new = (String) matricoleHash.get(param[37].trim().substring(1, param[37].trim().length()));

							if (resp_funz_nom_new == null) {
								nbplog.info(recNum + " matricola - " + param[37].trim().substring(1, param[37].trim().length())
										+ " no match assume RESP_FUNZIONALE_NOMINATIVO = blank");
								resp_funz_nom_new = "";
							}
						}
						
						if (!resp_funz_nom.equals(resp_funz_nom_new)) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIacronimo,
									"ale63_RESP_FUNZIONALE_NOMINATIVO", resp_funz_nom_new, url, user, password)) {
								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_FUNZIONALE_NOMINATIVO for object type Organization : "
										+ acronimo + " " + bsrURIacronimo);
							} else
								nbplog.info(recNum + " - " + "Acronimo : " + bsrURIacronimo
										+ " changed field - ale63_RESP_FUNZIONALE_NOMINATIVO - value before = "
										+ resp_funz_nom + " new value = " + resp_funz_nom_new);
						}
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.error(+recNum + " - "
								+ "No update for : ale63_RESP_FUNZIONALE_NOMINATIVO for object type Organization : "
								+ acronimo + " " + bsrURIacronimo+" because column 37 is not present or empty (columns start from 0)");
						
					}



					// [27] ale63_RESP_SERVIZIO_NOMINATIVO

					try {
						fromFile = param[27];
						if (!resp_serv_nom.equals(fromFile)) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIacronimo,
									"ale63_RESP_SERVIZIO_NOMINATIVO", fromFile, url, user, password)) {
								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_SERVIZIO_NOMINATIVO for object type Organization : "
										+ acronimo + " " + bsrURIacronimo);
							} else
								nbplog.info(recNum + " - " + "Acronimo : " + bsrURIacronimo
										+ " changed field - ale63_RESP_SERVIZIO_NOMINATIVO - value before = "
										+ resp_serv_nom + " new value = " + fromFile);
						}
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.error(+recNum + " - "
								+ "No update for : ale63_RESP_SERVIZIO_NOMINATIVO for object type Organization : "
								+ acronimo + " " + bsrURIacronimo+" because column 27 is not present or empty (columns start from 0)");
					}



					// [38] ale63_RESP_TECNICO_MATRICOLA

					try {
						if (param[38].contains(" ")){
							nbplog.error(recNum + " - "
									+ param[38] + " contains blank character/s");
						}
						fromFile = param[38].trim();
						if (!resp_tecn_matr.equals(fromFile)) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIacronimo, "ale63_RESP_TECNICO_MATRICOLA",
									fromFile, url, user, password)) {
								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_TECNICO_MATRICOLA for object type Organization : "
										+ acronimo + " " + bsrURIacronimo);
							} else
								nbplog.info(recNum + " - " + "Acronimo : " + bsrURIacronimo
										+ " changed field - ale63_RESP_TECNICO_MATRICOLA - value before = " + resp_tecn_matr
										+ " new value = " + fromFile);
						}
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.error(+recNum + " - "
								+ "No update for : ale63_RESP_TECNICO_MATRICOLA for object type Organization : "
								+ acronimo + " " + bsrURIacronimo+" because column 38 is not present or empty (columns start from 0)");
					}


					// [19] ale63_RESP_ATTIVITA_NOMINATIVO

					try {
						fromFile = param[19];
						if (!resp_att_nom.equals(fromFile)) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIacronimo,
									"ale63_RESP_ATTIVITA_NOMINATIVO", fromFile, url, user, password)) {
								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_ATTIVITA_NOMINATIVO for object type Organization : "
										+ acronimo + " " + bsrURIacronimo);
							} else
								nbplog.info(recNum + " - " + "Acronimo : " + bsrURIacronimo
										+ " changed field - ale63_RESP_ATTIVITA_NOMINATIVO - value before = " + resp_att_nom
										+ " new value = " + fromFile);
						}
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.error(+recNum + " - "
								+ "No update for : ale63_RESP_ATTIVITA_NOMINATIVO for object type Organization : "
								+ acronimo + " " + bsrURIacronimo+" because column 19 is not present or empty (columns start from 0)");
					}



					// [18] ale63_RESP_ATTIVITA_MATRICOLA

					try {
						if (param[18].contains(" ")){
							nbplog.error(recNum + " - "
									+ param[18] + " contains blank character/s");
						}
						fromFile = param[18].trim();
						if (!resp_att_matr.equals(fromFile)) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIacronimo, "ale63_RESP_ATTIVITA_MATRICOLA",
									fromFile, url, user, password)) {
								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_ATTIVITA_MATRICOLA for object type Organization : "
										+ acronimo + " " + bsrURIacronimo);
							} else
								nbplog.info(recNum + " - " + "Acronimo : " + bsrURIacronimo
										+ " changed field - ale63_RESP_ATTIVITA_MATRICOLA - value before = " + resp_att_matr
										+ " new value = " + fromFile);
						}
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.error(+recNum + " - "
								+ "No update for : ale63_RESP_ATTIVITA_MATRICOLA for object type Organization : "
								+ acronimo + " " + bsrURIacronimo+" because column 18 is not present or empty (columns start from 0)");
					}


					// ale63_RESP_TECNICO_NOMINATIVO [ricalcolato]

					String resp_tecn_nom_new = "";
					try {
						if (param[38].contains(" ")){
							nbplog.error(recNum + " - "
									+ param[38] + " contains blank character/s");
						}
						if (param[38].trim() != null && param[38].trim().length() >= 2) {

							resp_tecn_nom_new = (String) matricoleHash.get(param[38].trim().substring(1, param[38].trim().length()));

							if (resp_tecn_nom_new == null) {
								nbplog.info(recNum + " matricola - " + param[38].trim().substring(1, param[38].trim().length())
										+ " no match set RESP_TECNICO_NOMINATIVO = blank");
								resp_tecn_nom_new = "";
							}

						}
						if (!resp_tecn_nom.equals(resp_tecn_nom_new)) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIacronimo, "ale63_RESP_TECNICO_NOMINATIVO",
									resp_tecn_nom_new, url, user, password)) {
								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_TECNICO_NOMINATIVO for object type Organization : "
										+ acronimo + " " + bsrURIacronimo);
							} else
								nbplog.info(recNum + " - " + "Acronimo : " + bsrURIacronimo
										+ " changed field - ale63_RESP_TECNICO_NOMINATIVO - value before = " + resp_tecn_nom
										+ " new value = " + resp_tecn_nom_new);
						}
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.error(+recNum + " - "
								+ "No update for : ale63_RESP_TECNICO_NOMINATIVO for object type Organization : "
								+ acronimo + " " + bsrURIacronimo+" because column 38 is not present or empty (columns start from 0)");
					}



					// [37] ale63_RESP_FUNZIONALE_MATRICOLA

					try {
						if (param[37].contains(" ")){
							nbplog.error(recNum + " - "
									+ param[37] + " contains blank character/s");
						}
						fromFile = param[37].trim();
						if (!resp_funz_matr.equals(fromFile)) { // before
							// wasparam[36]
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIacronimo,
									"ale63_RESP_FUNZIONALE_MATRICOLA", fromFile, url, user, password)) {
								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_FUNZIONALE_MATRICOLA for object type Organization : "
										+ acronimo + " " + bsrURIacronimo);
							} else
								nbplog.info(recNum + " - " + "Acronimo : " + bsrURIacronimo
										+ " changed field - ale63_RESP_FUNZIONALE_MATRICOLA - value before = "
										+ resp_funz_matr + " new value = " + fromFile);
						}
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.error(+recNum + " - "
								+ "No update for :  ale63_RESP_FUNZIONALE_MATRICOLA for object type Organization : "
								+ acronimo + " " + bsrURIacronimo+" because column 37 is not present or empty (columns start from 0)");
					}



					// [26] ale63_RESP_SERVIZIO_MATRICOLA

					try {
						if (param[26].contains(" ")){
							nbplog.error(recNum + " - "
									+ param[26] + " contains blank character/s");
						}
						fromFile = param[26].trim();
						if (!resp_serv_matr.equals(fromFile)) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIacronimo, "ale63_RESP_SERVIZIO_MATRICOLA",
									fromFile, url, user, password)) {
								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_SERVIZIO_MATRICOLA for object type Organization : "
										+ acronimo + " " + bsrURIacronimo);
							} else
								nbplog.info(recNum + " - " + "Acronimo : " + bsrURIacronimo
										+ " changed field - ale63_RESP_SERVIZIO_MATRICOLA - value before = "
										+ resp_serv_matr + " new value = " + fromFile);
						}
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.error(+recNum + " - "
								+ "No update for :  ale63_RESP_SERVIZIO_MATRICOLA for object type Organization : "
								+ acronimo + " " + bsrURIacronimo+" because column 26 is not present or empty (columns start from 0)");
					}

			

					// [23] ale63_RESP_UFFICIO_NOMINATIVO

					try {
						fromFile = param[23];
						if (!resp_uff_nom.equals(fromFile)) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIacronimo, "ale63_RESP_UFFICIO_NOMINATIVO",
									fromFile, url, user, password)) {
								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_UFFICIO_NOMINATIVO for object type Organization : "
										+ acronimo + " " + bsrURIacronimo);
							} else
								nbplog.info(recNum + " - " + "Acronimo : " + bsrURIacronimo
										+ " changed field - ale63_RESP_UFFICIO_NOMINATIVO - value before = " + resp_uff_nom
										+ " new value = " + fromFile);
						}
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.error(+recNum + " - "
								+ "No update for : ale63_RESP_UFFICIO_NOMINATIVO for object type Organization : "
								+ acronimo + " " + bsrURIacronimo+" because column 23 is not present or empty (columns start from 0)");
					}

					// description

					if (!acronimoDescr.equalsIgnoreCase(acronimoDescrAtt)) {
						if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIacronimo, "description", acronimoDescr,
								url, user, password)) {
							nbplog.error(
									recNum + " - " + "Error : on updating description for object type Organization : "
											+ acronimo + " " + bsrURIacronimo);
						} else
							nbplog.info(recNum + " - " + "Acronimo : " + bsrURIacronimo
									+ " changed field - description - value before = " + acronimoDescrAtt
									+ " new value = " + acronimoDescr);

					}
					
					//ambito
					if (!ambito.equalsIgnoreCase(ambito_curr)) {
						if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIacronimo, "ale63_AMBITO", ambito,
								url, user, password)) {
							nbplog.error(
									recNum + " - " + "Error : on updating ambito for object type Organization : "
											+ acronimo + " " + bsrURIacronimo);
						} else
							nbplog.info(recNum + " - " + "Acronimo : " + bsrURIacronimo
									+ " changed field - ambito - value before = " + ambito_curr
									+ " new value = " + ambito);

					}
					
					//ambito desc
					if (!ambito_descr.equalsIgnoreCase(ambito_descr_curr)) {
						if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIacronimo, "ale63_DESC_AMBITO", ambito_descr,
								url, user, password)) {
							nbplog.error(
									recNum + " - " + "Error : on updating ambito_descr for object type Organization : "
											+ acronimo + " " + bsrURIacronimo);
						} else
							nbplog.info(recNum + " - " + "Acronimo : " + bsrURIacronimo
									+ " changed field - ambito - value before = " + ambito_descr_curr
									+ " new value = " + ambito_descr);

					}
					


				}

				if (!stepError) { // no prevoius errors in creation

					if (bsrURIssa == null) {
						
						String resp_uff_matricola="";
						String resp_attiv_matricola="";
						String resp_serv_matricola="";
						String resp_tecnico_matricola="";
						String resp_funz_matricola="";
						
						try {
							resp_uff_matricola=param[22].trim();
							if (param[22].contains(" ")){
								nbplog.error(recNum + " - "
										+ param[22] + " contains blank character/s");
							}
						}catch (java.lang.ArrayIndexOutOfBoundsException ex) {
							nbplog.info(recNum + " - ale63_RESP_UFFICIO_MATRICOLA not defined in input file assumed blank");
							resp_uff_matricola= "";
						}
						
						try {
							resp_serv_matricola=param[26].trim();
							if (param[26].contains(" ")){
								nbplog.error(recNum + " - "
										+ param[26] + " contains blank character/s");
							}
						}catch (java.lang.ArrayIndexOutOfBoundsException ex) {
							nbplog.info(recNum + " - ale63_RESP_SERVIZIO_MATRICOLA not defined in input file assumed blank");
							resp_serv_matricola= "";
						}
						
						try {
							resp_attiv_matricola=param[18].trim();
							if (param[18].contains(" ")){
								nbplog.error(recNum + " - "
										+ param[18] + " contains blank character/s");
							}
						}catch (java.lang.ArrayIndexOutOfBoundsException ex) {
							nbplog.info(recNum + " - ale63_RESP_ATTIVITA_MATRICOLA not defined in input file assumed blank");
							resp_attiv_matricola= "";
						}
		                
						try {
							resp_tecnico_matricola=param[14].trim();
							if (param[14].contains(" ")){
								nbplog.error(recNum + " - "
										+ param[14] + " contains blank character/s");
							}
						}catch (java.lang.ArrayIndexOutOfBoundsException ex) {
							nbplog.info(recNum + " - ale63_RESP_TECNICO_MATRICOLA not defined in input file assumed blank");
							resp_tecnico_matricola= "";
						}
						
						try {
							resp_funz_matricola=param[12].trim();
							if (param[12].contains(" ")){
								nbplog.error(recNum + " - "
										+ param[12] + " contains blank character/s");
							}
						}catch (java.lang.ArrayIndexOutOfBoundsException ex) {
							nbplog.info(recNum + " - ale63_RESP_FUNZIONALE_MATRICOLA not defined in input file assumed blank");
							resp_funz_matricola= "";
						}
						
						//xmlOEnvelope = wsrrenvelopes.createOrganizationXMLDataExtendedAmbito(ssa, ssaDescr, param[22].trim(),
						//		param[2], param[13], param[27], param[14].trim(), param[19], param[18].trim(), param[15], param[12].trim(),
						//		param[26].trim(), param[23], bsrURIacronimo, "SSA",ambito,ambito_descr);
						
						xmlOEnvelope = wsrrenvelopes.createOrganizationXMLDataExtendedAmbito(ssa, ssaDescr, resp_uff_matricola,
								param[2], param[13], param[27], resp_tecnico_matricola, param[19], resp_attiv_matricola, param[15], resp_funz_matricola,
								resp_serv_matricola, param[23], bsrURIacronimo, "SSA",ambito,ambito_descr);

						bsrURIssa = wsrrutility.createWSRRGenericObject(xmlOEnvelope, "POST", url, user, password);

						ssaNew = true;

						if (bsrURIssa == null) {

							stepError = true;

							nbplog.error(recNum + " - " + "Error : on creating SSA : " + ssa);

						} else
							nbplog.info(
									recNum + " - " + "Created Organization for ssa = " + ssa + " bsrURI " + bsrURIssa);

					} else {

						nbplog.info(recNum + " - " + "Found organization (SSA) = " + ssa + " bsrURI " + bsrURIssa);

						JSONArray dett = null;

						dett = wsrrutility.getPropertiesByURI(bsrURIssa,
								"&p1=description&p2=ale63_RESP_UFFICIO_MATRICOLA&p3=ale63_CODICE_SISTEMA_APPLICATIVO&p4=ale63_RESP_FUNZIONALE_NOMINATIVO&p5=ale63_RESP_TECNICO_MATRICOLA&p6=ale63_RESP_ATTIVITA_NOMINATIVO&p7=ale63_RESP_ATTIVITA_MATRICOLA&p8=ale63_RESP_TECNICO_NOMINATIVO&p9=ale63_RESP_FUNZIONALE_MATRICOLA&p10=ale63_RESP_UFFICIO_NOMINATIVO&p11=ale63_RESP_SERVIZIO_MATRICOLA&p12=ale63_RESP_SERVIZIO_NOMINATIVO&p13=ale63_AMBITO&p14=ale63_DESC_AMBITO",
								url, user, password);

						ssaDescrAtt = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
								"description");
						
						String ambito_curr=WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
								"ale63_AMBITO");
								
					    String ambito_descr_curr=WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
								"ale63_DESC_AMBITO");

						String resp_uff_matr = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
								"ale63_RESP_UFFICIO_MATRICOLA");

						String codice_sist_appl = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
								"ale63_CODICE_SISTEMA_APPLICATIVO");

						String resp_funz_nom = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
								"ale63_RESP_FUNZIONALE_NOMINATIVO");

						String resp_serv_nom = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
								"ale63_RESP_SERVIZIO_NOMINATIVO");

						String resp_tecn_matr = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
								"ale63_RESP_TECNICO_MATRICOLA");

						String resp_att_nom = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
								"ale63_RESP_ATTIVITA_NOMINATIVO");

						String resp_att_matr = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
								"ale63_RESP_ATTIVITA_MATRICOLA");

						String resp_tecn_nom = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
								"ale63_RESP_TECNICO_NOMINATIVO");

						String resp_funz_matr = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
								"ale63_RESP_FUNZIONALE_MATRICOLA");

						String resp_serv_matr = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
								"ale63_RESP_SERVIZIO_MATRICOLA");

						String resp_uff_nom = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
								"ale63_RESP_UFFICIO_NOMINATIVO");

						try{
							if (param[22].contains(" ")){
								nbplog.error(recNum + " - "
										+ param[22] + " contains blank character/s");
							}
						if (!resp_uff_matr.equals(param[22].trim())) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIssa, "ale63_RESP_UFFICIO_MATRICOLA",
									param[22].trim(), url, user, password)) {

								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_UFFICIO_MATRICOLA for object type Organization : "
										+ ssa + " " + bsrURIssa);
							} else
								nbplog.info(recNum + " - " + "ssa : " + ssa + " bsrUri : " + bsrURIssa
										+ " changed field - ale63_RESP_UFFICIO_MATRICOLA - value before = "
										+ resp_uff_matr + " new value = " + param[22].trim());
						}}
						catch (java.lang.ArrayIndexOutOfBoundsException ex) {

							nbplog.error(recNum + " - "
									+ "Error : on updating ale63_RESP_UFFICIO_MATRICOLA for object type Organization : "
									+ ssa + " " + bsrURIssa + " because column 22 is not present or empty (columns start from 0)");							
						}
						try {
						if (!codice_sist_appl.equals(param[2])) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIssa,
									"ale63_CODICE_SISTEMA_APPLICATIVO", param[2], url, user, password)) {

								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_CODICE_SISTEMA_APPLICATIVO for object type Organization : "
										+ ssa + " " + bsrURIssa);
							} else
								nbplog.info(recNum + " - " + "ssa : " + ssa + " bsrUri : " + bsrURIssa
										+ " changed field - ale63_CODICE_SISTEMA_APPLICATIVO - value before = "
										+ codice_sist_appl + " new value = " + param[2]);
						}}
						catch (java.lang.ArrayIndexOutOfBoundsException ex) {
							nbplog.error(recNum + " - "
									+ "Error : on updating ale63_CODICE_SISTEMA_APPLICATIVO for object type Organization : "
									+ ssa + " " + bsrURIssa + " because column 2 is not present or empty (columns start from 0)");
						}

						try{
						if (!resp_funz_nom.equals(param[13])) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIssa,
									"ale63_RESP_FUNZIONALE_NOMINATIVO", param[13], url, user, password)) {

								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_FUNZIONALE_NOMINATIVO for object type Organization : "
										+ ssa + " " + bsrURIssa);
							} else
								nbplog.info(recNum + " - " + "ssa : " + ssa + " bsrUri : " + bsrURIssa
										+ " changed field - ale63_RESP_FUNZIONALE_NOMINATIVO - value before = "
										+ resp_funz_nom + " new value = " + param[13]);
						}}
						catch (java.lang.ArrayIndexOutOfBoundsException ex) {
							nbplog.error(recNum + " - "
									+ "Error : on updating ale63_RESP_FUNZIONALE_NOMINATIVO for object type Organization : "
									+ ssa + " " + bsrURIssa + " because column 13 is not present or empty (columns start from 0)");						
						}

						try{
						if (!resp_serv_nom.equals(param[27])) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIssa, "ale63_RESP_SERVIZIO_NOMINATIVO",
									param[27], url, user, password)) {

								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_SERVIZIO_NOMINATIVO for object type Organization : "
										+ ssa + " " + bsrURIssa);
							} else
								nbplog.info(recNum + " - " + "ssa : " + ssa + " bsrUri : " + bsrURIssa
										+ " changed field - ale63_RESP_SERVIZIO_NOMINATIVO - value before = "
										+ resp_serv_nom + " new value = " + param[27]);
						}}
						catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.error(recNum + " - "
									+ "Error : on updating ale63_RESP_SERVIZIO_NOMINATIVO for object type Organization : "
									+ ssa + " " + bsrURIssa + " because column 27 is not present or empty (columns start from 0)");							
						}

						try{
							if (param[14].contains(" ")){
								nbplog.error(recNum + " - "
										+ param[14] + " contains blank character/s");
							}
							if (param[14].contains(" ")){
								nbplog.error(recNum + " - "
										+ param[14] + " contains blank character/s");
							}
						if (!resp_tecn_matr.equals(param[14].trim())) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIssa, "ale63_RESP_TECNICO_MATRICOLA",
									param[14].trim(), url, user, password)) {

								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_TECNICO_MATRICOLA for object type Organization : "
										+ ssa + " " + bsrURIssa);
							} else
								nbplog.info(recNum + " - " + "ssa : " + ssa + " bsrUri : " + bsrURIssa
										+ " changed field - ale63_RESP_TECNICO_MATRICOLA - value before = "
										+ resp_tecn_matr + " new value = " + param[14].trim());
						}}
						catch (java.lang.ArrayIndexOutOfBoundsException ex) {
							nbplog.error(recNum + " - "
									+ "Error : on updating ale63_RESP_TECNICO_MATRICOLA for object type Organization : "
									+ ssa + " " + bsrURIssa + " because column 14 is not present or empty (columns start from 0)");
						}
                        
						try{
						if (!resp_att_nom.equals(param[19])) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIssa, "ale63_RESP_ATTIVITA_NOMINATIVO",
									param[19], url, user, password)) {

								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_ATTIVITA_NOMINATIVO for object type Organization : "
										+ ssa + " " + bsrURIssa);
							} else
								nbplog.info(recNum + " - " + "ssa : " + ssa + " bsrUri : " + bsrURIssa
										+ " changed field - ale63_RESP_ATTIVITA_NOMINATIVO - value before = "
										+ resp_att_nom + " new value = " + param[19]);
						}}
						catch (java.lang.ArrayIndexOutOfBoundsException ex) {
							nbplog.error(recNum + " - "
									+ "Error : on updating ale63_RESP_ATTIVITA_NOMINATIVO for object type Organization : "
									+ ssa + " " + bsrURIssa + " because column 19 is not present or empty (columns start from 0)");							
						}

						try{
							if (param[18].contains(" ")){
								nbplog.error(recNum + " - "
										+ param[18] + " contains blank character/s");
							}
						if (!resp_att_matr.equals(param[18].trim())) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIssa, "ale63_RESP_ATTIVITA_MATRICOLA",
									param[18].trim(), url, user, password)) {

								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_ATTIVITA_MATRICOLA for object type Organization : "
										+ ssa + " " + bsrURIssa);
							} else
								nbplog.info(recNum + " - " + "ssa : " + ssa + " bsrUri : " + bsrURIssa
										+ " changed field - ale63_RESP_ATTIVITA_MATRICOLA - value before = "
										+ resp_att_matr + " new value = " + param[18].trim());
						}}
						catch (java.lang.ArrayIndexOutOfBoundsException ex) {
							nbplog.error(recNum + " - "
									+ "Error : on updating ale63_RESP_ATTIVITA_MATRICOLA for object type Organization : "
									+ ssa + " " + bsrURIssa + " because column 18 is not present or empty (columns start from 0)");							
						}

						try{
						if (!resp_tecn_nom.equals(param[15])) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIssa, "ale63_RESP_TECNICO_NOMINATIVO",
									param[15], url, user, password)) {

								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_TECNICO_NOMINATIVO for object type Organization : "
										+ ssa + " " + bsrURIssa);
							} else
								nbplog.info(recNum + " - " + "ssa : " + ssa + " bsrUri : " + bsrURIssa
										+ " changed field - ale63_RESP_TECNICO_NOMINATIVO - value before = "
										+ resp_tecn_nom + " new value = " + param[15]);
						}}
						catch (java.lang.ArrayIndexOutOfBoundsException ex) {
							nbplog.error(recNum + " - "
									+ "Error : on updating ale63_RESP_TECNICO_NOMINATIVO for object type Organization : "
									+ ssa + " " + bsrURIssa + " because column 15 is not present or empty (columns start from 0)");							
						}

						try {
							if (param[12].contains(" ")){
								nbplog.error(recNum + " - "
										+ param[12] + " contains blank character/s");
							}
						if (param[12].contains(" ")){
							nbplog.error(recNum + " - "
									+ param[12] + " contains blank character/s");
						}
						if (!resp_funz_matr.equals(param[12].trim())) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIssa,
									"ale63_RESP_FUNZIONALE_MATRICOLA", param[12].trim(), url, user, password)) {

								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_FUNZIONALE_MATRICOLA for object type Organization : "
										+ ssa + " " + bsrURIssa);
							} else
								nbplog.info(recNum + " - " + "ssa : " + ssa + " bsrUri : " + bsrURIssa
										+ " changed field - ale63_RESP_FUNZIONALE_MATRICOLA - value before = "
										+ resp_funz_matr + " new value = " + param[12].trim());
						}}
						catch (java.lang.ArrayIndexOutOfBoundsException ex) {
							nbplog.error(recNum + " - "
									+ "Error : on updating ale63_RESP_FUNZIONALE_MATRICOLA for object type Organization : "
									+ ssa + " " + bsrURIssa + " because column 12 is not present or empty (columns start from 0)");							
						}

						try{
							if (param[26].contains(" ")){
								nbplog.error(recNum + " - "
										+ param[26] + " contains blank character/s");
							}
						if (!resp_serv_matr.equals(param[26].trim())) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIssa, "ale63_RESP_SERVIZIO_MATRICOLA",
									param[26].trim(), url, user, password)) {

								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_SERVIZIO_MATRICOLA for object type Organization : "
										+ ssa + " " + bsrURIssa);
							} else
								nbplog.info(recNum + " - " + "ssa : " + ssa + " bsrUri : " + bsrURIssa
										+ " changed field - ale63_RESP_SERVIZIO_MATRICOLA - value before = "
										+ resp_serv_matr + " new value = " + param[26].trim());
						}}
						catch (java.lang.ArrayIndexOutOfBoundsException ex) {

							nbplog.error(recNum + " - "
									+ "Error : on updating ale63_RESP_SERVIZIO_MATRICOLA for object type Organization : "
									+ ssa + " " + bsrURIssa + " because column 26 is not present or empty (columns start from 0)");							
						}

						try{
						if (!resp_uff_nom.equals(param[23])) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIssa, "ale63_RESP_UFFICIO_NOMINATIVO",
									param[23], url, user, password)) {

								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_UFFICIO_NOMINATIVO for object type Organization : "
										+ ssa + " " + bsrURIssa);
							} else
								nbplog.info(recNum + " - " + "ssa : " + ssa + " bsrUri : " + bsrURIssa
										+ " changed field - ale63_RESP_UFFICIO_NOMINATIVO - value before = "
										+ resp_uff_nom + " new value = " + param[23]);
						}}
						catch (java.lang.ArrayIndexOutOfBoundsException ex) {
							nbplog.error(recNum + " - "
									+ "Error : on updating ale63_RESP_UFFICIO_NOMINATIVO for object type Organization : "
									+ ssa + " " + bsrURIssa + " because column 23 is not present or empty (columns start from 0)");							
						}

						if (!ssaDescr.equalsIgnoreCase(ssaDescrAtt)) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIssa, "description", ssaDescr, url,
									user, password)) {

								nbplog.error(recNum + " - "
										+ "Error : on updating description for object type Organization : " + ssa + " "
										+ bsrURIssa);
							} else
								nbplog.info(recNum + " - " + "ssa : " + ssa + " bsrUri : " + bsrURIssa
										+ " changed field - description - value before = " + ssaDescrAtt
										+ " new value = " + ssaDescr);

						}
						
						//ambito
						if (!ambito.equalsIgnoreCase(ambito_curr)) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIssa, "ale63_AMBITO", ambito,
									url, user, password)) {
								nbplog.error(
										recNum + " - " + "Error : on updating ambito for object type Organization : "
												+ ssa + " " + bsrURIssa);
							} else
								nbplog.info(recNum + " - " + "ssa : " + bsrURIssa
										+ " changed field - ambito - value before = " + ambito_curr
										+ " new value = " + ambito);

						}
						
						//ambito desc
						if (!ambito_descr.equalsIgnoreCase(ambito_descr_curr)) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIssa, "ale63_DESC_AMBITO", ambito_descr,
									url, user, password)) {
								nbplog.error(
										recNum + " - " + "Error : on updating ambito_descr for object type Organization : "
												+ ssa + " " + bsrURIssa);
							} else
								nbplog.info(recNum + " - " + "ssa : " + bsrURIssa
										+ " changed field - ambito - value before = " + ambito_descr_curr
										+ " new value = " + ambito_descr);

						}

					}

				}

				if (!stepError) {

					if (ssaNew & acronimoNew) {

						result = wsrrutility.updateRelationShip(bsrURIssa, "ale63_childOrganizations", bsrURIacronimo,
								url, user, password);

						if (!result)

							nbplog.error(recNum + " - " + "Error : on updating relation for object type Organization : "
									+ ssa + " " + bsrURIssa);
					} else {

						String verbose_result = wsrrutility.checkSSAAndAcronimoRelationShipVerbose(acronimo,
								bsrURIacronimo, bsrURIssa, url, user, password);

						if (verbose_result == null)
							result = false;
						else {
							result = true;
							if (verbose_result.length() != 0) {
								nbplog.info(recNum + " - " + verbose_result);
							}
						}

						if (!result)
							nbplog.error(recNum + " - " + "Error : on updating relation for object type Organization : "
									+ ssa + " " + bsrURIssa);
					}

				} else {
					nbplog.error(recNum + " - " + "Error : creation of object type Organization for Acronimo : " +acronimo + " or SSA : "+ssa);
				}
				
				
				//now work with Application Version and Business Application
				
				boolean checkApplicationVersion = true;
				boolean businessApplication = true;

				bsrURIApplicationVersion = wsrrutility.getGenericObjectByNameAndVersionAndPrimaryTypeExtended(acronimo,
						"00",
						"http://www.ibm.com/xmlns/prod/serviceregistry/profile/v6r3/GovernanceEnablementModel%23ApplicationVersion",
						url, user, password);

				if (bsrURIApplicationVersion != null && bsrURIApplicationVersion.contains(">>**ERROR**>>"))
					checkApplicationVersion = false;

				if (checkApplicationVersion) {

					if (bsrURIApplicationVersion == null) {

						xmlOEnvelope = wsrrenvelopes.createApplicationVersionXMLData(acronimo, bsrURIacronimo);//
						bsrURIApplicationVersion = wsrrutility.createWSRRGenericObject(xmlOEnvelope, "POST", url, user,
								password);

						if (bsrURIApplicationVersion == null) {
							nbplog.error(recNum + " Creation of object Application Version  : " + acronimo + " failed");
							result=false;
						} else {
							nbplog.info(recNum + " - Created Application Version Object : " + acronimo +" "+bsrURIApplicationVersion);
						}
					}
				} else {
					nbplog.error("Error : on query object ApplicationVersion : " + acronimo + " version : 00 "
							+ bsrURIApplicationVersion.substring(12, bsrURIApplicationVersion.length()));
					 result=false;
				    }
				
				if (result) {

				bsrURIBusinessApplication = wsrrutility.getGenericObjectByNameAndPrimaryTypeExtended(acronimo,
						"http://www.ibm.com/xmlns/prod/serviceregistry/profile/v6r3/GovernanceEnablementModel%23BusinessApplication",
						url, user, password);

				if (bsrURIBusinessApplication != null && bsrURIBusinessApplication.contains(">>**ERROR**>>"))
					businessApplication = false;

				if (businessApplication) {

					if (bsrURIBusinessApplication == null) {

						xmlOEnvelope = wsrrenvelopes.createBusinessApplicationXMLData(acronimo, acronimoDescr,
								bsrURIApplicationVersion, bsrURIacronimo);

						bsrURIBusinessApplication = wsrrutility.createWSRRGenericObject(xmlOEnvelope, "POST", url, user,
								password);

						if (bsrURIBusinessApplication == null){
							nbplog.error(recNum + " Creation of object Business application  : " + acronimo + " failed");
							result=false;
						}else {
							nbplog.info(recNum + " - Created Business application : " + acronimo +" "+bsrURIBusinessApplication);
						}
							
						
					} else {
						result = wsrrutility.updateSinglePropertyJSONFormat(bsrURIBusinessApplication, "description",
								acronimoDescr, url, user, password);
						if (!result)
							nbplog.error("Error : update description for object Business application : " + acronimo + " "
									+ bsrURIBusinessApplication);
					}

				} else {
					nbplog.error("Error : on query object Business Application: " + acronimo + " "
						+ bsrURIBusinessApplication.substring(12, bsrURIBusinessApplication.length()));
					result=false;
				}
				
		     } else {
					nbplog.error("Business application : " + acronimo + " not created due previous error on Application Version");
		     }

			} else {

				if (!checkObjectSSA) {

					nbplog.error("Error : on query object of type organization : " + ssa + " "
							+ bsrURIssa.substring(12, bsrURIssa.length()));
					result=false;
				}

				if (!checkObjectAcronimo) {

					nbplog.error("Error : on query object of type organization : " + acronimo + " "
							+ bsrURIacronimo.substring(12, bsrURIacronimo.length()));
					result=false;
				}

			}

		} catch (Exception ex) {
			nbplog.error("Found Runtime error on record - " + recNum + " description - " + ex.toString());
			result = false;
		}
		return result;
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
