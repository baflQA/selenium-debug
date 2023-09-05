package seleniumdebug;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.NetworkInterceptor;
import org.openqa.selenium.devtools.v116.network.Network;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.http.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URL;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.openqa.selenium.remote.http.Contents.utf8String;
import static seleniumdebug.NetworkCaptureTest.AwaitUtil.awaitAssertTrue;

public class NetworkCaptureTest {

    private final AtomicReference<String> responseAsString = new AtomicReference<>();

    private NetworkInterceptor interceptor;

    private static DevTools enableInterception(HasDevTools augmented) {
        var devTools = augmented.getDevTools();
        devTools.createSessionIfThereIsNotOne();
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
        return devTools;
    }

    private static Route getModifiedResponseFilter(AtomicBoolean flag, JsonNode map) {
        return Route.matching(req -> req.getUri().endsWith("pong/get")).to(() -> req -> {
            flag.set(true);
            return new HttpResponse().setStatus(200).addHeader("Content-Type", "application/json").setContent(utf8String(map.toString()));
        });
    }

    @SneakyThrows
    @BeforeMethod
    public void setUp() {
        var browser = new RemoteWebDriver(new URL("http://localhost:4444"), new ChromeOptions());
        var augmented = new Augmenter().augment(browser);
        var devTools = enableInterception((HasDevTools) augmented);
        captureResponse(devTools);
        browser.get("https://developer.mozilla.org/en-US/");
        var map = extractAndModifyCapturedResponse();
        var flag = new AtomicBoolean(false);
        interceptor = new NetworkInterceptor(augmented, getModifiedResponseFilter(flag, map));
        browser.get("https://developer.mozilla.org/en-US/");
        awaitAssertTrue(flag::get);
    }

    @Test
    public void whenValidationErrorIsThrownOnApprovalUpdateFromDetailsThenErrorMessageIsDisplayedTest() throws InterruptedException {
        interceptor.close();
        Thread.sleep(Duration.ofSeconds(10).toMillis());
    }

    @AfterMethod
    public void closeNetworkInterceptor() {
        interceptor.close();
    }

    private void captureResponse(DevTools devTools) {
        devTools.addListener(Network.responseReceived(), receivedResponse -> {
            var response = receivedResponse.getResponse();
            if (response.getUrl().endsWith("pong/get")) {
                var requestId = receivedResponse.getRequestId();
                responseAsString.set(devTools.send(Network.getResponseBody(requestId)).getBody());
            }
        });
    }

    private JsonNode extractAndModifyCapturedResponse() throws JsonProcessingException {
        var map = new JsonMapper().readTree(AwaitUtil.awaitNonNull(responseAsString::get));
        var colors = map.at("/hpMain/colors");
        ((ObjectNode) colors).put("backgroundColor", "red");
        return map;
    }

    static class AwaitUtil {
        private static final Logger logger = LoggerFactory.getLogger(AwaitUtil.class);

        private AwaitUtil() {
            //Util class
        }

        public static ConditionFactory await() {
            return Awaitility.await()
                    .ignoreExceptions()
                    .atMost(Duration.ofSeconds(10))
                    .pollInterval(500, MILLISECONDS)
                    .alias(String.valueOf(Thread.currentThread().getId()))
                    .conditionEvaluationListener(condition -> logger.info("Is satisfied?: {}; Remaining seconds: {}", condition.isSatisfied(), Duration.ofMillis(condition.getRemainingTimeInMS()).getSeconds()));
        }

        public static <T> T awaitNonNull(Callable<T> callable) {
            return await().until(callable, Objects::nonNull);
        }

        public static void awaitAssertTrue(BooleanSupplier actualProvider) {
            await().untilAsserted(() -> assertThat(actualProvider.getAsBoolean(), is(true)));
        }
    }
}
