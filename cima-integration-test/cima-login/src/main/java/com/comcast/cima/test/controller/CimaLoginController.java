package com.comcast.cima.test.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.comcast.test.citf.common.dataProvider.cima.IDataProviderEnums;
import com.comcast.test.citf.common.util.ICommonConstants;
import com.comcast.test.citf.common.util.MiscUtility;
import com.comcast.test.citf.common.util.cima.ICimaCommonConstants;
import com.comcast.test.citf.core.cache.ICitfCache;
import com.comcast.test.citf.core.controller.AbstractExecutionController;
import com.comcast.test.citf.core.init.cima.CoreCacheInitializer;
import com.comcast.test.citf.core.util.ExecutionCompletionChecker;

/**
 * This is main class to start test execution in cima-login.
 * 
 * @author Abhijit Rej (arej001c)
 * @since June 2015
 */
public class CimaLoginController extends AbstractExecutionController {

	private static final Logger LOGGER = LoggerFactory.getLogger(CimaLoginController.class);

	/**
     * To initialize and perform pre test execution setup. eg. initializing Spring contexts, initializing cache and initializing data.
     * 
	 * @param environment
	 *        	The Name of environment where test suite to execute (eg. QA or STAGE or PROD)
	 * @param domain
	 *        	Name of the domain under which test to execute 
	 * @param uiexecEnv
	 *        	Execution environment as LOCAL or REMOTE
	 * 
	 * <br> Note that if uiexecEnv is selected as REMOTE then the computer need Sauce tunnel enabled. </br>
	 * 
	 * @throws Exception
	 * 
	 */
	private static void initialize(String environment, String domain, String uiexecEnv) throws Exception {
		//System param initialization
		String tempDirectory = MiscUtility.setEnvironmentVariables(ICimaCommonConstants.LOG_FILE_NAME_CIMA_LOGIN);
		LOGGER.info("######### Temporary file path: {}", tempDirectory);
		Thread.currentThread().setName("Cima-Login-Main");

		//Context initialization
		CimaLoginContextInitializer.initializeContext();
		
		//Data initialization (for with LOCAL mode)
		loadData();

		//Cache Initialization
		Map<String, String> commonStringCacheParams = new HashMap<>();
		commonStringCacheParams.put(ICimaCommonConstants.STRING_CACHE_KEY_TEST_ENVIRONMENT, environment);
		commonStringCacheParams.put(ICimaCommonConstants.STRING_CACHE_KEY_TEST_DOMAIN, domain);

		if (uiexecEnv != null) {
			commonStringCacheParams.put(ICimaCommonConstants.STRING_CACHE_KEY_UI_TEST_EXEC_ENV, uiexecEnv);
		}
		((CoreCacheInitializer)CimaLoginContextInitializer.getBean(ICimaCommonConstants.SPRING_BEAN_CACHE_INITIALIZER)).initialize(commonStringCacheParams);

		LOGGER.info("All initializations done");
	}

	/**
     * 
     * Method is the starting point of CITF execution in LOGIN. It calls test execution , track execution status using MQ listener and also perform  post execution cleanup activities.
	 
	 * @param args:: [0] : environment
	 *                     The Name of environment where test suite to execute (eg. QA or STAGE or PROD)
	 *               [1] : test category
	 *                     The category of test (SMOKE or INTEGRATION)
	 *               [2] : test Type
	 *                     Type of Test (UI or API)
	 *               [3] : domain
	 *                     The name of domain under which test suite is going to execute
	 *               [4] : UI execution environment
	 *                     Execution environment as LOCAL or REMOTE
	 *               The input arguments are not case-sensitive.
	 */
	public static void main(String[] args) {
		boolean isDestroyerCalled = false;

		try {

			if (args == null || args.length < 4) {
				throw new Exception(ICommonConstants.EXCEPTION_INVALID_INPUT);
			}

			final String environment = isInEnvironment(args[0]);
			if (environment == null) {
				throw new Exception("Input Environment(1st argument) is invalid!");
			}

			final String testCategory = isInTestCategory(args[1]);
			if (testCategory == null) {
				throw new Exception("Input Test Category(2nd argument) is invalid!");
			}

			final String testType = isInTestType(args[2]);
			if (testType == null) {
				throw new Exception("Input Test Type(3rd argument) is invalid!");
			}

			final String domain = isInDomain(args[3]);
			if (domain == null) {
				throw new Exception("Input Domain(4th argument) is invalid!");
			}

			final String uiExecEnv = (args.length==5 && args[4]!=null)
					? isInUIExecutionEnv(args[4])
					: ICommonConstants.ENVIRONMENT_UI_REMOTE;
			if (uiExecEnv == null) {
				throw new Exception("Input UI execution environment(5th argument) is invalid!");
			}

			LOGGER.info("Starting execution with environment: {}, testCategory: {}, testType: {}, domain: {} and UI test execution env: {}",
							environment, testCategory, testType,
							domain, uiExecEnv);
			initialize(environment, domain, uiExecEnv);

			ICitfCache configCache = (ICitfCache) CimaLoginContextInitializer.getBean(ICimaCommonConstants.CACHE_CONFIGURARTION_PARAMS);

			final int noOfAsyncExecutions = execute(testCategory, testType, configCache);

			if (noOfAsyncExecutions <= 0) {
				throw new Exception("System error. Engine is not working properly may be due some invalid data!!!");
			}

			long totalExecTime = Long.parseLong(configCache.getString(IDataProviderEnums.ConfigurationPropKeys.MAX_CITF_EXECUTION_TIME.getValue(), ICimaCommonConstants.ENVIRONMENT_ALL));

			ExecutionCompletionChecker executionChecker = (ExecutionCompletionChecker) CimaLoginContextInitializer.getBean("execChecker");
			boolean isCompleted = executionChecker.isExecutionCompleted(noOfAsyncExecutions, totalExecTime);
			if (isCompleted) {
				LOGGER.info("All test case executions are over.");
				destroy();
				isDestroyerCalled = true;
			}

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Error occurred while executing controller", e);
		} finally {
			try {
				if (!isDestroyerCalled) {
					destroy();
				}
			} catch (Exception ignore) {
			}
		}
	}
}
