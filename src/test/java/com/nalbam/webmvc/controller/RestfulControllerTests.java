package com.nalbam.webmvc.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.lang.management.ManagementFactory;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract of /health, /live, /ready, /version and /read, served by the real web application
 * configuration (WEB-INF/appconfig-root.xml) through MockMvc, without a container.
 */
@NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.OVERRIDE)
public class RestfulControllerTests {

    private static final String CONFIG = "file:src/main/webapp/WEB-INF/appconfig-root.xml";

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    static JsonNode getJson(MockMvc mvc, String path) throws Exception {
        MvcResult result = mvc.perform(get(path).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();
        return MAPPER.readTree(result.getResponse().getContentAsString());
    }

    static long jvmUptimeSeconds() {
        return ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
    }

    /** Checks the common /health fields and returns the version. */
    static String assertHealth(MockMvc mvc) throws Exception {
        long before = jvmUptimeSeconds();
        JsonNode json = getJson(mvc, "/health");
        long after = jvmUptimeSeconds();

        assertTrue(json.get("result").isString(), "result must be a string: " + json);
        assertEquals("ok", json.get("result").asString());
        assertTrue(json.get("type").isString(), "type must be a string: " + json);
        assertEquals("health", json.get("type").asString());
        assertTrue(json.get("service") != null && json.get("service").isString(), "service must be a string: " + json);
        assertEquals("sample-tomcat", json.get("service").asString());
        assertTrue(json.get("version").isString(), "version must be a string: " + json);

        JsonNode uptime = json.get("uptime");
        assertTrue(uptime != null && uptime.isIntegralNumber(), "uptime must be an integer: " + json);
        long seconds = uptime.asLong();
        assertTrue(seconds >= 0, "uptime must not be negative: " + json);
        assertTrue(before <= seconds && seconds <= after,
                "uptime " + seconds + " must be the JVM uptime in seconds, between " + before + " and " + after);

        return json.get("version").asString();
    }

    /** Checks that /live is HTTP 200 with the boolean live true. */
    static void assertLive(MockMvc mvc) throws Exception {
        JsonNode json = getJson(mvc, "/live");
        assertTrue(json.get("live") != null && json.get("live").isBoolean(), "live must be a boolean: " + json);
        assertTrue(json.get("live").asBoolean(), "live must be true: " + json);
    }

    /** Checks /version and returns its version. */
    static String assertVersion(MockMvc mvc) throws Exception {
        JsonNode json = getJson(mvc, "/version");
        assertTrue(json.get("version") != null && json.get("version").isString(), "version must be a string: " + json);
        assertEquals(System.getProperty("java.version"), json.get("java").asString(), "java: " + json);
        return json.get("version").asString();
    }

    /** Checks the HTTP status and the boolean ready field of /ready. */
    static void assertReady(MockMvc mvc, HttpStatus expectedStatus, boolean expectedReady) throws Exception {
        MvcResult result = mvc.perform(get("/ready").accept(MediaType.APPLICATION_JSON))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();
        assertEquals(expectedStatus.value(), result.getResponse().getStatus(), "/ready status");
        JsonNode json = MAPPER.readTree(result.getResponse().getContentAsString());
        assertTrue(json.get("ready") != null && json.get("ready").isBoolean(), "ready must be a boolean: " + json);
        assertEquals(expectedReady, json.get("ready").asBoolean(), "ready: " + json);
    }

    @Nested
    @SpringJUnitWebConfig(locations = CONFIG)
    @TestPropertySource(properties = "version=v1.2.3-test")
    class ConfiguredVersion {

        private MockMvc mvc;

        @BeforeEach
        void setUp(WebApplicationContext context) {
            mvc = MockMvcBuilders.webAppContextSetup(context).build();
        }

        @Test
        void healthReturnsConfiguredVersion() throws Exception {
            assertEquals("v1.2.3-test", assertHealth(mvc));
        }

        @Test
        void liveReturnsLiveTrue() throws Exception {
            assertLive(mvc);
        }

        @Test
        void readKeepsItsContract() throws Exception {
            JsonNode json = getJson(mvc, "/read");
            assertEquals("OK", json.get("result").asString(), "/read: " + json);
            assertEquals("live", json.get("type").asString(), "/read: " + json);
        }

        @Test
        void readyReturnsReadyTrue() throws Exception {
            assertReady(mvc, HttpStatus.OK, true);
        }

        @Test
        void versionReturnsConfiguredVersion() throws Exception {
            assertEquals("v1.2.3-test", assertVersion(mvc));
        }
    }

    /** After ContextClosedEvent, /ready fails with 503 and /live keeps answering. */
    @Nested
    @SpringJUnitWebConfig(locations = CONFIG)
    @DirtiesContext
    class ShuttingDown {

        private MockMvc mvc;

        @BeforeEach
        void setUp(WebApplicationContext context) {
            mvc = MockMvcBuilders.webAppContextSetup(context).build();
        }

        @Test
        void readyReturns503AfterContextClosedEvent(WebApplicationContext context) throws Exception {
            assertReady(mvc, HttpStatus.OK, true);

            context.publishEvent(new ContextClosedEvent(context));

            assertReady(mvc, HttpStatus.SERVICE_UNAVAILABLE, false);
            assertLive(mvc);
        }
    }

    /** The real close path: ContextLoaderListener closes the root context when Tomcat stops the web application. */
    @Test
    void readyReturns503AfterTheContextCloses() {
        XmlWebApplicationContext context = new XmlWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.setConfigLocation(CONFIG);
        context.refresh();
        RestfulController controller = context.getBean(RestfulController.class);
        try {
            assertEquals(HttpStatus.OK, controller.ready().getStatusCode());
        } finally {
            context.close();
        }

        assertFalse(context.isActive(), "the context must be closed");
        ResponseEntity<Map<String, Object>> response = controller.ready();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(Map.of("ready", false), response.getBody());
    }

    /** No VERSION variable and no version property: the default of application.yml applies. */
    @Nested
    @SpringJUnitWebConfig(locations = CONFIG, initializers = UnsetVersion.WithoutSystemSources.class)
    class UnsetVersion {

        private MockMvc mvc;

        @BeforeEach
        void setUp(WebApplicationContext context) {
            mvc = MockMvcBuilders.webAppContextSetup(context).build();
        }

        @Test
        void healthReturnsDefaultVersion() throws Exception {
            assertEquals("v0.0.0", assertHealth(mvc));
        }

        @Test
        void versionReturnsDefaultVersion() throws Exception {
            assertEquals("v0.0.0", assertVersion(mvc));
        }

        /** Removes the environment variables and system properties, so a VERSION of the build machine does not apply. */
        static class WithoutSystemSources implements ApplicationContextInitializer<ConfigurableApplicationContext> {
            @Override
            public void initialize(ConfigurableApplicationContext context) {
                MutablePropertySources sources = context.getEnvironment().getPropertySources();
                sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
            }
        }
    }

}
