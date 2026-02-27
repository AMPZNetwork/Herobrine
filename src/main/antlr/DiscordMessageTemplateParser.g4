parser grammar DiscordMessageTemplateParser;

options { tokenVocab = DiscordMessageTemplateLexer; }

operator_unary
    : EXCLAMATION #opLogicNot
    | TILDE #opBinaryNot
    | MINUS #opNegate
;
operator_binary
    : PLUS #opPlus
    | MINUS #opMinus
    | ASTERISK #opMultiply
    | SLASH #opDivide
    | PERCENT #opModulo
    | BREAK BREAK #opLogicOr
    | AMPERSAND AMPERSAND #opLogicAnd
    | BREAK #opBinaryOr
    | AMPERSAND #opBinaryAnd
    | QUESTION QUESTION #opNonNullElse
;

reference_chain
    : WORD #refChainBase
    | reference_chain QUESTION? DOT WORD #refChainProperty
    | reference_chain QUESTION? DOT WORD arg_list #refChainFunction
;

property_initializer: reference_chain EQUALS expression;
property_list: property_initializer (COMMA property_initializer)*;
initializer: ACCOLADE_L property_list? ACCOLADE_R;

expression
    : NULL #exprNull
    | LITERAL_STRING #exprString
    | LITERAL_NUMBER #exprNumber
    | LITERAL_BOOLEAN #exprBoolean
    | PARENS_L expression PARENS_R #exprParentheses
    | operator_unary expression #exprOperatorUnary
    | left=expression operator_binary right=expression #exprOperatorBinary
    | condition=expression QUESTION onTrue=expression COLON onFalse=expression #exprTernary
    | NEW WORD initializer #exprCallCtor
    | reference_chain #exprCall
;

statement
    : IF PARENS_L expression PARENS_R onTrue=code_block (ELSE onFalse=code_block)? #stmtIfElse
    | FOR PARENS_L WORD IN expression PARENS_R code_block #stmtForIn
    | FOR PARENS_L init=statement? SEMICOLON condition=expression? SEMICOLON accumulate=statement? PARENS_R code_block #stmtForI
    | WHILE PARENS_L expression PARENS_R code_block #stmtWhile
    | DO code_block WHILE PARENS_L expression PARENS_R #stmtDoWhile
    | reference_chain operator_binary EQUALS expression SEMICOLON #stmtMutate
    | reference_chain SEMICOLON #stmtCall
    | reference_chain EQUALS expression SEMICOLON #stmtSet
    | reference_chain operator_binary EQUALS expression SEMICOLON #stmtModify
;

arg_list: PARENS_L expression (COMMA expression)* PARENS_R;
param_list: PARENS_L WORD (COMMA WORD)* PARENS_R;
function: FUNCTION WORD param_list ACCOLADE_L code_block ACCOLADE_R;

code_block
    : ACCOLADE_L statement* ACCOLADE_R #blockLarge
    | statement #blockSmall
    | SEMICOLON #blockEmpty
;

declaration
    : code_block #declStatement
    | function #declFunction
;

source_body: declaration*;
