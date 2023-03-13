package Exp;

import HAL.Rand;


import java.util.ArrayList;

import static Exp.EvolutionModel.StoreLine;


class Neural {
    int died;
   ArrayList<float[][]> pesos= new ArrayList<>();
   ArrayList<float[]> umbrales= new ArrayList<>();
   Rand rng ;
   int Samples = 0;
   boolean Extinct = false;
   ArraysManagement AC=new ArraysManagement();
   int[] ports;
   float[] acum_error;
   int nlayers;
   int generation;
   int index;
   int parentIndex;
   int population;
   int census;
   int bornedAt;
   Neural(int[] ports,int index){
       rng = new Rand();
       died= 0;
       acum_error = new float[ports[0]];
       this.index=index;
       this.population=1;
       this.bornedAt=0;
       this.ports=ports;
       this.parentIndex=0;
       this.generation=0;
       this.census=1;
       if(this.ports.length<2){
           System.out.print("no hay suficiente información para poder construir la red");
           System.exit(-1);
       }else{
           nlayers=this.ports.length-1;
           for (int layer = 0; layer < nlayers ; layer++) {
               pesos.add(new float[ports[layer+1]][ports[layer]]);
               umbrales.add(new float[ports[layer+1]]);
           }
       }

   }


    public void Randomize(long seed){
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
        if(layer==nlayers){
            return AC.Sigmoid(AC.Subtract(AC.Multiply(pesos.get(layer-1),input),umbrales.get(layer-1)));
        }else {
            return Compute(AC.Sigmoid(AC.Subtract(AC.Multiply(pesos.get(layer-1),input),umbrales.get(layer-1))),layer+1);
        }
   }
   public Neural Mutate(int disponible,float strenght,int parent,long seed,int bornAt){

       Rand rng=new Rand(seed);
       Neural newNeural=new Neural(this.ports,disponible);
       newNeural.parentIndex=parent;
       newNeural.bornedAt=bornAt;
       newNeural.generation = this.generation + 1;
       int geneLength=0;
       for (int i = 0; i < ports.length-1; i++) {
           geneLength+=(ports[i]+1)*ports[i+1];
       }
       int mutatedGene=rng.Int(geneLength)+1;
       for (int layer = 0; layer < nlayers; layer++) {
           for (int row = 0; row < ports[layer+1]; row++) {
               for(int col=0;col<ports[layer];col++){
                   newNeural.pesos.get(layer)[row][col]=this.pesos.get(layer)[row][col];
               }
               newNeural.umbrales.get(layer)[row]=this.umbrales.get(layer)[row];
           }
       }
       geneLength=0;
       for (int i = 0; i < nlayers; i++) {
           if(geneLength+(ports[i]+1)*ports[i+1]>=mutatedGene){
               mutatedGene-=geneLength;
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
       for (int i = 0; i < nlayers; i++) {
           AC.Print(this.pesos.get(i));
           AC.Print(this.umbrales.get(i));
       }
    }
    public void StoreSample(float[] limits,String filepath){
        StringBuilder AcumError = new StringBuilder();
        for(int i=0;i<limits.length;i++){
            AcumError.append(";").append(this.acum_error[i]);
        }
        StoreLine(filepath  + "Error.txt",this.index+";"+this.parentIndex+";"+this.bornedAt+";"+this.died+AcumError);
    }
   public void Sample(float[] limits,boolean communicate,boolean extended,int number_of_layers) {
       this.Samples++;
       float rng = (float) this.rng.Double(limits[0]);

       int length_of_input=12;
       if(communicate){
           length_of_input+=5*number_of_layers;
       }
       if(extended){
           length_of_input+=8;
       }
       float[] input= new float[length_of_input];
       input[0]=rng;
       for (int i = 1; i < length_of_input; i++) {
           input[i]=(float) this.rng.Double(limits[i]);
       }

       float[] input2;
       float[] output1 = this.Compute(input, 1);
       float[] output2;
       float[] error;
       for (int i = 0; i < input.length; i++) {
           error = new float[input.length];
           input2 = input;
           input2[i] += limits[i]*0.01f;
           output2 = this.Compute(input2, 1);
           for (int j = 0; j < output1.length; j++) {
               error[i] += Math.pow(output2[j] - output1[j], 2);
           }
           acum_error[i] += (float) Math.sqrt(error[i]);
       }

   }
}
