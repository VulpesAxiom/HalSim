package Exp;

import HAL.GridsAndAgents.AgentGrid2D;
import HAL.GridsAndAgents.AgentSQ2D;
import HAL.Gui.GridWindow;
import HAL.Rand;
import HAL.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.FileNotFoundException;


import java.lang.System;



public class EvolutionModel extends AgentGrid2D<Celula> {
    String filepath;
    long seed;
    boolean communicate,retrieve;
    int maxSamples;
    float [] limits;
    Rand rng;
    int maxInnerEnergy;
    int maximumFood;
    ArrayList<Neural> networks = new ArrayList<>();

    ArrayList<String> Dictionary = new ArrayList<>();

    ArraysManagement AC = new ArraysManagement();
    int maxIndex,signaled_moved;
    int[][] food;
    float[] movement,activity;
    int[][] neighborhood;
    int[][] signal,signal_buffer;

    boolean special_comms;

    public int FindIndex(int index) {
        for (int i = 0; i < networks.size(); i++) {
            if (networks.get(i).index == index) {
                return i;
            }
        }
        System.out.println(index);
        return -1;
    }

    public EvolutionModel(int x, int y, int initial, int maxInnerEnergy, int iteration, long seed,boolean communicates,boolean retrieve,boolean special_comms) throws FileNotFoundException {
        super(x, y, Celula.class, true, true);
        this.seed=seed;
        movement=new float[6];
        activity=new float[15];
        signaled_moved=0;
        this.special_comms=special_comms;
        if(communicates) {
            limits = new float[]{10, 20, 20, 20, 20, 20, 20, 5, 5, 5, 5, 5, 1, 1, 1, 1, 1};

        }else {
            limits = new float[]{10, 20, 20, 20, 20, 20, 20, 5, 5, 5, 5, 5};
        }
        this.communicate=communicates;
        if (retrieve) {
            maxSamples =0;
        }else{
            maxSamples=5000;
        }
        filepath = "C:\\Users\\bast_\\OneDrive\\Escritorio\\HalSim\\OutPutFile\\"+iteration+"/";
        this.retrieve=retrieve;
        if(retrieve){
            File myObj = new File(filepath+"Description.txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                int place=data.indexOf(",");
                this.seed=Integer.parseInt(data.substring(8,place));
            }
        }
        rng=new Rand(this.seed);
        food = new int[xDim][yDim];
        neighborhood = new int[xDim][yDim];
        signal = new int[xDim][yDim];
        signal_buffer= new int[xDim][yDim];
        AC.ConstantThis(food, initial);
        this.maxInnerEnergy = maxInnerEnergy;
    }
    static public void StoreLine(String filename, String lineToAppend) {
        if (!new File(filename).exists()) {
            File file = new File(filename);
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileWriter myWriter = new FileWriter(filename, true);
            myWriter.write(lineToAppend + "\n");
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public void Load(String filename) {
        try {
            File myObj = new File(filepath + filename);
            Scanner myReader = new Scanner(myObj);
            int i = 0, x, y;
            networks.clear();
            while (myReader.hasNextLine()) {
                x = i / yDim;
                y = i % yDim;
                String data = myReader.nextLine();
                String[] subData = data.split(";");
                food[x][y] = Integer.parseInt(subData[0]);
                String[] subData2 = subData[1].split(",");
                if (subData.length > 3) {
                    String[] subData3 = subData[2].split(",");
                    int[] ports = new int[subData3.length];
                    for (int j = 0; j < subData3.length; j++) {
                        ports[j] = Integer.parseInt(subData3[j]);
                    }
                    int index = Integer.parseInt(subData[3]);
                    networks.add(new Neural(ports, index));
                    networks.get(i).population = Integer.parseInt(subData[4]);
                    for (int j = 0; j < subData.length - 5; j++) {
                        String weights = subData[5 + j].split(":")[0];
                        String[] weight = weights.split("'");
                        for (int k = 0; k < weight.length; k++) {
                            String[] rows = weight[k].split(",");
                            for (int l = 0; l < rows.length; l++) {
                                networks.get(i).pesos.get(j)[k][l] = Float.parseFloat(rows[l]);
                            }
                        }
                        String biases = subData[5 + j].split(":")[1];
                        String[] bias = biases.split(",");
                        for (int k = 0; k < bias.length; k++) {
                            networks.get(i).umbrales.get(j)[k] = Float.parseFloat(bias[k]);
                        }
                    }
                }

                if (GetAgent(i) != null) {
                    GetAgent(i).Dispose();
                }
                if (subData2.length > 1) {
                    NewAgentSQ(i).Init(Integer.parseInt(subData2[0]), Float.parseFloat(subData2[3]), Float.parseFloat(subData2[2]));

                    GetAgent(i).energy = Integer.parseInt(subData2[1]);
                }
                i++;
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public void Save(String filename, int suffix) {
        File tempFile = new File(filepath + filename + suffix + ".txt");
        boolean exists = tempFile.exists();
        if (!exists) {

            try {
                tempFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Writer output;
        int x, y;
        try {
            output = new BufferedWriter(new FileWriter(filepath + filename + suffix + ".txt", true));
            for (int i = 0; i < xDim * yDim; i++) {
                x = i / yDim;
                y = i % yDim;
                String cell;
                StringBuilder neuron = new StringBuilder();
                if (GetAgent(i) != null) {
                    cell = GetAgent(i).index + "," + GetAgent(i).energy + "," + GetAgent(i).strength + "," + GetAgent(i).mutability;
                } else {
                    cell = "0";
                }
                if (i < networks.size()) {
                    for (int j = 0; j < networks.get(i).ports.length; j++) {
                        if (j != 0) {
                            neuron.append(",");
                        }
                        neuron.append(networks.get(i).ports[j]);
                    }
                    neuron.append(";").append(networks.get(i).index).append(";").append(networks.get(i).population).append(";");
                    for (int layer = 0; layer < networks.get(i).nlayers; layer++) {
                        for (int row = 0; row < networks.get(i).umbrales.get(layer).length; row++) {
                            for (int col = 0; col < networks.get(i).pesos.get(layer)[0].length; col++) {
                                neuron.append(networks.get(i).pesos.get(layer)[row][col]);
                                if (col < networks.get(i).pesos.get(layer)[0].length - 1) {
                                    neuron.append(",");
                                } else if (row < networks.get(i).umbrales.get(layer).length - 1) {
                                    neuron.append("'");
                                } else {
                                    neuron.append(":");
                                }
                            }

                        }
                        for (int row = 0; row < networks.get(i).umbrales.get(layer).length; row++) {
                            neuron.append(networks.get(i).umbrales.get(layer)[row]);
                            if (row < networks.get(i).umbrales.get(layer).length - 1) {
                                neuron.append(",");
                            }
                        }
                        neuron.append(";");
                    }
                } else {
                    neuron = new StringBuilder("0");
                }
                output.append(String.valueOf(food[x][y])).append(";").append(cell).append(";").append(neuron.toString()).append("\n");
            }
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Setup(int maxIndex,int initFood,float Strength,int[] architecture,float mutability) {
        for (int i = 1; i <= maxIndex; i++) {
            NewAgentSQ( rng.Int(xDim), rng.Int(yDim)).Init(i, mutability, Strength,initFood);
            networks.add(new Neural(architecture,i));
            networks.get(i-1).Randomize(rng.Long(2000));
            this.maxIndex =maxIndex;
        }
    }


    public void Feed(int amount, int frequency){

        for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < yDim; y++) {
                int up=Math.floorMod(y+1, yDim);
                int[][] food2=food;
                int down=Math.floorMod(y-1 ,yDim);
                int left= Math.floorMod(x-1 ,xDim);
                int right= Math.floorMod(x+1 ,xDim);
                int sum=food2[x][up]+food2[x][down]+food2[left][y]+food2[right][y]+food2[x][y];
                if(rng.Double()<((float)frequency)/(xDim*yDim)*Math.exp(-Math.pow(((float)sum/(maximumFood)-2.5f),2))) {
                    food[x][y] = Math.min((food[x][y] + amount), maximumFood);
                }
                if(special_comms){
                    signal_buffer[x][y]=signal[x][y];
                }
                signal[x][y]=0;
            }
        }
    }
    public void Draw(GridWindow win,int max) {
        int x,y;
        double c;
        for (int i = 0; i < xDim * yDim; i++) {
            x = i / yDim;
            y = i % yDim;
            c = (float) food[x][y]/max ;

            if (GetAgent(i) != null) {
                float colorcito=((float) FindIndex(GetAgent(i).index)) / networks.size();

                win.SetPix(i, Util.RGB(colorcito, colorcito, 1));
            } else {
                if(signal[x][y]+signal_buffer[x][y]>0) {
                    win.SetPix(i, Util.RGB(1, 0, 0));
                }else{
                    win.SetPix(i, Util.RGB(c, c, 0));
                }
            }
        }
    }


    public void CheckPop(){
        for (int i = 0; i < xDim; i++) {
            for (int j = 0; j < yDim; j++) {
                neighborhood[i][j]=this.PopAt(i,j);
            }
        }
    }
    public boolean CheckPop2(){
        for (int i = 0; i < xDim; i++) {
            for (int j = 0; j < yDim; j++) {
                if(this.PopAt(i,j)>0){
                    return true;
                }
            }
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        Rand rng=new Rand(745861L);
        int iteration =61,last_iteration=90;
        float MStrength=10;
        float mutability=0.16f;
        int initialNumber=250;
        boolean Suffocate=true;
        float subFreq=.05f;
        int Duration=10000;
        boolean to_draw=false,communicates=true,retrieve=false,signal_resistance=true,repeat_seed=false;


        while (iteration <= last_iteration ) {
            signal_resistance=!signal_resistance;
            int time = 0;
            int Pop;
            int xDim=200 , yDim=200;
            int BeginnerBoost=5;
            int[] architecture;
            int density = 1;

            int frequency = (int) ((subFreq * xDim * yDim) / density);

            int scale = 600 / Math.max(xDim, yDim);
            GridWindow win = new GridWindow( "",xDim, yDim, scale,to_draw,null,true);
            if(!to_draw){
                win.Close();
            }
            int initial = 10;
            int maxima = 10;
            long seed;
            if(repeat_seed) {
                seed = 1615135L;
            }else {
                seed = rng.Long(1000);
            }
            EvolutionModel model = new EvolutionModel(xDim, yDim, initial, maxima,iteration,seed,communicates,retrieve,signal_resistance);
            File directory = new File(model.filepath);
            if (! directory.exists()){
                boolean done=false;
                while(!done){
                    done=directory.mkdir();
                }
            }
            if(!retrieve) {
                StoreLine(model.filepath + "Description.txt", "Semilla:" + model.seed + ", xDim:" + xDim + ", yDim:" + yDim);
            }
            model.maximumFood = 10;
            model.Dictionary.add("Random");
            model.Dictionary.add("Energy");
            model.Dictionary.add("UnderFood");
            model.Dictionary.add("NorthFood");
            model.Dictionary.add("SouthFood");
            model.Dictionary.add("WestFood");
            model.Dictionary.add("EastFood");
            model.Dictionary.add("UnderNeigh");
            model.Dictionary.add("NorthNeigh");
            model.Dictionary.add("SouthNeigh");
            model.Dictionary.add("WestNeigh");
            model.Dictionary.add("EastNeigh");
            if(communicates) {
                model.Dictionary.add("UnderSignal");
                model.Dictionary.add("NorthSignal");
                model.Dictionary.add("SouthSignal");
                model.Dictionary.add("WestSignal");
                model.Dictionary.add("EastSignal");
                architecture=new int[]{model.Dictionary.size(),25,8};
            }else {
                architecture = new int[]{model.Dictionary.size(), 25, 7};
            }
            model.Setup(initialNumber,BeginnerBoost,MStrength,architecture,mutability);
            model.CheckPop();
            long starting = System.currentTimeMillis();
            long minute;
            long hour;
            long second;


            while (model.CheckPop2() && (time < Duration)) {

                second = ((System.currentTimeMillis() - starting) / 1000) % 60;
                minute = ((System.currentTimeMillis() - starting) / 60000) % 60;
                hour = ((System.currentTimeMillis() - starting) / 3600000);


                model.ShuffleAgents(model.rng);
                Pop=model.Pop();
                for (Celula cell : model) {
                    cell.Step(time,Suffocate);
                }
                if(!retrieve) {
                    for (int iterante = model.networks.size() - 1; iterante >= 0; iterante--) {
                        Neural net = model.networks.get(iterante);
                        if (net.Extinct && net.Samples < model.maxSamples) {
                            for (int contador = 0; contador < 10; contador++) {
                                net.Sample(model.limits, model.communicate);
                                if (net.Samples >= model.maxSamples) {
                                    if (!retrieve) {
                                        net.StoreSample(model.limits, model.filepath);
                                    }
                                    model.networks.remove(model.FindIndex(net.index));
                                    break;
                                }
                            }
                        } else if (net.Extinct) {


                            net.StoreSample(model.limits, model.filepath);

                            model.networks.remove(model.FindIndex(net.index));
                        }
                    }
                }
                model.Feed(density,frequency);
                if(to_draw) {
                    model.Draw(win, model.maximumFood);
                    win.TickPause(10);
                }




                System.out.println(hour + ":" + minute + ":" + second + "  ;" + time+ "  ;" +Pop);
                StoreLine(model.filepath+"Population.txt",time+";"+Pop);
                model.movement[0] /=Pop;
                model.movement[1] /=Pop;
                model.movement[2] /= model.signaled_moved;
                model.movement[3] /=model.signaled_moved;
                model.movement[4] /=(Pop-model.signaled_moved);
                model.movement[5] /=(Pop-model.signaled_moved);
                StoreLine(model.filepath+"Movement.txt",time+";"+model.AC.ToString(model.movement));
                for(int i=0;i<5;i++){
                    model.activity[i]/=Pop;
                    model.activity[i+5]/=model.signaled_moved;
                    model.activity[i+10]/=(Pop-model.signaled_moved);
                }
                StoreLine(model.filepath+"Activity.txt",time+";"+model.AC.ToString(model.activity));
                model.signaled_moved=0;
                model.movement=new float[6];
                model.activity=new float[15];
                ++time;
            }
            for (Celula cell: model ) {
                cell.Die(time);
            }

            if (!retrieve) {
                for (int iterante = model.networks.size() - 1; iterante >= 0; iterante--) {
                    Neural net = model.networks.get(iterante);
                    while (net.Samples < model.maxSamples) {
                        net.Sample(model.limits, model.communicate);
                    }

                    net.StoreSample(model.limits, model.filepath);

                    model.networks.remove(model.FindIndex(net.index));

                }
            }
            iteration++;
        }
        System.exit(100);
    }

}


class Celula extends AgentSQ2D<EvolutionModel> {
    int index;
    int energy;
    boolean any_signal;
    float mutability, strength;
    int order;
    public void Init(int index, float mutability, float strength) {
        energy = 1;
        order=-1;
        this.index = index;
        this.strength = strength;
        this.mutability = mutability;
    }
    public void Init(int index, float mutability, float strength,int food) {
        energy = food;
        order=-1;
        this.index = index;
        this.strength = strength;
        this.mutability = mutability;
    }

    public void Step(int time,boolean suffocates) {

        assert G != null;
        int bound = 10;
        float rng = (float) G.rng.Int(bound);


        int up=Math.floorMod(G.ItoY(this.Isq())-1,G.yDim);
        int down=Math.floorMod((G.ItoY(this.Isq())+1),G.yDim);
        int thisx=G.ItoX(this.Isq());
        int thisy=G.ItoY(this.Isq());
        int left=(Math.floorMod((G.ItoX(this.Isq())-1),G.xDim));
        int right=(Math.floorMod((G.ItoX(this.Isq())+1),G.xDim));
        int upneigh= G.neighborhood[thisx][up];
        int thidneigh= G.neighborhood[thisx][thisy];
        int downneigh= G.neighborhood[thisx][down];
        int leftneigh= G.neighborhood[left][thisy];
        int rightneigh= G.neighborhood[right][thisy];

        int upfood=G.food[thisx][up];
        int leftfood=G.food[left][thisy];
        int rightfood=G.food[right][thisy];
        int downfood=G.food[thisx][down];
        int thisfood=G.food[thisx][thisy];
        float[] input;
        this.any_signal=false;
        if(G.communicate) {
            int upsignal,thidsignal,downsignal,leftsignal,rightsignal;
            if(G.special_comms){
                upsignal = Math.max(G.signal[thisx][up],G.signal_buffer[thisx][up]);
                thidsignal = Math.max(G.signal[thisx][thisy],G.signal_buffer[thisx][thisy]);
                downsignal = Math.max(G.signal[thisx][down],G.signal_buffer[thisx][down]);
                leftsignal = Math.max(G.signal[left][thisy],G.signal_buffer[left][thisy]);
                rightsignal = Math.max(G.signal[right][thisy],G.signal_buffer[right][thisy]);
            }else{
                upsignal = G.signal[thisx][up];
                thidsignal = G.signal[thisx][thisy];
                downsignal = G.signal[thisx][down];
                leftsignal = G.signal[left][thisy];
                rightsignal = G.signal[right][thisy];
            }
            if(upsignal!=0 || downsignal!=0 || leftsignal!=0 || rightsignal!=0 || thidsignal!=0){
                this.any_signal=true;
                G.signaled_moved++;
            }
            input = new float[]{rng, this.energy,thisfood,upfood,downfood,rightfood,leftfood,thidneigh,upneigh,downneigh,rightneigh,leftneigh,thidsignal,upsignal,downsignal,rightsignal,leftsignal};

        }else {
            input = new float[]{rng, this.energy, thisfood, upfood, downfood, rightfood, leftfood, thidneigh, upneigh, downneigh, rightneigh, leftneigh};
        }

        float[] output = G.networks.get(G.FindIndex(index)).Compute(input, 1);
        if(G.networks.get(G.FindIndex(index)).Samples < G.maxSamples && !G.retrieve) {
            G.networks.get(G.FindIndex(index)).Sample(G.limits,G.communicate);
        }
        int responce = G.AC.getIndexOfLargest(output);
        if (output[responce] < 0) {
            responce = 3;
        }
        if(G.communicate) {
            if (output[7] > 0.5) {
                output[7] = 1;
            } else {
                output[7] = 0;
            }
            G.signal[thisx][thisy] = (int) output[7];
            G.signal[left][thisy] = (int) output[7];
            G.signal[right][thisy] = (int) output[7];
            G.signal[thisx][up] = (int) output[7];
            G.signal[thisx][down] = (int) output[7];
        }

        boolean dies=responce==1;
        if(dies){
            G.activity[3]++;
            if(any_signal){
                G.activity[8]++;
            }else{
                G.activity[13]++;
            }
        }
        this.energy--;
        if (this.energy < 0) {
            if(!dies) {
                G.activity[4]++;
                if (any_signal) {
                    G.activity[9]++;
                } else {
                    G.activity[14]++;
                }
            }
            dies = true;
        } else {
            switch (responce) {
                case 0 -> {
                    if (this.energy >= 2) {
                        if (Reproduce(time)) {
                            this.energy -= 2;
                            G.activity[2]++;
                            if(any_signal){
                                G.activity[7]++;
                            }else{
                                G.activity[12]++;
                            }
                        }
                    }
                }
                case 2 -> {
                    if (this.energy >= 1) {
                        if (MoveRandom()) {
                            this.energy--;
                            G.activity[1]++;
                            if(any_signal){
                                G.activity[6]++;
                            }else{
                                G.activity[11]++;
                            }
                        }
                    }
                }
                case 3 -> {
                    if (this.energy >= 1) {
                        if (MoveUp()) {
                            this.energy--;
                            G.activity[0]++;
                            if(any_signal){
                                G.activity[5]++;
                            }else{
                                G.activity[10]++;
                            }
                        }
                    }
                }
                case 4 -> {
                    if (this.energy >= 1) {
                        if (MoveDown()) {
                            this.energy--;
                            G.activity[0]++;
                            if(any_signal){
                                G.activity[5]++;
                            }else{
                                G.activity[10]++;
                            }
                        }
                    }
                }
                case 5 -> {
                    if (this.energy >= 1) {
                        if (MoveRight()) {
                            this.energy--;
                            G.activity[0]++;
                            if(any_signal){
                                G.activity[5]++;
                            }else{
                                G.activity[10]++;
                            }
                        }
                    }
                }
                case 6 -> {
                    if (this.energy >= 1) {
                        if (MoveLeft()) {
                            this.energy--;
                            G.activity[0]++;
                            if(any_signal){
                                G.activity[5]++;
                            }else{
                                G.activity[10]++;
                            }
                        }
                    }
                }
            }
        }


        if (this.energy < 0) {
            this.energy = 0;
        }
        if ((suffocates && thidneigh>5) || dies) {
            if(suffocates && thidneigh>5){
                G.activity[4]++;
                if(any_signal){
                    G.activity[9]++;
                }else{
                    G.activity[14]++;
                }
            }
            Die(time);
        }
        if (this.energy < G.maxInnerEnergy) {
            int remnant = G.maxInnerEnergy - energy;
            if (remnant <= G.food[G.ItoX(Isq())][G.ItoY(Isq())]) {
                G.food[G.ItoX(Isq())][G.ItoY(Isq())] -= remnant;
                this.energy += remnant;
            } else {
                this.energy += G.food[G.ItoX(Isq())][G.ItoY(Isq())];
                G.food[G.ItoX(Isq())][G.ItoY(Isq())] = 0;
            }
        }
    }
    public void Die(int time) {
        assert G != null;
        G.food[G.ItoX(Isq())][G.ItoY(Isq())] += Math.max(0, energy);
        G.neighborhood[G.ItoX(Isq())][G.ItoY(Isq())]--;

        Dispose();
        G.networks.get(G.FindIndex(index)).population--;
        if (G.networks.get(G.FindIndex(index)).population == 0) {
            G.networks.get(G.FindIndex(index)).Extinct = true;
            G.networks.get(G.FindIndex(index)).died=time;
        }
    }

    public boolean MoveRandom() {
        assert G != null;
        int option =G.rng.Int(4);
        switch (option) {
            case 0 -> {
                return MoveUp();
            }
            case 1 -> {
                return MoveDown();
            }
            case 2 -> {
                return MoveLeft();
            }
            case 3 -> {
                return MoveRight();
            }
            default -> {
                return false;
            }
        }
    }
    public boolean MoveUp() {
        assert G != null;
        G.neighborhood[G.ItoX(this.Isq())][Math.floorMod(G.ItoY(this.Isq())-1,G.yDim)]++;
        G.neighborhood[G.ItoX(this.Isq())][G.ItoY(this.Isq())]--;
        MoveSQ(G.ItoX(this.Isq()), Math.floorMod(G.ItoY(this.Isq())-1,G.yDim));
        G.movement[1]--;
        if(any_signal){
            G.movement[3]--;
        }else{
            G.movement[5]--;
        }
        return true;
    }
    public boolean MoveDown() {
        assert G != null;
        G.neighborhood[G.ItoX(this.Isq())][Math.floorMod(G.ItoY(this.Isq())+1,G.yDim)]++;
        G.neighborhood[G.ItoX(this.Isq())][G.ItoY(this.Isq())]--;
        MoveSQ(G.ItoX(this.Isq()), Math.floorMod(G.ItoY(this.Isq())+1,G.yDim));
        G.movement[1]++;
        if(any_signal){
            G.movement[3]++;
        }else{
            G.movement[5]++;
        }
        return true;
    }
    public boolean MoveLeft() {
        assert G != null;
        G.neighborhood[Math.floorMod(G.ItoX(this.Isq())-1,G.yDim)][G.ItoY(this.Isq())]++;
        G.neighborhood[G.ItoX(this.Isq())][G.ItoY(this.Isq())]--;
        MoveSQ( Math.floorMod(G.ItoX(this.Isq())-1,G.yDim),G.ItoY(this.Isq()));
        G.movement[0]--;
        if(any_signal){
            G.movement[2]--;
        }else{
            G.movement[4]--;
        }
        return true;
    }
    public boolean MoveRight() {
        assert G != null;
        G.neighborhood[ Math.floorMod(G.ItoX(this.Isq())+1,G.yDim)][G.ItoY(this.Isq())]++;
        G.neighborhood[G.ItoX(this.Isq())][G.ItoY(this.Isq())]--;
        MoveSQ( Math.floorMod(G.ItoX(this.Isq())+1,G.yDim),G.ItoY(this.Isq()));
        G.movement[0]++;
        if(any_signal){
            G.movement[2]++;
        }else{
            G.movement[4]++;
        }
        return true;
    }

    public boolean Reproduce(int time) {
        assert G != null;
        int newindex = this.index;
        float newMutability = this.mutability, newStrength = this.strength;

        if (mutability > G.rng.Double()) {
            if (.20f > G.rng.Double()) {
                if (G.rng.Bool()) {
                    newStrength += G.rng.Gaussian(0, 5);
                    newStrength = Math.abs(newStrength);
                } else {
                    newMutability += G.rng.Gaussian(0, 0.1);
                    float exponential = (float) Math.exp(-4.92 * (newMutability - 0.5f));
                    newMutability = 1 / (1 + exponential);
                }
                G.networks.get(G.FindIndex(newindex)).population++;
                G.networks.get(G.FindIndex(newindex)).census++;
            } else {
                newindex = ++G.maxIndex;
                G.networks.add(G.networks.get(G.FindIndex(this.index)).Mutate(newindex, strength, this.index, G.seed, time));
            }
        } else {
            G.networks.get(G.FindIndex(this.index)).population++;
            G.networks.get(G.FindIndex(this.index)).census++;
        }
        int place = this.Isq();
        G.NewAgentSQ(place).Init(newindex, newMutability, newStrength);
        G.neighborhood[G.ItoX(this.Isq())][G.ItoY(this.Isq())]++;

        return true;
    }
}