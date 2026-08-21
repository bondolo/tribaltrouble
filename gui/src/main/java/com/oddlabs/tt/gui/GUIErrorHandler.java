package com.oddlabs.tt.gui;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class GUIErrorHandler implements ErrorHandler {
    private static final Logger logger = Logger.getLogger("SAXParseError");

    @Override
    public void fatalError(SAXParseException e) {
        logger.log(Level.SEVERE, "fatal line " + e.getLineNumber() + ", uri " + e.getSystemId(), e);
        // ignore fatal errors (an exception is guaranteed)
    }

    // treat validation errors as fatal
    @Override
    public void error(SAXParseException e) throws SAXParseException {
        logger.log(Level.SEVERE, "error line " + e.getLineNumber() + ", uri " + e.getSystemId(), e);
        throw e;
    }

    // dump warnings too
    @Override
    public void warning(SAXParseException err) {
        logger.log(Level.WARNING, "line " + err.getLineNumber() + ", uri " + err.getSystemId(), err);
    }
}
