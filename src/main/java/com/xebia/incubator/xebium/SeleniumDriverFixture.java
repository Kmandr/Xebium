package com.xebia.incubator.xebium;

import static com.xebia.incubator.xebium.FitNesseUtil.asFile;
import static com.xebia.incubator.xebium.FitNesseUtil.removeAnchorTag;
import static com.xebia.incubator.xebium.FitNesseUtil.stringArrayToString;
import static org.apache.commons.lang.StringUtils.join;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverCommandProcessor;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.PreferencesWrapper;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.selenium.CommandProcessor;
import com.thoughtworks.selenium.HttpCommandProcessor;
import com.thoughtworks.selenium.SeleniumException;

/**
 * Main fixture. Starts a browser session and execute commands.
 */
public class SeleniumDriverFixture {

	private static final Logger LOG = LoggerFactory.getLogger(SeleniumDriverFixture.class);

	private CommandProcessor commandProcessor;

	private long timeout = 30000;
	
	private long stepDelay = 0;
	
	private long pollDelay = 100;

	private ScreenCapture screenCapture = new ScreenCapture();

	private LocatorCheck locatorCheck;
	
    private File customProfilePreferencesFile;

	public SeleniumDriverFixture() {
		LOG.info("Instantiating a fresh Selenium Driver Fixture");
	}
	
	private CommandProcessor startWebDriverCommandProcessor(final String browser, String browserUrl) {
		browserUrl = removeAnchorTag(browserUrl);
		WebDriver driver;
		
		if ("firefox".equalsIgnoreCase(browser)) {
			FirefoxProfile profile = new FirefoxProfile();

            if (customProfilePreferencesFile != null) {
                new PreferencesWrapper(customProfilePreferencesFile).addTo(profile);
            }

            // Ensure we deal with untrusted and unverified hosts.
			profile.setAcceptUntrustedCertificates(true);
			profile.setAssumeUntrustedCertificateIssuer(true);
			// Allow Basic Authentication without confirmation
			// Not accepted in Se2.5: profile.setPreference("network.http.phishy-userpass-length", 255);
			driver = new FirefoxDriver(profile);
		} else if ("iexplore".equalsIgnoreCase(browser)) {
			driver = new InternetExplorerDriver();
		} else if ("chrome".equalsIgnoreCase(browser)) {
			driver = new ChromeDriver();
		} else if ("htmlUnit".equalsIgnoreCase(browser)) {
			driver = new HtmlUnitDriver(true);
		} else {
			try {
				driver = new RemoteWebDriverBuilder(browser).newDriver();
			} catch (Exception e) {
				throw new RuntimeException("Unknown browser type. Should be one of 'firefox', 'iexplore', 'chrome' or 'htmlUnit'", e);
			}
		}
		return new WebDriverCommandProcessor(browserUrl, driver);
	}

    public void loadCustomBrowserPreferencesFromFile(String filename) {
        this.customProfilePreferencesFile = new File(filename);
    }
	
	/**
	 * <p><code>
	 * | start browser | <i>firefox</i> | on url | <i>http://localhost</i> |
	 * </code></p>
	 * 
	 * @param browser
	 * @param browserUrl
	 */
	public void startBrowserOnUrl(final String browser, final String browserUrl) {
		setCommandProcessor(startWebDriverCommandProcessor(browser, browserUrl));
		setTimeoutOnSelenium();
		LOG.debug("Started command processor");
	}

	/**
	 * <p><code>
	 * | start browser | <i>firefox</i> | on url | <i>http://localhost</i> | using remote server |
	 * </code></p>
	 * 
	 * @param browser
	 * @param browserUrl
	 */
	public void startBrowserOnUrlUsingRemoteServer(final String browser, final String browserUrl) {
		startBrowserOnUrlUsingRemoteServerOnHost(browser, browserUrl, "localhost");
	}

	/**
	 * <p><code>
	 * | start browser | <i>firefox</i> | on url | <i>http://localhost</i> | using remote server on host | <i>localhost</i> |
	 * </code></p>
	 * 
	 * @param browser
	 * @param browserUrl
	 * @param serverHost
	 */
	public void startBrowserOnUrlUsingRemoteServerOnHost(final String browser, final String browserUrl, final String serverHost) {
		startBrowserOnUrlUsingRemoteServerOnHostOnPort(browser, browserUrl, serverHost, 4444);
	}

	/**
	 * <p><code>
	 * | start browser | <i>firefox</i> | on url | <i>http://localhost</i> | using remote server on host | <i>localhost</i> | on port | <i>4444</i> |
	 * </code></p>
	 * 
	 * @param browser
	 * @param browserUrl
	 * @param serverHost
	 * @param serverPort
	 */
	public void startBrowserOnUrlUsingRemoteServerOnHostOnPort(final String browser, final String browserUrl, final String serverHost, final int serverPort) {
		setCommandProcessor(new HttpCommandProcessorAdapter(new HttpCommandProcessor(serverHost, serverPort, browser, removeAnchorTag(browserUrl))));
		commandProcessor.start();
		setTimeoutOnSelenium();
		LOG.debug("Started HTML command processor");
	}

	void setCommandProcessor(CommandProcessor commandProcessor) {
		this.commandProcessor = commandProcessor;
		screenCapture.setCommandProcessor(commandProcessor);
		locatorCheck = new LocatorCheck(commandProcessor);
		LOG.info("Started new command processor (timeout: " + timeout + "ms, step delay: " + stepDelay + "ms, poll interval: " + pollDelay + "ms)");
	}

	/**
	 * <p><code>
	 * | set timeout to | 500 |
	 * </code></p>
	 * 
	 * <p>Set the timeout, both local and on the running selenium server.</p>
	 * 
	 * @param timeout Timeout in milliseconds (ms)
	 */
	public void setTimeoutTo(long timeout) {
		this.timeout = timeout;
		doOn("setTimeout", "" + timeout);
	}

	/**
	 * Set the default timeout on the selenium instance.
	 */
	private void setTimeoutOnSelenium() {
		executeCommand("setTimeout", new String[] { "" + this.timeout });
	}
	
	/**
	 * <p>Set delay between steps.</p>
	 * <p><code>
	 * | set step delay to | 500 |
	 * | set step delay to | slow |
	 * | set step delay to | fast |
	 * </code></p>
	 * 
	 * @param stepDelay delay in milliseconds
	 */
	public void setStepDelayTo(String stepDelay) {
		if ("slow".equals(stepDelay)) {
			this.stepDelay = 1000;
		} else if ("fast".equals(stepDelay)) {
			this.stepDelay = 0;
		} else {
			this.stepDelay = Long.parseLong(stepDelay);
		}
	}
	
	/**
	 * Instruct the driver to create screenshots
	 * <p><code>
	 * | save screenshot after | <i>failure</i> |
	 * | save screenshot after | <i>error</i> |
	 * </code></p>
	 * 
	 * <p><code>
	 * | save screenshot after | <i>every step</i> |
	 * | save screenshot after | <i>step</i> |
	 * </code></p>
	 * 
	 * <p><code>
	 * | save screenshot after | <i>nothing</i> |
	 * | save screenshot after | <i>none</i> |
	 * </code></p>
	 */
	public void saveScreenshotAfter(String policy) {
		screenCapture.setScreenshotPolicy(policy);
	}
	
	/**
	 * <p><code>
	 * | save screenshot after | <i>failure</i> | in folder | <i>http://files/testResults/screenshots/${PAGE_NAME} |
	 * | save screenshot after | <i>error</i> |
	 * </code></p>
	 */
	public void saveScreenshotAfterInFolder(String policy, String baseDir) {
		saveScreenshotAfter(policy);
		screenCapture.setScreenshotBaseDir(removeAnchorTag(baseDir));
	}
	
	/**
	 * <p><code>
	 * | ensure | do | <i>open</i> | on | <i>/</i> |
	 * </code></p>
	 * 
	 * @param command
	 * @param target
	 * @return
	 */
	public boolean doOn(final String command, final String target) {
		LOG.info("Performing | " + command + " | " + target + " |");
		return executeDoCommand(command, new String[] { target });
	}

	/**
	 * <p><code>
	 * | ensure | do | <i>type</i> | on | <i>searchString</i> | with | <i>some text</i> |
	 * </code></p>
	 * 
	 * @param command
	 * @param target
	 * @param value
	 * @return
	 */
	public boolean doOnWith(final String command, final String target, final String value) {
		LOG.info("Performing | " + command + " | " + target + " | " + value + " |");
		return executeDoCommand(command, new String[] { target, value });
	}

	/**
	 * <p><code>
	 * | <i>$title=</i> | is | <i>getTitle</i> |
	 * </code></p>
	 * 
	 * @param command
	 * @return
	 */
	public String is(final String command) {
		LOG.info("Storing result from  | " + command + " |");
		return executeCommand(new ExtendedSeleniumCommand(command), new String[] { }, stepDelay);
	}

	/**
	 * <p><code>
	 * | <i>$pageName=</i> | is | <i>getText</i> | on | <i>//span</i> |
	 * </code></p>
	 * 
	 * @param command
	 * @param target
	 * @return
	 */
	public String isOn(final String command, final String target) {
		LOG.info("Storing result from | " + command + " | " + target + " |");
		return executeCommand(new ExtendedSeleniumCommand(command), new String[] { target }, stepDelay);
	}

	private boolean executeDoCommand(final String methodName, final String[] values) {
		
		final ExtendedSeleniumCommand command = new ExtendedSeleniumCommand(methodName);

		String output = null;
		boolean result = true;

		if (!locatorCheck.verifyElementPresent(command, values)) {
			result = false;
		} else if (command.requiresPolling()) {
			long timeoutTime = System.currentTimeMillis() + timeout;
			
			do {
				output = executeCommand(command, values, pollDelay);
				result = checkResult(command, values[values.length - 1], output);
			} while (!result && timeoutTime > System.currentTimeMillis());

		} else {

			output = executeCommand(command, values, stepDelay);

			if (command.isCaptureEntirePageScreenshotCommand()) {
				writeToFile(values[0], output);
				result = true;
			} else if (command.isAssertCommand() || command.isVerifyCommand() || command.isWaitForCommand()) {
				result = checkResult(command, values[values.length - 1], output);
			} else {
				LOG.info("Command '" + command.getSeleniumCommand() + "' returned '" + output + "'");
			}
		}
		
		if (screenCapture.requireScreenshot(command, result)) {
			screenCapture.captureScreenshot(methodName, values);
		}

		if (!result && command.isAssertCommand()) {
			throw new AssertionError(output);
		}
		
		return result;
	}

	private String executeCommand(final ExtendedSeleniumCommand command, final String[] values, long delay) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("executeCommand. Command: " + command.getSeleniumCommand() + " with values: [" + join(values, ", ") +"]");
		}
		
		if (commandProcessor == null) {
			throw new IllegalStateException("Command processor not running. First start it by invoking startBrowserOnUrl");
		}
		
		// Handle special cases first
		if ("pause".equals(command.getSeleniumCommand())) {
			try {
				Thread.sleep(Long.parseLong(values[0]));
			} catch (Exception e) {
				LOG.warn("Pause command interrupted", e);
			}
			return null;
		}
		
		String output = null;
		try {
			if (command.returnTypeIsArray()) {
				output = executeArrayCommand(command.getSeleniumCommand(), values);
			} else {
				output = executeCommand(command.getSeleniumCommand(), values);
			}
			
			if (command.isAndWaitCommand()) {
				commandProcessor.doCommand("waitForPageToLoad", new String[] { "" + timeout });
			}
		} catch (final SeleniumException e) {
			LOG.error("Execution of command failed: " + e.getMessage());
		}
		
		if (delay > 0) {
			try {
				Thread.sleep(delay);
			} catch (Exception e) {
				LOG.warn("Step delay sleep command interrupted", e);
			}
		}
		return output;
	}

	private String executeCommand(String methodName, final String[] values) {
		String output = commandProcessor.doCommand(methodName, values);

		if (output != null && LOG.isDebugEnabled()) {
			LOG.debug("Command processor returned '" + output + "'");
		}

		return output;
	}

	private String executeArrayCommand(String methodName, final String[] values) {
		String[] output = commandProcessor.getStringArray(methodName, values);

		if (output != null && LOG.isDebugEnabled()) {
			LOG.debug("Command processor returned '" + Arrays.asList(output) + "'");
		}

		return stringArrayToString(output);
	}

	private boolean checkResult(ExtendedSeleniumCommand command, String expected, String actual) {
		boolean result = command.matches(expected, actual);
		LOG.info("Command '" + command.getSeleniumCommand() + "' returned '" + actual + "' => " + (result ? "ok" : "not ok, expected '" + expected + "'"));
		return result;
	}

	private void writeToFile(final String filename, final String output) {
		File file = asFile(filename);
		try {
			ScreenCapture.writeToFile(file, output);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void stopBrowser() {
		commandProcessor.stop();
		commandProcessor = null;
		
		LOG.info("Command processor stopped");
	}
}
