ADDI  28 30 0
ADDI  29 28 0
ADDI  29 28 -40
PSH  1 29 -4
PSH  2 29 -4
PSH  3 29 -4
PSH  4 29 -4
PSH  5 29 -4
PSH  6 29 -4
PSH  7 29 -4
PSH  8 29 -4
PSH  31 29 -4
JSR  568
POP  31 29 4
POP  8 29 4
POP  7 29 4
POP  6 29 4
POP  5 29 4
POP  4 29 4
POP  3 29 4
POP  2 29 4
POP  1 29 4
ADDI  1 27 0
PSH  1 29 -4
PSH  2 29 -4
PSH  3 29 -4
PSH  4 29 -4
PSH  5 29 -4
PSH  6 29 -4
PSH  7 29 -4
PSH  8 29 -4
PSH  31 29 -4
ADDI  9 0 1
PSH  9 29 -4
JSR  620
ADDI  29 29 4
POP  31 29 4
POP  8 29 4
POP  7 29 4
POP  6 29 4
POP  5 29 4
POP  4 29 4
POP  3 29 4
POP  2 29 4
POP  1 29 4
ADDI  1 27 0
ADDI  2 30 -8
ADDI  9 0 8
STX  9 2 0
ADDI  3 30 -4
ADDI  9 0 1790
STX  9 3 0
PSH  1 29 -4
PSH  2 29 -4
PSH  3 29 -4
PSH  4 29 -4
PSH  5 29 -4
PSH  6 29 -4
PSH  7 29 -4
PSH  8 29 -4
PSH  31 29 -4
LDX  1 3 0
PSH  1 29 -4
LDX  1 2 0
PSH  1 29 -4
JSR  532
ADDI  29 29 8
POP  31 29 4
POP  8 29 4
POP  7 29 4
POP  6 29 4
POP  5 29 4
POP  4 29 4
POP  3 29 4
POP  2 29 4
POP  1 29 4
ADDI  1 27 0
PSH  1 29 -4
PSH  2 29 -4
PSH  3 29 -4
PSH  4 29 -4
PSH  5 29 -4
PSH  6 29 -4
PSH  7 29 -4
PSH  8 29 -4
PSH  31 29 -4
ADDI  9 0 4
PSH  9 29 -4
ADDI  9 0 2
PSH  9 29 -4
JSR  424
ADDI  29 29 8
POP  31 29 4
POP  8 29 4
POP  7 29 4
POP  6 29 4
POP  5 29 4
POP  4 29 4
POP  3 29 4
POP  2 29 4
POP  1 29 4
ADDI  2 27 0
LDX  1 3 0
ADD  1 1 2
STX  1 3 0
RET  0
PSH  28 29 -4
ADDI  28 29 0
ADDI  29 29 -40
ADDI  9 28 4
POP  1 9 4
POP  1 9 4
ADDI  1 30 -4
LDX  1 1 0
ADDI  9 0 0
CMP  1 9 1
BGE  1 7
ADDI  3 30 -8
LDX  2 3 0
LDX  1 3 0
MUL  1 2 1
STX  1 3 0
BEQ  0 -10
ADDI  1 30 -8
LDX  1 1 0
ADDI  1 1 4
ADD  27 0 1
ADDI  29 28 0
POP  28 29 4
RET  31
ADDI  29 28 0
POP  28 29 4
RET  31
PSH  28 29 -4
ADDI  28 29 0
ADDI  29 29 -40
ADDI  9 28 4
POP  1 9 4
POP  1 9 4
ADDI  29 28 0
POP  28 29 4
RET  31
PSH  28 29 -4
ADDI  28 29 0
ADDI  29 29 -40
ADDI  9 28 4
ADDI  1 30 -8
ADDI  9 0 1
STX  9 1 0
ADDI  1 30 -4
ADDI  9 0 2
STX  9 1 0
ADDI  29 28 0
POP  28 29 4
RET  31
PSH  28 29 -4
ADDI  28 29 0
ADDI  29 29 -40
ADDI  9 28 4
POP  1 9 4
ADDI  1 30 -4
ADDI  9 0 1
STX  9 1 0
ADDI  29 28 0
POP  28 29 4
RET  31
