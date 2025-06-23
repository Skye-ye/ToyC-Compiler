lexer grammar ToyCLexer;

INT: 'int';

VOID: 'void';

IF: 'if';

ELSE: 'else';

WHILE: 'while';

BREAK: 'break';

CONTINUE: 'continue';

RETURN: 'return';

PLUS: '+';

MINUS: '-';

MUL: '*';

DIV: '/';

MOD: '%';

ASSIGN: '=';

EQ: '==';

NEQ: '!=';

LT: '<';

GT: '>';

LE: '<=';

GE: '>=';

NOT: '!';

AND: '&&';

OR: '||';

L_PAREN: '(';

R_PAREN: ')';

L_BRACE: '{';

R_BRACE: '}';

COMMA: ',';

SEMICOLON: ';';

IDENT: [_a-z] [_a-z0-9]* ;

INTEGER_CONST: '-'? [1-9] [0-9]* | '0';

WS: [ \t\r\n] + -> skip;

LINE_COMMENT: '//'.*? '\n' -> skip ;

MULTILINE_COMMENT: '/*'.*? '*/' -> skip ;