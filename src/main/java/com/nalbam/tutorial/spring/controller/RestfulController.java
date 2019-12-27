package com.nalbam.tutorial.spring.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class RestfulController {

    @Autowired
    private Environment environment;

    @RequestMapping(path = { "/health" }, method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> map = new HashMap<>();
        map.put("result", "OK");
        map.put("type", "health");

        // version
        map.put("version", environment.getProperty("version"));

        return new ResponseEntity<>(map, HttpStatus.OK);
    }

    @RequestMapping(path = { "/stress" }, method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> stress() {
        Map<String, Object> map = new HashMap<>();
        map.put("result", "OK");
        map.put("type", "stress");

        Double sum = 0d;
        for (int i = 0; i < 1000000; i++) {
            sum += Math.sqrt(i);
        }

        // sum
        map.put("sum", sum.toString());

        return new ResponseEntity<>(map, HttpStatus.OK);
    }

}
