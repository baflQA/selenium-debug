package seleniumdebug;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.NetworkInterceptor;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static seleniumdebug.NetworkCaptureTest.AwaitUtil.awaitAssertTrue;

public class NetworkCapture2Test {

    private NetworkInterceptor interceptor;

    @SneakyThrows
    @BeforeMethod
    public void setUp() {
        var browser = new RemoteWebDriver(new URL("http://localhost:4444"), new ChromeOptions());
//        var browser = new ChromeDriver();
        var augmented = new Augmenter().augment(browser);
        var flag = new AtomicBoolean(false);
        interceptor = new NetworkInterceptor(augmented, (Filter) next -> req -> {
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
                } catch (Exception e) {
                }
            }
            return res;
        });
        browser.get("https://developer.mozilla.org/en-US/");
        awaitAssertTrue(flag::get);
    }

    @Test
    public void test() throws InterruptedException {
        interceptor.close();
        Thread.sleep(Duration.ofSeconds(10).toMillis());
    }

    @AfterMethod
    public void closeNetworkInterceptor() {
        interceptor.close();
    }

    static class AwaitUtil {
        private static final Logger logger = LoggerFactory.getLogger(AwaitUtil.class);

        private AwaitUtil() {
            //Util class
        }

        public static ConditionFactory await() {
            return Awaitility.await().ignoreExceptions().atMost(Duration.ofSeconds(10)).pollInterval(500, MILLISECONDS).alias(String.valueOf(Thread.currentThread().getId())).conditionEvaluationListener(condition -> logger.info("Is satisfied?: {}; Remaining seconds: {}", condition.isSatisfied(), Duration.ofMillis(condition.getRemainingTimeInMS()).getSeconds()));
        }

        public static <T> T awaitNonNull(Callable<T> callable) {
            return await().until(callable, Objects::nonNull);
        }

        public static void awaitAssertTrue(BooleanSupplier actualProvider) {
            await().untilAsserted(() -> assertThat(actualProvider.getAsBoolean(), is(true)));
        }
    }
}
