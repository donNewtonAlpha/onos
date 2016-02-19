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

import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 * import-stmt         = import-keyword sep identifier-arg-str optsep
 *                       "{" stmtsep
 *                           prefix-stmt stmtsep
 *                           [revision-date-stmt stmtsep]
 *                        "}"
 * include-stmt        = include-keyword sep identifier-arg-str optsep
 *                             (";" /
 *                              "{" stmtsep
 *                                  [revision-date-stmt stmtsep]
 *                            "}")
 * revision-date-stmt = revision-date-keyword sep revision-date stmtend
 *
 * ANTLR grammar rule
 * import_stmt : IMPORT_KEYWORD IDENTIFIER LEFT_CURLY_BRACE import_stmt_body
 *               RIGHT_CURLY_BRACE;
 * import_stmt_body : prefix_stmt revision_date_stmt?;
 *
 * include_stmt : INCLUDE_KEYWORD IDENTIFIER (STMTEND | LEFT_CURLY_BRACE
 *                revision_date_stmt_body? RIGHT_CURLY_BRACE);
 *
 * revision_date_stmt : REVISION_DATE_KEYWORD DATE_ARG STMTEND;
 *
 */

/**
 * Implements listener based call back function corresponding to the "revision date"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class RevisionDateListener {

    /**
     * Creates a new revision date listener.
     */
    private RevisionDateListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar
     * rule (revision date),perform validations and update the data model
     * tree.
     *
     * @param listener Listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processRevisionDateEntry(TreeWalkListener listener,
                                                GeneratedYangParser.RevisionDateStatementContext ctx) {
        // TODO method implementation
    }
}
