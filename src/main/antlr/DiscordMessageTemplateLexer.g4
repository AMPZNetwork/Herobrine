lexer grammar DiscordMessageTemplateLexer;

DOT: '.';
COMMA: ',';
COLON: ':';
SEMICOLON: ';';
DOUBLEQUOTE: '"';
AMPERSAND: '&';
BREAK: '|';
EXCLAMATION: '!';
QUESTION: '?';
TILDE: '~';
EQUALS: '=';
DOLLAR: '$';

PARENS_L: '(';
PARENS_R: ')';
INDEX_L: '[';
INDEX_R: ']';
ACCOLADE_L: '{';
ACCOLADE_R: '}';

PLUS: '+';
MINUS: '-';
ASTERISK: '*';
SLASH: '/';
PERCENT: '%';

TRUE: 'true' | 'yes' | 'on';
FALSE: 'false' | 'no' | 'off';
NEW: 'new';
IF: 'if';
ELSE: 'else';
FOR: 'for';
IN: 'in';
DO: 'do';
WHILE: 'while';
TRY: 'try';
CATCH: 'catch';
FINALLY: 'finally';
RETURN: 'return';
FUNCTION: 'function';

LITERAL_STRING: DOUBLEQUOTE ~[\r\n]* DOUBLEQUOTE;
LITERAL_NUMBER: [0-9]+ (DOT [0-9]+)?;
LITERAL_BOOLEAN: TRUE | FALSE;

WORD: [a-zA-Z0-9_]+;

SING_COMMENT: '//' ~[\r\n]* -> channel(HIDDEN);
WS: [ \n\r\t] -> channel(HIDDEN);

UNMATCHED: . ; //Should make an error
