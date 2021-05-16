package lt.lb.luceneindexandsearch;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import lt.lb.luceneindexandsearch.indexing.content.TextExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Lemmin
 */
public class SimpleTest {
     
    public static void main(String[] args) throws IOException, TextExtractor.ExtractorException {
        
        Logger log =  LogManager.getLogger();
        log.info("HELLO");
        Path get = Paths.get("D:\\test\\ok.txt");
        InputStream newInputStream = Files.newInputStream(get, StandardOpenOption.READ);
        String text = TextExtractor.extractAnyText(newInputStream);
        
        System.out.print(text);
         log.info("BYE");
    }
}
