package com.joshfurr.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Created by joshfurr on 10/6/16.
 */
@Service
public class PathProcessingService {

    private final Logger LOGGER = LoggerFactory.getLogger(PathProcessingService.class);

    WordCountingService wordCountingService;

    @Autowired
    public PathProcessingService(WordCountingService wordCountingService){
        this.wordCountingService = wordCountingService;
    }

    /**
     * This method is called by the Controller.  It returns the Map of Maps that contains the word counts.
     * The method is not multithreaded but could be made so with some changes.  It takes a map instead of
     * a string so that it can take multiple paths to search for words and produce a word list for each map.
     *
     *
     * @param pathMap
     * @return
     */
    public Map<String, Map<String, Integer>> processOriginalRequest(Map<String, String> pathMap) {

        Map<String, Map<String, Integer>> finalWordMap = new HashMap<>();

        if ( null != pathMap && 0 < pathMap.size() ) {

            for ( String pathNum : pathMap.keySet() ){

                finalWordMap.put( pathNum, createMapForOriginalPath( pathMap.get( pathNum ) ) );

            }

        }

        return finalWordMap;
    }

    /**
     * This method returns a Map for the path that is sent.  It spins up a new thread so that it can reuse
     * code in the threadedProcessPath() method.
     * @param originalPath
     * @return
     */
    private Map<String,Integer> createMapForOriginalPath(String originalPath) {

        Map<String, Integer> wordMap = new HashMap<>();

        Set<String> txtFilesToProcess = Collections.synchronizedSet( new HashSet<>() );

        File filePath = new File(originalPath);

        if ( filePath.exists() && filePath.isDirectory() ) {

            AtomicInteger counter = new AtomicInteger(0);

            ExecutorService es = Executors.newCachedThreadPool();

            counter.incrementAndGet();

            es.submit( new Runnable(){

                @Override
                public void run() {

                    threadedProcessPath(originalPath, txtFilesToProcess, es, counter);

                }

            });

            // this waits until all of the threads of processing paths has been completed.
            while( counter.get() != 0 ) {

                try {

                    Thread.sleep(1000);

                } catch ( InterruptedException e ) {

                    e.printStackTrace();

                    LOGGER.error( e.getMessage() );

                }

                LOGGER.info("Processing paths...");

            }

            LOGGER.info("Finished Processing paths");

            // if txt files are found it will then use the WordCountingService to count the words in those files
            if( 0 < txtFilesToProcess.size() ){

                wordMap = wordCountingService.countWordsInFiles(txtFilesToProcess);

            }

        }

        return wordMap;

    }

    /**
     * This is a threaded method that first unzips and files on the path, the finds the txt files on the path, then
     * finds any child paths so that it can recursively call this method.  Then once it has done those three tasks
     * the tread count is decremented and the thread is finished
     * @param path
     * @param txtFilesToProcess
     * @param es
     * @param counter
     */
    private void threadedProcessPath(String path, Set<String> txtFilesToProcess, ExecutorService es, AtomicInteger counter) {

        findfilesToUnzip(path);

        findTxtFilesToProcess(txtFilesToProcess, path);

        findPathsToProcess(path, txtFilesToProcess, es, counter);

        counter.decrementAndGet();

    }

    /**
     * This method searches a given path to find additional paths.  If it finds those paths it will spin up
     * another thread to process that path.
     * @param pathName
     * @param txtFilesToProcess
     * @param es
     * @param counter
     */
    private void findPathsToProcess(String pathName, Set<String> txtFilesToProcess, ExecutorService es, AtomicInteger counter) {

        // Creation of filter to find directories within the path
        DirectoryStream.Filter<Path> folderFilter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path file) throws IOException {
                return (Files.isDirectory(file));
            }
        };

        Path path = FileSystems.getDefault().getPath( pathName );

        try ( DirectoryStream<Path> stream = Files.newDirectoryStream( path, folderFilter ) ) {

            for (Path filepath : stream) {

                counter.incrementAndGet();

                es.submit( new Runnable(){

                    @Override
                    public void run() {
                        threadedProcessPath(filepath.toString(), txtFilesToProcess, es, counter);
                    }

                });

            }

        } catch (IOException e) {

            e.printStackTrace();

        }
    }

    /**
     * This method finds all of the txt files in a given path and adds them to the threadsafe set, so
     * that they can be processed at a later time.
     *
     * @param txtFilesToProcess
     * @param path
     */
    private void findTxtFilesToProcess(Set<String> txtFilesToProcess, String path) {

        // Creation of filter to find txt files
        FilenameFilter txtFileFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith( ".txt" );
            }
        };

        File folder = new File( path );

        File[] txtFiles = folder.listFiles( txtFileFilter );

        for ( File file : txtFiles ){

            txtFilesToProcess.add( file.getPath() );

        }

    }

    /**
     * This method finds all of the zip files to
     * @param path
     */
    private void findfilesToUnzip(String path) {

        FilenameFilter zipFileFilter = new FilenameFilter() {
            public boolean accept( File dir, String name ) {
                return name.toLowerCase().endsWith( ".zip" );
            }
        };

        File folder = new File( path );

        File[] zipFiles = folder.listFiles( zipFileFilter );

        for ( File file : zipFiles ){

            try{

                unzipFile(file);

            } catch (IOException e){

                e.printStackTrace();

                LOGGER.error("Error Unzipping file: " + file.getAbsolutePath() );            }

        }

    }

    /**
     * This method does the unzipping of a a zip file and writes it's contents into a folder structure difined
     * by the name of the file.
     * @param file
     * @throws IOException
     */
    private void unzipFile(File file) throws IOException {

        final int BUFFER = 2048;

        try {

            FileInputStream fis = new FileInputStream(file);

            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));

            ZipEntry entry;

            // make new directory
            File zippedFolder =
                    new File( file.getAbsolutePath().substring( 0, file.getAbsolutePath().length() - 4 ) );

            // if directory already exists add timestamp to name
            if ( zippedFolder.exists() && zippedFolder.isDirectory() ){

                zippedFolder =
                        new File( file.getAbsolutePath().substring(0, file.getAbsolutePath().length() - 4) + System.currentTimeMillis() );

            }

            zippedFolder.mkdirs();

            // iterate through the entries and write them only if they are txt or zip files
            while( (entry = zis.getNextEntry() ) != null) {

                if ( ( entry.getName().length() > 4 && entry.getName().substring( entry.getName().length() - 4 ).equals(".zip") ) ||
                        ( entry.getName().length() > 4 && entry.getName().substring( entry.getName().length() - 4 ).equals(".txt") ) ){

                    File newFile = new File( zippedFolder + "/" + entry.getName() );

                    new File( newFile.getParent() ).mkdirs();

                    BufferedInputStream bis = new BufferedInputStream( new ZipFile( file ).getInputStream( entry ) );

                    FileOutputStream fos = new FileOutputStream( newFile );

                    BufferedOutputStream bos = new BufferedOutputStream( fos, BUFFER );

                    int currentByte;

                    byte data[] = new byte[BUFFER];

                    // read and write until last byte is encountered
                    while ( (currentByte = bis.read(data, 0, BUFFER) ) > 0) {

                        bos.write(data, 0, currentByte);

                    }

                    bos.flush();

                    bos.close();

                    fos.close();

                    bis.close();

                }

            }

            zis.close();

        } catch( Exception e ) {

            e.printStackTrace();

            LOGGER.error( e.getMessage() );

        }

    }

}
