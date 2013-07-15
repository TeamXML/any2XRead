package org.apache.any23.servlet;

import java.io.File;
import java.util.Iterator;

import org.apache.any23.extractor.ExtractionParameters;
import org.apache.any23.source.DocumentSource;
import org.apache.any23.writer.RepositoryWriter;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

/**
 * Encapsulates the RDF repository. Translates a given resource and saves the
 * RDF statements in the repository.
 * 
 * @author NemoNessuno
 */

class XReadExtractor extends BaseExtractor {

	private Repository _repository;

	public XReadExtractor() throws RepositoryException {
		super();
		File dir = new File("../history");
		dir.mkdirs();
		_repository = new SailRepository(new MemoryStore(dir));
		_repository.initialize();
	}

	public void runExtraction(DocumentSource documentSource,
			ExtractionParameters eps) throws Exception {

		RepositoryConnection connection = null;
		try {
			connection = _repository.getConnection();
			try {
				initializeWriter(new RepositoryWriter(connection));
				getRunner().extract(eps, documentSource, getRdfWriter());
			} finally {
				closeWriter();
			}
		} finally {
			if (connection != null)
				connection.close();
		}
	}

	public String runSPARQL(String queryString) throws RepositoryException,
			MalformedQueryException, QueryEvaluationException {

		RepositoryConnection connection = null;
		TupleQueryResult result = null;
		StringBuilder sb = new StringBuilder();
		String resultString;

		try {
			connection = _repository.getConnection();
			TupleQuery query = connection.prepareTupleQuery(
					QueryLanguage.SPARQL, queryString);
			result = query.evaluate();

			while (result.hasNext()) {
				BindingSet next = result.next();
				Iterator<Binding> iterator = next.iterator();

				while (iterator.hasNext()) {
					Binding nextBinding = iterator.next();
					sb.append(nextBinding.getName() + ": " + nextBinding.getValue() + "\n");
				}
			}
		} finally {
			if (result != null)
				result.close();
			if (connection != null)
				connection.close();
		}
		if (sb.length() < 1){
			resultString = "Empty";
		} else {
			resultString = sb.toString();
		}
		return resultString;
	}
}
