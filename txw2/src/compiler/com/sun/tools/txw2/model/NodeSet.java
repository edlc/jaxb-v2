/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */

package com.sun.tools.txw2.model;

import com.sun.codemodel.ClassType;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMod;
import com.sun.tools.txw2.NameUtil;
import com.sun.tools.txw2.TxwOptions;
import com.sun.xml.txw2.annotation.XmlNamespace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Root of the model.
 * 
 * @author Kohsuke Kawaguchi
 */
public class NodeSet extends LinkedHashSet<WriterNode> {

    /*package*/ final TxwOptions opts;
    /*package*/ final JCodeModel codeModel;

    /**
     * Set of all the {@link Element}s that can be root.
     */
    private final Set<Element> rootElements = new HashSet<Element>();

    /** The namespace URI declared in {@link XmlNamespace}. */
    /*package*/ final String defaultNamespace;

    public NodeSet(TxwOptions opts, Leaf entry) {
        this.opts = opts;
        this.codeModel = opts.codeModel;
        addAll(entry.siblings());
        markRoot(entry.siblings(),rootElements);

        // decide what to put in @XmlNamespace
        Set<String> ns = new HashSet<String>();
        for( Element e : rootElements )
            ns.add(e.name.getNamespaceURI());

        if(ns.size()!=1 || opts.noPackageNamespace || opts._package.isUnnamed())
            defaultNamespace = null;
        else {
            defaultNamespace = ns.iterator().next();

            opts._package.annotate(XmlNamespace.class)
                .param("value",defaultNamespace);
        }
    }

    /**
     * Marks all the element children as root.
     */
    private void markRoot(Iterable<Leaf> c, Set<Element> rootElements) {
        for( Leaf l : c ) {
            if( l instanceof Element ) {
                Element e = (Element)l;
                rootElements.add(e);
                e.isRoot = true;
            }
            if( l instanceof Ref ) {
                markRoot(((Ref)l).def,rootElements);
            }
        }
    }

    private void addAll(Iterable<Leaf> c) {
        for( Leaf l : c ) {
            if(l instanceof Element)
                if(add((Element)l))
                    addAll((Element)l);
            if(l instanceof Grammar) {
                Grammar g = (Grammar)l;
                for( Define d : g.getDefinitions() )
                    add(d);
            }
            if(l instanceof Ref) {
                Ref r = (Ref)l;
                Define def = r.def;
//                if(def instanceof Grammar) {
//                    for( Define d : ((Grammar)def).getDefinitions() )
//                        if(add(d))
//                            addAll(d);
//                }
                add(def);
            }
        }
    }

    private boolean add(Define def) {
        boolean b = super.add(def);
        if(b)
            addAll(def);
        return b;
    }

    public <T extends WriterNode> Collection<T> subset(Class<T> t) {
        ArrayList<T> r = new ArrayList<T>(size());
        for( WriterNode n : this )
            if(t.isInstance(n))
                r.add((T)n);
        return r;
    }

    /**
     * Generate code
     */
    public void write(TxwOptions opts) {
        for( WriterNode n : this )
            n.prepare(this);
        for( WriterNode n : this )
            n.declare(this);
        for( WriterNode n : this )
            n.generate(this);
    }

    /*package*/ final JDefinedClass createClass(String name) {
        try {
            return opts._package._class(
                JMod.PUBLIC, NameUtil.toClassName(name), ClassType.INTERFACE );
        } catch (JClassAlreadyExistsException e) {
            for( int i=2; true; i++ ) {
                try {
                    return opts._package._class(
                        JMod.PUBLIC, NameUtil.toClassName(name+String.valueOf(i)), ClassType.INTERFACE );
                } catch (JClassAlreadyExistsException e1) {
                    ; // continue
                }
            }
        }
    }
}
