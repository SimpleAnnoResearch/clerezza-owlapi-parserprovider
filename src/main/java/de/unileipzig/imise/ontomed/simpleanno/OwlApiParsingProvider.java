package de.unileipzig.imise.ontomed.simpleanno;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.rdf.core.serializedform.ParsingProvider;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * see http://jena.sourceforge.net/IO/iohowto.html
 */
/**
 * @author ralph
 */
@Component(immediate=true)
@Service(ParsingProvider.class)
@SupportedFormat({"application/owl+xml", "text/owl-manchester", "text/owl-functional"})
@Property(name="supportedFormat", value={"application/owl+xml", "text/owl-manchester", "text/owl-functional"})
public class OwlApiParsingProvider implements ParsingProvider {
	
	private static Logger log = LoggerFactory.getLogger(OwlApiParsingProvider.class);

	@Override
	public void parse(Graph target, InputStream serializedGraph, String formatIdentifier, IRI baseUri) {
			OWLOntology onto = AccessController.doPrivileged(new PrivilegedAction<OWLOntology>() {
				@Override
				public OWLOntology run() {
					try {
						OWLOntologyManager man = OWLManager.createOWLOntologyManager();
						return man.loadOntologyFromOntologyDocument(new StreamDocumentSource(serializedGraph), new OWLOntologyLoaderConfiguration().setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT));
					} catch (Throwable e) {
						throw new RuntimeException(e);
					}
				}
			});
			target.addAll(owlOntologyToClerezzaTriples(onto));
	}
	
    /**
     * 
     * Converts an OWL API {@link OWLOntology} to an {@link ArrayList} of Clerezza triples (instances of class
     * {@link Triple}).
     * 
     * @param ontology
     *            {@link OWLOntology}
     * @return an {@link ArrayList} that contains the generated Clerezza triples (see {@link Triple})
     */
    private List<Triple> owlOntologyToClerezzaTriples(OWLOntology ontology) {
        ArrayList<Triple> clerezzaTriples = new ArrayList<Triple>();
        org.apache.clerezza.commons.rdf.Graph mGraph = owlOntologyToClerezzaGraph(ontology);
        Iterator<Triple> tripleIterator = mGraph.iterator();
        while (tripleIterator.hasNext()) {
            Triple triple = tripleIterator.next();
            clerezzaTriples.add(triple);
        }
        return clerezzaTriples;
    }

    /**
     * 
     * Converts a OWL API {@link OWLOntology} to Clerezza {@link Graph}.
     * 
     * @param ontology
     *            {@link OWLOntology}
     * @return the equivalent Clerezza {@link Graph}.
     */

    private org.apache.clerezza.commons.rdf.Graph owlOntologyToClerezzaGraph(OWLOntology ontology) {
        org.apache.clerezza.commons.rdf.Graph mGraph = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        try {
            manager.saveOntology(ontology, new RDFXMLOntologyFormat(), out);
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            ParsingProvider parser = new JenaParserProvider();
            mGraph = new SimpleGraph();
            parser.parse(mGraph, in, SupportedFormat.RDF_XML, null);
        } catch (OWLOntologyStorageException e) {
            log.error("Failed to serialize OWL Ontology " + ontology + "for conversion", e);
        }
        return mGraph;

    }

}
