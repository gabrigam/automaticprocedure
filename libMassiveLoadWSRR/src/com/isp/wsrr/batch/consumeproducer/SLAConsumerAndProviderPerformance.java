package com.isp.wsrr.batch.consumeproducer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
import com.isp.wsrr.utility.WSRRUtilityPerformance;

public class SLAConsumerAndProviderPerformance {

	private static FileInputStream fis;
	private static BufferedReader br;
	private static String line = null;
	// private static StringTokenizer st = null;
	// private static ArrayList<String> list;

	protected static Logger log = LogManager.getLogger(SLAConsumerAndProvider.class.getName());
	
	protected static boolean performanceDataLog=false;

	public static void main(String[] args) throws Exception {

		// Runtime.getRuntime().exit(0);

		///////////////////////
		// sla data utlimo utilizzo in system appl e prod
		////////////////////////
		
		
		///////////23052017////////
		//getSLAassociatedToSLDExtendedNew da usare al posto di getSLAassociatedToSLDExtended [getSLAassociatedToSLDExtendedNew � da testare]
		//getSLAassociatedToSLDWithPrimaryTypeExtended � da ottimizzare nel codice c'e' il fake da controllare

		String logFileName = System.getProperty("LogFileName");

		if (logFileName != null && logFileName.length() != 0)

			updateLogger(logFileName, "caricamentiSLAISPAppender",
					"com.isp.wsrr.batch.consumeproducer.SLAConsumerAndProvider");
		log.info(
				"----------------------------------------------------------------------------------------------------------------------");
		log.info("Batch SLAConsumerAndProvider & performance data V2.6 Giugno 2018");
		log.info("migliorata gestione file non trovato o non leggibile (1.5)");
		log.info("22.11.2016 se DESIGNTIME non bisogna aggiorare le date");
		log.info(
				"23.11.2016 inserito controllo nel caso in cui non vengano passati tutti i valori presenti nei file in modo che non venga generata l'eccezzione indice non valido"
						+ "\n per designtime la data non deve essere passata e' stata creata quindi una funzione di controllo ah hoch: checkInputData4DesignTime"
						+ "\n sempre nel caso di designtime il time stamp viene forzato a stringa vuota per compatibilita' con la tipologia runtime");
		log.info("21.01.2017 inserito un controllo sul consumer che deve essere di tipo : SCOPEN - SCOPEN - SOPEN(IIPARAL)");
		log.info("15.03.2017 fix per prendere le Application Version che prima scartava");
		log.info("24-03.2017 tolta riga che generava exception (voluta) fatto su branch del 1503");
		log.info("24-03.2017 modificato il metodo richiamato getEndpointInfo su branch del 1503 di baselib ");
		log.info("11-05-2017 aggiunta la gestione della specializzazione dell' endpoint per getEndpointInfo");
		log.info("14-05-2017 aggiunta la tracciatura dei dati di performance");
		log.info("03-02-2018 inserita gestione http header (da libreria di utility) per tracciatura su dynatrace");
		log.info("24-03-2018 anche in caso di consumer non presente viene aggiornata comunque la data di ultimo utilizzo");
		log.info("06-06-2018 inserita la gestione della tipologia (C-S) canale e relativo messaggio nel log; (C-S) viene gestita come (A-S)");
		log.info(
				"----------------------------------------------------------------------------------------------------------------------");
		log.info("");

		// check Input parameters
		if (args.length == 0) {

			System.out.println(
					"----------------------------------------------------------------------------------------------------------------------");
			System.out.println(
					"Errore : fornire due parametri : WSRRURL(0) nomedelfile(1) ALL/ONLYDATE(2) user(3) password(4)");
			System.out.println("user password solo se https");
			System.out.println(
					"----------------------------------------------------------------------------------------------------------------------");
			Runtime.getRuntime().exit(0); // brutale :)

		}
		
		if (args.length !=6) performanceDataLog=true;

		if (!args[2].equalsIgnoreCase("ALL") && !args[2].equalsIgnoreCase("ONLYDATE")) {

			System.out.println(
					"----------------------------------------------------------------------------------------------------------------------");
			System.out.println("Errore : specificare ALL o ONLYDATE come valore del secondo parametro");
			System.out.println(
					"----------------------------------------------------------------------------------------------------------------------");

			Runtime.getRuntime().exit(0);
		}

		ConnectionDataBeanSingleton cdb = null;

		try {
			cdb = ConnectionDataBeanSingleton.setData(); // fake � stata
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

		log.info(
				"-----------------------------------------------------------------------------------------------------------------------");
		log.info("URL " + cdb.getUrl());
		log.info("File " + args[1]);
		log.info("ALL/ONLYDATE " + args[2]);
		log.info(
				"----------------------------------------------------------------------------------------------------------------------");

		try {

			fis = new FileInputStream(args[1]);
			br = new BufferedReader(new InputStreamReader(fis));
			boolean firstrow = true;
			int recNum = 1;

			String environment = null;
			String tipology = null;

			SLAConsumerAndProviderPerformance slaconsumerandprovider = new SLAConsumerAndProviderPerformance();

			while ((line = br.readLine()) != null) {

				log.info("current Record (" + recNum + ") >> " + line);

				String[] paramArray = line.split("\\;");

				if (firstrow == true) {
					// [0]=environment - application,systemTest..
					// [1]=tipology - runtime - designtime

					boolean inputdataOkHeader = slaconsumerandprovider.checkInputDataHeader(paramArray[0],
							paramArray[1]);

					if (inputdataOkHeader) {

						log.info("InputFile " + args[1] + " Header data are OK");

						environment = paramArray[0];

						//
						if (environment.trim().equalsIgnoreCase("APPLICATION"))
							environment = "Application";
						if (environment.trim().equalsIgnoreCase("SYSTEM"))
							environment = "SystemTest";
						if (environment.trim().equalsIgnoreCase("PRODUZIONE"))
							environment = "Produzione";

						tipology = paramArray[1];

					} else {

						log.error("InputFile " + args[1] + " Header data  contains invalida data : " + line);
						System.exit(0);
					}

					firstrow = false;
				}

				else if (firstrow == false) {

					boolean inputdataOk = true;

					try {

						if (tipology.equalsIgnoreCase("DESIGNTIME")) {

							inputdataOk = slaconsumerandprovider.checkInputData4DesignTime(paramArray[1], paramArray[3],
									paramArray[2], paramArray[4], paramArray[5], paramArray[0]);
						} else {

							inputdataOk = slaconsumerandprovider.checkInputData(paramArray[1], paramArray[3],
									paramArray[2], paramArray[4], paramArray[5], paramArray[0], paramArray[6]);
						}
					} catch (java.lang.ArrayIndexOutOfBoundsException exex) {

						inputdataOk = false;
					}

					boolean onlyOne=true;
					
					if (inputdataOk) {

						if (line != null && !line.startsWith("#")) {

							String providerInvocationTs = "";

							if (!tipology.equalsIgnoreCase("DESIGNTIME")) {
								providerInvocationTs = paramArray[6];
							}
							//060618
							if (onlyOne && paramArray[0].trim().equals("C-S")) {
								onlyOne=false;
								log.info("--------------------------------------------------------------------------------------------------------------------");
								log.info("Found Tipology (C-S)"); 
								log.info("--------------------------------------------------------------------------------------------------------------------");
							}
								
							Long ti = System.nanoTime();
							slaconsumerandprovider.makeSLAConsumerAndProvider(cdb, environment.trim(), tipology.trim(),
									paramArray[1].trim(), paramArray[3].trim(), paramArray[2].trim(),
									paramArray[4].trim(), paramArray[5].trim(), paramArray[0].trim(),
									providerInvocationTs.trim(), args[2], args[1], recNum,line);
							Long tf = System.nanoTime();
							SLAConsumerAndProviderPerformance.logDuration(" current Record("+recNum+")", ti, tf);
						} else {
							// user request a skip
							if (line != null) {
								log.error("record(" + recNum + ") " + line);
								log.error("record(" + recNum + ")  Record skipped");
							} else
								log.error("record(" + recNum + ")  invalid record no data");
						}

					} else
						log.error("Found incorrect Values o values not specified at record Number : " + "(" + recNum
								+ ")");

					recNum++;
				}

			}
			log.info("(*) Last Used Date updated");
			log.info("Batch SLAConsumerAndProviderPerformance finished..CS");
		} catch (IOException e) {
			log.error("Exception File : " + args[1] + " not exist / not redeable !");

		}
	}

	public void makeSLAConsumerAndProvider(ConnectionDataBeanSingleton cdb, String environment, String tipology,
			String consumer, String provider, String consumerVersion, String providerVersion, String interfaceType,
			String bind, String providerInvocationTs, String onlyDate, String filename, int recNum,String record) throws Exception {

		WSRREnvelopes wsrrenvelopes = new WSRREnvelopes();
		WSRRUtilityPerformance wsrrutility = new WSRRUtilityPerformance();

		String url = null;
		String user = null;
		String password = null;

		String bsrURIProvider = "";
		String bsrURIConsumer = "";
		String bsrURIApplicatioVersion = "";
		String sldProvider = "";
		String bsrURISLD = "";
		String bsrURISLA_SV = null;//
		String bsrURISLA_AV = null;
		String acroName = "";
		String envelopeSLA = "";

		String typeService = "";
		String typeServiceSubtype = "";
		
		long ti=0L;
		long tf=0L;

		boolean error = false;

		String[] transactions = {
				"http://www.ibm.com/xmlns/prod/serviceregistry/lifecycle/v6r3/LifecycleDefinition%23RequestSLA",
		"http://www.ibm.com/xmlns/prod/serviceregistry/lifecycle/v6r3/LifecycleDefinition%23ApproveSLARequest" };

		String avPrimaryType = "http://www.ibm.com/xmlns/prod/serviceregistry/profile/v6r3/GovernanceEnablementModel%23ApplicationVersion";

		String inactiveStateSLA = "SLAInactive";

		String activeStateSLA = "SLAActive";

		if (consumerVersion == null || consumerVersion.length() == 0)
			consumerVersion = "00";
		if (providerVersion == null || providerVersion.length() == 0)
			providerVersion = "00";

		try {

			StringBuffer sb = new StringBuffer();

			url = cdb.getUrl();
			user = cdb.getUser();
			password = cdb.getPassword();

			
			ti = System.nanoTime();
			bsrURIProvider = wsrrutility.getGenericObjectByNameAndVersionExtended(provider.trim(),
					providerVersion.trim(), url, user, password,"QUERY 1: "+line);
			tf = System.nanoTime();
			SLAConsumerAndProviderPerformance.logDuration(" QUERY 1 ", ti, tf);
			
			if (bsrURIProvider != null && !bsrURIProvider.contains(">>>***ERROR**>>>")) {

				if (onlyDate != null && !onlyDate.equalsIgnoreCase("ONLYDATE")) {

					ti = System.nanoTime();
					bsrURIConsumer = wsrrutility.getGenericObjectByNameAndVersionExtended(consumer.trim(),
							consumerVersion.trim(), url, user, password,"QUERY 2: "+line);
					tf = System.nanoTime();				
					SLAConsumerAndProviderPerformance.logDuration(" QUERY 2 ",ti, tf);

					// 21012017
					// consumer accettabili: SCOPEN - SCHOST - SOPEN(IIBPARAL)
					ti = System.nanoTime();
					typeService = wsrrutility.getServiceVersionTipologyBybsrURI(bsrURIConsumer, url, user, password,"QUERY 3: "+line);
					tf = System.nanoTime();
					SLAConsumerAndProviderPerformance.logDuration(" QUERY 3 ",ti, tf);
					
					ti = System.nanoTime();
					typeServiceSubtype = wsrrutility.getServiceVersionSubTipologyBybsrURI(bsrURIConsumer, url, user,password,"QUERY 4: "+line);
					tf = System.nanoTime();
					SLAConsumerAndProviderPerformance.logDuration(" QUERY 4 ",ti, tf);
					
					Boolean consumerCompatible = false;

					//15032017 fix per prendere le Application Version che prima scartava

					if (typeService != null && typeService.equals("ApplicationVersion")) {

						consumerCompatible=true;

					} else {

						if (typeServiceSubtype != null && typeService != null) {

							if (typeService.equals("SCOPEN") || typeService.equals("SCHOST")
									|| (typeService.equals("SOPEN") & typeServiceSubtype.equals("IIBPARAL"))) {
								consumerCompatible = true;
							}
						}
					}

					//consumerCompatible = true;
					
					if (consumerCompatible) {

						if (bsrURIConsumer != null && !bsrURIConsumer.contains(">>>***ERROR**>>>")) {

							sldProvider = sb.append("SLD%20-%20").append(provider).append("_").append(providerVersion) // se
									// non
									// metto
									// %20
									// da
									// 505
									.append("_").append(interfaceType).toString();
							ti = System.nanoTime();
							bsrURISLD = wsrrutility.getGenericObjectByNameAndPrimaryTypeExtended(sldProvider,
									"http://www.ibm.com/xmlns/prod/serviceregistry/profile/v6r3/GovernanceEnablementModel%23ServiceLevelDefinition",
									url, user, password,"QUERY 5: "+line);
							tf = System.nanoTime();
							SLAConsumerAndProviderPerformance.logDuration(" QUERY 5 ",ti, tf);

							if (bsrURISLD != null && !bsrURISLD.contains(">>>***ERROR**>>>")) { // error
								// if
								// sld
								// not
								// defined

								if (bind.equalsIgnoreCase("S-S")) {
									//ti = System.nanoTime();
									bsrURISLA_SV = wsrrutility.getSLAassociatedToSLDExtended(consumer, consumerVersion, 
											bsrURISLD, url, user, password,log,"QUERY 6: "+line);
									// " QUERY 6 " il logging � stato spostato nella libreria
									//tf = System.nanoTime();
									//SLAConsumerAndProviderPerformance.logDuration(" QUERY 6 ",ti, tf);

									if (bsrURISLA_SV == null) { // if null no
										// sla
										// associated to
										// sld

										sb.delete(0, sb.length());
										sb.append("SLA - ").append(consumer).append(" (").append(consumerVersion)
										.append(")");

										String applicationData = "";
										String systemData = "";
										String productionData = "";

										if (environment.trim().equals("Application"))
											applicationData = providerInvocationTs;
										if (environment.trim().equals("SystemTest"))
											systemData = providerInvocationTs;
										if (environment.trim().equals("Produzione"))
											productionData = providerInvocationTs;

										envelopeSLA = wsrrenvelopes.createServiceLevelAgreementXMLDAta(sb.toString(),
												"", applicationData, systemData, productionData, bsrURISLD);
										ti = System.nanoTime();
										bsrURISLA_SV = wsrrutility.createWSRRGenericObject(envelopeSLA, "POST", url,
												user, password,"QUERY 7: "+line);
										tf = System.nanoTime();
										SLAConsumerAndProviderPerformance.logDuration(" QUERY 7 ",ti, tf);

										if (bsrURISLA_SV != null) {

											// add SLA to consumer by
											// gep63_consume
											// relations
											ti = System.nanoTime();
											if (wsrrutility.updateRelationShip(bsrURIConsumer, "gep63_consumes",
													bsrURISLA_SV, url, user, password,"QUERY 8: "+line)) {
												tf = System.nanoTime();
												SLAConsumerAndProviderPerformance.logDuration(" QUERY 8 ",ti, tf);
												ti = System.nanoTime();
												if (!wsrrutility.changeGovernanceState(bsrURISLA_SV, transactions, url,
														user, // approved
														password,"QUERY 9: "+line)) {
													tf = System.nanoTime();
													SLAConsumerAndProviderPerformance.logDuration(" QUERY 9 ",ti, tf);
													error = true;
													log.error("record(" + recNum
															+ ") Error on changing SLA state to Inactive SLA : "
															+ bsrURISLA_SV);
												} else {//new
												tf = System.nanoTime();
												SLAConsumerAndProviderPerformance.logDuration(" QUERY 9 ",ti, tf);
												}

											} else {
												tf = System.nanoTime();
												SLAConsumerAndProviderPerformance.logDuration(" QUERY 8 ",ti, tf);
												error = true;
												log.error("record(" + recNum
														+ ") Error on updating SLA relation during binding to object (BusinessService) : "
														+ bsrURIConsumer + " with object : " + bsrURISLA_SV);
											}

										} else {
											error = true;
											log.error("record(" + recNum + ") Error on SLA creation tipology S-S");
										}
									} else {

										if (bsrURISLA_SV.contains(">>**ERROR**>>")) {
											error = true;
											log.error("record(" + recNum
													+ ") error getting SLA associate to SLD for consumer : " + consumer
													+ " sld : " + bsrURISLD + " S-S");
											log.error("error " + bsrURISLA_SV.substring(12, bsrURISLA_SV.length()));
										}
									} // bsrURISLA_SV

									if (!error) { // here acronimo

										// get acroname
										ti = System.nanoTime();
										acroName = wsrrutility.getOrganizationFromGenericObjectByNameAndVersionExtended(
												consumer, consumerVersion, url, user, password,"QUERY 10: "+line);
										tf = System.nanoTime();
										SLAConsumerAndProviderPerformance.logDuration(" QUERY 10 ",ti, tf);

										if (acroName != null && !acroName.contains(">>**ERROR**>>")) { // error
											// if
											// not
											// defined
											//ti = System.nanoTime();
											bsrURISLA_AV = wsrrutility.getSLAassociatedToSLDWithPrimaryTypeExtended(
													acroName, "00", avPrimaryType, bsrURISLD, url, user, password,log,"QUERY 11: "+line);
											
											// " QUERY 11 " il logging � stato spostato nella libreria
											//tf = System.nanoTime();
											//SLAConsumerAndProviderPerformance.logDuration(" QUERY 11 ",ti, tf);

											if (bsrURISLA_AV == null) { // if
												// null
												// no
												// sla
												// associated
												// to
												// sld
												ti = System.nanoTime();
												bsrURIApplicatioVersion = wsrrutility
														.getGenericObjectByNameAndVersionAndPrimaryTypeExtended(
																acroName, "00", avPrimaryType, url, user, password,"QUERY 12: "+line);
												tf = System.nanoTime();
												SLAConsumerAndProviderPerformance.logDuration(" QUERY 12 ",ti, tf);

												if (bsrURIApplicatioVersion != null
														&& !bsrURIApplicatioVersion.contains(">>**ERROR**>>")) {

													sb.delete(0, sb.length());
													sb.append("SLA - ").append(acroName).append(" (").append("00")
													.append(")");

													String applicationData = "";
													String systemData = "";
													String productionData = "";

													if (environment.trim().equals("Application"))

														applicationData = providerInvocationTs;
													if (environment.trim().equals("SystemTest"))
														systemData = providerInvocationTs;
													if (environment.trim().equals("Produzione"))
														productionData = providerInvocationTs;

													envelopeSLA = wsrrenvelopes.createServiceLevelAgreementXMLDAta(
															sb.toString(), "", applicationData, systemData,
															productionData, bsrURISLD);
													
													ti = System.nanoTime();
													bsrURISLA_AV = wsrrutility.createWSRRGenericObject(envelopeSLA,
															"POST", url, user, password,"QUERY 13: "+line);
													tf = System.nanoTime();
													SLAConsumerAndProviderPerformance.logDuration(" QUERY 13 ",ti, tf);

													if (bsrURISLA_AV != null) {
														ti = System.nanoTime();
														if (wsrrutility.updateRelationShip(bsrURIApplicatioVersion,
																"gep63_consumes", bsrURISLA_AV, url, user, password,"QUERY 14: "+line)) {
															tf = System.nanoTime();
															SLAConsumerAndProviderPerformance.logDuration(" QUERY 14 ",ti, tf);
															ti = System.nanoTime();
															if (wsrrutility.changeGovernanceState(bsrURISLA_AV,
																	transactions, url, user, password,"QUERY 15: "+line)) {
															   tf = System.nanoTime();
															   SLAConsumerAndProviderPerformance.logDuration(" QUERY 15 ",ti, tf);
															} else {
																tf = System.nanoTime();
																SLAConsumerAndProviderPerformance.logDuration(" QUERY 15 ",ti, tf);
																error = true;
																log.error("record(" + recNum
																		+ ") Error on changing SLA(Business Application) state to Inactive SLA : "
																		+ bsrURISLA_AV);
															}

														} else {
															tf = System.nanoTime();
															SLAConsumerAndProviderPerformance.logDuration(" QUERY 14 ",ti, tf);
															error = true;

															log.error("record(" + recNum
																	+ ") Error on updating SLA relation during binding to object (ApplicationVersion) : "
																	+ bsrURIApplicatioVersion + " with object : "
																	+ bsrURISLA_AV);
														}
													} else {
														error = true;
														log.error("record(" + recNum
																+ ") Error on SLA creation for business application");
													}
												} else {

													if (bsrURIApplicatioVersion == null) {
														error = true;
														log.error("record(" + recNum
																+ ") Error object of type application version : "
																+ acroName + " not found");

													} else {

														error = true;
														log.error("record(" + recNum
																+ ") error getting application Version : " + acroName
																+ " sld : " + bsrURISLD + " S-S");
														log.error("error " + bsrURIApplicatioVersion.substring(12,
																bsrURIApplicatioVersion.length()));
													}
												}
											} else {

												if (bsrURISLA_AV.contains(">>**ERROR**>>")) {
													error = true;
													log.error("record(" + recNum
															+ ") error getting SLA associate to SLD  for application Version : "
															+ acroName + " sld : " + bsrURISLD + " S-S");
													log.error("error "
															+ bsrURISLA_AV.substring(12, bsrURISLA_AV.length()));
												}
											}

										} else {

											if (acroName == null) {

												log.error("record(" + recNum
														+ ") No organization associated to consumer service :  "
														+ consumer);

											} else
												error = true;
											log.error("record(" + recNum + ") error getting acronimo  from consumer : "
													+ consumer + " version : " + consumerVersion);
											log.error("error " + acroName.substring(12, acroName.length()));

										}

									}

									// update date for SLA: bsrURISLA_SV and
									// bsrURISLA_AV
                                    // query 16-17-23 non loggate ma generano a secondo dell'ambiente : query 39 40 41
									
									if (!error && bsrURISLA_SV != null && !tipology.equalsIgnoreCase("DESIGNTIME")) {
										ti = System.nanoTime(); 
										error = updateDateSLA(wsrrutility, environment, bsrURISLA_SV,
												providerInvocationTs, bind, recNum, url, user, password);
										tf = System.nanoTime();
										//SLAConsumerAndProviderPerformance.logDuration(" QUERY 16 ",ti, tf);

										if (error) {

											log.error("record(" + recNum + ") Error updating Date on SLA : "
													+ bsrURISLA_SV + " environment : " + environment + " " + bind);
										}
									}

									if (!error && bsrURISLA_AV != null && !tipology.equalsIgnoreCase("DESIGNTIME")) {

										ti = System.nanoTime(); 
										error = updateDateSLA(wsrrutility, environment, bsrURISLA_AV,
												providerInvocationTs, bind, recNum, url, user, password);
										tf = System.nanoTime();
										//SLAConsumerAndProviderPerformance.logDuration(" QUERY 17 ",ti, tf);

										if (error) {

											log.error("record(" + recNum + ") Error updating Date on SLA : "
													+ bsrURISLA_AV + " environment : " + environment + " " + bind);
										}
									}

								} else { // A-S or C-S

									// new
									//ti = System.nanoTime();
									bsrURISLA_AV = wsrrutility.getSLAassociatedToSLDWithPrimaryTypeExtended(consumer,
											"00", avPrimaryType, bsrURISLD, url, user, password,log,"QUERY 18: "+line);//QUERY18
									tf = System.nanoTime();
									//SLAConsumerAndProviderPerformance.logDuration(" QUERY 18 ",ti, tf);

									if (bsrURISLA_AV == null) {
										ti = System.nanoTime();
										bsrURIApplicatioVersion = wsrrutility
												.getGenericObjectByNameAndVersionAndPrimaryTypeExtended(consumer, "00",
														avPrimaryType, url, user, password,"QUERY 19: "+line);
										tf = System.nanoTime();
										SLAConsumerAndProviderPerformance.logDuration(" QUERY 19 ",ti, tf);

										if (bsrURIApplicatioVersion != null
												&& !bsrURIApplicatioVersion.contains(">>**ERROR**>>")) { // cambiare

											sb.delete(0, sb.length());
											sb.append("SLA - ").append(consumer).append(" (").append("00").append(")");

											String applicationData = "";
											String systemData = "";
											String productionData = "";

											if (environment.trim().equalsIgnoreCase("APPLICATION"))
												applicationData = providerInvocationTs;
											if (environment.trim().equalsIgnoreCase("SYSTEM"))
												systemData = providerInvocationTs;
											if (environment.trim().equalsIgnoreCase("PRODUZIONE"))
												productionData = providerInvocationTs;

											envelopeSLA = wsrrenvelopes.createServiceLevelAgreementXMLDAta(
													sb.toString(), "", applicationData, systemData, productionData,
													bsrURISLD);
											ti = System.nanoTime();
											bsrURISLA_AV = wsrrutility.createWSRRGenericObject(envelopeSLA, "POST", url,
													user, password,"QUERY 20: "+line);
											tf = System.nanoTime();
											SLAConsumerAndProviderPerformance.logDuration(" QUERY 20 ",ti, tf);

											if (bsrURISLA_AV != null) {
												ti = System.nanoTime();
												if (wsrrutility.updateRelationShip(bsrURIApplicatioVersion,
														"gep63_consumes", bsrURISLA_AV, url, user, password,"QUERY 21: "+line)) {
													tf = System.nanoTime();
													SLAConsumerAndProviderPerformance.logDuration(" QUERY 21 ",ti, tf);
													ti = System.nanoTime();
													if (wsrrutility.changeGovernanceState(bsrURISLA_AV, transactions,
															url, user, password,"QUERY 22: "+line)) {
														tf = System.nanoTime();
														SLAConsumerAndProviderPerformance.logDuration(" QUERY 22 ",ti, tf);
													} else {
														tf = System.nanoTime();
														SLAConsumerAndProviderPerformance.logDuration(" QUERY 22 ",ti, tf);
														error = true;
														log.error("record(" + recNum
																+ ") Error on changing SLA(Business Application) state to Inactive ");
													}
												} else {
													tf = System.nanoTime();
													SLAConsumerAndProviderPerformance.logDuration(" QUERY 21 ",ti, tf);
													error = true;
													log.error("record(" + recNum
															+ ") Error on updating SLA relation during binding to object (ApplicationVersion) : "
															+ bsrURIApplicatioVersion + " with object : "
															+ bsrURISLA_AV);
												}

											} else {
												log.error("record(" + recNum
														+ ") Error on SLA creation for business application tipology "+bind);
											}
										} else {

											if (bsrURIApplicatioVersion == null) {
												error = true;
												log.error("record(" + recNum
														+ ") Error object of type application version : " + consumer
														+ " not found");

											} else {
												error = true;
												log.error("record(" + recNum
														+ ") Error on query for object application version : "
														+ consumer + " "+bind);
												log.error("error " + bsrURIApplicatioVersion.substring(12,
														bsrURIApplicatioVersion.length()));

											}

										}
									}

									else {

										if (bsrURISLA_AV.contains(">>**ERROR**>>")) {
											error = true;
											log.error("record(" + recNum
													+ ") error getting SLA associate to SLD for consumer : " + consumer
													+ " sld : " + bsrURISLD + " "+bind);
											log.error("error " + bsrURISLA_AV.substring(12, bsrURISLA_AV.length()));
										}
									}

									// update date for SLA: bsrURISLA_AV
									if (!error && bsrURISLA_AV != null) {
										ti = System.nanoTime();
										error = updateDateSLA(wsrrutility, environment, bsrURISLA_AV,
												providerInvocationTs, bind, recNum, url, user, password);
										tf = System.nanoTime();
										//SLAConsumerAndProviderPerformance.logDuration(" QUERY 23 ",ti, tf);
										if (error) {

											log.error("record(" + recNum + ") Error updating Date on SLA : "
													+ bsrURISLA_AV + " environment : " + environment + " " + bind);
										}

									}
								}

								boolean notError = false;

								String[] SLAactivateTransaction = {
								"http://www.ibm.com/xmlns/prod/serviceregistry/lifecycle/v6r3/LifecycleDefinition%23ActivateSLA" };

								if ((bsrURISLA_SV != null || bsrURISLA_AV != null) && !error) {

									if (tipology.equalsIgnoreCase("RUNTIME")) {

										if (bsrURISLA_SV != null) {
											ti = System.nanoTime();
											notError = wsrrutility.updateSinglePropertyJSONFormat(bsrURISLA_SV,
													"gpx63_RUNTIME", "Y", url, user, password,"QUERY 24: "+line);
											tf = System.nanoTime();
											SLAConsumerAndProviderPerformance.logDuration(" QUERY 24 ",ti, tf);

											if (notError) {

												boolean changeGovernancestate = true;
												ti = System.nanoTime();
												String slaState = wsrrutility.checkClassification(bsrURISLA_SV,
														activeStateSLA, url, user, password,"QUERY 25: "+line);
												tf = System.nanoTime();
												SLAConsumerAndProviderPerformance.logDuration(" QUERY 25 ",ti, tf);

												if (slaState == null) {
													ti = System.nanoTime();
													slaState = wsrrutility.checkClassification(bsrURISLA_SV,
															inactiveStateSLA, url, user, password,"QUERY 26: "+line);
													tf = System.nanoTime();
													SLAConsumerAndProviderPerformance.logDuration(" QUERY 26 ",ti, tf);

													if (slaState == null) {

														notError = false;
														log.error("record(" + recNum + ") Error SLA : " + bsrURISLA_SV
																+ " is not is state : " + inactiveStateSLA + " or "
																+ activeStateSLA);
													} else {

														if (slaState != null && slaState.contains(">>**ERROR**>>")) {
															notError = false;
															log.error("record(" + recNum
																	+ ") Error checkin governance state for object SLA  : "
																	+ bsrURISLA_AV);
															log.error("record(" + recNum + ") Error : "
																	+ slaState.substring(12, slaState.length()));
														}
													}

												} else {

													if (slaState != null && slaState.contains(">>**ERROR**>>")) {

														notError = false;
														log.error("record(" + recNum
																+ ") Error checkin governance state for object SLA  : "
																+ bsrURISLA_AV);
														log.error("record(" + recNum + ") Error : "
																+ slaState.substring(12, slaState.length()));

													} else
														changeGovernancestate = false;

												}

												if (notError && changeGovernancestate) {// was!
													ti = System.nanoTime();
													if (!wsrrutility.changeGovernanceState(bsrURISLA_SV,
															SLAactivateTransaction, url, user, password,"QUERY 27: "+line)) {
														tf = System.nanoTime();
														SLAConsumerAndProviderPerformance.logDuration(" QUERY 27 ",ti, tf);
														notError = false;

														log.error("record(" + recNum
																+ ") Error during change state from INACTIVE to ACTIVE for SLA bsrURI : "
																+ bsrURISLA_SV
																+ " check if SLD governance state is Approved");
													}else {
														//new
														tf = System.nanoTime();
														SLAConsumerAndProviderPerformance.logDuration(" QUERY 27 ",ti, tf);
													}

												}

											} else
												log.error("record(" + recNum + ") Error updating property gpx63_"
														+ tipology + " SLA bsrURI : " + bsrURISLA_SV);
										}

										if (bsrURISLA_AV != null) {
											ti = System.nanoTime();
											notError = wsrrutility.updateSinglePropertyJSONFormat(bsrURISLA_AV,
													"gpx63_RUNTIME", "Y", url, user, password,"QUERY 28: "+line);
											tf = System.nanoTime();
											SLAConsumerAndProviderPerformance.logDuration(" QUERY 28 ",ti, tf);

											if (notError) {

												boolean changeGovernancestate = true;
												ti = System.nanoTime();
												String slaState = wsrrutility.checkClassification(bsrURISLA_AV,
														activeStateSLA, url, user, password,"QUERY 29: "+line);
												tf = System.nanoTime();
												SLAConsumerAndProviderPerformance.logDuration(" QUERY 29 ",ti, tf);
												
												if (slaState == null) {
													ti = System.nanoTime();
													slaState = wsrrutility.checkClassification(bsrURISLA_AV,
															inactiveStateSLA, url, user, password,"QUERY 30: "+line);
													tf = System.nanoTime();
													SLAConsumerAndProviderPerformance.logDuration(" QUERY 30 ",ti, tf);

													if (slaState == null) {

														notError = false;
														log.error("record(" + recNum + ") Error SLA : " + bsrURISLA_AV
																+ " is not is state : " + inactiveStateSLA + " or "
																+ activeStateSLA);
													} else {

														if (slaState != null && slaState.contains(">>**ERROR**>>")) {
															notError = false;
															log.error("record(" + recNum
																	+ ") Error checkin governance state for object SLA  : "
																	+ bsrURISLA_AV);
															log.error("record(" + recNum + ") Error : "
																	+ slaState.substring(12, slaState.length()));
														}
													}

												} else {

													if (slaState != null && slaState.contains(">>**ERROR**>>")) {

														notError = false;
														log.error("record(" + recNum
																+ ") Error checkin governance state for object SLA  : "
																+ bsrURISLA_AV);
														log.error("record(" + recNum + ") Error : "
																+ slaState.substring(12, slaState.length()));

													} else
														changeGovernancestate = false;

												}

												if (notError && changeGovernancestate) {
													ti = System.nanoTime();
													if (!wsrrutility.changeGovernanceState(bsrURISLA_AV,
															SLAactivateTransaction, url, user, password,"QUERY 31: "+line)) {
														tf = System.nanoTime();
														SLAConsumerAndProviderPerformance.logDuration(" QUERY 31 ",ti, tf);
														notError = false;

														log.error("record(" + recNum
																+ ") Error during change state from INACTIVE to ACTIVE for SLA bsrURI : "
																+ bsrURISLA_AV
																+ " check if SLD governance state is Approved");

													} else {
														//new
														tf = System.nanoTime();
														SLAConsumerAndProviderPerformance.logDuration(" QUERY 31 ",ti, tf);
													}
												}
											} else
												log.error("record(" + recNum + ") Error updating property gpx63_"
														+ tipology + " SLA bsrURI : " + bsrURISLA_AV);

										}

									} else { // DESIGNTIME

										if (bsrURISLA_SV != null) {
											ti = System.nanoTime();
											notError = wsrrutility.updateSinglePropertyJSONFormat(bsrURISLA_SV,
													"gpx63_DESIGNTIME", "Y", url, user, password,"QUERY 32: "+line);
											tf = System.nanoTime();
											SLAConsumerAndProviderPerformance.logDuration(" QUERY 32 ",ti, tf);

											if (!notError)
												log.error("record(" + recNum + ") Error updating property gpx63_"
														+ tipology + " SLA bsrURI : " + bsrURISLA_SV);
										}

										if (bsrURISLA_AV != null) {
											ti = System.nanoTime();
											notError = wsrrutility.updateSinglePropertyJSONFormat(bsrURISLA_AV,
													"gpx63_DESIGNTIME", "Y", url, user, password,"QUERY 33: "+line);
											tf = System.nanoTime();
											SLAConsumerAndProviderPerformance.logDuration(" QUERY 33 ",ti, tf);

											if (!notError)
												log.error("record(" + recNum + ") Error updating property gpx63_"
														+ tipology + " SLA bsrURI : " + bsrURISLA_AV);
										}

									}

									if (tipology.equalsIgnoreCase("RUNTIME")) { // 22112016
										// se
										// DESIGNTIME
										// non
										// bisogna
										// aggiorare
										// le
										// date

										// update date service activation
										updateDate(wsrrenvelopes, wsrrutility, environment, tipology, consumer,
												provider, consumerVersion, providerVersion, interfaceType, bind,
												providerInvocationTs, sldProvider, filename, recNum, url, user,
												password);
									}

								}

								else {

									if (bsrURISLA_SV == null && bsrURISLA_AV == null) {

										// log.error("record(" + recNum + ")
										// Error Service SLA not found
										// bsrURISLA_AV and bsrURISLA_AV");
									}

								}
							} else { // sld not found or in error

								if (bsrURISLD == null) {

									error = true;
									log.error("record(" + recNum + ") Error SLD not found : " + bsrURISLD);

								} else {

									error = true;
									log.error("record(" + recNum + ") error getting SLD  : " + sldProvider);
									log.error("error " + bsrURISLD.substring(12, bsrURISLD.length()));
								}
							}

						} else {

							if (bsrURIConsumer == null) {

								//24032018
								updateDate(wsrrenvelopes, wsrrutility, environment, tipology, consumer, provider, consumerVersion,
										providerVersion, interfaceType, bind, providerInvocationTs, sldProvider, filename, recNum,
										url, user, password);
								error = true;
								log.error(" record(" + recNum + ")  Error Consumer not found(*) " + consumer.trim()
								+ " version :  " + consumerVersion.trim());
								

							} else {

								error = true;
								log.error("record(" + recNum + ") error getting Consumer : " + consumer.trim()
								+ " version : " + consumerVersion);
								log.error("error " + bsrURIConsumer.substring(12, bsrURIConsumer.length()));
							}

						}
					} else {
						error = true;
						log.error("record(" + recNum
								+ ") error Consumer Service is not acceptable [ acceptable  are : SCOPEN/SCHOST/SOPEN(IIPARAL)] "
								+ consumer.trim() + " version : " + consumerVersion + " - type : " + typeService
								+ " - subtype : " + typeServiceSubtype);
					}

				} else // ONLYDATE

					updateDate(wsrrenvelopes, wsrrutility, environment, tipology, consumer, provider, consumerVersion,
							providerVersion, interfaceType, bind, providerInvocationTs, sldProvider, filename, recNum,
							url, user, password);

			} else {

				if (bsrURIProvider == null) {
					error = true;
					log.error(" record(" + recNum + ")  Error Provider not found " + provider.trim() + " version "
							+ providerVersion.trim());

				} else {
					error = true;
					log.error(" record(" + recNum + ")  Error on query  " + provider.trim() + " version "
							+ providerVersion.trim() + " " + bsrURIProvider.substring(12, bsrURIProvider.length()));

				}

			}

		} catch (Exception ex) {
			log.error(" record(" + recNum + ")  runtimeError captured");
			log.error(" record(" + recNum + ") " + ex.getMessage());
			//throw new Exception(ex); tolta l'elaborazione deve proseguire 24032017
		}

	}//

	public boolean updateDate(WSRREnvelopes wsrrenvelopes, WSRRUtilityPerformance wsrrutility, String environment, String tipology,
			String consumer, String provider, String consumerVersion, String providerVersion, String interfaceType,
			String bind, String providerInvocationTs, String sldProvider, String filename, int recNum, String url,
			String user, String password) {

		boolean result = true;
		long ti=0L;
		long tf=0L;

		log.info("record(" + recNum + ") get endpoint info : provider - " + provider + " version - " + providerVersion
				+ " interface type - " + interfaceType);
		
        //11052017 utilizzo metodo che supporta la specializzazione
		ti = System.nanoTime();
		String endpointData = wsrrutility.getEndpointInfo(provider, providerVersion, interfaceType, environment,"", url,
				user, password,"QUERY 34: "+line);
		tf = System.nanoTime();
		SLAConsumerAndProviderPerformance.logDuration(" QUERY 34 ",ti, tf);

		String bsrURIEndpoint = null;
		String firstUsedTs = null;
		JSONArray jsa = null;

		String dataPrimoUtilizzo = null;
		String dataUltimoUtilizzo = null;

		if (endpointData != null) {

			jsa = new JSONArray(endpointData);

			bsrURIEndpoint = WSRRUtilityPerformance.getObjectValueFromJSONArrayData((JSONArray) jsa.get(0), "bsrURI");

			if (interfaceType.equalsIgnoreCase("MQ")) {

				dataPrimoUtilizzo = "sm63_DATA_PRIMO_UTILIZZO_MQM";
				dataUltimoUtilizzo = "sm63_DATA_ULTIMO_UTILIZZO_MQM";

				jsa = null;
				ti = System.nanoTime();
				jsa = new JSONArray(wsrrutility.getManualMQEndpointInfo(bsrURIEndpoint, url, user, password,"QUERY 35: "+line));
				tf = System.nanoTime();
				SLAConsumerAndProviderPerformance.logDuration(" QUERY 35 ",ti, tf);

				if (jsa != null && jsa.length() != 0) {

					bsrURIEndpoint = WSRRUtilityPerformance.getObjectValueFromJSONArrayData((JSONArray) jsa.get(0), "bsrURI");

					firstUsedTs = WSRRUtilityPerformance.getObjectValueFromJSONArrayData((JSONArray) jsa.get(0),
							dataPrimoUtilizzo);

				} else {

					log.info("record(" + recNum + ") not found manual MQ endpoint associated to MQ endpoint : "
							+ bsrURIEndpoint);
				}

			} else {

				dataPrimoUtilizzo = "sm63_DATA_PRIMO_UTILIZZO";
				dataUltimoUtilizzo = "sm63_DATA_ULTIMO_UTILIZZO";

				firstUsedTs = WSRRUtilityPerformance.getObjectValueFromJSONArrayData((JSONArray) jsa.get(0), dataPrimoUtilizzo);
			}

			log.info("record(" + recNum + ") Get " + dataPrimoUtilizzo + " from endpoint :  " + bsrURIEndpoint
					+ " value is : " + firstUsedTs);

			try {

				if (firstUsedTs == null || firstUsedTs.length() == 0) {
					ti = System.nanoTime();
					result = wsrrutility.updateSinglePropertyJSONFormat(bsrURIEndpoint, dataPrimoUtilizzo,
							providerInvocationTs, url, user, password,"QUERY 36: "+line);
					tf = System.nanoTime();
					SLAConsumerAndProviderPerformance.logDuration(" QUERY 36 ",ti, tf);

					if (result) {

						log.info("record(" + recNum + ") Updated " + dataPrimoUtilizzo + " for endpoint : "
								+ bsrURIEndpoint + " to : " + providerInvocationTs);
						ti = System.nanoTime();
						result = wsrrutility.updateSinglePropertyJSONFormat(bsrURIEndpoint, dataUltimoUtilizzo,
								providerInvocationTs, url, user, password,"QUERY 37: "+line);
						tf = System.nanoTime();
						SLAConsumerAndProviderPerformance.logDuration(" QUERY 37 ",ti, tf);

						if (!result) {

							log.error("record(" + recNum + ")  Error on updating " + dataUltimoUtilizzo
									+ " for endpoint : " + bsrURIEndpoint);
						} else
							log.info("record(" + recNum + ") Updated " + dataUltimoUtilizzo + " for endpoint : "
									+ bsrURIEndpoint + " to : " + providerInvocationTs);

					} else
						log.error("record(" + recNum + ")  Error on updating " + dataPrimoUtilizzo + "for endpoint : "
								+ bsrURIEndpoint);

				} else {
					ti = System.nanoTime();
					if (!wsrrutility.updateSinglePropertyJSONFormat(bsrURIEndpoint, dataUltimoUtilizzo,
							providerInvocationTs, url, user, password,"QUERY 38: "+line)) {
						tf = System.nanoTime();
						SLAConsumerAndProviderPerformance.logDuration(" QUERY 38 ",ti, tf);
						result = false;
						log.error("record(" + recNum + ")  Error on updating " + dataUltimoUtilizzo + " for endpoint : "
								+ bsrURIEndpoint);

					} else
						tf = System.nanoTime();
						SLAConsumerAndProviderPerformance.logDuration(" QUERY 38 ",ti, tf);
						log.info("record(" + recNum + ") Updated " + dataUltimoUtilizzo + " for endpoint : "
								+ bsrURIEndpoint + " to : " + providerInvocationTs);
				}

			} catch (Exception ex) {

				log.error("record(" + recNum + ") Runtime Error" + ex.getMessage());
				result = false;
			}

		} else {
			result = false;
			log.info("record(" + recNum + ")  Not found provider endpoint  - provider : " + provider + " version : "
					+ providerVersion + " interface type : " + interfaceType);

		}

		return result;

	}

	// chiamata da all
	public boolean updateDateSLA(WSRRUtilityPerformance wsrrutility, String environment, String bsrURISLA,
			String providerInvocationTs, String bind, int recNum, String url, String user, String password) {

		boolean result = true;
		Long ti=0L;
		Long tf=0L;

		// mettere valore precedente

		if (environment.trim().equals("Application")) {
			ti = System.nanoTime();
			result = wsrrutility.updateSinglePropertyJSONFormat(bsrURISLA, "gpx63_DATA_ULTIMO_UTILIZZO_LEGAME_APPL",
					providerInvocationTs, url, user, password,"QUERY 39: "+line);
			tf = System.nanoTime();
			SLAConsumerAndProviderPerformance.logDuration(" QUERY 39 ",ti, tf);
			if (result) {
				log.info("record(" + recNum + ")  Updated SLA gpx63_DATA_ULTIMO_UTILIZZO_LEGAME_APPL for SLA : "
						+ bsrURISLA + " type : " + bind);

				result = false;

			} else {
				log.error("record(" + recNum + ")  Error Updated SLA gpx63_DATA_ULTIMO_UTILIZZO_LEGAME_APPL for SLA : "
						+ bsrURISLA + " type : " + bind);
			}

		}
		if (environment.trim().equals("SystemTest")) {
			ti = System.nanoTime();
			result = wsrrutility.updateSinglePropertyJSONFormat(bsrURISLA, "gpx63_DATA_ULTIMO_UTILIZZO_LEGAME_SYST",
					providerInvocationTs, url, user, password,"QUERY 40: "+line);
			tf = System.nanoTime();
			SLAConsumerAndProviderPerformance.logDuration(" QUERY 40 ",ti, tf);

			if (result) {
				log.info("record(" + recNum + ")  Updated SLA gpx63_DATA_ULTIMO_UTILIZZO_LEGAME_SYST for SLA : "
						+ bsrURISLA + " type : " + bind);

				result = false;

			} else {
				log.error("record(" + recNum + ")  Error Updated SLA gpx63_DATA_ULTIMO_UTILIZZO_LEGAME_SYST for SLA : "
						+ bsrURISLA + " type : " + bind);
			}

		}
		if (environment.trim().equals("Produzione")) {
			ti = System.nanoTime();
			result = wsrrutility.updateSinglePropertyJSONFormat(bsrURISLA, "gpx63_DATA_ULTIMO_UTILIZZO_LEGAME_PROD",
					providerInvocationTs, url, user, password,"QUERY 41: "+line);
			tf = System.nanoTime();
			SLAConsumerAndProviderPerformance.logDuration(" QUERY 41 ",ti, tf);
			if (result) {
				log.info("record(" + recNum + ")  Updated SLA gpx63_DATA_ULTIMO_UTILIZZO_LEGAME_PROD for SLA : "
						+ bsrURISLA + " type : " + bind);

				result = false;

			} else {
				log.error("record(" + recNum + ")  Error Updated SLA gpx63_DATA_ULTIMO_UTILIZZO_LEGAME_PROD for SLA : "
						+ bsrURISLA + " type : " + bind);
			}
		}

		return result;

	}

	public boolean checkInputDataHeader(String environment, String tipology) {

		boolean dataOk = true;

		if (environment == null || tipology == null)
			dataOk = false;

		if (dataOk) {

			if (!tipology.equals("RUNTIME") && !tipology.equals("DESIGNTIME"))
				dataOk = false;

			if (!environment.equalsIgnoreCase("Application") && !environment.equalsIgnoreCase("System")
					&& !environment.equalsIgnoreCase("Produzione"))
				dataOk = false;

		}

		return dataOk;
	}

	public boolean checkInputData(String consumer, String provider, String consumerVersion, String providerVersion,
			String interfaceType, String bind, String providerInvocationTs) {

		boolean dataOk = true;

		if (consumer == null || provider == null || consumerVersion == null || providerVersion == null
				|| interfaceType == null || bind == null || providerInvocationTs == null)
			dataOk = false;

		Date date = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			date = sdf.parse(providerInvocationTs);

			String[] datesplit = providerInvocationTs.split("-", 0);
			String years = datesplit[0];

			if (!providerInvocationTs.equals(sdf.format(date)) || (years.length() > 4 || years.length() < 4))
				dataOk = false;

		} catch (ParseException ex) {
			dataOk = false;
		}

		if (dataOk) {

			if (!interfaceType.equals("REST") && !interfaceType.equals("SOAP") && !interfaceType.equals("CICS")
					&& !interfaceType.equals("MQ") && !interfaceType.equals("CALLABLE") && !interfaceType.equals("WOLA") && !interfaceType.equals("ZRES") && !interfaceType.equals("SHC"))
				dataOk = false;

			if (!bind.equals("S-S") && !bind.equals("A-S") && !bind.equals("C-S")) //060618
				dataOk = false;
		}
					
		return dataOk;
	}

	public boolean checkInputData4DesignTime(String consumer, String provider, String consumerVersion,
			String providerVersion, String interfaceType, String bind) {

		boolean dataOk = true;

		if (consumer == null || provider == null || consumerVersion == null || providerVersion == null
				|| interfaceType == null || bind == null)
			dataOk = false;

		if (dataOk) {

			if (!interfaceType.equals("REST") && !interfaceType.equals("SOAP") && !interfaceType.equals("CICS")
					&& !interfaceType.equals("MQ") && !interfaceType.equals("CALLABLE") && !interfaceType.equals("WOLA") && !interfaceType.equals("ZRES") && !interfaceType.equals("SHC"))
				dataOk = false;

			if (!bind.equals("S-S") && !bind.equals("A-S") && !bind.equals("C-S")) //060618
				dataOk = false;
		}
		
		return dataOk;
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
	private static void logDuration(String target,double ti,double tf) {
		if (performanceDataLog) log.info("-------Performance Data---->["+target+"] duration (ms) " + (tf-ti)/1e6);
	}
	
}