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
JSR  96
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
RET  0
PSH  28 29 -4
ADDI  28 29 0
ADDI  29 29 -84
ADDI  9 28 4
ADD  9 0 0
STW  9 28 -84
LDW  9 28 -84
LDW  10 28 -84
MUL  9 9 10
STW  9 28 -80
LDW  9 28 -84
ADD  9 0 9
STW  9 28 -68
LDW  9 28 -80
LDW  10 28 -84
ADD  9 9 10
STW  9 28 -76
LDW  9 28 -76
LDW  10 28 -84
CMP  1 9 10
BGE  1 11
LDW  9 28 -80
ADDI  9 9 1
STW  9 28 -44
LDW  10 28 -68
ADD  9 0 10
LDW  10 28 -84
ADD  9 0 10
LDW  10 28 -44
ADD  9 0 10
BEQ  0 18
LDW  9 28 -84
LDW  10 28 -80
SUB  9 9 10
STW  9 28 -72
LDW  9 28 -72
LDW  10 28 -76
CMP  1 9 10
BLE  1 3
ADDI  9 0 2
BEQ  0 3
LDW  10 28 -84
ADD  9 0 10
LDW  10 28 -72
ADD  9 0 10
LDW  10 28 -48
ADD  9 0 10
ADD  9 0 0
LDW  9 28 -80
LDW  10 28 -76
MUL  9 9 10
STW  9 28 -64
LDW  9 28 -76
WRD  9
LDW  9 28 -64
WRD  9
LDW  9 28 -52
WRD  9
LDW  9 28 -84
WRD  9
LDW  9 28 -56
WRD  9
LDW  9 28 -80
WRD  9
LDW  9 28 -60
WRD  9
ADDI  29 28 0
POP  28 29 4
RET  31
