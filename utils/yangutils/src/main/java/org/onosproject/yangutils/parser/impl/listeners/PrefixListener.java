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

package org.onosproject.yangutils.parser.impl.listeners;

import org.onosproject.yangutils.datamodel.YangBelongsTo;
import org.onosproject.yangutils.datamodel.YangImport;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.parser.ParsableDataType;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 * module-header-stmts = ;; these stmts can appear in any order
 *                       [yang-version-stmt stmtsep]
 *                        namespace-stmt stmtsep
 *                        prefix-stmt stmtsep
 *
 * prefix-stmt         = prefix-keyword sep prefix-arg-str
 *                       optsep stmtend
 *
 * ANTLR grammar rule
 * module_header_statement : yang_version_stmt? namespace_stmt prefix_stmt
 *                         | yang_version_stmt? prefix_stmt namespace_stmt
 *                         | namespace_stmt yang_version_stmt? prefix_stmt
 *                         | namespace_stmt prefix_stmt yang_version_stmt?
 *                         | prefix_stmt namespace_stmt yang_version_stmt?
 *                         | prefix_stmt yang_version_stmt? namespace_stmt
 *                         ;
 * prefix_stmt : PREFIX_KEYWORD IDENTIFIER STMTEND;
 */

/**
 * Implements listener based call back function corresponding to the "prefix"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class PrefixListener {

    /**
     * Creates a new prefix listener.
     */
    private PrefixListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar
     * rule (prefix),perform validations and update the data model
     * tree.
     *
     * @param listener Listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processPrefixEntry(TreeWalkListener listener, GeneratedYangParser.PrefixStatementContext ctx) {

        // Check for stack to be non empty.
        ListenerValidation.checkStackIsNotEmpty(listener, ListenerErrorType.MISSING_HOLDER,
                                                ParsableDataType.PREFIX_DATA,
                                                String.valueOf(ctx.IDENTIFIER().getText()),
                                                ListenerErrorLocation.ENTRY);

        // Obtain the node of the stack.
        Parsable tmpNode = listener.getParsedDataStack().peek();
        switch (tmpNode.getParsableDataType()) {
        case MODULE_DATA: {
            YangModule module = (YangModule) tmpNode;
            module.setPrefix(ctx.IDENTIFIER().getText());
            break;
        }
        case IMPORT_DATA: {
            YangImport importNode = (YangImport) tmpNode;
            importNode.setPrefixId(ctx.IDENTIFIER().getText());
            break;
        }
        case BELONGS_TO_DATA: {
            YangBelongsTo belongstoNode = (YangBelongsTo) tmpNode;
            belongstoNode.setPrefix(ctx.IDENTIFIER().getText());
            break;
        }
        default:
            throw new ParserException(
                                      ListenerErrorMessageConstruction
                                              .constructListenerErrorMessage(ListenerErrorType.INVALID_HOLDER,
                                                                             ParsableDataType.PREFIX_DATA, String
                                                                                     .valueOf(ctx.IDENTIFIER()
                                                                                             .getText()),
                                                                             ListenerErrorLocation.ENTRY));
        }
    }
}