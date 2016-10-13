package com.joshfurr.controllers;

import com.joshfurr.services.PathProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Created by joshfurr on 10/6/16.
 */
@RestController
@RequestMapping(value = "/path")
public class PathController {

    private PathProcessingService pathProcessingService;

    @Autowired
    public PathController(PathProcessingService pathProcessingService){
        this.pathProcessingService = pathProcessingService;
    }

    /**
     * Endpoint is a POST request that has a body of Map<String, String>. The key in the map is the identifier
     * for the returned map, and the value is a filepath that will be searched for words on text files
     *
     *
     * Returns a map of maps, the key of the map being the what is provided by front end, as the Key on the original
     * request and the value of the map being another map.  The inner map has a key being a word, and the value being
     * the number of times that that word is found on txt files
     * @param pathMap
     * @return
     */
    @CrossOrigin(origins = "http://localhost:8080")
    @RequestMapping(value = "/words", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE )
    public Map<String,Map<String, Integer>> getWords(@RequestBody Map<String, String> pathMap){

        return pathProcessingService.processOriginalRequest(pathMap);
    }

}
