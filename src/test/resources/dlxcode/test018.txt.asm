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
ADDI  29 29 -124
ADDI  9 28 4
ADD  9 0 0
STW  9 28 -21
LDW  9 28 -21
LDW  10 28 -21
MUL  9 9 10
STW  9 28 -20
LDW  9 28 -21
ADD  9 0 9
STW  9 28 -17
LDW  9 28 -20
LDW  10 28 -21
ADD  9 9 10
STW  9 28 -19
LDW  9 28 -19
LDW  10 28 -21
CMP  1 9 10
BGE  1 11
LDW  9 28 -20
ADDI  9 9 1
STW  9 28 -11
LDW  10 28 -17
ADD  9 0 10
LDW  10 28 -21
ADD  9 0 10
LDW  10 28 -11
ADD  9 0 10
BEQ  0 18
LDW  9 28 -21
LDW  10 28 -20
SUB  9 9 10
STW  9 28 -18
LDW  9 28 -18
LDW  10 28 -19
CMP  1 9 10
BLE  1 3
ADDI  9 0 2
BEQ  0 3
LDW  10 28 -21
ADD  9 0 10
LDW  10 28 -18
ADD  9 0 10
LDW  10 28 -12
ADD  9 0 10
ADD  9 0 0
LDW  9 28 -20
LDW  10 28 -19
MUL  9 9 10
STW  9 28 -16
LDW  9 28 -19
WRD  9
LDW  9 28 -16
WRD  9
LDW  9 28 -13
WRD  9
LDW  9 28 -21
WRD  9
LDW  9 28 -14
WRD  9
LDW  9 28 -20
WRD  9
LDW  9 28 -15
WRD  9
ADDI  29 28 4
POP  28 29 4
RET  31
