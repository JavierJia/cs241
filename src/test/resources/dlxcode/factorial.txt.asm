ADDI  28 30 0
ADDI  29 28 0
ADDI  29 28 -44
RDI  2
PSH  1 29 -4
PSH  2 29 -4
PSH  3 29 -4
PSH  4 29 -4
PSH  5 29 -4
PSH  6 29 -4
PSH  7 29 -4
PSH  8 29 -4
PSH  2 29 -4
JSR  188
ADDI  29 29 4
POP  8 29 4
POP  7 29 4
POP  6 29 4
POP  5 29 4
POP  4 29 4
POP  3 29 4
POP  2 29 4
POP  1 29 4
ADDI  1 27 0
WRD  1
PSH  1 29 -4
PSH  2 29 -4
PSH  3 29 -4
PSH  4 29 -4
PSH  5 29 -4
PSH  6 29 -4
PSH  7 29 -4
PSH  8 29 -4
PSH  2 29 -4
JSR  272
ADDI  29 29 4
POP  8 29 4
POP  7 29 4
POP  6 29 4
POP  5 29 4
POP  4 29 4
POP  3 29 4
POP  2 29 4
POP  1 29 4
ADDI  1 27 0
WRD  1
RET  0
PSH  28 29 -4
ADDI  28 29 0
ADDI  29 29 -40
ADDI  9 28 4
POP  1 9 4
ADDI  2 0 1
ADDI  1 0 1
CMP  5 1 6
BGT  5 6
MUL  4 2 1
ADDI  3 1 1
ADD  2 0 4
ADD  1 0 3
BEQ  0 -6
ADD  27 0 2
ADDI  29 28 4
POP  28 29 4
RET  31
ADDI  29 28 4
POP  28 29 4
RET  31
PSH  28 29 -4
ADDI  28 29 0
ADDI  29 29 -40
ADDI  9 28 4
POP  10 9 4
CMPI  1 2 1
BGT  1 7
ADDI  9 0 1
ADD  27 0 9
ADDI  29 28 4
POP  28 29 4
RET  31
BEQ  0 1
SUBI  1 2 1
PSH  1 29 -4
PSH  2 29 -4
PSH  3 29 -4
PSH  4 29 -4
PSH  5 29 -4
PSH  6 29 -4
PSH  7 29 -4
PSH  8 29 -4
PSH  1 29 -4
JSR  272
ADDI  29 29 4
POP  8 29 4
POP  7 29 4
POP  6 29 4
POP  5 29 4
POP  4 29 4
POP  3 29 4
POP  2 29 4
POP  1 29 4
ADDI  1 27 0
MUL  1 1 2
ADD  27 0 1
ADDI  29 28 4
POP  28 29 4
RET  31
ADDI  29 28 4
POP  28 29 4
RET  31
