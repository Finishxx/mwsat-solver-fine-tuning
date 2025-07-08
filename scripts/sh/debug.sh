#! /bin/sh

NAME=debug.dat
SCALA=../scripts/scala
BEGIN=$PWD
OUT=debugOut

echo file $1 startTemp $2 endTemp $3 cooling $4 equilibrium $5 optimum $6 clauses $7

echo output into $OUT/$NAME

./main --file $1 --seed "0xFF" -t $2 -T $3 -c $4 -e $5 -d $OUT/$NAME

cp $OUT/$NAME $SCALA
cd $SCALA

scala-cli . -- visualize -f $NAME -o $6 -c $7 -s $BEGIN/$OUT/satisfiability.png -w $BEGIN/$OUT/weight.png
