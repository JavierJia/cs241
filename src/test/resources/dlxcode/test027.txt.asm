ADDI  28 30 0
ADDI  29 28 0
ADDI  29 28 -76
MULI  3 0 2
SUBI  4 3 6
MULI  4 4 4
ADDI  2 28 -64
ADD  4 4 2
LDX  1 4 0
SUBI  4 3 6
MULI  4 4 4
ADD  4 4 2
LDX  4 4 0
SUBI  4 3 6
MULI  4 4 4
ADD  4 4 2
LDX  4 4 0
CMPI  4 4 4
BLT  4 7
MULI  4 1 4
ADDI  5 28 -52
ADD  4 4 5
ADDI  9 0 3
STX  9 4 0
BEQ  0 -11
SUBI  4 3 6
MULI  4 4 4
ADD  4 4 2
LDX  4 4 0
BEQ  0 -20
RET  0
