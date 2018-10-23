package com.nalbam.tutorial.spring.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Controller
public class HelloWorldController {

    @Autowired
    private Environment environment;

    @RequestMapping(path = {"/"}, method = RequestMethod.GET)
    public String index(Model model) {
        // profile
        model.addAttribute("profile", environment.getProperty("profile"));

        // message
        // model.addAttribute("message", "Hello Spring MVC!");
        model.addAttribute("message", environment.getProperty("message"));

        // host
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            host = "Unknown";
            e.printStackTrace();
        }
        model.addAttribute("host", host);

        // date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        model.addAttribute("date", sdf.format(new Date()));

        return "index";
    }

    @RequestMapping(path = {"/stress"}, method = RequestMethod.GET)
    public String stress(Model model) {
        Double sum = 0d;
        for (int i = 0; i < 1000000; i++) {
            sum += Math.sqrt(i);
        }

        // sum
        model.addAttribute("sum", sum);

        return "stress";
    }

}
