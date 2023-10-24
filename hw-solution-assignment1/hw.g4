grammar hw;

/* A small hardware description language */

start   :  '.hardware' name=ID
	   ('.inputs' ins+=ID+)?
	   ('.outputs' outs+=ID+)?
	   '.latches' ls+=latchdecl*
	   '.update' up+=updatedecl+
	   '.simulate' simin+=simInp+
	   EOF ;

latchdecl : in=ID '->' out=ID  ;

updatedecl : write=ID '=' e=expr ;

simInp : in=ID '=' str=BITSTRING ;

expr	: '!' e=expr   	       # Negation
	| e1=expr '&&' e2=expr # Conjunction
	| e1=expr '||' e2=expr # Disjunction
	| x=ID 		       # Signal
	| '(' e=expr ')'       # Parenthesis
	;

ID    : ALPHA (ALPHA|NUM)* ;
BITSTRING : [01]+ ;

fragment
ALPHA : [a-zA-Z_ÆØÅæøå] ;

fragment
NUM   : [0-9] ;

WHITESPACE : [ \n\t\r]+ -> skip;
COMMENT    : '//'~[\n]*  -> skip;
COMMENT2   : '/*' (~[*] | '*'~[/]  )*   '*/'  -> skip;
