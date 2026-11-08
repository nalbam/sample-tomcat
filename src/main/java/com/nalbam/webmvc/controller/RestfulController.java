package com.nalbam.webmvc.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeoutException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

@Slf4j
@RestController
public class RestfulController {

    @Autowired
    private Environment environment;

    // Resolved by the property placeholder of appconfig-root.xml: the environment
    // (VERSION variable, -Dversion) first, then application.yml (${VERSION:v0.0.0}).
    @Value("${version}")
    private String version;

    // Set when the Spring context starts to close. On SIGTERM, Tomcat stops the web
    // application, ContextLoaderListener closes the root context and publishes
    // ContextClosedEvent. A close of the DispatcherServlet child context also
    // reaches this listener, and it also means shutdown.
    private volatile boolean shuttingDown = false;

    @EventListener(ContextClosedEvent.class)
    public void onContextClosed() {
        log.info("context closed: /ready returns 503");
        shuttingDown = true;
    }

    @GetMapping("/live")
    public Map<String, Object> live() {
        log.debug("live");

        Map<String, Object> map = new HashMap<>();
        map.put("live", true);

        return map;
    }

    // Once shutdown begins, /ready fails so the load balancer stops routing here
    // while in-flight requests finish; /live keeps answering.
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        log.debug("ready");

        Map<String, Object> map = new HashMap<>();
        if (shuttingDown) {
            map.put("ready", false);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(map);
        }
        map.put("ready", true);
        return ResponseEntity.ok(map);
    }

    @GetMapping("/version")
    public Map<String, Object> version() {
        log.debug("version");

        Map<String, Object> map = new HashMap<>();
        map.put("version", version);
        map.put("java", System.getProperty("java.version"));

        return map;
    }

    @GetMapping("/read")
    public Map<String, Object> read() {
        log.debug("read");

        Map<String, Object> map = new HashMap<>();
        map.put("result", "OK");
        map.put("type", "live");

        return map;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        log.debug("health");

        final Map<String, Object> map = new HashMap<>();
        map.put("result", "ok");
        map.put("type", "health");
        // sample-node's /tomcat checks this value to confirm which service answered
        map.put("service", "sample-tomcat");
        map.put("version", version);

        // whole seconds since the JVM started, not since the WAR was deployed
        map.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);

        return map;
    }

    @GetMapping("/stress")
    public Map<String, Object> stress() {
        log.debug("stress");

        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

        Double sum = 0d;
        for (int i = 0; i < 1000000; i++) {
            sum += Math.sqrt(i);
        }

        final Map<String, Object> map = new HashMap<>();
        map.put("result", "OK");
        map.put("type", "stress");
        map.put("sum", sum);
        map.put("date", sdf.format(new Date()));

        return map;
    }

    @GetMapping("/node")
    public String node() {
        log.debug("node");

        String url;

        if ("default".equals(environment.getProperty("spring.profiles.active"))) {
            url = "http://localhost:3000/tomcat";
        } else {
            url = "http://sample-node:3000/tomcat";
        }

        String res = getRestTemplate().getForObject(url, String.class);

        return res;
    }

    @GetMapping("/loop/{count}")
    public Map<String, Object> loop(@PathVariable Integer count) {
        log.debug("loop {}", count);

        Map<String, Object> map = new HashMap<>();
        map.put("result", "OK");

        if (count <= 0) {
            return map;
        }

        count--;

        String url;

        if ("default".equals(environment.getProperty("spring.profiles.active"))) {
            url = "http://localhost:8080/loop/" + count;
        } else {
            url = "http://sample-tomcat:8080/loop/" + count;
        }

        String json = getRestTemplate().getForObject(url, String.class);

        JsonMapper mapper = JsonMapper.builder().build();
        Map<String, Object> res = null;

        try {
            res = mapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.info("Exception converting {} to map", json, e);
        }

        map.put("data", res);

        return map;
    }

    @GetMapping("/delay/{sec}")
    public Map<String, Object> delay(@PathVariable final Integer sec) {
        log.debug("delay {}", sec);

        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

        if (sec > 0) {
            try {
                Thread.sleep(sec * 1000);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }

        final Map<String, Object> map = new HashMap<>();
        map.put("result", "OK");
        map.put("type", "delay");
        map.put("date", sdf.format(new Date()));

        return map;
    }

    @GetMapping("/timeout/{sec}")
    public Map<String, Object> timeout(@PathVariable final Integer sec) throws TimeoutException {
        log.debug("timeout {}", sec);

        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

        try {
            Thread.sleep(sec * 1000);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        throw new TimeoutException("Timeout");
    }

    @GetMapping("/fault/{rate}")
    public Map<String, Object> fault(@PathVariable final Integer rate) throws RuntimeException {
        log.debug("fault {}", rate);

        final Integer random = (new Random()).nextInt(100);

        if (random < rate) {
            throw new RuntimeException("Fault! " + random);
        }

        final Map<String, Object> map = new HashMap<>();
        map.put("result", "OK");
        map.put("type", "fault");

        return map;
    }

    private RestTemplate getRestTemplate() {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(3000))
                .setSocketTimeout(Timeout.ofMilliseconds(5000))
                .build();
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setMaxConnTotal(100)
                        .setMaxConnPerRoute(5)
                        .setDefaultConnectionConfig(connectionConfig)
                        .build())
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setResponseTimeout(Timeout.ofMilliseconds(5000))
                        .build())
                .build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }

}
