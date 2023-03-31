package Exp;

import HAL.Rand;


import java.util.ArrayList;

import static Exp.EvolutionModel.StoreLine;


class Neural {
    /*
    Esta Clase define las redes neuronales, una red neuronal es un conjunto de capas
    Cada capa está compuesta por una matriz (vector 2D) y un vector (1D)
     pesos es un arreglo que contiene todas las matrices que forman la red neuronal
     umbrales contiene todos los vectores que forman la red neuronal
     nLayer es la cantidad de capas de la red neuronal
     Samples es la cantidad de muestras que se han tomado para esta red neuronal
     generation, index, parenIndex, population,bornAt y died son información sobre la historia de la red neuronal
     Extinct es una bandera que señala si no existen más celulas con la red neuronal en cuestion
     ports es la lista del numero de neuronas en cada capa, por nesecidad debe haber al menos 2 numeros, neuronas de entrada y de salida
     AC es para manejar la aritmetica de matrices y otras cosas
     rng es un generador aleatorio
     accum error es para acumular el error del muestreo de monte carlo
     */
    int died,Samples = 0,nlayers, generation,index,parentIndex,population, bornAt;
    ArrayList<float[][]> pesos= new ArrayList<>();
    ArrayList<float[]> umbrales= new ArrayList<>();
    Rand rng ;
    boolean Extinct = false;
    ArraysManagement AC=new ArraysManagement();
    int[] ports;
    float[] acum_error;

   Neural(int[] ports,int index) {
       rng = new Rand();
       died = 0;
       acum_error = new float[ports[0]];
       this.index = index;
       this.population = 1;
       this.bornAt = 0;
       this.ports = ports;
       this.parentIndex = 0;
       this.generation = 0;
       // Toda la meta data se asigna inicialmente
       if (this.ports.length < 2) {
           //Se garantiza que haya al menos una entrada y una salida
           System.out.print("no hay suficiente información para poder construir la red");
           System.exit(-1);
       } else {
           nlayers = this.ports.length - 1;
           for (int layer = 0; layer < nlayers; layer++) {
               // se inicializan los pesos y umbrales
               pesos.add(new float[ports[layer + 1]][ports[layer]]);
               umbrales.add(new float[ports[layer + 1]]);
           }
       }
   }


    public void Randomize(long seed){
       //Esta funcion asigna valoes aleatorios a las entradas de las redes neuronales
       Rand rng=new Rand(seed);
        for (int layer = 0; layer < nlayers; layer++) {
            for (int row = 0; row < ports[layer + 1]; row++) {
                for (int col = 0; col < ports[layer]; col++) {
                    this.pesos.get(layer)[row][col] = (float) rng.Double( 200)-100;
                }
                this.umbrales.get(layer)[row] = (float) rng.Double( 200)-100;
            }
        }
    }

   public float[] Compute(float[] input,int layer){
       /* un algoritmo recurrente que capa por capa computa y=s(M*x-b) donde s es el sigmoide vecotial M es la matriz de pesos
       x es un input y b son los umbrales
       y será el input de la siguiente capa
       */
        if(layer==nlayers){
            return AC.Sigmoid(AC.Subtract(AC.Multiply(pesos.get(layer-1),input),umbrales.get(layer-1)));
        }else {
            return Compute(AC.Sigmoid(AC.Subtract(AC.Multiply(pesos.get(layer-1),input),umbrales.get(layer-1))),layer+1);
        }
   }
   public Neural Mutate(int disponible,float strenght,int parent,long seed,int bornAt){
            // Este subrutina elige, busca y cambia una entrada de la red neuronal
       Rand rng=new Rand(seed);
       Neural newNeural=new Neural(this.ports,disponible);
       newNeural.parentIndex=parent;
       newNeural.bornAt =bornAt;
       newNeural.generation = this.generation + 1;
       // Se inicializa una nueva red neuronal vacia
       int geneLength=0;
       for (int i = 0; i < ports.length-1; i++) {
           geneLength+=(ports[i]+1)*ports[i+1];
       }
       //Se establece la cantidad de entradas que posee la red neuronal.
       int mutatedGene=rng.Int(geneLength)+1;
       //se escoge uno de las entradas para modificarla
       for (int layer = 0; layer < nlayers; layer++) {
           for (int row = 0; row < ports[layer+1]; row++) {
               for(int col=0;col<ports[layer];col++){
                   newNeural.pesos.get(layer)[row][col]=this.pesos.get(layer)[row][col];
               }
               newNeural.umbrales.get(layer)[row]=this.umbrales.get(layer)[row];
           }
       }
       // Se copian todos los valores de la red del padre a la red del hijo.
       geneLength=0;
       for (int i = 0; i < nlayers; i++) {
           //Este ciclo recorre una a una las entradas de la red hasta encontrar la esogida
           if(geneLength+(ports[i]+1)*ports[i+1]>=mutatedGene){
               mutatedGene-=geneLength;
               //Se modifica el valor, ya sea de un peso o de un umbral
               if(mutatedGene%(ports[i]+1)==0){
                   newNeural.umbrales.get(i)[mutatedGene/(ports[i]+1)-1]+=rng.Gaussian(0,strenght);
               }else{
                   newNeural.pesos.get(i)[mutatedGene/(ports[i]+1)][mutatedGene%(ports[i]+1)-1]+=rng.Gaussian(0,strenght);
               }
               break;
           }
           geneLength+=(ports[i]+1)*ports[i+1];
       }
   return newNeural;
   }
   public void Print(){
       //Esta función imprime en la consola los valores de la matriz
       for (int i = 0; i < nlayers; i++) {
           AC.Print(this.pesos.get(i));
           AC.Print(this.umbrales.get(i));
       }
   }
   public void StoreSample(float[] limits,String filepath) {
       //Una vez todas las muestras han sido tomadas se almacenan en un archivo de texto
       //El archivo se llama error
       //Cada linea corresponde a un genoma
       //Cada linea tiene data separada por ";"
       //en orden la data es: indice, indice del padre, iteracion de nacimiento, iteracion de muerte y luego la lista de error acumulado
       //La lista de error acumulado esta en el mismo orden que el dicionario de estimulos
       StringBuilder AcumError = new StringBuilder();
       for (int i = 0; i < limits.length; i++) {
           AcumError.append(";").append(this.acum_error[i]);
       }
       StoreLine(filepath + "Error.txt", this.index + ";" + this.parentIndex + ";" + this.bornAt + ";" + this.died + AcumError, false);
    }
   public void Sample(float[] limits,boolean communicate,boolean extended,int number_of_layers,boolean memory) {
       //en esta subrutina se realizan las muestras
       //Para ello primero se calcula la cantidad de inputs que necesita simular un estimulo
       this.Samples++;
       float rng = (float) this.rng.Double(limits[0]);

       int length_of_input = 12;
       if (communicate) {
           length_of_input += 5 * number_of_layers;
       }
       if (extended) {
           length_of_input += 8;
       }
       if (memory) {
           length_of_input++;
       }
       float[] input = new float[length_of_input];
       //Tras haber determinado el largo de los vectores, se llena el vector input basado en los limites de los inputs
       input[0] = rng;
       for (int i = 1; i < length_of_input; i++) {
           input[i] = (float) this.rng.Double(limits[i]);
       }

       float[] input2;
       float[] output1 = this.Compute(input, 1);
       float[] output2;
       float[] error;
       //Por cada input se añade una perturbación y calcula la diferencia o error asociado
       for (int i = 0; i < input.length; i++) {
           error = new float[input.length];
           input2 = input;
           input2[i] += limits[i] * 0.01f;
           output2 = this.Compute(input2, 1);
           for (int j = 0; j < output1.length; j++) {
               error[i] += Math.pow(output2[j] - output1[j], 2);
           }
           acum_error[i] += (float) Math.sqrt(error[i]);
       }

   }
}
