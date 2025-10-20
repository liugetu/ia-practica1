#!/bin/bash
#echo "10 ejecuciones Random, HC" "
java -cp .:AIMA.jar:Gasolina.jar Main 0 0 10 >> output.txt
#echo "Segunda ejecucion Greedy1, HC"
java -cp .:AIMA.jar:Gasolina.jar Main 1 0 10 >> output.txt
#echo "Tercera ejecucion Greedy2, HC"
java -cp .:AIMA.jar:Gasolina.jar Main 2 0 10 >> output.txt
#echo "Cuarta ejecucion Vacio, HC"
java -cp .:AIMA.jar:Gasolina.jar Main 3 0 10 >> output.txt
#echo "10 ejecuciones Random, SA" "
java -cp .:AIMA.jar:Gasolina.jar Main 0 1 10 >> output.txt
#echo "Segunda ejecucion Greedy1, SA"
java -cp .:AIMA.jar:Gasolina.jar Main 1 1 10 >> output.txt
#echo "Tercera ejecucion Greedy2, SA"
java -cp .:AIMA.jar:Gasolina.jar Main 2 1 10 >> output.txt
#echo "Cuarta ejecucion Vacio, SA"
java -cp .:AIMA.jar:Gasolina.jar Main 3 1 10 >> output.txt