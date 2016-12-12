package com.isp.wsrr.batch.consumeproducer;

import java.io.BufferedReader;
import java.io.FileInputStream;
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
 * V1.2
 * 
 * aggiunti metodi che ritornano l'errore in formato esteso esempio:
 * getGenericObjectByNameAndPrimaryTypeExtended tolta la cancellazione in caso
 * di errore
 */

public class SSAAcronimoBusinessApplication extends SLAConsumerAndProvider {

	private static FileInputStream fis;
	private static BufferedReader br;
	private static String line = null;

	private static final Logger nbplog = LogManager.getLogger(SSAAcronimoBusinessApplication.class.getName());

	public static void main(String[] args) {

		String logFileName = System.getProperty("LogFileName");

		if (logFileName != null && logFileName.length() != 0)
			updateLogger(logFileName, "caricamentiISPAppender",
					"com.isp.wsrr.batch.consumeproducer.SSAAcronimoBusinessApplication");

		nbplog.info(
				"----------------------------------------------------------------------------------------------------------------------");
		nbplog.info("Batch SSAAcronimoBusinessApplication V1.3 Settembre 2016");
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
		SSAAcronimoBusinessApplication ssatoacronimoassociation = new SSAAcronimoBusinessApplication();

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
						surname = line.substring(6, 31);
					} catch (Exception ex2) {
						inError = true;
						errors++;
						nbplog.error("Error on getting surname field for record  " + line);
					}

					try {
						name = line.substring(31, 47);

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
			e.printStackTrace();
			nbplog.error("Exception while reading file: " + args[2]);

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
						paramArray[6], paramArray[7], paramArray, recNum, matricoleHash))
					recordsInError.append(recNum).append(",");

				recNum++;
			}

		} catch (IOException e) {
			e.printStackTrace();
			nbplog.error("Exception while reading  file: " + args[1]);

		}

		if (recordsInError.length() != 0) {
			nbplog.error("Found  errors in input recors see log for more details...");
			nbplog.error("Records with errors :" + recordsInError.toString());
		}

		nbplog.info("Batch SSAAcronimoBusinessApplication Finish .. CS");
	}

	private boolean createSSAToAcronimoAssociation(ConnectionDataBeanSingleton cdb, String ssa, String ssaDescr,
			String acronimo, String acronimoDescr, String[] param, int recNum, HashMap matricoleHash) {

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
						resp_funz_nom_current = param[37];
						if (param[37] != null && param[37].length() >= 2) {

							resp_funz_nom_new = (String) matricoleHash.get(param[37].substring(1, param[37].length()));

							if (resp_funz_nom_new == null) {

								nbplog.info(recNum + " matricola - " + param[37].substring(1, param[37].length())
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
					try {
						resp_tecn_nom_current = param[38];
						if (param[38] != null && param[38].length() >= 2) {

							resp_tecn_nom_new = (String) matricoleHash.get(param[38].substring(1, param[38].length()));

							if (resp_tecn_nom_new == null) {

								nbplog.info(recNum + " matricola - " + param[38].substring(1, param[38].length())
										+ " no match set RESP_TECNICO_NOMINATIVO = blank");
								resp_tecn_nom_new = "";
							}

						}
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.info(recNum + " - RESP_TECNICO_NOMINATIVO not defined in input file assumed blank");
						resp_tecn_nom_current = "";
					}

					// xmlOEnvelope =
					// wsrrenvelopes.createOrganizationXMLDataExtended(acronimo,
					// acronimoDescr, param[22],
					// param[2], resp_funz_nom_new, param[27], param[38],
					// param[19],
					// param[18], resp_tecn_nom_new,
					// param[37], param[26], param[23], null, "ACRONIMO");

					xmlOEnvelope = wsrrenvelopes.createOrganizationXMLDataExtended(acronimo, acronimoDescr, param[22],
							param[2], resp_funz_nom_new, param[27], resp_tecn_nom_current, param[19], param[18],
							resp_tecn_nom_new, resp_funz_nom_current, param[26], param[23], null, "ACRONIMO");

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
							"&p1=description&p2=ale63_RESP_UFFICIO_MATRICOLA&p3=ale63_CODICE_SISTEMA_APPLICATIVO&p4=ale63_RESP_FUNZIONALE_NOMINATIVO&p5=ale63_RESP_TECNICO_MATRICOLA&p6=ale63_RESP_ATTIVITA_NOMINATIVO&p7=ale63_RESP_ATTIVITA_MATRICOLA&p8=ale63_RESP_TECNICO_NOMINATIVO&p9=ale63_RESP_FUNZIONALE_MATRICOLA&p10=ale63_RESP_UFFICIO_NOMINATIVO&p11=ale63_RESP_SERVIZIO_MATRICOLA&p12=ale63_RESP_SERVIZIO_NOMINATIVO",
							url, user, password);

					acronimoDescrAtt = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
							"description");

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
						fromFile = param[22];
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						fromFile = "";
					}

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

					// [2] ale63_CODICE_SISTEMA_APPLICATIVO

					try {
						fromFile = param[2];
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						fromFile = "";
					}

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

					// ale63_RESP_FUNZIONALE_NOMINATIVO [ricalcolato]

					String resp_funz_nom_new = "";
					try {
						if (param[37] != null && param[37].length() >= 2) {

							resp_funz_nom_new = (String) matricoleHash.get(param[37].substring(1, param[37].length()));

							if (resp_funz_nom_new == null) {
								nbplog.info(recNum + " matricola - " + param[37].substring(1, param[37].length())
										+ " no match assume RESP_FUNZIONALE_NOMINATIVO = blank");
								resp_funz_nom_new = "";
							}
						}
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.info(recNum + " - RESP_FUNZIONALE_NOMINATIVO not defined in input file assumed blank");
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

					// [27] ale63_RESP_SERVIZIO_NOMINATIVO

					try {
						fromFile = param[27];
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						fromFile = "";
					}

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

					// [38] ale63_RESP_TECNICO_MATRICOLA

					try {
						fromFile = param[38];
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						fromFile = "";
					}

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

					// [19] ale63_RESP_ATTIVITA_NOMINATIVO

					try {
						fromFile = param[19];
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						fromFile = "";
					}

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

					// [18] ale63_RESP_ATTIVITA_MATRICOLA

					try {
						fromFile = param[18];
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						fromFile = "";
					}
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

					// ale63_RESP_TECNICO_NOMINATIVO [ricalcolato]

					String resp_tecn_nom_new = "";
					try {
						if (param[38] != null && param[38].length() >= 2) {

							resp_tecn_nom_new = (String) matricoleHash.get(param[38].substring(1, param[38].length()));

							if (resp_tecn_nom_new == null) {
								nbplog.info(recNum + " matricola - " + param[38].substring(1, param[38].length())
										+ " no match set RESP_TECNICO_NOMINATIVO = blank");
								resp_tecn_nom_new = "";
							}

						}
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						nbplog.info(recNum + " - RESP_TECNICO_NOMINATIVO not defined in input file assumed blank");
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

					// [37] ale63_RESP_FUNZIONALE_MATRICOLA

					try {
						fromFile = param[37];
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						fromFile = "";
					}

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

					// [26] ale63_RESP_SERVIZIO_MATRICOLA

					try {
						fromFile = param[26];
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						fromFile = "";
					}

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

					// [23] ale63_RESP_UFFICIO_NOMINATIVO

					try {
						fromFile = param[23];
					} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
						fromFile = "";
					}

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

				}

				if (!stepError) { // no prevoius errors in creation

					if (bsrURIssa == null) {

						xmlOEnvelope = wsrrenvelopes.createOrganizationXMLDataExtended(ssa, ssaDescr, param[22],
								param[2], param[13], param[27], param[14], param[19], param[18], param[15], param[12],
								param[26], param[23], bsrURIacronimo, "SSA");

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
								"&p1=description&p2=ale63_RESP_UFFICIO_MATRICOLA&p3=ale63_CODICE_SISTEMA_APPLICATIVO&p4=ale63_RESP_FUNZIONALE_NOMINATIVO&p5=ale63_RESP_TECNICO_MATRICOLA&p6=ale63_RESP_ATTIVITA_NOMINATIVO&p7=ale63_RESP_ATTIVITA_MATRICOLA&p8=ale63_RESP_TECNICO_NOMINATIVO&p9=ale63_RESP_FUNZIONALE_MATRICOLA&p10=ale63_RESP_UFFICIO_NOMINATIVO&p11=ale63_RESP_SERVIZIO_MATRICOLA&p12=ale63_RESP_SERVIZIO_NOMINATIVO",
								url, user, password);

						ssaDescrAtt = WSRRUtility.getObjectValueFromJSONArrayData((JSONArray) dett.get(0),
								"description");

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

						if (!resp_uff_matr.equals(param[22])) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIssa, "ale63_RESP_UFFICIO_MATRICOLA",
									param[22], url, user, password)) {

								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_UFFICIO_MATRICOLA for object type Organization : "
										+ ssa + " " + bsrURIssa);
							} else
								nbplog.info(recNum + " - " + "ssa : " + ssa + " bsrUri : " + bsrURIssa
										+ " changed field - ale63_RESP_UFFICIO_MATRICOLA - value before = "
										+ resp_uff_matr + " new value = " + param[22]);
						}

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
						}

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
						}

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
						}

						if (!resp_tecn_matr.equals(param[14])) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIssa, "ale63_RESP_TECNICO_MATRICOLA",
									param[14], url, user, password)) {

								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_TECNICO_MATRICOLA for object type Organization : "
										+ ssa + " " + bsrURIssa);
							} else
								nbplog.info(recNum + " - " + "ssa : " + ssa + " bsrUri : " + bsrURIssa
										+ " changed field - ale63_RESP_TECNICO_MATRICOLA - value before = "
										+ resp_tecn_matr + " new value = " + param[14]);
						}

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
						}

						if (!resp_att_matr.equals(param[18])) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIssa, "ale63_RESP_ATTIVITA_MATRICOLA",
									param[18], url, user, password)) {

								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_ATTIVITA_MATRICOLA for object type Organization : "
										+ ssa + " " + bsrURIssa);
							} else
								nbplog.info(recNum + " - " + "ssa : " + ssa + " bsrUri : " + bsrURIssa
										+ " changed field - ale63_RESP_ATTIVITA_MATRICOLA - value before = "
										+ resp_att_matr + " new value = " + param[18]);
						}

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
						}

						if (!resp_funz_matr.equals(param[12])) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIssa,
									"ale63_RESP_FUNZIONALE_MATRICOLA", param[12], url, user, password)) {

								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_FUNZIONALE_MATRICOLA for object type Organization : "
										+ ssa + " " + bsrURIssa);
							} else
								nbplog.info(recNum + " - " + "ssa : " + ssa + " bsrUri : " + bsrURIssa
										+ " changed field - ale63_RESP_FUNZIONALE_MATRICOLA - value before = "
										+ resp_funz_matr + " new value = " + param[12]);
						}

						if (!resp_serv_matr.equals(param[26])) {
							if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIssa, "ale63_RESP_SERVIZIO_MATRICOLA",
									param[26], url, user, password)) {

								nbplog.error(recNum + " - "
										+ "Error : on updating ale63_RESP_SERVIZIO_MATRICOLA for object type Organization : "
										+ ssa + " " + bsrURIssa);
							} else
								nbplog.info(recNum + " - " + "ssa : " + ssa + " bsrUri : " + bsrURIssa
										+ " changed field - ale63_RESP_SERVIZIO_MATRICOLA - value before = "
										+ resp_serv_matr + " new value = " + param[26]);
						}

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

					}

				}

				if (!stepError) {

					if (ssaNew & acronimoNew) {

						// not useful but idempotent
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
						} else {
							nbplog.info(recNum + " Created Application Version Object : " + acronimo +" "+bsrURIApplicationVersion);
						}
					}
				} else
					nbplog.error("Error : on query object ApplicationVersion : " + acronimo + " version : 00 "
							+ bsrURIApplicationVersion.substring(12, bsrURIApplicationVersion.length()));

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
						}else {
							nbplog.info(recNum + " Created Business application : " + acronimo +" "+bsrURIBusinessApplication);
						}
							
						
					} else {
						result = wsrrutility.updateSinglePropertyJSONFormat(bsrURIBusinessApplication, "description",
								acronimoDescr, url, user, password);
						if (!result)
							nbplog.error("Error : update description for object Business application : " + acronimo + " "
									+ bsrURIBusinessApplication);
					}

				} else
					nbplog.error("Error : on query object Business Application: " + acronimo + " "
						+ bsrURIBusinessApplication.substring(12, bsrURIBusinessApplication.length()));			

			} else {

				if (!checkObjectSSA) {

					nbplog.error("Error : on query object of type organization : " + ssa + " "
							+ bsrURIssa.substring(12, bsrURIssa.length()));
				}

				if (!checkObjectAcronimo) {

					nbplog.error("Error : on query object of type organization : " + acronimo + " "
							+ bsrURIacronimo.substring(12, bsrURIacronimo.length()));
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
