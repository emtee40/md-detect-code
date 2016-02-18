import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/*
 *  Copyright (c) Microsoft. All rights reserved. Licensed under the MIT license. See full license at the bottom of this file.
 */
public class Main {

    private static final boolean VERBOSE = false;

    /**
     * Delim for md regex
     */
    public static final String MD_CODE_REGEX = "^[^`]*```.*$";

    /**
     * Our thread runner
     */
    private static ExecutorService executorService;

    /**
     * construct an array of files from string paths
     *
     * @param lines paths to files for processing
     * @return those paths, as java.io.Files
     */
    private static List<File> asFiles(final List<String> lines) {
        return new ArrayList<File>() {{
            for (String path : lines) add(new File(path));
        }};
    }

    /**
     * Convenience function to grab STDIN
     *
     * @return strings of lines from STDIN
     */
    static List<String> getSysIn() {
        InputStreamReader inputStreamReader = null;
        try {
            // initialize the stream reader
            inputStreamReader = new InputStreamReader(System.in);
            // load it into a buffer
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            // init the out list
            List<String> input = new ArrayList<>();

            String line;
            while (null != ( // iterate lines
                    line = bufferedReader.readLine()
            )) {
                input.add(line);
            }

            return input;
        } catch (IOException e) {
            // bail
            throw new RuntimeException(e.getCause());
        } finally {
            if (null != inputStreamReader) {
                IOUtils.closeQuietly(inputStreamReader);
            }
        }
    }

    /**
     * Set up a ThreadPoolExecutor to process files in parallel
     *
     * @param capacity pool size
     */
    private static void initExecutor(int capacity) {
        executorService =
                new ThreadPoolExecutor(
                        1, // min pool size
                        Runtime.getRuntime().availableProcessors() * 4, // max pool shouldn't exceed cores
                        2, // how many * how long?
                        TimeUnit.SECONDS, // unit of time
                        new ArrayBlockingQueue<Runnable>(
                                capacity // initialize the list with a finite capacity
                        )
                );
    }

    /**
     * Create instances of the process function per file
     *
     * @param file the file to process
     * @return nothing
     */
    private static Callable<Void> newProcessFileAction(final File file) {
        return new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // read the lines of the 'in'-file
                List<String> inLines = FileUtils.readLines(file);

                // queue up the output
                List<String> outLines = new ArrayList<>();

                // flip/flop for blocks
                boolean inBlock = false;

                // line counter
                int ii = 0;
                for (String line : inLines) {
                    if (line.matches(MD_CODE_REGEX)) { // is the line the start of a code block?
                        line = line.trim(); // clean it up

                        // flip the block toggle
                        inBlock = !inBlock;

                        if (inBlock && !isAlreadyAnnotated(line)) {
                            // there needs to be a kind of 'look-ahead' here to figure out what tag to use...
                            // lets do a best effort to figure our what it is
                            // how about we grab the next few lines and so some casual checks
                            List<String> codeBlockLines = new ArrayList<>();

                            // grab the rest of this code block
                            grabCodeBlock(inLines, ii, codeBlockLines);

                            // grab the tag to use
                            String tag = supposeLang(codeBlockLines);

                            // then use it
                            line = line.replace("```", "```" + tag);
                        }
                    }
                    outLines.add(line);
                    ii++; // increment the lines counter
                }

                // write the outlines over the old file....
                FileUtils.writeLines(file, outLines);
                return null;
            }
        };
    }

    private static void grabCodeBlock(List<String> inLines, int ii, List<String> codeBlockLines) {
        for (int jj = ii + 1; jj < inLines.size(); jj++) {
            if (!inLines.get(jj).matches(MD_CODE_REGEX)) {
                codeBlockLines.add(inLines.get(jj));
                continue;
            }
            break;
        }
    }

    private static String[] langCodes = new String[]{
            "C#",
            "c#",
            "java",
            "html",
            "HTML",
            "VB.net",
            "vb",
            "vba",
            "xml",
            "XML",
            "sql",
            "SQL"
    };

    /**
     * Checks if a codeblock is already annotated
     *
     * @param line the line to inspect
     * @return true if the line is already annotated, false otherwise
     */
    static boolean isAlreadyAnnotated(String line) {
        for (String langCode : langCodes) {
            if (line.contains(langCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Langs Im expecting to encounter
     */
    private enum Lang {
        C_SHARP(
                0,
                new String[]{
                        "c#"
                },
                new String[]{
                        "abstract",
                        "as",
                        "base",
                        "bool",
                        "break",
                        "byte",
                        "case",
                        "catch",
                        "char",
                        "checked",
                        "class",
                        "const",
                        "continue",
                        "decimal",
                        "default",
                        "delegate",
                        "do",
                        "double",
                        "else",
                        "enum",
                        "event",
                        "explicit",
                        "extern",
                        "false",
                        "finally",
                        "fixed",
                        "float",
                        "for",
                        "foreach",
                        "goto",
                        "if",
                        "implicit",
                        "in",
                        "in (generic modifier)",
                        "int",
                        "interface",
                        "internal",
                        "is",
                        "lock",
                        "long",
                        "namespace",
                        "new",
                        "null",
                        "object",
                        "operator",
                        "out",
                        "out (generic modifier)",
                        "override",
                        "params",
                        "private",
                        "protected",
                        "public",
                        "readonly",
                        "ref",
                        "return",
                        "sbyte",
                        "sealed",
                        "short",
                        "sizeof",
                        "stackalloc",
                        "static",
                        "string",
                        "struct",
                        "switch",
                        "this",
                        "throw",
                        "true",
                        "try",
                        "typeof",
                        "uint",
                        "ulong",
                        "unchecked",
                        "unsafe",
                        "ushort",
                        "using",
                        "virtual",
                        "void",
                        "volatile",
                        "while"
                }
        ),

        HTML(
                1,
                new String[]{
                        "html"
                },
                new String[]{
                        "<html>",
                        "<p>",
                        "<span>",
                        "<div>",
                        "<body>",
                        "<h1>",
                        "<h2>",
                        "<h3>",
                        "<h4>",
                        "<h5>",
                        "<h6>",
                        "<td>",
                        "<head>",
                        "<title>",
                        "<br />"
                }
        ),

        VB(
                2,
                new String[]{
                        "vb",
                        "VB.net"
                },
                new String[]{
                        "AddHandler",
                        "AddressOf",
                        "Alias",
                        "And",
                        "AndAlso",
                        "As",
                        "Boolean",
                        "ByRef",
                        "Byte",
                        "ByVal",
                        "Call",
                        "Case",
                        "Catch",
                        "CBool",
                        "CByte",
                        "CChar",
                        "CDate",
                        "CDec",
                        "CDbl",
                        "Char",
                        "CInt",
                        "Class",
                        "CLng",
                        "CObj",
                        "Const",
                        "Continue",
                        "CSByte",
                        "CShort",
                        "CSng",
                        "CStr",
                        "CType",
                        "CUInt",
                        "CULng",
                        "CUShort",
                        "Date",
                        "Decimal",
                        "Declare",
                        "Default",
                        "Delegate",
                        "Dim",
                        "DirectCast",
                        "Do",
                        "Double",
                        "Each",
                        "Else",
                        "ElseIf",
                        "End",
                        "EndIf",
                        "Enum",
                        "Erase",
                        "Error",
                        "Event",
                        "Exit",
                        "False",
                        "Finally",
                        "For",
                        "Friend",
                        "Function",
                        "Get",
                        "GetType",
                        "GetXMLNamespace",
                        "Global",
                        "GoSub",
                        "GoTo",
                        "Handles",
                        "If",
                        "If()",
                        "Implements",
                        "Imports",
                        "In",
                        "Inherits",
                        "Integer",
                        "Interface",
                        "Is",
                        "IsNot",
                        "Let",
                        "Lib",
                        "Like",
                        "Long",
                        "Loop",
                        "Me",
                        "Mod",
                        "Module",
                        "MustInherit",
                        "MustOverride",
                        "MyBase",
                        "MyClass",
                        "Namespace",
                        "Narrowing",
                        "New",
                        "Next",
                        "Not",
                        "Nothing",
                        "NotInheritable",
                        "NotOverridable",
                        "Object",
                        "Of",
                        "On",
                        "Operator",
                        "Option",
                        "Optional",
                        "Or",
                        "OrElse",
                        "Overloads",
                        "Overridable",
                        "Overrides",
                        "ParamArray",
                        "Partial",
                        "Private",
                        "Property",
                        "Protected",
                        "Public",
                        "RaiseEvent",
                        "ReadOnly",
                        "ReDim",
                        "REM",
                        "RemoveHandler",
                        "Resume",
                        "Return",
                        "SByte",
                        "Select",
                        "Set",
                        "Shadows",
                        "Shared",
                        "Short",
                        "Single",
                        "Static",
                        "Step",
                        "Stop",
                        "String",
                        "Structure",
                        "Sub",
                        "SyncLock",
                        "Then",
                        "Throw",
                        "To",
                        "True",
                        "Try",
                        "TryCast",
                        "TypeOf",
                        "Variant",
                        "Wend",
                        "UInteger",
                        "ULong",
                        "UShort",
                        "Using",
                        "When",
                        "While",
                        "Widening",
                        "With",
                        "WithEvents",
                        "WriteOnly",
                        "Xor",
                        "#Const",
                        "#Else",
                        "#ElseIf",
                        "#End",
                        "#If",
                }
        ),

        XML(
                3,
                new String[]{
                        "XML"
                },
                new String[]{}
        ),

        SQL(
                4,
                new String[]{
                        "sql"
                },
                new String[]{
                        "ABSOLUTE",
                        "ACTION",
                        "ADA",
                        "ADD",
                        "ADMIN",
                        "AFTER",
                        "AGGREGATE",
                        "ALIAS",
                        "ALL",
                        "ALLOCATE",
                        "ALTER",
                        "AND",
                        "ANY",
                        "ARE",
                        "ARRAY",
                        "AS",
                        "ASC",
                        "ASSERTION",
                        "AT",
                        "AUTHORIZATION",
                        "AVG",
                        "BACKUP",
                        "BEFORE",
                        "BEGIN",
                        "BETWEEN",
                        "BINARY",
                        "BIT",
                        "BIT_LENGTH",
                        "BLOB",
                        "BOOLEAN",
                        "BOTH",
                        "BREADTH",
                        "BREAK",
                        "BROWSE",
                        "BULK",
                        "BY",
                        "CALL",
                        "CASCADE",
                        "CASCADED",
                        "CASE",
                        "CAST",
                        "CATALOG",
                        "CHAR",
                        "CHARACTER",
                        "CHARACTER_LENGTH",
                        "CHAR_LENGTH",
                        "CHECK",
                        "CHECKPOINT",
                        "CLASS",
                        "CLOB",
                        "CLOSE",
                        "CLUSTERED",
                        "COALESCE",
                        "COLLATE",
                        "COLLATION",
                        "COLUMN",
                        "COMMIT",
                        "COMPLETION",
                        "COMPUTE",
                        "CONNECT",
                        "CONNECTION",
                        "CONSTRAINT",
                        "CONSTRAINTS",
                        "CONSTRUCTOR",
                        "CONTAINS",
                        "CONTAINSTABLE",
                        "CONTINUE",
                        "CONVERT",
                        "CORRESPONDING",
                        "COUNT",
                        "CREATE",
                        "CROSS",
                        "CUBE",
                        "CURRENT",
                        "CURRENT_DATE",
                        "CURRENT_PATH",
                        "CURRENT_ROLE",
                        "CURRENT_TIME",
                        "CURRENT_TIMESTAMP",
                        "CURRENT_USER",
                        "CURSOR",
                        "CYCLE",
                        "DATA",
                        "DATABASE",
                        "DATE",
                        "DAY",
                        "DBCC",
                        "DEALLOCATE",
                        "DEC",
                        "DECIMAL",
                        "DECLARE",
                        "DEFAULT",
                        "DEFERRABLE",
                        "DEFERRED",
                        "DELETE",
                        "DENY",
                        "DEPTH",
                        "DEREF",
                        "DESC",
                        "DESCRIBE",
                        "DESCRIPTOR",
                        "DESTROY",
                        "DESTRUCTOR",
                        "DETERMINISTIC",
                        "DIAGNOSTICS",
                        "DICTIONARY",
                        "DISCONNECT",
                        "DISK",
                        "DISTINCT",
                        "DISTRIBUTED",
                        "DOMAIN",
                        "DOUBLE",
                        "DROP",
                        "DUMMY",
                        "DUMP",
                        "DYNAMIC",
                        "EACH",
                        "ELSE",
                        "END",
                        "END-EXEC",
                        "EQUALS",
                        "ERRLVL",
                        "ESCAPE",
                        "EVERY",
                        "EXCEPT",
                        "EXCEPTION",
                        "EXEC",
                        "EXECUTE",
                        "EXISTS",
                        "EXIT",
                        "EXTERNAL",
                        "EXTRACT",
                        "FALSE",
                        "FETCH",
                        "FILE",
                        "FILLFACTOR",
                        "FIRST",
                        "FLOAT",
                        "FOR",
                        "FOREIGN",
                        "FORTRAN",
                        "FOUND",
                        "FREE",
                        "FREETEXT",
                        "FREETEXTTABLE",
                        "FROM",
                        "FULL",
                        "FUNCTION",
                        "GENERAL",
                        "GET",
                        "GLOBAL",
                        "GO",
                        "GOTO",
                        "GRANT",
                        "GROUP",
                        "GROUPING",
                        "HAVING",
                        "HOLDLOCK",
                        "HOST",
                        "HOUR",
                        "IDENTITY",
                        "IDENTITYCOL",
                        "IDENTITY_INSERT",
                        "IF",
                        "IGNORE",
                        "IMMEDIATE",
                        "IN",
                        "INCLUDE",
                        "INDEX",
                        "INDICATOR",
                        "INITIALIZE",
                        "INITIALLY",
                        "INNER",
                        "INOUT",
                        "INPUT",
                        "INSENSITIVE",
                        "INSERT",
                        "INT",
                        "INTEGER",
                        "INTERSECT",
                        "INTERVAL",
                        "INTO",
                        "IS",
                        "ISOLATION",
                        "ITERATE",
                        "JOIN",
                        "KEY",
                        "KILL",
                        "LANGUAGE",
                        "LARGE",
                        "LAST",
                        "LATERAL",
                        "LEADING",
                        "LEFT",
                        "LESS",
                        "LEVEL",
                        "LIKE",
                        "LIMIT",
                        "LINENO",
                        "LOAD",
                        "LOCAL",
                        "LOCALTIME",
                        "LOCALTIMESTAMP",
                        "LOCATOR",
                        "LOWER",
                        "MAP",
                        "MATCH",
                        "MAX",
                        "MIN",
                        "MINUTE",
                        "MODIFIES",
                        "MODIFY",
                        "MODULE",
                        "MONTH",
                        "NAMES",
                        "NATIONAL",
                        "NATURAL",
                        "NCHAR",
                        "NCLOB",
                        "NEW",
                        "NEXT",
                        "NO",
                        "NOCHECK",
                        "NONCLUSTERED",
                        "NONE",
                        "NOT",
                        "NULL",
                        "NULLIF",
                        "NUMERIC",
                        "OBJECT",
                        "OCTET_LENGTH",
                        "OF",
                        "OFF",
                        "OFFSETS",
                        "OLD",
                        "ON",
                        "ONLY",
                        "OPEN",
                        "OPENDATASOURCE",
                        "OPENQUERY",
                        "OPENROWSET",
                        "OPENXML",
                        "OPERATION",
                        "OPTION",
                        "OR",
                        "ORDER",
                        "ORDINALITY",
                        "OUT",
                        "OUTER",
                        "OUTPUT",
                        "OVER",
                        "OVERLAPS",
                        "PAD",
                        "PARAMETER",
                        "PARAMETERS",
                        "PARTIAL",
                        "PASCAL",
                        "PATH",
                        "PERCENT",
                        "PLAN",
                        "POSITION",
                        "POSTFIX",
                        "PRECISION",
                        "PREFIX",
                        "PREORDER",
                        "PREPARE",
                        "PRESERVE",
                        "PRIMARY",
                        "PRINT",
                        "PRIOR",
                        "PRIVILEGES",
                        "PROC",
                        "PROCEDURE",
                        "PUBLIC",
                        "RAISERROR",
                        "READ",
                        "READS",
                        "READTEXT",
                        "REAL",
                        "RECONFIGURE",
                        "RECURSIVE",
                        "REF",
                        "REFERENCES",
                        "REFERENCING",
                        "RELATIVE",
                        "REPLICATION",
                        "RESTORE",
                        "RESTRICT",
                        "RESULT",
                        "RETURN",
                        "RETURNS",
                        "REVOKE",
                        "RIGHT",
                        "ROLE",
                        "ROLLBACK",
                        "ROLLUP",
                        "ROUTINE",
                        "ROW",
                        "ROWCOUNT",
                        "ROWGUIDCOL",
                        "ROWS",
                        "RULE",
                        "SAVE",
                        "SAVEPOINT",
                        "SCHEMA",
                        "SCOPE",
                        "SCROLL",
                        "SEARCH",
                        "SECOND",
                        "SECTION",
                        "SELECT",
                        "SEQUENCE",
                        "SESSION",
                        "SESSION_USER",
                        "SET",
                        "SETS",
                        "SETUSER",
                        "SHUTDOWN",
                        "SIZE",
                        "SMALLINT",
                        "SOME",
                        "SPACE",
                        "SPECIFIC",
                        "SPECIFICTYPE",
                        "SQL",
                        "SQLCA",
                        "SQLCODE",
                        "SQLERROR",
                        "SQLEXCEPTION",
                        "SQLSTATE",
                        "SQLWARNING",
                        "START",
                        "STATE",
                        "STATEMENT",
                        "STATIC",
                        "STATISTICS",
                        "STRUCTURE",
                        "SUBSTRING",
                        "SUM",
                        "SYSTEM_USER",
                        "TABLE",
                        "TEMPORARY",
                        "TERMINATE",
                        "TEXTSIZE",
                        "THAN",
                        "THEN",
                        "TIME",
                        "TIMESTAMP",
                        "TIMEZONE_HOUR",
                        "TIMEZONE_MINUTE",
                        "TO",
                        "TOP",
                        "TRAILING",
                        "TRAN",
                        "TRANSACTION",
                        "TRANSLATE",
                        "TRANSLATION",
                        "TREAT",
                        "TRIGGER",
                        "TRIM",
                        "TRUE",
                        "TRUNCATE",
                        "TSEQUAL",
                        "UNDER",
                        "UNION",
                        "UNIQUE",
                        "UNKNOWN",
                        "UNNEST",
                        "UPDATE",
                        "UPDATETEXT",
                        "UPPER",
                        "USAGE",
                        "USE",
                        "USER",
                        "USING",
                        "VALUE",
                        "VALUES",
                        "VARCHAR",
                        "VARIABLE",
                        "VARYING",
                        "VIEW",
                        "WAITFOR",
                        "WHEN",
                        "WHENEVER",
                        "WHERE",
                        "WHILE",
                        "WITH",
                        "WITHOUT",
                        "WORK",
                        "WRITE",
                        "WRITETEXT",
                        "YEAR",
                        "ZONE"
                }
        ),

        UNKNOWN(
                5,
                new String[]{
                        ""
                },
                new String[]{}
        );

        /**
         * pos in score counter
         */
        final int index;

        /**
         * md abbrev
         */
        final String[] abbrev;

        /**
         * keywords
         */
        final String[] tokens;

        Lang(int index, String[] abbrev, String[] tokens) {
            this.index = index;
            this.abbrev = abbrev;
            this.tokens = tokens;
        }

    }

    private static String supposeLang(List<String> codeBlockLines) {
        // calculate a 'score' which naively tries to determine language based on keywords
        // let's log out the code block...
        String codeBlock = null;
        for (String s : codeBlockLines) {
            codeBlock += s;
        }
        String[] tokens = codeBlock.split(" ");

        final int[] scores = new int[Lang.values().length];

        for (String token : tokens) {

            if (looksLike(token, Lang.C_SHARP)) {
                scores[Lang.C_SHARP.index]++;
            }

            if (looksLike(token, Lang.HTML)) {
                scores[Lang.HTML.index]++;
            }

            if (looksLike(token, Lang.VB)) {
                scores[Lang.VB.index]++;
            }

            if (looksLike(token, Lang.SQL)) {
                scores[Lang.SQL.index]++;
            }

// TODO add XML support
//            if (looksLike(token, Lang.XML)) {
//                scores[Lang.XML.index]++;
//            }

        }

        // total the scores & pick
        int bestIndex = Lang.UNKNOWN.index;

        int largest = 0;

        boolean uncertain = true;

        for (int ii = 0; ii < scores.length; ii++) {
            if (scores[ii] > largest) {
                largest = scores[ii];
                bestIndex = ii;
                uncertain = false;
            } else if (scores[ii] == largest) {
                // we have a tie - we're uncertain
                uncertain = true;
            }
        }

        if (VERBOSE) {
            System.out.println("Array contents [0] = " + scores[0]);
            System.out.println("Array contents [1] = " + scores[1]);
            System.out.println("Array contents [2] = " + scores[2]);
            System.out.println("Array contents [3] = " + scores[3]);
        }

        if (!uncertain // not uncertain
                && null != prefer // and we should prefer one
                && bestIndex == prefer.index) { // and it matches this bestIndex

            // use this one - NOOP

        } else { // dont
            uncertain = true;
        }

        String appendValue = Lang.values()[uncertain ? Lang.UNKNOWN.index : bestIndex].abbrev[0];

        if (bestIndex != Lang.UNKNOWN.index && VERBOSE)
            System.out.println("Appending tag: " + appendValue);


        return appendValue;
    }

    private static boolean looksLike(String token, Lang lang) {
        return Arrays.asList(lang.tokens).contains(token);
    }

    private static Lang prefer = null; // null preference indicates 'all'

    public static void main(String[] args) throws InterruptedException {
        if (null != args && args.length > 0 && null != args[0]) {
            System.out.println("Parsing lang: " + args[0]);
            try {
                prefer = Lang.valueOf(args[0]);
            } catch (IllegalArgumentException e) {
                System.err.println("Lang switch must be one of:");
                for (Lang lang : Lang.values()) {
                    System.err.println("\t" + lang.name());
                }
                System.exit(1);
            }
        }

        List<File> files = asFiles(getSysIn());

        System.out.println("Processing " + files.size() + " files");

        if (files.size() < 1) System.exit(0); // quit if nothing to do

        initExecutor(files.size());

        for (File file : files) executorService.submit(newProcessFileAction(file));

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);
    }


}
// *********************************************************
// Copyright (c) Microsoft Corporation
// All rights reserved.
//
// MIT License:
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
// *********************************************************
