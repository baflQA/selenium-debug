package seleniumdebug;

import lombok.SneakyThrows;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.annotations.Test;

import java.net.URL;

import static org.testng.Assert.assertNotNull;

public class ManagedDownloadsSessionTest {


    @SneakyThrows
    @Test
    public void shouldCreateSessionTest() {
        ChromeOptions capabilities = new ChromeOptions();
        capabilities.setCapability("se:downloadsEnabled", true);
        var browser = new RemoteWebDriver(new URL("http://localhost:4444"), capabilities);
        assertNotNull(browser.getSessionId());
    }

    @SneakyThrows
    @Test
    public void shouldCreateSessionWithoutCapabilityTest() {
        ChromeOptions capabilities = new ChromeOptions();
        var browser = new RemoteWebDriver(new URL("http://localhost:4444"), capabilities);
        assertNotNull(browser.getSessionId());
    }
}
