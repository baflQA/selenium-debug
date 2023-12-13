package seleniumdebug;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.NetworkInterceptor;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.Filter;
import org.testng.annotations.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static seleniumdebug.NetworkCapture2Test.AwaitUtil.awaitAssertTrue;

public class NetworkCapture2Test {

    @SneakyThrows
    @Test
    public void test() {
        var browser = new RemoteWebDriver(new URL("http://localhost:4444"), new ChromeOptions());
//        var browser = new ChromeDriver();
        browser.manage().window().maximize();
        var augmented = new Augmenter().augment(browser);
        var flag = new AtomicBoolean(false);
        var interceptor = new NetworkInterceptor(augmented, (Filter) next -> req -> {
            var res = next.execute(req);
            if (req.getUri().endsWith("pong/get")) {
                try {
                    var inputStream = res.getContent().get();
                    String string = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    var map = new JsonMapper().readTree(string);
                    var colors = map.at("/hpMain/colors");
                    ((ObjectNode) colors).put("backgroundColor", "red");
                    res.setHeader("Content-Type", "application/json");
                    res.setContent(Contents.utf8String(map.toString()));
                    flag.set(true);
                } catch (Exception ignored) {
                }
            }
            return res;
        });
        browser.get("https://developer.mozilla.org/en-US/");
        awaitAssertTrue(flag::get);
        interceptor.close();
        Thread.sleep(Duration.ofSeconds(10).toMillis());
    }

    static class AwaitUtil {
        private AwaitUtil() {
            //Util class
        }

        public static void awaitAssertTrue(BooleanSupplier actualProvider) {
            Awaitility.await()
                    .ignoreExceptions()
                    .atMost(Duration.ofSeconds(10))
                    .pollInterval(500, MILLISECONDS)
                    .untilAsserted(() -> assertThat(actualProvider.getAsBoolean(), is(true)));
        }
    }
}
