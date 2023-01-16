package Exp;

import HAL.Rand;


import java.util.ArrayList;


class Neural {
   ArrayList<float[][]> pesos= new ArrayList<>();
   ArrayList<float[]> umbrales= new ArrayList<>();
   ArraysManagement AC=new ArraysManagement();
   int[] ports;
   int nlayers;
   int generation;
   int index;
   int parentIndex;
   int population;
   int census;
   int bornedAt;
   Neural(int[] ports,int index){
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
   public void Print(){
       for (int layer = 0; layer < nlayers; layer++) {
           System.out.println("Pesos de la capa "+(layer+1)+":");
           AC.Print(pesos.get(layer));
           System.out.println("Umbrales de la capa "+(layer+1)+":");
           AC.Print(umbrales.get(layer));
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
    public float TestDiameter(float[] originalInput,int minInput,int maxInput,int inputIndex){
        float[] originalOutput=this.Compute(originalInput,1);
        float[] newOutput;
        int newIndex;
        int index= AC.getIndexOfLargest(originalOutput);
        if(originalOutput[index]<=0){
            index=-1;
        }
        int a=0,b=0;
        boolean cond=true;
        while(originalInput[inputIndex]-a>minInput && cond){
            originalInput[inputIndex]=originalInput[inputIndex]-a-1;
            newOutput=this.Compute(originalInput,1);
            newIndex=AC.getIndexOfLargest(newOutput);
            cond=false;
            if((index <0 && newOutput[newIndex]<0) || newIndex==index){
                cond=true;
                a++;
            }
        }
        while(originalInput[inputIndex]+b<maxInput && cond){
            originalInput[inputIndex]=originalInput[inputIndex]+b+1;
            newOutput=this.Compute(originalInput,1);
            newIndex=AC.getIndexOfLargest(newOutput);
            cond=false;
            if((index <0 && newOutput[newIndex]<0) || newIndex==index){
                cond=true;
                b++;
            }
        }
        return ((float) b+a+1);
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
               if (ports[layer] >= 0) {
                   System.arraycopy(this.pesos.get(layer)[row], 0, newNeural.pesos.get(layer)[row], 0, ports[nlayers]);
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
}
