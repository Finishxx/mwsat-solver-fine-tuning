scala-cli . -- run -E -T 0.5 -c 0.98 -d ../../input/white/wruf36-157/ -e 200 -o output -r 2 -s ../../bin/main --seed "0xFF" -t 20

scala-cli . -- grid-search -c 0.95 -C 0.99 -P 0.01 -t 20 -T 200 -p 10 -e 200 -f ../../input/white/wruf36-157/wruf36-157-M/wruf36-157-1.mwcnf -O 23667 -s ../../bin/main -r 5 -R "0xFF" -X success.dat -x step.dat -S 0.5

scala-cli . -- grid-search -c 0.99 -C 0.999 -P 0.001 -t 0.1 -T 15 -p 0.25 -e 200 -f ../../input/whiteBox/wuf20-91-N/wuf20-01.mwcnf -O 34684 -s ../../bin/main -r 5 -R "0xFF" -X success.dat -x step.dat -S 0.001

