package com.nalbam.webmvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeoutException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

@RestController
public class RestfulController {

    @Autowired
    private Environment environment;

    @GetMapping("/live")
    public Map<String, Object> live() {
        Map<String, Object> map = new HashMap<>();
        map.put("result", "OK");
        map.put("type", "live");

        return map;
    }

    @GetMapping("/read")
    public Map<String, Object> read() {
        Map<String, Object> map = new HashMap<>();
        map.put("result", "OK");
        map.put("type", "live");

        return map;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> map = new HashMap<>();
        map.put("result", "OK");
        map.put("type", "health");

        // version
        map.put("version", environment.getProperty("version"));

        return map;
    }

    @GetMapping("/stress")
    public Map<String, Object> stress() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

        Double sum = 0d;
        for (int i = 0; i < 1000000; i++) {
            sum += Math.sqrt(i);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("result", "OK");
        map.put("type", "stress");
        map.put("sum", sum);
        map.put("date", sdf.format(new Date()));

        return map;
    }

    // @GetMapping("/node")
    // public String node() {
    // String url;

    // if ("default".equals(profile)) {
    // url = "http://localhost:3000/spring";
    // } else {
    // url = "http://sample-node/spring";
    // }

    // String res = restTemplate.getForObject(url, String.class);

    // return res;
    // }

    // @GetMapping("/loop/{count}")
    // public Map<String, Object> loop(@PathVariable Integer count) {
    // Map<String, Object> map = new HashMap<>();
    // map.put("result", "OK");

    // if (count <= 0) {
    // return map;
    // }

    // count--;

    // String url;

    // if ("default".equals(profile)) {
    // url = "http://localhost:8080/loop/" + count;
    // } else {
    // url = "http://sample-spring/loop/" + count;
    // }

    // Map<String, Object> res = restTemplate.getForObject(url, Map.class);

    // map.put("data", res);

    // return map;
    // }

    @GetMapping("/dealy/{sec}")
    public Map<String, Object> dealy(@PathVariable Integer sec) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

        if (sec > 0) {
            try {
                Thread.sleep(sec * 1000);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }

        Map<String, Object> map = new HashMap<>();
        map.put("result", "OK");
        map.put("type", "dealy");
        map.put("date", sdf.format(new Date()));

        return map;
    }

    @GetMapping("/timeout/{sec}")
    public Map<String, Object> timeout(@PathVariable Integer sec) throws TimeoutException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

        try {
            Thread.sleep(sec * 1000);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        throw new TimeoutException("Timeout");
    }

    @GetMapping("/fault/{rate}")
    public Map<String, Object> fault(@PathVariable Integer rate) {
        Random random = new Random();

        try {
            Thread.sleep(random.nextInt(500) + 100);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        Map<String, Object> map = new HashMap<>();
        map.put("result", "OK");
        map.put("type", "fault");

        return map;
    }

}
