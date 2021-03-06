package base;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import base.failures.TestCaseFailure;
import base.tools.DynamicCheck;
import base.xml.XmlDynamicData;


/**
 * Generic page test case.
 * All test cases will extend this.
 * 
 * @author dan.rusu
 *
 */
/**
 * @author dan.rusu
 *
 */
/**
 * @author dan.rusu
 *
 */
abstract public class TestCase implements Runnable, TestCaseScenario{	
	public Logger logger;
	public WebDriver driver = null;
	public static WebDriver currentDriver = null;
	
	private boolean internalTest;
	private String startWindowHandle;

	private DynamicCheck dc;
	private Map<String, String> testCaseAttributes;



	/**
	 * Constructor 
	 */
	public TestCase() {		
		this.internalTest = false;
		logger = Logger.getLogger();
		dc = new DynamicCheck(logger);
		this.driver = Driver.driver;
	}

	

	/**
	 * Constructor
	 */
	public TestCase(WebDriver driver) {
		this.internalTest = true;
		logger = Logger.getLogger();
		this.driver = driver;
		dc = new DynamicCheck(logger);
	}

	
	
	/**
	 * Close all opened windows; kill driver.   
	 * 
	 */
	public void quit(){
		try {
			logger.log("Close driver");
			if (driver != null){
				driver.quit();
				driver = null;
			}
		}
		catch (Exception e){
			logger.log(e.getMessage());
		}
	}



	/**
	 * Get the test attributes.
	 * 
	 * @return - a map of test's attributes
	 */
	public Map<String, String> getTestCaseAttributes() {
		return testCaseAttributes;
	}



	// Get an instance for dynamic waits 
	public DynamicCheck getDynamicCheck() {
		return dc;
	}


	
	/**
	 * Wait until page titles becomes the expected one.
	 * 
	 * @param expectedTitle - expected page title
	 * @param totalMilisTimeout - total timeout
	 * @param stepTimeout - timeout step
	 * @return - true is the title matches the expected title
	 */
	public boolean checkPageTitle(String expectedTitle, 
			long totalMilisTimeout,
			long stepTimeout){

		return dc.waitUntilFunctionReturnsExpectedValue(
				totalMilisTimeout, 
				stepTimeout, 
				object -> ((WebDriver)object).getTitle(), 
				driver, 
				expectedTitle);
	}



	
	/**
	 * Wait until page URL becomes the expected one.
	 * 
	 * @param expectedUrl - expected page URL
	 * @param totalMilisTimeout - total timeout
	 * @param stepTimeout - timeout step
	 * @return - true is the pages's URL matches the expected URL
	 */
	public boolean checkPageUrl(String expectedUrl, 
			long totalMilisTimeout,
			long stepTimeout){

		return dc.waitUntilFunctionReturnsExpectedValue(
				totalMilisTimeout, 
				stepTimeout, 
				object -> ((WebDriver)object).getCurrentUrl(), 
				driver, 
				expectedUrl);
	}


	
	/**
	 * Check for alert presence.
	 * 
	 * @return - true if an alert is present
	 */
	public boolean isAlertPresent(){
		boolean foundAlert = false;
		// check for alert presence with no timeout (0 seconds)
		WebDriverWait wait = new WebDriverWait(driver, 0);
		try {
			wait.until(ExpectedConditions.alertIsPresent());
			foundAlert = true;
		} catch (TimeoutException eTO) {
			foundAlert = false;
		}
		return foundAlert;
	}

	

	/**
	 * Set the test's attributes.
	 * 
	 * @param testAttributes - test's attributes map 
	 */
	public void setTestCaseAttributes(Map<String, String> testAttributes) {
		this.testCaseAttributes = testAttributes;
		dynamicEval(testAttributes);
	}


	
	public void addAttribute(String name, String value){
		this.testCaseAttributes.put(name, value);
	}

	
	
	public String getStartWindowHandle() {
		return this.startWindowHandle;
	}

	

	public boolean isInternalTest() {
		return internalTest;
	}



	/**
	 * @param test
	 * @param testAttributes
	 */
	public void runInternalTestCase(TestCase test,
			Map<String, String> testAttributes){

		test.setTestCaseAttributes(testAttributes);
		test.run();
	}



	/**
	 * Set test cases' first window handle.
	 * 
	 * @param startWindowHandle - fist window opened
	 */
	public void setStartWindowHandle(String startWindowHandle) {
		this.startWindowHandle = startWindowHandle;
	}
	

	
	/**
	 * Dynamic evaluation of the XML attributes.
	 * - save data
	 * - evaluate global variables
	 * 
	 * @param testCaseAttributes - test case (attributeName, attributeValus) map
	 */
	public void dynamicEval(Map<String, String> testCaseAttributes) {

		// evaluate "save" first (so saved values can be used at all the other attributes)
		String save = evalAttribute("save");
		if (save != null){
			saveAll(save);
		}

		// evaluate all other attributes but "save"
		testCaseAttributes.keySet().forEach(
				key -> {
					if (! key.equals("save")){
						final String value = TestConfig.nullToEmptyString(testCaseAttributes.get(key));
						final String newValue = XmlDynamicData.getDynamicValue(
								XmlDynamicData.getSavedData(), value);

						if ( ! value.equals(newValue) ){
							testCaseAttributes.replace(
									key, newValue);
							logger.log("Attribute replaced: " + key + "=" + newValue);
						}
					}
				});
	}



	/**
	 * Save String variable into memory so that 
	 * they can be used for following test cases. 
	 * 
	 * @param saveString - a list of semicolon separated saveName=saveValue pairs.
	 */
	public void saveAll(String saveString) {
		if (! saveString.isEmpty()){

			List<String> entries = Arrays.asList(saveString.split(";"));
			entries.forEach(
					entry -> {
						final String savedKey =  entry.split("=")[0];
						final String savedValue =  entry.split("=")[1];
						XmlDynamicData.saveData(savedKey, 
								XmlDynamicData.getDynamicValue(XmlDynamicData.getSavedData(),savedValue));

					});

			String saved = XmlDynamicData.getSavedData().toString(); 
			testCaseAttributes.put("save", saved);
			logger.log("Saved data: save=" + saved);
		}
	}

	

	/**
	 * Save test case results (field values) into memory 
	 * at the end of a test case so that they can be reported 
	 * or used by the following test cases.
	 * 
	 * Fields that can be saved will be listed in the 
	 * getTestScenario method, Results section.
	 * 
	 * Use the "saveResult" attribute for this.
	 * 
	 */
	public void saveResults(){
		String saveString = evalAttribute("saveResult");
		
		if (!saveString.isEmpty()){
			List<String> entries = Arrays.asList(saveString.split(";"));
			entries.forEach(
					entry -> {
						final String savedKey =  entry.split("=")[0];
						final String fieldName =  entry.split("=")[1];

						try {
							Field field;
							field = this.getClass().getDeclaredField(fieldName);
							field.setAccessible(true);					
							Object objectValue = field.get(this);
							
							if (objectValue!=null){
								XmlDynamicData.saveData(savedKey, objectValue.toString());
							}
							else {
								throw new TestCaseFailure("Saving results failed! Field " 
										+ this.getClass().getSimpleName() + "." + fieldName +" is null!");
							}

							 
						}catch (NoSuchFieldException
								|SecurityException
								|IllegalArgumentException
								|IllegalAccessException e) {

							throw new TestCaseFailure("Saving results failed!", e);
						}
					});
			
			String saved = XmlDynamicData.getSavedData().toString(); 
			testCaseAttributes.put("save", saved);
			logger.log("Saved data: save=" + saved);
		}
	}

	

	
	/**
	 * Add an error as test case failure.
	 * 
	 * @param th - thrown error
	 */
	public void addFailure(Throwable th) {
		logger.logLines( TestCaseFailure.stackToString(th) );
		if (th instanceof TestCaseFailure){
			addAttribute("failure", th.toString() + th.getCause());
		}
		else{
			addAttribute("failure", 
					th.toString());
		}
	}
	
	
	
	/**
	 * Return an empty string for null.
	 * 
	 * @param s - string input
	 * @return - empty string for null input, or the string otherwise.
	 */
	public String nullToEmptyString(String s){
		return TestConfig.nullToEmptyString(s);
	}

	

	
	/**
	 * Return attribute's value from attributes' map.
	 * 
	 * @param attribute - attribute's name
	 * @return - attribute's value
	 */
	public String evalAttribute(String attribute){
		return getTestCaseAttributes().get(attribute);
	}
	
	
	
	
	
	/**
	 * Check if an attribute field is available within a test case.
	 * 
	 * @param attributeField
	 * @return - true if there is a field corresponding to an attribute.
	 */
	public boolean isAvailable(Object attributeField){
		if ( attributeField == null ){
			return false;
		}
		else {
			return true;
		}	
	}

	
	
	public Boolean evalBooleanAttribute(String attribute){
		return nullToEmptyString(getTestCaseAttributes().get(attribute)).equalsIgnoreCase("true");
	}

	

	public void assertStaticText(Object staticTextField, 
			String currentText,
			String message){
		
		if ( isAvailable(staticTextField) ){
			Assert.assertTrue(currentText.equals(staticTextField.toString()),
					message,
					staticTextField.toString(),
					currentText);
		}
	}
	
	
	
	public String getFailure() {
		return nullToEmptyString(getTestCaseAttributes().get("failure"));
	}
	
	
	
	// Expected failure of a test case - set to true for test cases that are expected to fail; 
	// a test case that fails and is expected to fail will be reported as successful 
	// and the failure will be available in the final report.
	
	public String getExpectedFailure() {
		return nullToEmptyString(getTestCaseAttributes().get("expectedFailure"));
	}
		
	
	
	public boolean expectFailure(){
		return (! getExpectedFailure().isEmpty());
	}

}
