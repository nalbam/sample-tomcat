package com.nalbam.webmvc.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

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

    @GetMapping("/live")
    public Map<String, Object> live() {
        log.debug("live");

        Map<String, Object> map = new HashMap<>();
        map.put("result", "OK");
        map.put("type", "live");

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
        map.put("result", "OK");
        map.put("type", "health");

        // version
        map.put("version", environment.getProperty("version"));

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

        ObjectMapper mapper = new ObjectMapper();
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
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setReadTimeout(5000);
        factory.setConnectTimeout(3000);
        HttpClient httpClient = HttpClientBuilder.create().setMaxConnTotal(100).setMaxConnPerRoute(5).build();
        factory.setHttpClient(httpClient);
        return new RestTemplate(factory);
    }

}
