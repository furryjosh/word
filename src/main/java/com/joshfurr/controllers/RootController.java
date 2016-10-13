package com.joshfurr.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created by joshfurr on 10/6/16.
 */
@Controller
@RequestMapping(value = "/")
public class RootController {

    private static final String INDEX_PAGE = "index.html";

}
