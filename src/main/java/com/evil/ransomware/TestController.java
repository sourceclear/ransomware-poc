package com.evil.ransomware;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class TestController {

    @RequestMapping(value = "/foobar", method = RequestMethod.GET)
	public String getSecureTest(Model model) {
		System.out.println("we are in test controller");
		return "test";
	}
}