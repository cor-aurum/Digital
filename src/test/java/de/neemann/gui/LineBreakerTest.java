/*
 * Copyright (c) 2017 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.gui;

import junit.framework.TestCase;

/**
 */
public class LineBreakerTest extends TestCase {

    public void testBreakLines() throws Exception {
        assertEquals("this is a test string", new LineBreaker(60).breakLines("this \n\n is \n a   test \n\r    string"));
        assertEquals("this is a test\nstring", new LineBreaker(14).breakLines("this \n\n is \n a   test \n\r    string"));
        assertEquals("This is a test string. This\n" +
                "is a test string. This is a\n" +
                "test string.", new LineBreaker(27).breakLines("This is a test string. This is a test string. This is a test string."));
        assertEquals("this is\naWordThatIsFarToLongToFitInASingleLine\nThis is a test string", new LineBreaker(21).breakLines("this is aWordThatIsFarToLongToFitInASingleLine This is a test string"));
    }

    public void testBreakLinesLabel() throws Exception {
        assertEquals("a) This is a test string. This\n" +
                "   is a test string. This is a\n" +
                "   test string.", new LineBreaker("a)", 3, 30).breakLines("This is a test string. This is a test string. This is a test string."));
    }


    public void testBreakLinesPreserve() throws Exception {
        assertEquals("this is a\ntest string", new LineBreaker(60).preserveContainedLineBreaks().breakLines("this is a\ntest  string"));
        assertEquals("this is a\ntest string", new LineBreaker(60).preserveContainedLineBreaks().breakLines("this is a  \n   test  string"));
        assertEquals("this is a\ntest string\n", new LineBreaker(60).preserveContainedLineBreaks().breakLines("\n\nthis is a\n\ntest  string\n\n\n"));
        assertEquals("this is a\ntest string. This is\na test string.", new LineBreaker(20).preserveContainedLineBreaks().breakLines("this is a\n   test  string. This is a test string."));
    }

    public void testBreakLinesHTML() throws Exception {
        assertEquals("this is a test string", new LineBreaker(60).toHTML().breakLines("this is a\n   test  string"));
        assertEquals("<html>this is a<br>test string</html>", new LineBreaker(60).toHTML().preserveContainedLineBreaks().breakLines("this is a\n   test  string"));
    }


}
