package com.nalbam.webmvc.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.lang.management.ManagementFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract of /health, /live and /read, served by the real web application
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
        assertTrue(json.get("version").isString(), "version must be a string: " + json);

        JsonNode uptime = json.get("uptime");
        assertTrue(uptime != null && uptime.isIntegralNumber(), "uptime must be an integer: " + json);
        long seconds = uptime.asLong();
        assertTrue(seconds >= 0, "uptime must not be negative: " + json);
        assertTrue(before <= seconds && seconds <= after,
                "uptime " + seconds + " must be the JVM uptime in seconds, between " + before + " and " + after);

        return json.get("version").asString();
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
        void liveAndReadKeepTheirContract() throws Exception {
            for (String path : new String[] { "/live", "/read" }) {
                JsonNode json = getJson(mvc, path);
                assertEquals("OK", json.get("result").asString(), path + ": " + json);
                assertEquals("live", json.get("type").asString(), path + ": " + json);
            }
        }
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
