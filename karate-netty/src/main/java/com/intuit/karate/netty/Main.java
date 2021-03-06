/*
 * The MIT License
 *
 * Copyright 2018 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuit.karate.netty;

import com.intuit.karate.FileUtils;
import com.intuit.karate.StringUtils;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.io.File;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 *
 * @author pthomas3
 */
public class Main implements Callable<Void> {

    private static final String LOGBACK_CONFIG = "logback.configurationFile";
    private static final String CERT_FILE = "cert.pem";
    private static final String KEY_FILE = "key.pem";

    private static Logger logger;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean help;

    @Option(names = {"-m", "--mock"}, required = true, description = "mock server file")
    File mock;

    @Option(names = {"-p", "--port"}, required = true, description = "mock server port")
    int port;

    @Option(names = {"-s", "--ssl"}, description = "use ssl / https, will use '"
            + CERT_FILE + "' and '" + KEY_FILE + "' if they exist in the working directory, or generate them")
    boolean ssl;
    
    @Option(names = {"-c", "--cert"}, description = "ssl certificate (default: " + CERT_FILE + ")")
    File cert;
    
    @Option(names = {"-k", "--key"}, description = "ssl private key (default: " + KEY_FILE + ")")
    File key;    
    
    public static void main(String[] args) {
        // ensure WE init logback before anything else
        String logbackConfig = System.getProperty(LOGBACK_CONFIG);
        if (StringUtils.isBlank(logbackConfig)) {
            System.setProperty(LOGBACK_CONFIG, new File("logback.xml").getAbsolutePath());
        }
        logger = LoggerFactory.getLogger(Main.class);
        CommandLine.call(new Main(), System.err, args);
    }

    @Override
    public Void call() throws Exception {
        FeatureServer server;
        if (cert != null) {
            ssl = true;
        }
        if (ssl) {
            if (cert == null) {
                cert = new File(CERT_FILE);
                key = new File(KEY_FILE);
            }
            if (!cert.exists() || !key.exists()) {
                logger.warn("ssl requested, but " + CERT_FILE + " and/or " + KEY_FILE + " not found in working directory, will create");
                try {
                    SelfSignedCertificate ssc = new SelfSignedCertificate();
                    FileUtils.copy(ssc.certificate(), cert);
                    FileUtils.copy(ssc.privateKey(), key);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                logger.info("ssl on, using existing files: {} and {}", CERT_FILE, KEY_FILE);
            }
            server = FeatureServer.start(mock, port, cert, key, null);
        } else {
            server = FeatureServer.start(mock, port, false, null);
        }
        server.waitSync();
        return null;
    }        

}
