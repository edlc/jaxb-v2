/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.tools.jxc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.sun.tools.jxc.gen.config.NGCCRuntime;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**
 * Controls the  validating and converting  of values obtained
 * from the config file.
 * 
 * @author
 *     Bhakti Mehta (bhakti.mehta@sun.com)
 */
public final class NGCCRuntimeEx extends NGCCRuntime {
    /**
     * All the errors shall be sent to this object.
     */
    private final ErrorHandler errorHandler;

    public NGCCRuntimeEx(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     *  This will check if the baseDir provided by the user
     *  in the config file exists. If not it throws an error
     * @param baseDir
     *    The baseDir attribute passed by the user in the xml config file as a path
     * @return
     *     The file representation of the path name
     */
    public File getBaseDir(String baseDir) throws SAXException {
        File dir = new File(baseDir);
        if (dir.exists()) {
            return dir;
        } else {
            SAXParseException e = new SAXParseException(
                                Messages.BASEDIR_DOESNT_EXIST.format(dir.getAbsolutePath()),
                                getLocator());
            errorHandler.error(e);
            throw e;    // we can't recover from this error
        }
    }

    /**
     * This takes the include list provided by the user in the config file
     * It converts the user values to {@link Pattern}
     * @param includeContent
     *        The include list specified by the user
     * @return
     *        A list of regular expression patterns {@link Pattern}
     */
    public List<Pattern> getIncludePatterns(List<String> includeContent ) {
        List<Pattern> includeRegexList = new ArrayList<Pattern>();
        for (String includes : includeContent) {
            String regex = convertToRegex(includes);
            Pattern pattern = Pattern.compile(regex);
            includeRegexList.add(pattern);
        }
        return includeRegexList;
    }


    /**
     * This takes the exclude list provided by the user in the config file
     * It converts the user values to {@link Pattern}
     * @param excludeContent
     *        The exclude list specified by the user
     * @return
     *        A list of regular expression patterns {@link Pattern}
     */
    public List getExcludePatterns(List<String> excludeContent ) {
        List<Pattern> excludeRegexList = new ArrayList<Pattern>();
        for (String excludes : excludeContent) {
            String regex = convertToRegex(excludes);
            Pattern pattern = Pattern.compile(regex);
            excludeRegexList.add(pattern);
        }
        return excludeRegexList;
    }


    /**
     * This will tokenize the pattern and convert it into a regular expression
     * @param pattern
     */
    private String convertToRegex(String pattern) {
        StringBuilder regex = new StringBuilder();
        char nc = ' ';
        if (pattern.length() >0 ) {

            for ( int i = 0 ; i < pattern.length(); i ++ ) {
                char c = pattern.charAt(i);
                nc = ' ';
                if ((i +1) != pattern.length()) {
                    nc = pattern.charAt(i +1);
                }
                //escape single '.'
                if (c == '.' &&  nc != '.'){
                    regex.append('\\');
                    regex.append('.');
                    //do not allow patterns like a..b
                } else if (c == '.'){
                    continue;
                    // "**" gets replaced by ".*"
                } else if ((c=='*') && (nc == '*')) {
                    regex.append(".*");
                    break;
                    //'*' replaced by anything but '.' i.e [^\\.]+
                } else if (c=='*') {
                    regex.append("[^\\.]+");
                    continue;
                    //'?' replaced by anything but '.' i.e [^\\.]
                } else if (c=='?') {
                    regex.append("[^\\.]");
                    //else leave the chars as they occur in the pattern
                } else
                    regex.append(c);
            }

        }

        return regex.toString();
    }

    protected void unexpectedX(String token) throws SAXException {
        errorHandler.error(
            new SAXParseException(Messages.UNEXPECTED_NGCC_TOKEN.format(
                token, getLocator().getLineNumber(), getLocator().getColumnNumber()),
                getLocator()));
    }
}
