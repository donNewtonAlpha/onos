/*
 * Copyright 2016 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.yangutils.parser.impl.parserutils;

import org.antlr.v4.runtime.ParserRuleContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Pattern;

import org.onosproject.yangutils.datamodel.YangNodeIdentifier;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.utils.YangConstructType;
import org.onosproject.yangutils.parser.exceptions.ParserException;

/**
 * It's a utility for listener.
 */
public final class ListenerUtil {
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_.-]*");
    private static final String NON_NEGATIVE_INTEGER_PATTERN = "[0-9]+";
    private static final String PLUS = "+";
    private static final String ONE = "1";
    private static final String TRUE_KEYWORD = "true";
    private static final String FALSE_KEYWORD = "false";
    private static final int IDENTIFIER_LENGTH = 64;
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String EMPTY_STRING = "";
    private static final String HYPHEN = "-";
    private static final String SLASH = "/";
    private static final String SPACE = " ";
    private static final String COLON = ":";

    /**
     * Creates a new listener util.
     */
    private ListenerUtil() {
    }

    /**
     * Removes doubles quotes and concatenates if string has plus symbol.
     *
     * @param yangStringData string from yang file
     * @return concatenated string after removing double quotes
     */
    public static String removeQuotesAndHandleConcat(String yangStringData) {

        yangStringData = yangStringData.replace("\"", EMPTY_STRING);
        String[] tmpData = yangStringData.split(Pattern.quote(PLUS));
        StringBuilder builder = new StringBuilder();
        for (String yangString : tmpData) {
            builder.append(yangString);
        }
        return builder.toString();
    }

    /**
     * Validates identifier and returns concatenated string if string contains plus symbol.
     *
     * @param identifier string from yang file
     * @param yangConstruct yang construct for creating error message
     * @param ctx yang construct's context to get the line number and character position
     * @return concatenated string after removing double quotes
     */
    public static String getValidIdentifier(String identifier, YangConstructType yangConstruct, ParserRuleContext ctx) {

        String identifierString = removeQuotesAndHandleConcat(identifier);
        ParserException parserException;

        if (identifierString.length() > IDENTIFIER_LENGTH) {
            parserException = new ParserException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + " name " + identifierString + " is " +
                    "greater than 64 characters.");
        } else if (!IDENTIFIER_PATTERN.matcher(identifierString).matches()) {
            parserException = new ParserException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + " name " + identifierString + " is not " +
                    "valid.");
        } else {
            return identifierString;
        }

        parserException.setLine(ctx.getStart().getLine());
        parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
        throw parserException;
    }

    /**
     * Validates the revision date.
     *
     * @param dateToValidate input revision date
     * @return validation result, true for success, false for failure
     */
    public static boolean isDateValid(String dateToValidate) {

        if (dateToValidate == null || !dateToValidate.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        sdf.setLenient(false);

        try {
            //if not valid, it will throw ParseException
            sdf.parse(dateToValidate);
        } catch (ParseException e) {
            return false;
        }

        return true;
    }

    /**
     * Validates YANG version.
     *
     * @param ctx version context object of the grammar rule
     * @return valid version
     */
    public static byte getValidVersion(GeneratedYangParser.YangVersionStatementContext ctx) {

        String value = removeQuotesAndHandleConcat(ctx.version().getText());
        if (!value.equals(ONE)) {
            ParserException parserException = new ParserException("YANG file error: Input version not supported");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }

        return Byte.valueOf(value);
    }

    /**
     * Validates non negative integer value.
     *
     * @param integerValue integer to be validated
     * @param yangConstruct yang construct for creating error message
     * @param ctx context object of the grammar rule
     * @return valid non negative integer value
     */
    public static int getValidNonNegativeIntegerValue(String integerValue, YangConstructType yangConstruct,
                                           ParserRuleContext ctx) {

        String value = removeQuotesAndHandleConcat(integerValue);
        if (!value.matches(NON_NEGATIVE_INTEGER_PATTERN)) {
            ParserException parserException = new ParserException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + " value " + value + " is not " +
                    "valid.");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }

        return Integer.parseInt(value);
    }

    /**
     * Validates boolean value.
     *
     * @param booleanValue value to be validated
     * @param yangConstruct yang construct for creating error message
     * @param ctx context object of the grammar rule
     * @return boolean value either true or false
     */
    public static boolean getValidBooleanValue(String booleanValue, YangConstructType yangConstruct,
                                                ParserRuleContext ctx) {

        String value = removeQuotesAndHandleConcat(booleanValue);
        if (value.equals(TRUE_KEYWORD)) {
            return true;
        } else if (value.equals(FALSE_KEYWORD)) {
            return false;
        } else {
            ParserException parserException = new ParserException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + " value " + value + " is not " +
                    "valid.");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }
    }

    /**
     * Sets current date and makes it in usable format for revision.
     *
     * @return usable current date format for revision
     */
    public static String setCurrentDateForRevision() {

        Calendar date = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String dateForRevision = ((dateFormat.format(date.getTime())).replaceAll(SLASH, HYPHEN)).replaceAll(SPACE,
                EMPTY_STRING);
        return dateForRevision;
    }

    /**
     * Checks and return valid node identifier.
     *
     * @param nodeIdentifierString string from yang file
     * @param yangConstruct yang construct for creating error message
     * @param ctx yang construct's context to get the line number and character position
     * @return valid node identifier
     */
    public static YangNodeIdentifier getValidNodeIdentifier(String nodeIdentifierString, YangConstructType
            yangConstruct, ParserRuleContext ctx) {
        String tmpIdentifierString = removeQuotesAndHandleConcat(nodeIdentifierString);
        String[] tmpData = tmpIdentifierString.split(Pattern.quote(COLON));
        if (tmpData.length == 1) {
            YangNodeIdentifier nodeIdentifier = new YangNodeIdentifier();
            nodeIdentifier.setName(getValidIdentifier(tmpData[0], yangConstruct, ctx));
            return nodeIdentifier;
        } else if (tmpData.length == 2) {
            YangNodeIdentifier nodeIdentifier = new YangNodeIdentifier();
            nodeIdentifier.setPrefix(getValidIdentifier(tmpData[0], yangConstruct, ctx));
            nodeIdentifier.setName(getValidIdentifier(tmpData[1], yangConstruct, ctx));
            return nodeIdentifier;
        } else {
            ParserException parserException = new ParserException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + " name " + nodeIdentifierString +
                    " is not valid.");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }
    }
}