ADDI  28 30 0
ADDI  29 28 0
ADDI  29 28 -40
ADDI  1 30 -8
LDX  1 1 0
ADDI  1 1 3
ADDI  2 30 -4
STX  1 2 0
LDX  1 2 0
ADDI  1 1 6
ADDI  2 30 -12
STX  1 2 0
RET  0
PSH  28 29 -4
ADDI  28 29 0
ADDI  29 29 -40
ADDI  9 28 -4
POP  10 9 4
ADDI  1 0 3
ADDI  3 0 9
CMPI  2 1 3
BLT  2 12
ADD  4 1 3
ADD  5 0 4
CMPI  2 5 4
BGE  2 4
SUB  3 1 5
ADD  5 0 3
BEQ  0 -4
SUBI  4 5 3
ADD  1 0 4
ADD  3 0 5
BEQ  0 -12
ADD  2 1 3
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
ADDI  9 28 -4
POP  1 9 4
ADDI  3 0 3
ADDI  4 0 9
CMPI  5 3 3
BLT  5 12
ADD  5 3 4
ADD  2 0 5
CMPI  4 2 4
BGE  4 4
SUB  1 3 2
ADD  2 0 1
BEQ  0 -4
SUBI  1 2 3
ADD  3 0 1
ADD  4 0 2
BEQ  0 -12
ADD  2 3 4
ADDI  1 30 -12
ADDI  9 0 4
STX  9 1 0
ADD  27 0 2
ADDI  29 28 4
POP  28 29 4
RET  31
ADDI  29 28 4
POP  28 29 4
RET  31
