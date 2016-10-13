package com.joshfurr.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by joshfurr on 10/10/16.
 */
@Service
public class WordCountingService {

    private final Logger LOGGER = LoggerFactory.getLogger(WordCountingService.class);

    /**
     * This is a method that starts a thread for each txt file that needs to be processed.  It returns
     * a TreeMap constructed from a ConcurrentHashmap so that it is sorted.
     * @param txtFilesToProcess
     * @return
     */
    public Map<String, Integer> countWordsInFiles(Set<String> txtFilesToProcess) {

        Map<String, Integer> map = new ConcurrentHashMap<>();

        AtomicInteger counter = new AtomicInteger(0);

        ExecutorService es = Executors.newCachedThreadPool();

        for( String fileName : txtFilesToProcess ){

            counter.incrementAndGet();

            es.submit( new Runnable(){

                @Override
                public void run() {

                    threadedProcessTextFile( fileName, counter, map );

                }

            });

        }

        while( counter.get() != 0 ) {

            try {

                Thread.sleep(1000);

            } catch (InterruptedException e) {

                e.printStackTrace();

                LOGGER.error( e.getMessage() );

            }

            LOGGER.info("Processing txt files...");

        }

        LOGGER.info("Finished Processing txt files");

        return new TreeMap<>(map);

    }

    /**
     * This is a method that scans a given txt file then processes the words in it so that they
     * can be added to the map that stores them and their counts.
     *
     * @param fileName
     * @param counter
     * @param map
     */
    private void threadedProcessTextFile(String fileName, AtomicInteger counter, Map<String, Integer> map) {

        try {

            Scanner input = new Scanner( new File( fileName ) );

            while ( input.hasNextLine() ) {

                if( input.hasNext() ) {

                    String word = removePunctuation( input.next() );

                    addWordToMap( word.toLowerCase().trim(), map );

                }

            }

        } catch (FileNotFoundException e){

            e.printStackTrace();

            LOGGER.error( e.getMessage() );

        } finally {

            counter.decrementAndGet();

        }

    }

    /**
     * Method that removes punctuation marks from the word and leaves any additional special characters
     *
     * @param word
     * @return
     */
    private String removePunctuation(String word) {

        return word.replaceAll( "[\\.\\?!:;,]", "");

    }

    /**
     * Method that adds the word to the Map and increments the count of the word if it already
     * is present in the map.
     *
     * @param word
     * @param map
     */
    private void addWordToMap(String word, Map<String, Integer> map) {

        Integer count;

        count = map.get( word );

        if ( null != count ){

            map.put( word, count + 1 );

        } else {

            map.put( word, 1 );

        }

    }

}

