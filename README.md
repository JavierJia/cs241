This is the project for cs241 : Advanced Compiler

course reference: http://www.michaelfranz.com/w14cs241.html

The vcg tools:
http://pp.info.uni-karlsruhe.de/git/yComp/tree/samples/fib.vcg


# Build it
`mvn clean package`

# Usage
```
-optimize-level N : recursively run something
                     , 0: Nothing, 1: Copy propagation, 2: Common expression
                     elimination, 3: Const branch elimination; each bigger one
                     is include the previous level, default all
 -regNum N         : set register num, default 8
 -run              : if run the dlxcode

```
Example:
```
mvn clean package

cd target
cp ../src/test/resources/testprogs/factorial.txt .
java -jar cs241-0.0.1-SNAPSHOT-jar-with-dependencies.jar -run factorial.txt
```

It will compile the program and run after successfully compiled.

The byproduct while compiling is below:
```
factorial.txt.ini.vcg       # The initial vcg graph
factorial.txt.opt.vcg       # The optimized vcg graph
factorial.txt.final.vcg     # The removed phi vcg graph
factorial.txt.reg.vcg       # The regesiter allocation interference graph
factorial.txt.dlx           # The final dlx code
```

# Demo process
## Step1: CFG
Show some basic graph, order
1. test009.txt # Nested if
2. test010.txt # While statement
const expression calculated:
3. test005.txt
const branch eliminated:
4. test029.txt
most compliated:
5. test024.txt

## Step2: Optimization
copy propagation
1. test026.txt
2. test018.txt | test017.txt
common expression elimination
3. test025.txt
const branch remove
4. test023.txt
5. test022.txt
6. test008.txt // dead loop
empty code 
6.  test019.txt

## Step3: Register allocator
big.svg

## Step4: run the program
test001
test002
test003
test008 //dead loop
factorial   
