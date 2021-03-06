/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor  license  agreements.  See the NOTICE file distributed
 * with this work  for  additional  information  regarding  copyright
 * ownership.  The ASF  licenses  this file to you under  the  Apache
 * License, Version 2.0 (the "License"); you may not  use  this  file
 * except in compliance with the License.  You may obtain  a copy  of
 * the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless  required  by  applicable law  or  agreed  to  in  writing,
 * software  distributed  under  the  License  is  distributed  on an
 * "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR  CONDITIONS  OF ANY KIND,
 * either  express  or implied.  See  the License  for  the  specific
 * language governing permissions and limitations under  the License.
 */
package org.apache.clerezza.rdf.facade.blackbox;

import org.apache.clerezza.Graph;
import org.apache.clerezza.IRI;
import org.apache.clerezza.implementation.TripleImpl;
import org.apache.clerezza.implementation.in_memory.SimpleGraph;
import org.apache.clerezza.implementation.literal.PlainLiteralImpl;
import org.apache.jena.datatypes.xsd.impl.XMLLiteralType;
import org.apache.jena.rdf.model.Literal;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.RSIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import java.io.StringWriter;
import org.apache.clerezza.rdf.jena.facade.JenaGraph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 *
 * @author reto
 */
@RunWith(JUnitPlatform.class)
public class TestBasics {
    
    @Test
    public void serializeGraph() {
        final String uriString = "http://example.org/foo#bar";
        IRI uri = new IRI(uriString);
        Graph mGraph = new SimpleGraph();
        mGraph.add(new TripleImpl(uri, uri, new PlainLiteralImpl("bla bla")));
        org.apache.jena.graph.Graph graph = new JenaGraph(mGraph);
        Model model = ModelFactory.createModelForGraph(graph);
        StringWriter writer = new StringWriter();
        model.write(writer);
        Assertions.assertTrue(writer.toString().contains("about=\""+uriString));
    }
    
    @Test
    public void graphSize() {
        IRI uri = new IRI("http://example.org/foo#bar");
        Graph mGraph = new SimpleGraph();
        mGraph.add(new TripleImpl(uri, uri, new PlainLiteralImpl("bla bla")));
        org.apache.jena.graph.Graph graph = new JenaGraph(mGraph);
        Assertions.assertEquals(1, graph.size());
    }

    @Test
    public void modifyingJenaGraph() {
        Graph mGraph = new SimpleGraph();
        org.apache.jena.graph.Graph graph = new JenaGraph(mGraph);
        Model model = ModelFactory.createModelForGraph(graph);
        model.add(RDFS.Class, RDF.type, RDFS.Class);
        Assertions.assertEquals(1, mGraph.size());
    }
    
    @Test
    public void typedLiterals() {
        Graph mGraph = new SimpleGraph();
        org.apache.jena.graph.Graph graph = new JenaGraph(mGraph);
        Model model = ModelFactory.createModelForGraph(graph);
        Literal typedLiteral = model.createTypedLiteral("<elem>foo</elem>", XMLLiteralType.theXMLLiteralType);
        model.add(RDFS.Class, RDFS.label, typedLiteral);
        Assertions.assertEquals(1, mGraph.size());
        StmtIterator iter = model.listStatements(RDFS.Class, RDFS.label, (RDFNode)null);
        Assertions.assertTrue(iter.hasNext());
        RDFNode gotObject = iter.nextStatement().getObject();
        Assertions.assertEquals(typedLiteral, gotObject);
    }
    
    @Test
    public void reifications() {
        Graph mGraph = new SimpleGraph();
        org.apache.jena.graph.Graph graph = new JenaGraph(mGraph);
        //Model memModel = ModelFactory.createDefaultModel();
        Model model = ModelFactory.createModelForGraph(graph);
        model.add(RDFS.Resource, RDF.type, RDFS.Resource);
        Resource bnode = model.createResource();
        model.add(bnode, RDF.type, RDF.Statement);
        model.add(bnode, RDF.subject, RDFS.Resource);
        model.add(bnode, RDF.predicate, RDF.type);
        model.add(bnode, RDF.object, RDFS.Resource);
        model.add(bnode, RDFS.comment, "we knew that before");
        StmtIterator stmts = model.listStatements(RDFS.Resource, null, (RDFNode)null);
        Statement returnedStmt = stmts.nextStatement();
        RSIterator rsIterator = returnedStmt.listReifiedStatements();
        Assertions.assertTrue(rsIterator.hasNext(), "got back reified statement");
        //recreating jena-graph
        graph = new JenaGraph(mGraph);
        model = ModelFactory.createModelForGraph(graph);
        stmts = model.listStatements(RDFS.Resource, null, (RDFNode)null);
        returnedStmt = stmts.nextStatement();
        rsIterator = returnedStmt.listReifiedStatements();
        Assertions.assertTrue(rsIterator.hasNext(), "got back reified statement on recreated graph");
    }

}
