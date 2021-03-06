package com.nalbam.webmvc.controller;

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
public class HomeController {

    @Autowired
    private Environment environment;

    @RequestMapping(path = { "/" }, method = RequestMethod.GET)
    public String index(Model model) {
        // cluster
        model.addAttribute("cluster", environment.getProperty("cluster"));

        // profile
        model.addAttribute("profile", environment.getProperty("spring.profiles.active"));

        // version
        model.addAttribute("version", environment.getProperty("version"));

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

}
