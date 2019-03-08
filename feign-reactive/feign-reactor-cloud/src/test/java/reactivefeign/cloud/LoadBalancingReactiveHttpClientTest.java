package reactivefeign.cloud;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.netflix.client.*;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import feign.RequestLine;
import feign.RetryableException;
import org.junit.*;
import org.junit.rules.ExpectedException;
import reactivefeign.publisher.RetryPublisherHttpClient;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static reactivefeign.ReactiveRetryers.retry;

/**
 * @author Sergii Karpenko
 */
public class LoadBalancingReactiveHttpClientTest {

    @ClassRule
    public static WireMockClassRule server1 = new WireMockClassRule(wireMockConfig().dynamicPort());
    @ClassRule
    public static WireMockClassRule server2 = new WireMockClassRule(wireMockConfig().dynamicPort());

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static String serviceName = "LoadBalancingTargetTest-loadBalancingDefaultPolicyRoundRobin";

    @BeforeClass
    public static void setupServersList() throws ClientException {
        DefaultClientConfigImpl clientConfig = new DefaultClientConfigImpl();
        clientConfig.loadDefaultValues();
        clientConfig.setProperty(CommonClientConfigKey.NFLoadBalancerClassName, BaseLoadBalancer.class.getName());
        ILoadBalancer lb = ClientFactory.registerNamedLoadBalancerFromclientConfig(serviceName, clientConfig);
        lb.addServers(asList(new Server("localhost", server1.port()), new Server("localhost", server2.port())));
    }

    @Before
    public void resetServers() {
        server1.resetAll();
        server2.resetAll();
    }

    @Test
    public void shouldLoadBalanceRequests() {
        String body = "success!";
        mockSuccess(server1, body);
        mockSuccess(server2, body);

        TestInterface client = CloudReactiveFeign.<TestInterface>builder()
                .enableLoadBalancer()
                .setHystrixCommandSetterFactory(getSetterFactoryWithTimeoutDisabled())
                .target(TestInterface.class, "http://" + serviceName);

        String result1 = client.get().block();
        String result2 = client.get().block();

        assertThat(result1)
                .isEqualTo(result2)
                .isEqualTo(body);

        server1.verify(1, getRequestedFor(urlEqualTo("/")));
        server2.verify(1, getRequestedFor(urlEqualTo("/")));
    }

    @Test
    public void shouldFailAsPolicyWoRetries() {

        expectedException.expect(RuntimeException.class);
        expectedException.expectCause(allOf(isA(RetryPublisherHttpClient.OutOfRetriesException.class),
                hasProperty("cause", isA(RetryableException.class))));

        try {
            loadBalancingWithRetry(2, 0, 0);
        } catch (Throwable t) {
            assertThat(server1.getAllServeEvents().size() == 1
                    ^ server2.getAllServeEvents().size() == 1);
            throw t;
        }
    }

    @Test
    public void shouldRetryOnSameAndFail() {

        expectedException.expect(RuntimeException.class);
        expectedException.expectCause(allOf(isA(RetryPublisherHttpClient.OutOfRetriesException.class),
                                            hasProperty("cause", isA(RetryableException.class))));

        try {
            loadBalancingWithRetry(2, 1, 0);
        } catch (Throwable t) {
            assertThat(server1.getAllServeEvents().size() == 2
                    ^ server2.getAllServeEvents().size() == 2);
            throw t;
        }
    }

    @Test
    public void shouldRetryOnNextAndFail() {

        expectedException.expect(RuntimeException.class);
        expectedException.expectCause(isA(ClientException.class));

        try {
            loadBalancingWithRetry(2, 1, 1);
        } catch (Throwable t) {
            assertThat(server1.getAllServeEvents().size() == 2
                    && server2.getAllServeEvents().size() == 2);
            throw t;
        }
    }

    @Test
    public void shouldRetryOnSameAndSuccess() {

        loadBalancingWithRetry(2, 2, 0);

        assertThat(server1.getAllServeEvents().size() == 3
                ^ server2.getAllServeEvents().size() == 3);

    }

    private void loadBalancingWithRetry(int failedAttemptsNo, int retryOnSame, int retryOnNext) {
        String body = "success!";
        Stream.of(server1, server2).forEach(server -> {
            mockSuccessAfterSeveralAttempts(server, "/",
                    failedAttemptsNo, 503,
                    aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(body));
        });

        RetryHandler retryHandler = new RequestSpecificRetryHandler(true, true,
                new DefaultLoadBalancerRetryHandler(0, retryOnNext, true), null);

        TestInterface client = CloudReactiveFeign.<TestInterface>builder()
                .retryWhen(retry(retryOnSame))
                .enableLoadBalancer(retryHandler)
                .setHystrixCommandSetterFactory(getSetterFactoryWithTimeoutDisabled())
                .target(TestInterface.class, "http://" + serviceName);

        String result = client.get().block();
        assertThat(result).isEqualTo(body);
    }

    private CloudReactiveFeign.SetterFactory getSetterFactoryWithTimeoutDisabled() {
        return (target, methodMetadata) -> {
			String groupKey = target.name();
			HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(methodMetadata.configKey());
			return HystrixObservableCommand.Setter
					.withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
					.andCommandKey(commandKey)
					.andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
							.withExecutionTimeoutEnabled(false)
					);
		};
    }

    static void mockSuccess(WireMockClassRule server, String body) {
        server.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    static void mockSuccessAfterSeveralAttempts(WireMockClassRule server, String url,
                                                int failedAttemptsNo, int errorCode, ResponseDefinitionBuilder response) {
        String state = STARTED;
        for (int attempt = 0; attempt < failedAttemptsNo; attempt++) {
            String nextState = "attempt" + attempt;
            server.stubFor(get(urlEqualTo(url))
                    .inScenario("testScenario")
                    .whenScenarioStateIs(state)
                    .willReturn(aResponse()
                            .withStatus(errorCode)
                            .withHeader("Retry-After", "1"))
                    .willSetStateTo(nextState));

            state = nextState;
        }

        server.stubFor(get(urlEqualTo(url))
                .inScenario("testScenario")
                .whenScenarioStateIs(state)
                .willReturn(response));
    }


    interface TestInterface {

        @RequestLine("GET /")
        Mono<String> get();
    }
}
