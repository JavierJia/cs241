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
JSR  88
POP  8 29 4
POP  7 29 4
POP  6 29 4
POP  5 29 4
POP  4 29 4
POP  3 29 4
POP  2 29 4
POP  1 29 4
ADDI  1 27 0
RET  0
PSH  28 29 -4
ADDI  28 29 0
ADDI  29 29 -40
ADDI  9 28 -4
ADDI  2 0 2
CMPI  3 2 9
BGE  3 4
ADDI  1 2 1
ADD  2 0 1
BEQ  0 -4
WRD  2
ADDI  9 0 2
WRD  9
ADDI  9 0 9
WRD  9
ADDI  9 0 4
WRD  9
ADDI  9 0 6
WRD  9
ADDI  9 0 6
WRD  9
ADDI  9 0 8
WRD  9
ADDI  29 28 4
POP  28 29 4
RET  31
