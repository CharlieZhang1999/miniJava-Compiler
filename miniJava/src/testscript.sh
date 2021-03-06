echo `javac miniJava/Compiler.java`

for item in `ls ../tests/pa1_tests | grep ^pass`
do
    echo "parsing $item"
    echo `java miniJava.Compiler ../tests/pa1_tests/$item > ../tests/pa1_results/$item-out.txt`
done