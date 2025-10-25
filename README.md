# Pràctica 1: Búsqueda Local - Distribució de Gasolina

Implementació d'algoritmes de cerca local (Hill Climbing i Simulated Annealing) per resoldre el problema d'optimització de distribució de combustible a gasolineres, minimitzant costos de transport i maximitzant beneficis.

## Compilació i Execució

**Compilar:**
javac -cp .:AIMA.jar:Gasolina.jar Main.java

**Executar:**
java -cp .:AIMA.jar:Gasolina.jar Main <init> <alg> <exec> <iter> [seed]

### Paràmetres:
- `init`: Solució inicial → `0` Random | `1` Greedy 1 | `2` Greedy 2 | `3` Buida
- `alg`: Algoritme → `0` Hill Climbing | `1` Simulated Annealing
- `exec`: Nombre d'execucions
- `iter`: Límit d'iteracions (`-1` = ilimitat)
- `seed`: (Opcional) Llavor aleatòria

### Exemples:
# Hill Climbing amb solució buida, 1 execució, sense límit
java -cp .:AIMA.jar:Gasolina.jar Main 3 0 1 -1

# Simulated Annealing amb greedy 2, 10 execucions, seed 42
java -cp .:AIMA.jar:Gasolina.jar Main 2 1 10 -1 42