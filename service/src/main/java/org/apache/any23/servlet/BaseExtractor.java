package org.apache.any23.servlet;

import java.util.ArrayList;
import java.util.List;

import org.apache.any23.Any23;
import org.apache.any23.filter.IgnoreAccidentalRDFa;
import org.apache.any23.filter.IgnoreTitlesOfEmptyDocuments;
import org.apache.any23.writer.CompositeTripleHandler;
import org.apache.any23.writer.CountingTripleHandler;
import org.apache.any23.writer.ReportingTripleHandler;
import org.apache.any23.writer.TripleHandler;
import org.apache.any23.writer.TripleHandlerException;

class BaseExtractor {
	
    /**
     * Library facade.
     */
    private final Any23 runner;
    
    /**
     * RDF triple writer.
     */
    private TripleHandler rdfWriter = null;
    
    /**
     * Error and statistics reporter.
     */
    private ReportingTripleHandler reporter = null;
	
	public BaseExtractor() {
        this.runner = new Any23();
        runner.setHTTPUserAgent("Any23-Servlet");
	}
	
	protected void initializeWriter(TripleHandler fw) {
		List<TripleHandler> tripleHandlers = new ArrayList<TripleHandler>();
        tripleHandlers.add(new IgnoreAccidentalRDFa(fw));
        tripleHandlers.add(new CountingTripleHandler());
        rdfWriter = new CompositeTripleHandler(tripleHandlers);
        reporter = new ReportingTripleHandler(rdfWriter);
        rdfWriter = new IgnoreAccidentalRDFa(
            new IgnoreTitlesOfEmptyDocuments(reporter),
            true    // suppress stylesheet triples.
        );
	}
	
	protected void closeWriter() throws TripleHandlerException {
		rdfWriter.close();
	}
	
    protected Any23 getRunner() {
        return runner;
    }

	protected TripleHandler getRdfWriter() {
		return rdfWriter;
	}

	protected ReportingTripleHandler getReporter() {
		return reporter;
	}
	
}
