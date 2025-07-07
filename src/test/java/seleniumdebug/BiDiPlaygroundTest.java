package seleniumdebug;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.browsingcontext.BrowsingContext;
import org.openqa.selenium.bidi.module.Network;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;

public class BiDiPlaygroundTest {

    private WebDriver webDriver;

//    @Attachment(type = "image/png", fileExtension = "png")
    private static byte[] getScreenshot(BrowsingContext browsingContext) {
        return OutputType.BYTES.convertFromBase64Png(browsingContext.captureScreenshot());
    }

    @BeforeMethod
    public void setUp() {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setBrowserVersion("dev");
        chromeOptions.setCapability("webSocketUrl", true);
        webDriver = new ChromeDriver(chromeOptions);
    }

    @Test
    public void bidiTest() {
        Network network = new Network(webDriver);
//        network.onResponseCompleted(responseDetails -> System.out.println(responseDetails.getResponseData().getContent().get()));
//        network.onResponseStarted(resp -> System.out.println(resp.getResponseData().getContent().get()));
        network.onBeforeRequestSent(req -> System.out.println(req.getRequest().getUrl()));

        var browsingContext = new BrowsingContext(webDriver, webDriver.getWindowHandle());
        webDriver.get("https://www.w3schools.com");
        var webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(5));
        webDriverWait.until(wd -> !wd.findElements(By.cssSelector(".w3-content")).isEmpty());
        getScreenshot(browsingContext);
    }

    @AfterMethod
    private void tearDown() {
        webDriver.close();
        webDriver.quit();
    }
}