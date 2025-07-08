parser grammar ToyCParser;

options {
    tokenVocab = ToyCLexer;
}

program
    : compUnit
    ;

compUnit
    : funcDef+ EOF
    ;

funcDef
    : funcType funcName L_PAREN funcFParams? R_PAREN block
    ;

funcName
    : IDENT
    ;

funcType
    : VOID
    | INT
    ;

funcFParams
    : funcFParam (COMMA funcFParam)*
    ;

funcFParam
    : INT IDENT
    ;

block
    : L_BRACE stmt* R_BRACE
    ;

stmt
    : block
    | exp? SEMICOLON
    | lVal ASSIGN exp SEMICOLON
    | varDef SEMICOLON
    | IF L_PAREN exp R_PAREN stmt (ELSE stmt)?
    | WHILE L_PAREN exp R_PAREN stmt
    | BREAK SEMICOLON
    | CONTINUE SEMICOLON
    | RETURN exp? SEMICOLON
    ;

varDef
    : INT IDENT ASSIGN exp
    ;

exp
    : L_PAREN exp R_PAREN
    | lVal
    | number
    | funcName L_PAREN funcRParams? R_PAREN
    | unaryOp exp
    | exp (MUL | DIV | MOD) exp
    | exp (PLUS | MINUS) exp
    | exp (LT | GT | LE | GE) exp
    | exp (EQ | NEQ) exp
    | exp AND exp
    | exp OR exp
    ;

lVal
    : IDENT
    ;

number
    : INTEGER_CONST
    | MINUS INTEGER_CONST
    ;

unaryOp
    : PLUS
    | MINUS
    | NOT
    ;

funcRParams
    : funcRParam ( COMMA funcRParam )*
    ;

funcRParam
    : exp
    ;