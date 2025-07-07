package seleniumdebug;

import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;

public class CreateNewSessionTest {

    //run runGridStandalone.sh before the test

    @Test
    public void createNewSession() {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--remote-debugging-port=9222", "--disable-dev-shm-usage", "--disable-gpu");
        chromeOptions.enableBiDi();
        assertThatNoException().isThrownBy(() -> RemoteWebDriver.builder()
                .address("http://localhost:4444")
                .oneOf(chromeOptions)
                .build());
    }
}
