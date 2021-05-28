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
package org.apache.clerezza.rdf.jena.facade;

import org.apache.clerezza.*;
import org.apache.clerezza.rdf.jena.commons.Jena2TriaUtil;
import org.apache.clerezza.rdf.jena.commons.Tria2JenaUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.impl.GraphBase;
import org.apache.jena.mem.TrackingTripleIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.wymiwyg.commons.util.collections.BidiMap;
import org.wymiwyg.commons.util.collections.BidiMapImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

/**
 * This class implements {@link org.apache.jena.graph.Graph} basing
 * on a {@link org.apache.clerezza.Graph}. A <code>JenaGraph</code>
 * can be instantiated using mutable <code>Graph</code>s
 * as well as immutable ones (i.e. <code>ImmutableGraph</code>s),
 * an attempt to add or remove triples to a <code>JenaGraph</code> based on
 * an immutable <code>ImmutableGraph</code> will result in
 * a <code>UnsupportedOperationException</code> being thrown by the
 * underlying <code>ImmutableGraph</code>.
 *
 * Typically an instance of this class is passed as argument
 * to {@link org.apache.jena.rdf.model.ModelFactory#createModelForGraph} to
 * get a <code>Model</code>.
 *
 * @author reto
 */
public class JenaGraph extends GraphBase implements org.apache.jena.graph.Graph {
    final org.apache.clerezza.Graph graph;
    final BidiMap<BlankNode, Node> tria2JenaBNodes = new BidiMapImpl<BlankNode, Node>();
    final Jena2TriaUtil jena2TriaUtil =
            new Jena2TriaUtil(tria2JenaBNodes.inverse());
    final Tria2JenaUtil tria2JenaUtil =
            new Tria2JenaUtil(tria2JenaBNodes);

    public JenaGraph(org.apache.clerezza.Graph graph) {
        this.graph = graph;
    }

    @Override
    public void performAdd(org.apache.jena.graph.Triple triple) {
        graph.add(jena2TriaUtil.convertTriple(triple));
    }

    @Override
    public void performDelete(org.apache.jena.graph.Triple triple) {
        Triple clerezzaTriple = jena2TriaUtil.convertTriple(triple);
        if (clerezzaTriple != null) {
            graph.remove(clerezzaTriple);
        }
    }

    private Iterator<org.apache.jena.graph.Triple> convert(
            final Iterator<Triple> base) {
        return new Iterator<org.apache.jena.graph.Triple>() {

            Triple lastReturned = null;

            @Override
            public boolean hasNext() {
                return base.hasNext();
            }

            @Override
            public org.apache.jena.graph.Triple next() {
                Triple baseNext = base.next();
                lastReturned = baseNext;
                return (baseNext == null) ? null : tria2JenaUtil.convertTriple(baseNext, true);
            }

            @Override
            public void remove() {
                graph.remove(lastReturned);
            }
        };
    }

    /**
     * An iterator (over a filtered Graph) that can return its next element as a triple.
     * As parameter a tripleMatch is required.
     * Triple matches are defined by subject, predicate, and object.
     * @param m
     * @return Graph
     */
    private Iterator<Triple> filter(org.apache.jena.graph.Triple m) {
        BlankNodeOrIRI subject = null;
        IRI predicate = null;
        RDFTerm object = null;
        if (m.getMatchSubject() != null) {
            if (m.getSubject().isLiteral()) {
                return Collections.EMPTY_SET.iterator();
            }
            subject = jena2TriaUtil.convertNonLiteral(m.getSubject());
            if (subject == null) {
                return Collections.EMPTY_SET.iterator();
            }
        }
        if (m.getMatchObject() != null) {
            object = jena2TriaUtil.convertJenaNode2Resource(m.getObject());
            if (object == null) {
                return Collections.EMPTY_SET.iterator();
            }
        }        
        if (m.getMatchPredicate() != null) {
            if (!m.getPredicate().isURI()) {
                return Collections.EMPTY_SET.iterator();
            }
            predicate = jena2TriaUtil.convertJenaUri2UriRef(m.getPredicate());
        }

        try {
            return graph.filter(subject, predicate, object);
        } catch (IllegalArgumentException e) {
            //jena serializers are known to query with invalid URIs
            //see http://tech.groups.yahoo.com/group/jena-dev/message/37221
            //an invalid Uris hould not be in the graph and thus lead to an
            //empty result
            return new HashSet<Triple>().iterator();
        }
    }

    @Override
    protected ExtendedIterator<org.apache.jena.graph.Triple> graphBaseFind(org.apache.jena.graph.Triple m) {
        return new TrackingTripleIterator(convert(filter(m)));
    }

    //not yet @Override
    /*protected ExtendedIterator<org.apache.jena.graph.Triple> graphBaseFind(org.apache.jena.graph.Triple triple) {
        return new TrackingTripleIterator(convert(filter(triple)));
    }*/
}
