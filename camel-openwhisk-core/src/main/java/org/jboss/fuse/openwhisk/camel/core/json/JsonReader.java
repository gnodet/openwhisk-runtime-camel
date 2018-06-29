/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.fuse.openwhisk.camel.core.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class JsonReader {

    //
    // Implementation
    //

    protected final Reader reader;
    protected final StringBuilder recorder;
    protected final Deque<Object> stack = new ArrayDeque<>();
    protected int current;
    protected int line = 1;
    protected int column;

    protected JsonReader(Reader reader) {
        this.reader = reader;
        recorder = new StringBuilder();
    }

    public static Object read(Reader reader) throws IOException {
        return new JsonReader(reader).parse();
    }

    public static Object read(InputStream is) throws IOException {
        return new JsonReader(new InputStreamReader(is)).parse();
    }

    protected Object parse() throws IOException {
        read();
        skipWhiteSpace();
        Object result = readValue();
        skipWhiteSpace();
        if (!endOfText()) {
            throw error("Unexpected character");
        }
        return result;
    }

    protected Object readValue() throws IOException {
        switch (current) {
        case 'n':
            return readNull();
        case 't':
            return readTrue();
        case 'f':
            return readFalse();
        case '"':
            return readString();
        case '[':
            return readArray();
        case '{':
            return readObject();
        case '-':
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            return readNumber();
        default:
            throw expected("value");
        }
    }

    protected Collection<?> readArray() throws IOException {
        read();
        Collection<Object> array = new ArrayList<>();
        stack.push(array);
        skipWhiteSpace();
        if (readChar(']')) {
            return array;
        }
        do {
            skipWhiteSpace();
            array.add(readValue());
            skipWhiteSpace();
        } while (readChar(','));
        if (!readChar(']')) {
            throw expected("',' or ']'");
        }
        stack.pop();
        return array;
    }

    protected Map<String, Object> readObject() throws IOException {
        read();
        Map<String, Object> object = new HashMap<>();
        stack.push(object);
        skipWhiteSpace();
        if (readChar('}')) {
            return object;
        }
        do {
            skipWhiteSpace();
            String name = readName();
            stack.push(name);
            skipWhiteSpace();
            if (!readChar(':')) {
                throw expected("':'");
            }
            skipWhiteSpace();
            object.put(name, readValue());
            stack.pop();
            skipWhiteSpace();
        } while (readChar(','));
        if (!readChar('}')) {
            throw expected("',' or '}'");
        }
        stack.pop();
        return object;
    }

    protected Object readNull() throws IOException {
        read();
        readRequiredChar('u');
        readRequiredChar('l');
        readRequiredChar('l');
        return null;
    }

    protected Boolean readTrue() throws IOException {
        read();
        readRequiredChar('r');
        readRequiredChar('u');
        readRequiredChar('e');
        return Boolean.TRUE;
    }

    protected Boolean readFalse() throws IOException {
        read();
        readRequiredChar('a');
        readRequiredChar('l');
        readRequiredChar('s');
        readRequiredChar('e');
        return Boolean.FALSE;
    }

    protected void readRequiredChar(char ch) throws IOException {
        if (!readChar(ch)) {
            throw expected("'" + ch + "'");
        }
    }

    protected String readString() throws IOException {
        read();
        recorder.setLength(0);
        while (current != '"') {
            if (current == '\\') {
                readEscape();
            } else if (current < 0x20) {
                throw expected("valid string character");
            } else {
                recorder.append((char) current);
                read();
            }
        }
        read();
        return recorder.toString();
    }

    protected void readEscape() throws IOException {
        read();
        switch (current) {
        case '"':
        case '/':
        case '\\':
            recorder.append((char) current);
            break;
        case 'b':
            recorder.append('\b');
            break;
        case 'f':
            recorder.append('\f');
            break;
        case 'n':
            recorder.append('\n');
            break;
        case 'r':
            recorder.append('\r');
            break;
        case 't':
            recorder.append('\t');
            break;
        case 'u':
            char[] hexChars = new char[4];
            for (int i = 0; i < 4; i++) {
                read();
                if (!isHexDigit(current)) {
                    throw expected("hexadecimal digit");
                }
                hexChars[i] = (char) current;
            }
            recorder.append((char) Integer.parseInt(String.valueOf(hexChars), 16));
            break;
        default:
            throw expected("valid escape sequence");
        }
        read();
    }

    protected Number readNumber() throws IOException {
        recorder.setLength(0);
        readAndAppendChar('-');
        int firstDigit = current;
        if (!readAndAppendDigit()) {
            throw expected("digit");
        }
        if (firstDigit != '0') {
            while (readAndAppendDigit()) {
                // Do nothing
            }
        }
        readFraction();
        readExponent();
        return Double.parseDouble(recorder.toString());
    }

    protected boolean readFraction() throws IOException {
        if (!readAndAppendChar('.')) {
            return false;
        }
        if (!readAndAppendDigit()) {
            throw expected("digit");
        }
        while (readAndAppendDigit()) {
            // Do nothing
        }
        return true;
    }

    protected boolean readExponent() throws IOException {
        if (!readAndAppendChar('e') && !readAndAppendChar('E')) {
            return false;
        }
        if (!readAndAppendChar('+')) {
            readAndAppendChar('-');
        }
        if (!readAndAppendDigit()) {
            throw expected("digit");
        }
        while (readAndAppendDigit()) {
            // Do nothing
        }
        return true;
    }

    protected String readName() throws IOException {
        if (current != '"') {
            throw expected("name");
        }
        readString();
        return recorder.toString();
    }

    protected boolean readAndAppendChar(char ch) throws IOException {
        if (current != ch) {
            return false;
        }
        recorder.append(ch);
        read();
        return true;
    }

    protected boolean readChar(char ch) throws IOException {
        if (current != ch) {
            return false;
        }
        read();
        return true;
    }

    protected boolean readAndAppendDigit() throws IOException {
        if (!isDigit(current)) {
            return false;
        }
        recorder.append((char) current);
        read();
        return true;
    }

    protected void skipWhiteSpace() throws IOException {
        while (isWhiteSpace(current) && !endOfText()) {
            read();
        }
    }

    protected void read() throws IOException {
        if (endOfText()) {
            throw error("Unexpected end of input");
        }
        column++;
        if (current == '\n') {
            line++;
            column = 0;
        }
        current = reader.read();
    }

    protected boolean endOfText() {
        return current == -1;
    }

    protected IOException expected(String expected) {
        if (endOfText()) {
            return error("Unexpected end of input");
        }
        return error("Expected " + expected);
    }

    protected IOException error(String message) {
        return new IOException(message + " at " + line + ":" + column);
    }

    protected static boolean isWhiteSpace(int ch) {
        switch (ch) {
            case ' ':
            case '\t':
            case '\n':
            case '\r':
                return true;
            default:
                return false;
        }
    }

    protected static boolean isDigit(int ch) {
        switch (ch) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return true;
            default:
                return false;
        }
    }

    protected static boolean isHexDigit(int ch) {
        switch (ch) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
                return true;
            default:
                return false;
        }
    }

}
