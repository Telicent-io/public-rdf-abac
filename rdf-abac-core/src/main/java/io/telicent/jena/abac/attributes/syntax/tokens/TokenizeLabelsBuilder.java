/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.telicent.jena.abac.attributes.syntax.tokens;

import static org.apache.jena.riot.SysRIOT.fmtMessage;

import java.io.InputStream;
import java.io.Reader;

import io.telicent.jena.abac.ABAC;
import io.telicent.jena.abac.attributes.AttributeSyntaxError;
import org.apache.jena.atlas.io.PeekReader;
import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.riot.system.ErrorHandler;
import org.slf4j.Logger;

/** Builder for TokenizerLabels */
public class TokenizeLabelsBuilder {

    // One of these.
    private PeekReader   peekReader   = null;
    private InputStream  input        = null;
    private Reader       reader       = null;
    private String       string       = null;

    private boolean      lineMode     = false;
    private boolean      utf8         = true;
    private ErrorHandler errorHandler = null;

    TokenizeLabelsBuilder() {}

    private void clearInput() {
        this.peekReader = null;
        this.input = null;
        this.reader = null;
        this.string = null;
    }

    public TokenizeLabelsBuilder source(InputStream input) {
        clearInput();
        this.input = input;
        return this;
    }

    public TokenizeLabelsBuilder source(Reader reader) {
        clearInput();
        this.reader = reader;
        return this;
    }

    public TokenizeLabelsBuilder source(PeekReader peekReader) {
        clearInput();
        this.peekReader = peekReader;
        return this;
    }

    public TokenizeLabelsBuilder fromString(String string) {
        clearInput();
        this.string = string;
        return this;
    }

    public TokenizeLabelsBuilder lineMode(boolean lineMode) {
        this.lineMode = lineMode;
        return this;
    }

    public TokenizeLabelsBuilder asciiOnly(boolean asciiOnly) {
        this.utf8 = !asciiOnly;
        return this;
    }

    public TokenizeLabelsBuilder errorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    private static int countNulls(Object ... objs) {
        int x = 0;
        for ( Object obj : objs )
            if ( obj == null )
                x++;
        return x;
    }

    private static int countNotNulls(Object ... objs) {
        int x = 0;
        for ( Object obj : objs )
            if ( obj != null )
                x++;
        return x;
    }

    /** Log warnings, throws exceptions for errors */
    private static class ErrorHandlerLabelsParse implements ErrorHandler {
        private Logger log;

        public ErrorHandlerLabelsParse(Logger log) {
            this.log = log;
        }

        @Override
        public void error(String message, long line, long col)
        { throw new AttributeSyntaxError(fmtMessage(message, line, col)); }

        @Override
        public void warning(String message, long line, long col) {
            log.warn(fmtMessage(message, line, col));
        }

        @Override
        public void fatal(String message, long line, long col)
        { throw new AttributeSyntaxError(fmtMessage(message, line, col)); }
    }


    private static Logger LOG = ABAC.AzLOG;
    // Default - errors are exceptions, warning are logged.
    private static ErrorHandler errorHandlerDft() {
        return new ErrorHandlerLabelsParse(LOG);
    }

    public Tokenizer build() {
        ErrorHandler errHandler = (errorHandler != null) ? errorHandler : errorHandlerDft();
        int x = countNotNulls(peekReader, input, reader, string);
        if ( x > 1 )
            throw new InternalErrorException("Too many data sources");
        PeekReader pr;
        if ( input != null ) {
            pr = utf8 ? PeekReader.makeUTF8(input) : PeekReader.makeASCII(input);
        } else if ( string != null ) {
            pr = PeekReader.readString(string);
        } else if ( reader != null ) {
            pr = PeekReader.make(reader);
        } else if ( peekReader != null ) {
            pr = peekReader;
        } else {
            throw new IllegalStateException("No data source");
        }

        return TokenizerABAC.internal(pr, lineMode, errHandler);
    }
}
