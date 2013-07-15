/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.any23.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Collection;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.any23.ExtractionReport;
import org.apache.any23.extractor.ExtractionException;
import org.apache.any23.extractor.ExtractionParameters;
import org.apache.any23.extractor.Extractor;
import org.apache.any23.extractor.IssueReport;
import org.apache.any23.source.DocumentSource;
import org.apache.any23.validator.SerializationException;
import org.apache.any23.validator.XMLValidationReportSerializer;
import org.apache.any23.writer.FormatWriter;
import org.apache.any23.writer.ReportingTripleHandler;
import org.apache.any23.writer.WriterFactory;
import org.apache.any23.writer.WriterFactoryRegistry;

import sun.security.validator.ValidatorException;

/**
 * This class is responsible for building the {@link Servlet}
 * web response.
 */
class WebResponder extends BaseExtractor{

    private static final WriterFactoryRegistry writerRegistry = WriterFactoryRegistry.getInstance();

    /**
     * Servlet for which building the response.
     */
    private Servlet any23servlet;

    /**
     * Servlet response object.
     */
    private HttpServletResponse response;    

    /**
     * Type of expected output.
     */
    private String outputMediaType = null;

    /**
     * The output stream.
     */
    private ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();

    public WebResponder(Servlet any23servlet, HttpServletResponse response) {
    	super();
        this.any23servlet = any23servlet;
        this.response = response;
    }

    public void runExtraction(
            DocumentSource in,
            ExtractionParameters eps,
            String format,
            boolean report, boolean annotate
    ) throws IOException {
        if (in == null) return;
        if (!initRdfWriter(format, report, annotate)) return;
        final ExtractionReport er;
        try {
            er = getRunner().extract(eps, in, getRdfWriter());
            closeWriter();
            if (! er.hasMatchingExtractors() ) {
                sendError(
                        415,
                        "No suitable extractor found for this media type",
                        null,
                        er,
                        report
                );
                return;
            }
        } catch (IOException ioe) {
            // IO Error.
            if (ioe.getCause() != null && ValidatorException.class.equals(ioe.getCause().getClass())) {
                final String errMsg = "Could not fetch input, IO Error.";
                any23servlet.log(errMsg, ioe.getCause());
                sendError(502, errMsg, ioe, null, report);
                return;
            }
            any23servlet.log("Could not fetch input", ioe);
            sendError(502, "Could not fetch input.", ioe, null, report);
            return;
        } catch (ExtractionException e) {
            // Extraction error.
            any23servlet.log("Could not parse input", e);
            sendError(502, "Could not parse input.", e, null, report);
            return;
        } catch (Exception e) {
            any23servlet.log("Internal error", e);
            sendError(500, "Internal error.", e, null, report);
            return;
        }

        /* *** No triples found. *** */
        any23servlet.log("Extraction complete, " + getReporter().getTotalTriples() + " triples");
        if (getReporter().getTotalTriples() == 0) {
            sendError(
                    501,
                    "Extraction completed. No triples have been found.",
                    null,
                    er, report
            );
            return;
        }

        sendResponse(report, er, byteOutStream.toByteArray());
    }

	public void sendResponse(boolean report, final ExtractionReport er, final byte[] data)
			throws IOException, UnsupportedEncodingException {
		// Regular response.
        response.setContentType(outputMediaType);
        response.setStatus(200);
        // Set the output encoding equals to the input one.
        if (er != null && Charset.isSupported(er.getEncoding())) {
            response.setCharacterEncoding(er.getEncoding());
        } else {
            response.setCharacterEncoding("UTF-8");
        }

        final ServletOutputStream sos = response.getOutputStream();
        System.out.println(new String(data, "UTF-8"));
        if(report) {
            final PrintStream ps = new PrintStream(sos);
            try {
                printHeader(ps);
                printResponse(getReporter(), er, data, ps);
            } catch (Exception e) {
                throw new RuntimeException("An error occurred while serializing the output response.", e);
            } finally {
                ps.close();
            }
        } else {
            sos.write(data);
        }
	}

    public void sendError(int code, String msg, boolean report) throws IOException {
        sendError(code, msg, null, null, report);
    }
    
    private void printHeader(PrintStream ps) {
        ps.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
    }

    private void printResponse(ReportingTripleHandler rth, ExtractionReport er, byte[] data, PrintStream ps) {
        ps.println("<response>");
        printExtractors(rth, ps);
        printReport(null, null, er, ps);
        printData(data, ps);
        ps.println("</response>");
    }

    private void printExtractors(ReportingTripleHandler rth, PrintStream ps) {
        ps.println("<extractors>");
        for (String extractor : rth.getExtractorNames()) {
            ps.print("<extractor>");
            ps.print(extractor);
            ps.println("</extractor>");
        }
        ps.println("</extractors>");
    }
    
    private void printIssueReport(ExtractionReport er, PrintStream ps) {
        ps.println("<issueReport>");
        for(Extractor<?> extractor : er.getMatchingExtractors()) {
            final String name = extractor.getDescription().getExtractorName();
            final Collection<IssueReport.Issue> extractorIssues = er.getExtractorIssues(name);
            if(extractorIssues.isEmpty()) continue;
            ps.println( String.format("<extractorIssues extractor=\"%s\">", name));
            for(IssueReport.Issue issue : er.getExtractorIssues(name)) {
                ps.println(
                        String.format(
                                "<issue level=\"%s\" row=\"%d\" col=\"%d\">%s</issue>",
                                issue.getLevel().toString(),
                                issue.getRow(),
                                issue.getCol(),
                                issue.getMessage()
                        )
                );
            }
            ps.println("</extractorIssues>");
        }
        ps.println("</issueReport>");

    }

    private void printReport(String msg, Throwable e, ExtractionReport er, PrintStream ps) {
        XMLValidationReportSerializer reportSerializer = new XMLValidationReportSerializer();
        ps.println("<report>");

        // Human readable error message.
        if(msg != null) {
            ps.printf("<message>%s</message>\n", msg);
        } else {
            ps.print("<message/>\n");
        }

        // Error stack trace.
        if(e != null) {
            ps.println("<error>");
            ps.println("<![CDATA[");
            e.printStackTrace(ps);
            ps.println("]]>");
            ps.println("</error>");
        } else {
            ps.println("<error/>");
        }

        // Issue Report.
        printIssueReport(er, ps);

        // Validation report.
        try {
            reportSerializer.serialize(er.getValidationReport(), ps);
        } catch (SerializationException se) {
            ps.println("An error occurred while serializing error.");
            se.printStackTrace(ps);
        }
        ps.println("</report>");
    }

    private void printData(byte[] data, PrintStream ps) {
        ps.println("<data>");
        ps.println("<![CDATA[");
        try {
            ps.write(data);
        } catch (IOException ioe) {
            ps.println("An error occurred while serializing data.");
            ioe.printStackTrace(ps);
        }
        ps.println("]]>");
        ps.println("</data>");
    }

    private void sendError(int code, String msg, Exception e, ExtractionReport er, boolean report)
    throws IOException {
        response.setStatus(code);
        response.setContentType("text/plain");
        final PrintStream ps = new PrintStream(response.getOutputStream());
        if (report) {
            try {
                printHeader(ps);
                printReport(msg, e, er, ps);
            } finally {
                ps.close();
            }
        } else {
            ps.println(msg);
            if (e != null) {
                ps.println("================================================================");
                e.printStackTrace(ps);
                ps.println("================================================================");
            }
        }
    }

    private boolean initRdfWriter(String format, boolean report, boolean annotate) throws IOException {
        final WriterFactory factory = getFormatWriter(format);
        if (factory == null) {
            sendError(
                    400,
                    "Invalid format '" + format + "', try one of: [rdfxml, turtle, ntriples, nquads, trix, json]",
                    null,
                    null,
                    report
            );
            return false;
        }
        FormatWriter fw = factory.getRdfWriter(byteOutStream);
        fw.setAnnotated(annotate);
        outputMediaType = factory.getMimeType();
        initializeWriter(fw);
        return true;
    }	

    private WriterFactory getFormatWriter(String format) throws IOException {
        final String finalFormat;
        // FIXME: Remove this hardcoded set
        if ("rdf".equals(format) || "xml".equals(format) || "rdfxml".equals(format)) {
            finalFormat = "rdfxml";
        } else if ("turtle".equals(format) || "ttl".equals(format)) {
            finalFormat = "turtle";
        } else if ("n3".equals(format)) {
            finalFormat = "turtle";
        } else if ("n-triples".equals(format) || "ntriples".equals(format) || "nt".equals(format)) {
            finalFormat = "ntriples";
        } else if("nquads".equals(format) || "n-quads".equals(format) || "nq".equals(format)) {
            finalFormat = "nquads";
        } else if("trix".equals(format)) {
            finalFormat = "trix";
        } else if("json".equals(format)) {
            finalFormat = "json";
        } else {
            return null;
        }
        final WriterFactory writer = writerRegistry.getWriterByIdentifier(finalFormat);
        return writer;
    }
    
    public void setOutPutMediaType(String type){
    	outputMediaType = type;
    }

}
