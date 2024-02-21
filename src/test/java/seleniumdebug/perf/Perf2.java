package seleniumdebug.perf;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.logging.Level;

import static java.util.logging.Level.SEVERE;
import static org.openqa.selenium.Platform.ANY;
import static org.openqa.selenium.logging.LogType.BROWSER;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

public class Perf2 {

    private WebDriver webDriver;

    @BeforeClass
    public void createDriver() throws MalformedURLException {
        var prefs = new DesiredCapabilities();
        prefs.setCapability("credentials_enable_service", false);
        prefs.setCapability("profile.password_manager_enabled", false);
        prefs.setPlatform(ANY);
        var chromeOptions = new ChromeOptions();
        var loggingPreferences = new LoggingPreferences();
        loggingPreferences.enable(BROWSER, SEVERE);
        loggingPreferences.enable(LogType.PERFORMANCE, Level.INFO);
        chromeOptions.setCapability("goog:loggingPrefs", loggingPreferences);
        chromeOptions.addArguments("--remote-debugging-port=9222", "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");
//        chromeOptions.addArguments("--headless=new");
        var chromePrefs = new HashMap<>();
        chromePrefs.put("download.prompt_for_download", Boolean.FALSE);
        chromePrefs.put("plugins.always_open_pdf_externally", Boolean.TRUE);
        chromePrefs.put("safebrowsing_for_trusted_sources_enabled", Boolean.FALSE);
        chromeOptions.setExperimentalOption("prefs", chromePrefs);
        chromeOptions.merge(prefs);
        webDriver = new RemoteWebDriver(new URL("http://localhost:4444/"), chromeOptions);
        Runtime.getRuntime().addShutdownHook(new Thread(webDriver::quit));
    }

    @BeforeMethod
    public void navigate() {
        webDriver.get("https://www.selenium.dev");
    }

    @Test(invocationCount = 100)
    public void perfIssueTest() {
        var webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(5)).pollingEvery(Duration.ofMillis(50));
        webDriverWait.until(elementToBeClickable(By.cssSelector("#main_navbar li"))).click();
        webDriverWait.until(elementToBeClickable(By.cssSelector("a[href='/ecosystem']"))).click();
        for (int i = 0; i < 100; i++) {
            webDriverWait.until(presenceOfElementLocated(By.cssSelector("#language-bindings")));
        }
    }

    @AfterClass
    private void tearDown() {
        webDriver.close();
        webDriver.quit();
    }
}